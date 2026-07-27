package com.songsit.fuellogpro.data

import androidx.room.withTransaction
import com.songsit.fuellogpro.data.local.ExpenseEntity
import com.songsit.fuellogpro.data.local.FuelEntryEntity
import com.songsit.fuellogpro.data.local.FuelLogDatabase
import com.songsit.fuellogpro.data.local.VehicleEntity
import java.io.ByteArrayInputStream
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Ports V8's Fuelio importer (app.js:1612-1945, "Fuelio import (CSV / .fuelio / .zip —
 * multi-vehicle ... )"). A .fuelio backup is a zip archive containing one quoted
 * pseudo-CSV per vehicle. Sections inside each CSV are marked by lines starting with
 * (quote-stripped) "##", e.g. "##Log" for the fuel-log rows and "##Costs" for
 * maintenance/expense rows. This does NOT rename any existing Room entity fields —
 * it is purely a translation layer from Fuelio's column names into the existing
 * VehicleEntity/FuelEntryEntity/ExpenseEntity shapes, so FirestoreSyncRepository.kt and
 * every other consumer of those entities needs no changes.
 *
 * NOT PORTED: Fuelio's `pictures.data` (a second, nested zip holding the actual photo
 * files referenced by the `##Pictures` section) is intentionally skipped. FuelEntryEntity
 * has no photo/attachment column in the current Room schema (see FuelEntryEntity.kt) and
 * ExpenseEntity likewise has none, so persisting imported photos would require a schema
 * migration touching entities that other in-flight work (Firestore sync) also depends on.
 * That risk was judged not worth taking for this change; photo import can be added later
 * once a photo storage column/table exists.
 */
class FuelioImportRepository(
    private val database: FuelLogDatabase,
) {
    suspend fun importFuelioZip(bytes: ByteArray): BackupImportResult {
        val vehicles = mutableListOf<VehicleEntity>()
        val fuelEntries = mutableListOf<FuelEntryEntity>()
        val expenses = mutableListOf<ExpenseEntity>()

        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory && name.lowercase().endsWith(".csv")) {
                    val text = zip.readBytes().toString(Charsets.UTF_8)
                    val vehicleId = UUID.randomUUID().toString()
                    val parsed = parseFuelioCsv(text)
                    if (parsed.fuelRows.isNotEmpty() || parsed.costRows.isNotEmpty()) {
                        val vehicleName = parsed.vehicleName
                            ?: name.substringAfterLast('/').substringBeforeLast('.').ifBlank { "รถนำเข้า" }
                        vehicles += VehicleEntity(
                            id = vehicleId,
                            name = vehicleName,
                            registration = parsed.vehicleRegistration ?: "",
                            fuelType = parsed.vehicleFuelType ?: "",
                            createdAt = System.currentTimeMillis(),
                        )
                        parsed.fuelRows.forEach { row ->
                            fuelEntries += FuelEntryEntity(
                                id = UUID.randomUUID().toString(),
                                vehicleId = vehicleId,
                                date = row.date,
                                time = row.time,
                                odometerKm = row.odometerKm,
                                liters = row.liters,
                                pricePerLiter = row.pricePerLiter,
                                amount = row.amount,
                                fullTank = row.fullTank,
                                station = row.station,
                                createdAt = System.currentTimeMillis(),
                            )
                        }
                        parsed.costRows.forEach { row ->
                            expenses += ExpenseEntity(
                                id = UUID.randomUUID().toString(),
                                vehicleId = vehicleId,
                                date = row.date,
                                category = row.category,
                                description = row.title,
                                amount = row.amount,
                                odometerKm = row.odometerKm,
                                income = row.income,
                                recurring = false,
                                reminderDate = null,
                                createdAt = System.currentTimeMillis(),
                            )
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        database.withTransaction {
            database.vehicleDao().upsertAll(vehicles)
            database.fuelEntryDao().upsertAll(fuelEntries)
            database.expenseDao().upsertAll(expenses)
        }
        return BackupImportResult(
            vehicles = vehicles.size,
            fuelEntries = fuelEntries.size,
            expenses = expenses.size,
            maintenanceTasks = 0,
            trips = 0,
        )
    }

    // Single, un-zipped .csv (one vehicle) — Fuelio also supports exporting just this.
    suspend fun importFuelioCsv(text: String): BackupImportResult {
        val vehicleId = UUID.randomUUID().toString()
        val parsed = parseFuelioCsv(text)
        val vehicle = VehicleEntity(
            id = vehicleId,
            name = parsed.vehicleName ?: "รถนำเข้า",
            registration = parsed.vehicleRegistration ?: "",
            fuelType = parsed.vehicleFuelType ?: "",
            createdAt = System.currentTimeMillis(),
        )
        val fuelEntries = parsed.fuelRows.map { row ->
            FuelEntryEntity(
                id = UUID.randomUUID().toString(),
                vehicleId = vehicleId,
                date = row.date,
                time = row.time,
                odometerKm = row.odometerKm,
                liters = row.liters,
                pricePerLiter = row.pricePerLiter,
                amount = row.amount,
                fullTank = row.fullTank,
                station = row.station,
                createdAt = System.currentTimeMillis(),
            )
        }
        val expenses = parsed.costRows.map { row ->
            ExpenseEntity(
                id = UUID.randomUUID().toString(),
                vehicleId = vehicleId,
                date = row.date,
                category = row.category,
                description = row.title,
                amount = row.amount,
                odometerKm = row.odometerKm,
                income = row.income,
                recurring = false,
                reminderDate = null,
                createdAt = System.currentTimeMillis(),
            )
        }
        database.withTransaction {
            database.vehicleDao().upsert(vehicle)
            database.fuelEntryDao().upsertAll(fuelEntries)
            database.expenseDao().upsertAll(expenses)
        }
        return BackupImportResult(1, fuelEntries.size, expenses.size, 0, 0)
    }
}

private data class FuelioFuelRow(
    val date: String,
    val time: String,
    val odometerKm: Double,
    val liters: Double,
    val pricePerLiter: Double,
    val amount: Double,
    val fullTank: Boolean,
    val station: String,
)

private data class FuelioCostRow(
    val date: String,
    val title: String,
    val category: String,
    val odometerKm: Double?,
    val amount: Double,
    val income: Boolean,
)

private data class FuelioParseResult(
    val vehicleName: String?,
    val vehicleRegistration: String?,
    val vehicleFuelType: String?,
    val fuelRows: List<FuelioFuelRow>,
    val costRows: List<FuelioCostRow>,
)

private fun stripQuotes(field: String) = field.trim().removeSurrounding("\"").trim()

private fun detectDelimiter(line: String): Char {
    val semicolons = line.count { it == ';' }
    val commas = line.count { it == ',' }
    return if (semicolons >= commas) ';' else ','
}

private fun splitLine(line: String, delimiter: Char): List<String> =
    line.split(delimiter).map(::stripQuotes)

private fun parseFuelioDate(raw: String): String {
    val value = raw.trim()
    if (value.isEmpty()) return ""
    // Fuelio dates are typically yyyy-MM-dd already, or dd/MM/yyyy.
    Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(value)?.let { return it.value }
    Regex("""^(\d{1,2})/(\d{1,2})/(\d{4})""").find(value)?.let { match ->
        val (day, month, year) = match.destructured
        return "$year-${month.padStart(2, '0')}-${day.padStart(2, '0')}"
    }
    return value
}

private fun parseFuelioCsv(text: String): FuelioParseResult {
    val rawLines = text.split('\n').map { it.trimEnd('\r') }
    var vehicleName: String? = null
    var vehicleRegistration: String? = null
    var vehicleFuelType: String? = null
    val fuelRows = mutableListOf<FuelioFuelRow>()
    val costRows = mutableListOf<FuelioCostRow>()
    val costCategories = mutableMapOf<String, String>()

    var section = ""
    var headerColumns: List<String> = emptyList()
    var idxDate = -1
    var idxOdo = -1
    var idxFuel = -1
    var idxFull = -1
    var idxPrice = -1
    var idxStation = -1
    var idxMissed = -1
    // ##Costs columns
    var idxCostDate = -1
    var idxCostOdo = -1
    var idxCostCategory = -1
    var idxCostTitle = -1
    var idxCostPrice = -1
    var idxCostIncome = -1

    for (line in rawLines) {
        if (line.isBlank()) continue
        val trimmed = stripQuotes(line.substringBefore(detectDelimiter(line)))
        if (trimmed.startsWith("##")) {
            section = trimmed.removePrefix("##").trim().lowercase()
            headerColumns = emptyList()
            continue
        }
        // Vehicle metadata is often on lines before the "##Log" header, e.g. "Car;MyCar",
        // "Plate;1กก1234", "Fuel;Gasoline". Ported alongside the vehicle name lookup so
        // registration/fuel type aren't silently dropped when the source file has them.
        if (section.isEmpty() && (vehicleName == null || vehicleRegistration == null || vehicleFuelType == null)) {
            val delimiter = detectDelimiter(line)
            val fields = splitLine(line, delimiter)
            if (fields.size >= 2) {
                when {
                    fields[0].equals("car", ignoreCase = true) ->
                        vehicleName = vehicleName ?: fields[1].takeIf { it.isNotBlank() }
                    fields[0].equals("plate", ignoreCase = true) || fields[0].equals("license", ignoreCase = true) ->
                        vehicleRegistration = vehicleRegistration ?: fields[1].takeIf { it.isNotBlank() }
                    fields[0].equals("fuel", ignoreCase = true) || fields[0].equals("fueltype", ignoreCase = true) ->
                        vehicleFuelType = vehicleFuelType ?: fields[1].takeIf { it.isNotBlank() }
                }
            }
        }
        val delimiter = detectDelimiter(line)
        val fields = splitLine(line, delimiter)
        when (section) {
            "log" -> {
                val lowerFields = fields.map { it.lowercase() }
                val looksLikeHeader = lowerFields.any { it.contains("odo") } && lowerFields.any { it.contains("fuel") || it.contains("liter") || it.contains("volume") }
                if (headerColumns.isEmpty() && looksLikeHeader) {
                    headerColumns = lowerFields
                    idxDate = headerColumns.indexOfFirst { it.contains("date") }
                    idxOdo = headerColumns.indexOfFirst { it.contains("odo") }
                    idxFuel = headerColumns.indexOfFirst { it.contains("fuel") || it.contains("liter") || it.contains("volume") }
                    idxFull = headerColumns.indexOfFirst { it.contains("full") }
                    idxPrice = headerColumns.indexOfFirst { it.contains("price") }
                    idxStation = headerColumns.indexOfFirst { it.contains("station") || it.contains("place") }
                    idxMissed = headerColumns.indexOfFirst { it.contains("missed") }
                } else if (headerColumns.isNotEmpty()) {
                    if (idxMissed >= 0 && fields.getOrNull(idxMissed)?.let { it == "1" || it.equals("true", true) } == true) continue
                    val date = if (idxDate in fields.indices) parseFuelioDate(fields[idxDate]) else ""
                    val odo = fields.getOrNull(idxOdo)?.toDoubleOrNull()
                    val liters = fields.getOrNull(idxFuel)?.toDoubleOrNull()
                    if (date.isBlank() || odo == null || liters == null || liters <= 0) continue
                    val price = fields.getOrNull(idxPrice)?.toDoubleOrNull() ?: 0.0
                    val station = fields.getOrNull(idxStation)?.takeIf { it.isNotBlank() } ?: ""
                    val fullField = fields.getOrNull(idxFull)?.lowercase()
                    val fullTank = fullField == null || fullField == "1" || fullField == "true" || fullField == "full"
                    fuelRows += FuelioFuelRow(
                        date = date,
                        time = "00:00",
                        odometerKm = odo,
                        liters = liters,
                        pricePerLiter = price,
                        amount = liters * price,
                        fullTank = fullTank,
                        station = station,
                    )
                }
            }
            "costcategories" -> {
                if (fields.size >= 2 && fields[0].toIntOrNull() != null) {
                    costCategories[fields[0]] = fields[1]
                }
            }
            "costs" -> {
                val lowerFields = fields.map { it.lowercase() }
                val looksLikeHeader = lowerFields.any { it.contains("title") } && lowerFields.any { it.contains("price") || it.contains("amount") }
                if (idxCostDate < 0 && looksLikeHeader) {
                    idxCostDate = lowerFields.indexOfFirst { it.contains("date") }
                    idxCostOdo = lowerFields.indexOfFirst { it.contains("odo") }
                    idxCostCategory = lowerFields.indexOfFirst { it.contains("categ") }
                    idxCostTitle = lowerFields.indexOfFirst { it.contains("title") }
                    idxCostPrice = lowerFields.indexOfFirst { it.contains("price") || it.contains("amount") }
                    idxCostIncome = lowerFields.indexOfFirst { it.contains("income") }
                } else if (idxCostDate >= 0) {
                    val date = fields.getOrNull(idxCostDate)?.let(::parseFuelioDate) ?: ""
                    val amount = fields.getOrNull(idxCostPrice)?.toDoubleOrNull() ?: 0.0
                    if (date.isBlank() || amount <= 0) continue
                    val categoryRaw = fields.getOrNull(idxCostCategory) ?: ""
                    val category = costCategories[categoryRaw] ?: categoryRaw.ifBlank { "อื่นๆ" }
                    val title = fields.getOrNull(idxCostTitle) ?: ""
                    val odo = fields.getOrNull(idxCostOdo)?.toDoubleOrNull()
                    val income = fields.getOrNull(idxCostIncome)?.let { it == "1" || it.equals("true", true) } ?: false
                    costRows += FuelioCostRow(date, title, category, odo, amount, income)
                }
            }
        }
    }
    return FuelioParseResult(vehicleName, vehicleRegistration, vehicleFuelType, fuelRows, costRows)
}
