package com.songsit.fuellogpro.data

import android.content.Context
import androidx.room.withTransaction
import com.songsit.fuellogpro.data.local.ExpenseEntity
import com.songsit.fuellogpro.data.local.FuelEntryEntity
import com.songsit.fuellogpro.data.local.FuelLogDatabase
import com.songsit.fuellogpro.data.local.PhotoUris
import com.songsit.fuellogpro.data.local.VehicleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.math.abs

/**
 * Ports V8's Fuelio importer (app.js:1612-1945, "Fuelio import (CSV / .fuelio / .zip —
 * multi-vehicle ... )"). A .fuelio backup is a zip archive containing one quoted
 * pseudo-CSV per vehicle. Sections inside each CSV are marked by lines starting with
 * (quote-stripped) "##", e.g. "##Log" for the fuel-log rows, "##Costs" for
 * maintenance/expense rows, and "##Pictures" linking a row's unique id to photo
 * filenames stored in a nested `pictures.data` zip. This does NOT rename any existing
 * Room entity fields — it is purely a translation layer from Fuelio's column names into
 * the existing VehicleEntity/FuelEntryEntity/ExpenseEntity shapes.
 *
 * Re-importing the same (or an updated) Fuelio backup must not create duplicate rows:
 * a vehicle is matched by name, and within that vehicle a fuel/cost row is matched by
 * date + time + odometer (+ title, for costs) against what's already stored — see
 * [findMatchingFuelEntry]/[findMatchingExpense]. A match updates the existing row in
 * place (keeping its id and, unless it has none yet, its photo) instead of inserting a
 * new one.
 *
 * [importFuelioZip] never buffers the whole backup (or its bundled photos) into memory: it
 * re-opens the source twice via [openStream] and streams both passes through
 * ZipInputStream, so a 160MB+ .fuelio full of receipt/odometer photos imports without an
 * OutOfMemoryError. Pass 1 streams every photo inside the nested `pictures.data` zip
 * straight to app-private storage (one photo's bytes in memory at a time); pass 2 streams
 * each vehicle's small text .csv entry and merges its rows against the local DB.
 */
class FuelioImportRepository(
    private val context: Context,
    private val database: FuelLogDatabase,
) {
    suspend fun importFuelioZip(
        openStream: () -> InputStream,
        totalBytes: Long? = null,
        onProgress: (percent: Int) -> Unit = {},
    ): BackupImportResult = withContext(Dispatchers.IO) {
        val totalWorkBytes = totalBytes?.let { it * 2 } // two full streaming passes over the source
        var bytesReadSoFar = 0L
        var lastReportedPercent = -1
        val reportProgress: (Int) -> Unit = { count ->
            bytesReadSoFar += count
            val total = totalWorkBytes
            if (total != null && total > 0) {
                val percent = ((bytesReadSoFar * 100) / total).toInt().coerceIn(0, 99)
                if (percent != lastReportedPercent) {
                    lastReportedPercent = percent
                    onProgress(percent)
                }
            }
        }

        // Pass 1: extract every photo inside the nested pictures.data zip straight to disk,
        // keyed by lowercased filename, without ever holding more than one photo in memory.
        val imageMap = extractPictures(openStream(), reportProgress)

        // Pass 2: re-open the same source and stream each vehicle's (small, text-only) .csv
        // entry, merging its rows against what's already in the local DB. Fuelio sometimes
        // splits a vehicle's log and its costs across separate .csv entries in the same zip
        // (see the isCostFile detection below), and each entry's own ##Pictures table may only
        // cover photos for *that* file — so every entry is parsed first, then every entry's
        // ##Pictures table is combined into one map before any row is linked to a photo, so a
        // picture table sitting in one file can still resolve a row parsed from another.
        data class ParsedCsvEntry(val entryName: String, val isCostFile: Boolean, val parsed: FuelioParseResult)
        val parsedEntries = mutableListOf<ParsedCsvEntry>()
        CountingInputStream(openStream(), reportProgress).use { counting ->
            ZipInputStream(counting).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory && name.lowercase().endsWith(".csv")) {
                        val isCostFile = name.lowercase().contains("cost") || name.lowercase().contains("expense")
                        val defaultSection = if (isCostFile) "costs" else ""
                        val parsed = parseFuelioCsv(zip.readBytes().toString(Charsets.UTF_8), defaultSection)
                        if (parsed.fuelRows.isNotEmpty() || parsed.costRows.isNotEmpty() || parsed.pictureMap.isNotEmpty()) {
                            parsedEntries += ParsedCsvEntry(name, isCostFile, parsed)
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        // Real multi-vehicle exports repeat the *same* ##Pictures rows verbatim in every
        // vehicle's own .csv (confirmed against a real two-vehicle backup — not just the
        // legitimate different-vehicle-owns-the-picture-row case the doc describes), so combine
        // must dedupe per target id or a photo shared this way ends up attached twice.
        val combinedPictureMap = mutableMapOf<String, MutableList<String>>()
        for (csvEntry in parsedEntries) {
            for ((targetId, filenames) in csvEntry.parsed.pictureMap) {
                val existing = combinedPictureMap.getOrPut(targetId) { mutableListOf() }
                filenames.forEach { if (it !in existing) existing += it }
            }
        }

        val existingVehicles = database.vehicleDao().getAll().toMutableList()
        val vehiclesToUpsert = mutableListOf<VehicleEntity>()
        val fuelEntries = mutableListOf<FuelEntryEntity>()
        val expenses = mutableListOf<ExpenseEntity>()
        var vehicleCount = 0

        for (csvEntry in parsedEntries) {
            val parsed = csvEntry.parsed
            if (parsed.fuelRows.isEmpty() && parsed.costRows.isEmpty()) continue
            var parsedVehicleName = parsed.vehicleName
                ?: csvEntry.entryName.substringAfterLast('/').substringBeforeLast('.').ifBlank { "รถนำเข้า" }
            if (csvEntry.isCostFile && (parsedVehicleName.lowercase().contains("cost") || parsedVehicleName.lowercase().contains("expense"))) {
                parsedVehicleName = existingVehicles.firstOrNull()?.name ?: "รถนำเข้า"
            }
            val matchedVehicle = existingVehicles.find { it.name.trim().equals(parsedVehicleName.trim(), ignoreCase = true) }
            val vehicleId = matchedVehicle?.id ?: UUID.randomUUID().toString()
            if (matchedVehicle == null) {
                val newVehicle = VehicleEntity(
                    id = vehicleId,
                    name = parsedVehicleName,
                    registration = parsed.vehicleRegistration ?: "",
                    fuelType = parsed.vehicleFuelType ?: "",
                    createdAt = System.currentTimeMillis(),
                )
                vehiclesToUpsert += newVehicle
                existingVehicles += newVehicle
            }
            vehicleCount++
            val parsedWithCombinedPictures = parsed.copy(pictureMap = combinedPictureMap)
            fuelEntries += mergeFuelRows(vehicleId, parsedWithCombinedPictures, imageMap)
            expenses += mergeCostRows(vehicleId, parsedWithCombinedPictures, imageMap)
        }

        database.withTransaction {
            database.vehicleDao().upsertAll(vehiclesToUpsert)
            database.fuelEntryDao().upsertAll(fuelEntries)
            database.expenseDao().upsertAll(expenses)
        }
        onProgress(100)
        BackupImportResult(
            vehicles = vehicleCount,
            fuelEntries = fuelEntries.size,
            expenses = expenses.size,
            maintenanceTasks = 0,
            trips = 0,
            photos = imageMap.size,
        )
    }

    // Single, un-zipped .csv (one vehicle) — Fuelio also supports exporting just this.
    // There is no nested pictures.data to draw photos from here.
    suspend fun importFuelioCsv(text: String): BackupImportResult = withContext(Dispatchers.IO) {
        val parsed = parseFuelioCsv(text)
        val vehicleName = parsed.vehicleName ?: "รถนำเข้า"
        val existingVehicles = database.vehicleDao().getAll()
        val matchedVehicle = existingVehicles.find { it.name.trim().equals(vehicleName.trim(), ignoreCase = true) }
        val vehicleId = matchedVehicle?.id ?: UUID.randomUUID().toString()
        val vehicle = matchedVehicle ?: VehicleEntity(
            id = vehicleId,
            name = vehicleName,
            registration = parsed.vehicleRegistration ?: "",
            fuelType = parsed.vehicleFuelType ?: "",
            createdAt = System.currentTimeMillis(),
        )

        val fuelEntries = mergeFuelRows(vehicleId, parsed, emptyMap())
        val expenses = mergeCostRows(vehicleId, parsed, emptyMap())

        database.withTransaction {
            database.vehicleDao().upsert(vehicle)
            database.fuelEntryDao().upsertAll(fuelEntries)
            database.expenseDao().upsertAll(expenses)
        }
        BackupImportResult(if (matchedVehicle == null) 1 else 0, fuelEntries.size, expenses.size, 0, 0)
    }

    // Streams the backup once, looking only for the nested pictures.data entry (itself a
    // zip). Its contents are read via a second ZipInputStream wrapped around the *outer*
    // stream — safe because a nested ZipInputStream naturally stops at its entry's data
    // boundary — with close() on that inner wrapper intercepted so it doesn't take down the
    // outer stream before pass 1 finishes walking the rest of the top-level entries.
    private fun extractPictures(source: InputStream, onBytesRead: (Int) -> Unit): Map<String, String> =
        extractPicturesToDir(source, File(context.filesDir, "fuelio_images"), onBytesRead)

    private suspend fun mergeFuelRows(
        vehicleId: String,
        parsed: FuelioParseResult,
        imageMap: Map<String, String>,
    ): List<FuelEntryEntity> {
        val existingEntries = database.fuelEntryDao().getForVehicle(vehicleId)
        return parsed.fuelRows.map { row ->
            val existing = findMatchingFuelEntry(existingEntries, row)
            FuelEntryEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                vehicleId = vehicleId,
                date = row.date,
                time = row.time,
                odometerKm = row.odometerKm,
                liters = row.liters,
                pricePerLiter = row.pricePerLiter,
                amount = row.amount,
                fullTank = row.fullTank,
                station = row.station,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                photoUri = resolvePhotoUri(existing?.photoUri, row.uniqueId, "1", parsed.pictureMap, imageMap),
                missedPreviousFillUp = row.missedPreviousFillUp,
            )
        }
    }

    private suspend fun mergeCostRows(
        vehicleId: String,
        parsed: FuelioParseResult,
        imageMap: Map<String, String>,
    ): List<ExpenseEntity> {
        val existingExpenses = database.expenseDao().getForVehicle(vehicleId)
        return parsed.costRows.map { row ->
            val existing = findMatchingExpense(existingExpenses, row)
            ExpenseEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                vehicleId = vehicleId,
                date = row.date,
                time = row.time,
                category = row.category,
                description = row.title,
                amount = row.amount,
                odometerKm = row.odometerKm,
                income = row.income,
                recurring = existing?.recurring ?: false,
                reminderDate = existing?.reminderDate,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                photoUri = resolvePhotoUri(existing?.photoUri, row.uniqueId, "2", parsed.pictureMap, imageMap),
            )
        }
    }

    private fun findMatchingFuelEntry(existing: List<FuelEntryEntity>, row: FuelioFuelRow): FuelEntryEntity? =
        existing.find { it.date == row.date && it.time == row.time && abs(it.odometerKm - row.odometerKm) < 0.01 }

    private fun findMatchingExpense(existing: List<ExpenseEntity>, row: FuelioCostRow): ExpenseEntity? =
        existing.find {
            it.date == row.date &&
                it.time == row.time &&
                it.description.trim().equals(row.title.trim(), ignoreCase = true) &&
                odometerMatches(it.odometerKm, row.odometerKm)
        }

    private fun odometerMatches(a: Double?, b: Double?): Boolean = when {
        a == null && b == null -> true
        a == null || b == null -> false
        else -> abs(a - b) < 0.01
    }

    // Only fills in a photo when the matched record doesn't already have one, so a
    // re-import never overwrites a photo the user already attached or imported. The photo
    // itself was already streamed to disk in pass 1 — this just looks up its saved path.
    //
    // Real Fuelio exports assign UniqueId from two *separate* auto-increment sequences (one
    // for ##Log rows, one for ##Costs rows) that are shared across every vehicle in the same
    // backup — so a Log row and a Costs row can legitimately have the same UniqueId. The
    // ##Pictures table's "Type" column (rowType here: "1" = log, "2" = costs) disambiguates
    // them; [pictureMap] is keyed "type:targetId" first, falling back to the bare targetId
    // for exports that omit a Type column.
    private fun resolvePhotoUri(
        existingPhotoUri: String?,
        uniqueId: String,
        rowType: String,
        pictureMap: Map<String, List<String>>,
        imageMap: Map<String, String>,
    ): String? {
        if (!existingPhotoUri.isNullOrBlank()) return existingPhotoUri
        if (uniqueId.isBlank()) return existingPhotoUri
        val filenames = pictureMap["$rowType:$uniqueId"] ?: pictureMap[uniqueId].orEmpty()
        if (filenames.isEmpty()) return existingPhotoUri
        val savedPaths = filenames.mapNotNull { filename ->
            val base = filename.substringAfterLast('/').substringAfterLast('\\').lowercase()
            imageMap[base]
        }
        return PhotoUris.join(savedPaths) ?: existingPhotoUri
    }
}

// Streams the backup once, looking only for the nested pictures.data entry (itself a zip).
// Its contents are read via a second ZipInputStream wrapped around the *outer* stream — safe
// because a nested ZipInputStream naturally stops at its entry's data boundary — with close()
// on that inner wrapper intercepted so it doesn't take down the outer stream before pass 1
// finishes walking the rest of the top-level entries. Pulled out of FuelioImportRepository
// (which otherwise needs an Android Context just for filesDir) so it's directly unit-testable
// against a real nested zip-in-zip fixture.
internal fun extractPicturesToDir(source: InputStream, photosDir: File, onBytesRead: (Int) -> Unit = {}): Map<String, String> {
    val imageMap = mutableMapOf<String, String>()
    photosDir.mkdirs()
    CountingInputStream(source, onBytesRead).use { counting ->
        ZipInputStream(counting).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val baseName = entry.name.substringAfterLast('/').substringAfterLast('\\')
                if (!entry.isDirectory && baseName.equals("pictures.data", ignoreCase = true)) {
                    ZipInputStream(NonClosingInputStream(zip)).use { inner ->
                        var innerEntry = inner.nextEntry
                        while (innerEntry != null) {
                            if (!innerEntry.isDirectory) {
                                val innerBase = innerEntry.name.substringAfterLast('/').substringAfterLast('\\').lowercase()
                                val destFile = File(photosDir, "${UUID.randomUUID()}.jpg")
                                destFile.outputStream().use { output -> inner.copyTo(output) }
                                imageMap[innerBase] = destFile.absolutePath
                            }
                            inner.closeEntry()
                            innerEntry = inner.nextEntry
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
    return imageMap
}

// Tracks cumulative bytes read from the delegate stream, reporting each chunk so the
// caller can turn it into a rough "X% imported" figure without needing to buffer anything.
private class CountingInputStream(
    private val delegate: InputStream,
    private val onBytesRead: (Int) -> Unit,
) : InputStream() {
    override fun read(): Int {
        val result = delegate.read()
        if (result >= 0) onBytesRead(1)
        return result
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val count = delegate.read(b, off, len)
        if (count > 0) onBytesRead(count)
        return count
    }

    override fun close() = delegate.close()
}

// Wraps a stream (the outer ZipInputStream, while positioned on its pictures.data entry) so
// a nested ZipInputStream can be closed after reading that entry's inner zip without closing
// the outer stream out from under the pass-1 loop that still needs to reach later entries.
private class NonClosingInputStream(private val delegate: InputStream) : InputStream() {
    override fun read(): Int = delegate.read()
    override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)
    override fun close() {}
}

internal data class FuelioFuelRow(
    val date: String,
    val time: String,
    val odometerKm: Double,
    val liters: Double,
    val pricePerLiter: Double,
    val amount: Double,
    val fullTank: Boolean,
    val station: String,
    val uniqueId: String,
    val missedPreviousFillUp: Boolean,
)

internal data class FuelioCostRow(
    val date: String,
    val time: String,
    val title: String,
    val category: String,
    val odometerKm: Double?,
    val amount: Double,
    val income: Boolean,
    val uniqueId: String,
)

internal data class FuelioParseResult(
    val vehicleName: String?,
    val vehicleRegistration: String?,
    val vehicleFuelType: String?,
    val fuelRows: List<FuelioFuelRow>,
    val costRows: List<FuelioCostRow>,
    val pictureMap: Map<String, List<String>>,
)

private fun stripQuotes(field: String) = field.trim().removeSurrounding("\"").trim()

// Fuelio header names vary in separator style across export versions/locales (e.g.
// "Cost Title", "Cost_Title", "CostTitle"), so column matching strips underscores, spaces
// and dashes before comparing — mirrors the legacy web importer's costIndex() helper.
private fun normalizeHeader(field: String): String = field.lowercase().replace(Regex("[_\\s-]"), "")

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

internal fun parseFuelioCsv(text: String, defaultSection: String = ""): FuelioParseResult {
    val rawLines = text.split('\n').map { it.trimEnd('\r') }
    var vehicleName: String? = null
    var vehicleRegistration: String? = null
    var vehicleFuelType: String? = null
    val fuelRows = mutableListOf<FuelioFuelRow>()
    val costRows = mutableListOf<FuelioCostRow>()
    val costCategories = mutableMapOf<String, String>()
    val pictureMap = mutableMapOf<String, MutableList<String>>()

    var section = defaultSection
    var headerColumns: List<String> = emptyList()
    var idxDate = -1
    var idxTime = -1
    var idxOdo = -1
    var idxFuel = -1
    var idxFull = -1
    var idxAmount = -1
    var idxVolumePrice = -1
    var idxStation = -1
    var idxMissed = -1
    var idxUniqueId = -1
    // ##Costs columns
    var idxCostDate = -1
    var idxCostTime = -1
    var idxCostOdo = -1
    var idxCostCategory = -1
    var idxCostTitle = -1
    var idxCostPrice = -1
    var idxCostIncome = -1
    var idxCostUniqueId = -1
    // ##Vehicle columns
    var idxVehicleHeaderSeen = false
    var idxVehicleName = -1
    var idxVehiclePlate = -1
    // ##Pictures columns
    var idxPicFilename = -1
    var idxPicTargetId = -1
    var idxPicType = -1

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
        if ((section.isEmpty() || section == "costs") && (vehicleName == null || vehicleRegistration == null || vehicleFuelType == null)) {
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
                    // Real Fuelio exports spell the date+time column "Data" (not "Date" — a
                    // known quirk of the app) and pack both into one "yyyy-MM-dd HH:mm" value;
                    // "date"/"data" both matched here, and the two are split apart below. The
                    // gross total is a column literally named "Price (optional)" while the
                    // *actual* per-liter price is a separate "VolumePrice" column — both
                    // contain "price", so idxVolumePrice must be resolved first and idxAmount
                    // must explicitly exclude it to avoid picking the wrong column.
                    idxDate = headerColumns.indexOfFirst { it.contains("date") || it == "data" }
                    idxTime = headerColumns.indexOfFirst { it.contains("time") }
                    idxOdo = headerColumns.indexOfFirst { it.contains("odo") }
                    idxFuel = headerColumns.indexOfFirst { it.contains("fuel") || it.contains("liter") || it.contains("volume") && !it.contains("price") }
                    idxFull = headerColumns.indexOfFirst { it.contains("full") }
                    idxVolumePrice = headerColumns.indexOfFirst { it.contains("volume") && it.contains("price") }
                    idxAmount = headerColumns.indexOfFirst { it.contains("price") && !it.contains("volume") || it.contains("cost") }
                    idxStation = headerColumns.indexOfFirst { it.contains("station") || it.contains("place") || it.contains("city") }
                    idxMissed = headerColumns.indexOfFirst { it.contains("missed") }
                    idxUniqueId = headerColumns.indexOfFirst { normalizeHeader(it).contains("uniqueid") }
                } else if (headerColumns.isNotEmpty()) {
                    val rawDateTime = (if (idxDate in fields.indices) fields[idxDate] else "").trim()
                    val spaceIdx = rawDateTime.indexOf(' ')
                    val rawDate = if (spaceIdx > 0) rawDateTime.substring(0, spaceIdx) else rawDateTime
                    val timeFromDateTime = if (spaceIdx > 0) rawDateTime.substring(spaceIdx + 1).trim() else ""
                    val date = parseFuelioDate(rawDate)
                    val odo = fields.getOrNull(idxOdo)?.toDoubleOrNull()
                    val liters = fields.getOrNull(idxFuel)?.toDoubleOrNull()
                    if (date.isBlank() || odo == null || liters == null || liters <= 0) continue
                    val amount = fields.getOrNull(idxAmount)?.toDoubleOrNull() ?: 0.0
                    val volumePrice = fields.getOrNull(idxVolumePrice)?.toDoubleOrNull()?.takeIf { it > 0 }
                    val pricePerLiter = volumePrice ?: (if (amount > 0) amount / liters else 0.0)
                    val station = fields.getOrNull(idxStation)?.takeIf { it.isNotBlank() } ?: ""
                    val fullField = fields.getOrNull(idxFull)?.trim()?.lowercase()
                    val fullTank = fullField == null || fullField != "0"
                    val time = timeFromDateTime.takeIf { it.isNotBlank() }
                        ?: fields.getOrNull(idxTime)?.trim()?.takeIf { it.isNotBlank() }
                        ?: "00:00"
                    val missed = idxMissed >= 0 && fields.getOrNull(idxMissed)?.let { it == "1" || it.equals("true", true) } == true
                    fuelRows += FuelioFuelRow(
                        date = date,
                        time = time,
                        odometerKm = odo,
                        liters = liters,
                        pricePerLiter = pricePerLiter,
                        amount = amount,
                        fullTank = fullTank,
                        station = station,
                        uniqueId = fields.getOrNull(idxUniqueId)?.trim() ?: "",
                        missedPreviousFillUp = missed,
                    )
                }
            }
            "vehicle" -> {
                // Real Fuelio exports store vehicle metadata as its own header+data row (not
                // the "Car;MyCar" key-value lines the block above guards against), e.g.
                // "Name","Description",...,"Plate",... followed by "V-Cross M/AT 2022","",...
                val lowerFields = fields.map { it.lowercase() }
                if (!idxVehicleHeaderSeen && lowerFields.any { it == "name" } && lowerFields.any { it.contains("distunit") || it.contains("fuelunit") }) {
                    idxVehicleHeaderSeen = true
                    idxVehicleName = lowerFields.indexOfFirst { it == "name" }
                    idxVehiclePlate = lowerFields.indexOfFirst { it == "plate" }
                } else if (idxVehicleHeaderSeen && idxVehicleName >= 0) {
                    vehicleName = vehicleName ?: fields.getOrNull(idxVehicleName)?.trim()?.takeIf { it.isNotBlank() }
                    vehicleRegistration = vehicleRegistration ?: fields.getOrNull(idxVehiclePlate)?.trim()?.takeIf { it.isNotBlank() }
                }
            }
            "costcategories" -> {
                if (fields.size >= 2 && fields[0].toIntOrNull() != null) {
                    costCategories[fields[0]] = fields[1]
                }
            }
            "costs" -> {
                // Matched against Fuelio's real ##Costs column names (per the legacy web
                // importer's costIndex() ported from app.js:1716-1717): the money column is
                // literally named "Cost", not "Price"/"Amount" — normalizeHeader() strips
                // separators so "Cost_Title"/"Cost Title"/"CostTitle" all resolve the same way,
                // and the amount column is matched *exactly* against "cost"/"amount"/"price" so
                // it never collides with the "Cost Title" column (which also contains "cost").
                val normalizedFields = fields.map { normalizeHeader(it) }
                val looksLikeHeader = normalizedFields.any { it == "costtitle" || it == "title" } &&
                    normalizedFields.any { it == "cost" || it == "amount" || it == "price" }
                if (idxCostDate < 0 && looksLikeHeader) {
                    idxCostTitle = normalizedFields.indexOfFirst { it == "costtitle" || it == "title" }
                    idxCostDate = normalizedFields.indexOfFirst { it == "date" || it == "datetime" }
                    idxCostTime = normalizedFields.indexOfFirst { it == "time" }
                    idxCostOdo = normalizedFields.indexOfFirst { it == "odo" || it == "odometer" }
                    idxCostCategory = normalizedFields.indexOfFirst { it == "costtypeid" || it == "categoryid" || it.contains("categ") }
                    idxCostPrice = normalizedFields.indexOfFirst { it == "cost" || it == "amount" || it == "price" }
                    idxCostIncome = normalizedFields.indexOfFirst { it == "isincome" || it == "income" }
                    idxCostUniqueId = normalizedFields.indexOfFirst { it == "uniqueid" || it == "costid" || it == "id" }
                } else if (idxCostDate >= 0) {
                    // Fuelio's ##Costs "Date" column is also a combined "yyyy-MM-dd HH:mm"
                    // value (same as ##Log's "Data" column) — parseFuelioDate() only returns
                    // the date portion, so the time is split out the same way here.
                    val rawCostDateTime = (fields.getOrNull(idxCostDate) ?: "").trim()
                    val costSpaceIdx = rawCostDateTime.indexOf(' ')
                    val rawCostDate = if (costSpaceIdx > 0) rawCostDateTime.substring(0, costSpaceIdx) else rawCostDateTime
                    val timeFromCostDate = if (costSpaceIdx > 0) rawCostDateTime.substring(costSpaceIdx + 1).trim() else ""
                    val date = parseFuelioDate(rawCostDate)
                    val amount = fields.getOrNull(idxCostPrice)?.toDoubleOrNull() ?: 0.0
                    if (date.isBlank() || amount <= 0) continue
                    val categoryRaw = fields.getOrNull(idxCostCategory) ?: ""
                    val category = costCategories[categoryRaw] ?: categoryRaw.ifBlank { "อื่นๆ" }
                    val title = fields.getOrNull(idxCostTitle) ?: ""
                    val odo = fields.getOrNull(idxCostOdo)?.toDoubleOrNull()
                    val income = fields.getOrNull(idxCostIncome)?.let { it == "1" || it.equals("true", true) } ?: false
                    val time = timeFromCostDate.takeIf { it.isNotBlank() }
                        ?: fields.getOrNull(idxCostTime)?.trim()?.takeIf { it.isNotBlank() }
                        ?: "00:00"
                    costRows += FuelioCostRow(
                        date = date,
                        time = time,
                        title = title,
                        category = category,
                        odometerKm = odo,
                        amount = amount,
                        income = income,
                        uniqueId = fields.getOrNull(idxCostUniqueId)?.trim() ?: "",
                    )
                }
            }
            "pictures" -> {
                // "Type" (1 = ##Log row, 2 = ##Costs row) disambiguates the shared UniqueId
                // numbering — see the comment on resolvePhotoUri(). Both a type-scoped key
                // ("type:targetId") and a bare targetId key are stored so exports without a
                // Type column still resolve via the fallback lookup.
                val lowerFields = fields.map { it.lowercase() }
                val looksLikeHeader = lowerFields.any { it == "filename" } && lowerFields.any { it.replace("_", "") == "targetid" }
                if (idxPicFilename < 0 && looksLikeHeader) {
                    idxPicFilename = lowerFields.indexOfFirst { it == "filename" }
                    idxPicTargetId = lowerFields.indexOfFirst { it.replace("_", "") == "targetid" }
                    idxPicType = lowerFields.indexOfFirst { it == "type" }
                } else if (idxPicFilename >= 0) {
                    val targetId = fields.getOrNull(idxPicTargetId)?.trim() ?: ""
                    val filename = fields.getOrNull(idxPicFilename)?.trim() ?: ""
                    val type = if (idxPicType >= 0) fields.getOrNull(idxPicType)?.trim().orEmpty() else ""
                    if (targetId.isNotBlank() && filename.isNotBlank()) {
                        pictureMap.getOrPut(targetId) { mutableListOf() } += filename
                        if (type.isNotBlank()) {
                            pictureMap.getOrPut("$type:$targetId") { mutableListOf() } += filename
                        }
                    }
                }
            }
        }
    }
    return FuelioParseResult(vehicleName, vehicleRegistration, vehicleFuelType, fuelRows, costRows, pictureMap)
}
