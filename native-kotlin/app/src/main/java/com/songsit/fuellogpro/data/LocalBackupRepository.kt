package com.songsit.fuellogpro.data

import androidx.room.withTransaction
import com.songsit.fuellogpro.data.local.ExpenseEntity
import com.songsit.fuellogpro.data.local.FuelEntryEntity
import com.songsit.fuellogpro.data.local.FuelLogDatabase
import com.songsit.fuellogpro.data.local.MaintenanceEntity
import com.songsit.fuellogpro.data.local.TripEntity
import com.songsit.fuellogpro.data.local.VehicleEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupImportResult(
    val vehicles: Int,
    val fuelEntries: Int,
    val expenses: Int,
    val maintenanceTasks: Int,
    val trips: Int,
) {
    val totalRecords: Int
        get() = vehicles + fuelEntries + expenses + maintenanceTasks + trips
}

class LocalBackupRepository(
    private val database: FuelLogDatabase,
) {
    suspend fun exportJson(): String = database.withTransaction {
        JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("vehicles", database.vehicleDao().getAll().toJsonArray(::vehicleJson))
            .put("fuelEntries", database.fuelEntryDao().getAll().toJsonArray(::fuelJson))
            .put("expenses", database.expenseDao().getAll().toJsonArray(::expenseJson))
            .put("maintenanceTasks", database.maintenanceDao().getAll().toJsonArray(::maintenanceJson))
            .put("trips", database.tripDao().getAll().toJsonArray(::tripJson))
            .toString(2)
    }

    suspend fun importJson(rawJson: String): BackupImportResult {
        val root = JSONObject(rawJson)
        require(root.optString("format") == FORMAT) { "ไฟล์นี้ไม่ใช่ข้อมูลสำรอง FuelLog Native" }
        val version = root.optInt("version", 0)
        require(version in 1..VERSION) { "เวอร์ชันไฟล์สำรองยังไม่รองรับ" }

        val snapshot = BackupSnapshot(
            vehicles = root.array("vehicles").mapObjects(::parseVehicle),
            fuelEntries = root.array("fuelEntries").mapObjects(::parseFuel),
            expenses = root.array("expenses").mapObjects(::parseExpense),
            maintenanceTasks = root.array("maintenanceTasks").mapObjects(::parseMaintenance),
            trips = root.array("trips").mapObjects(::parseTrip),
        )
        database.withTransaction {
            val vehicleIds = database.vehicleDao().getAll().mapTo(mutableSetOf()) { it.id }
            vehicleIds += snapshot.vehicles.map(VehicleEntity::id)
            val orphan = buildList {
                addAll(snapshot.fuelEntries.map(FuelEntryEntity::vehicleId).filterNot(vehicleIds::contains))
                addAll(snapshot.expenses.map(ExpenseEntity::vehicleId).filterNot(vehicleIds::contains))
                addAll(snapshot.maintenanceTasks.map(MaintenanceEntity::vehicleId).filterNot(vehicleIds::contains))
                addAll(snapshot.trips.map(TripEntity::vehicleId).filterNot(vehicleIds::contains))
            }.firstOrNull()
            require(orphan == null) { "พบข้อมูลที่อ้างถึงรถซึ่งไม่มีอยู่ในไฟล์สำรอง" }

            database.vehicleDao().upsertAll(snapshot.vehicles)
            database.fuelEntryDao().upsertAll(snapshot.fuelEntries)
            database.expenseDao().upsertAll(snapshot.expenses)
            database.maintenanceDao().upsertAll(snapshot.maintenanceTasks)
            database.tripDao().upsertAll(snapshot.trips)
        }
        return BackupImportResult(
            snapshot.vehicles.size,
            snapshot.fuelEntries.size,
            snapshot.expenses.size,
            snapshot.maintenanceTasks.size,
            snapshot.trips.size,
        )
    }

    companion object {
        private const val FORMAT = "fuellog-native-backup"
        private const val VERSION = 1
    }
}

private data class BackupSnapshot(
    val vehicles: List<VehicleEntity>,
    val fuelEntries: List<FuelEntryEntity>,
    val expenses: List<ExpenseEntity>,
    val maintenanceTasks: List<MaintenanceEntity>,
    val trips: List<TripEntity>,
)

private fun <T> List<T>.toJsonArray(mapper: (T) -> JSONObject) =
    JSONArray().also { array -> forEach { array.put(mapper(it)) } }

private fun JSONObject.array(name: String): JSONArray = optJSONArray(name) ?: JSONArray()

private fun <T> JSONArray.mapObjects(mapper: (JSONObject) -> T): List<T> =
    List(length()) { index -> mapper(getJSONObject(index)) }

private fun JSONObject.requiredString(name: String): String =
    getString(name).also { require(it.isNotBlank()) { "ช่อง $name ต้องไม่ว่าง" } }

private fun JSONObject.nullableString(name: String): String? =
    if (!has(name) || isNull(name)) null else optString(name).takeIf(String::isNotBlank)

private fun JSONObject.nullableDouble(name: String): Double? =
    if (!has(name) || isNull(name)) null else getDouble(name)

private fun vehicleJson(item: VehicleEntity) = JSONObject()
    .put("id", item.id).put("name", item.name).put("registration", item.registration)
    .put("fuelType", item.fuelType).put("createdAt", item.createdAt)

private fun fuelJson(item: FuelEntryEntity) = JSONObject()
    .put("id", item.id).put("vehicleId", item.vehicleId).put("date", item.date)
    .put("time", item.time).put("odometerKm", item.odometerKm).put("liters", item.liters)
    .put("pricePerLiter", item.pricePerLiter).put("amount", item.amount)
    .put("fullTank", item.fullTank).put("station", item.station).put("createdAt", item.createdAt)

private fun expenseJson(item: ExpenseEntity) = JSONObject()
    .put("id", item.id).put("vehicleId", item.vehicleId).put("date", item.date).put("time", item.time)
    .put("category", item.category).put("description", item.description).put("amount", item.amount)
    .put("odometerKm", item.odometerKm ?: JSONObject.NULL).put("income", item.income)
    .put("recurring", item.recurring).put("reminderDate", item.reminderDate ?: JSONObject.NULL)
    .put("createdAt", item.createdAt)

private fun maintenanceJson(item: MaintenanceEntity) = JSONObject()
    .put("id", item.id).put("vehicleId", item.vehicleId).put("name", item.name)
    .put("category", item.category).put("nextDate", item.nextDate ?: JSONObject.NULL)
    .put("nextOdometerKm", item.nextOdometerKm ?: JSONObject.NULL).put("warningDays", item.warningDays)
    .put("warningOdometerKm", item.warningOdometerKm).put("repeatMonths", item.repeatMonths ?: JSONObject.NULL)
    .put("repeatOdometerKm", item.repeatOdometerKm ?: JSONObject.NULL).put("provider", item.provider)
    .put("referenceNumber", item.referenceNumber).put("note", item.note).put("createdAt", item.createdAt)

private fun tripJson(item: TripEntity) = JSONObject()
    .put("id", item.id).put("vehicleId", item.vehicleId).put("name", item.name).put("date", item.date)
    .put("distanceKm", item.distanceKm).put("fuelCost", item.fuelCost).put("tollCost", item.tollCost)
    .put("parkingCost", item.parkingCost).put("foodCost", item.foodCost).put("otherCost", item.otherCost)
    .put("createdAt", item.createdAt)

private fun parseVehicle(json: JSONObject) = VehicleEntity(
    id = json.requiredString("id"),
    name = json.requiredString("name"),
    registration = json.optString("registration"),
    fuelType = json.optString("fuelType"),
    createdAt = json.optLong("createdAt", System.currentTimeMillis()),
)

private fun parseFuel(json: JSONObject) = FuelEntryEntity(
    id = json.requiredString("id"),
    vehicleId = json.requiredString("vehicleId"),
    date = json.requiredString("date"),
    time = json.optString("time"),
    odometerKm = json.getDouble("odometerKm"),
    liters = json.getDouble("liters"),
    pricePerLiter = json.getDouble("pricePerLiter"),
    amount = json.getDouble("amount"),
    fullTank = json.optBoolean("fullTank", true),
    station = json.optString("station"),
    createdAt = json.optLong("createdAt", System.currentTimeMillis()),
)

private fun parseExpense(json: JSONObject) = ExpenseEntity(
    id = json.requiredString("id"),
    vehicleId = json.requiredString("vehicleId"),
    date = json.requiredString("date"),
    time = json.optString("time", "00:00"),
    category = json.requiredString("category"),
    description = json.optString("description"),
    amount = json.getDouble("amount"),
    odometerKm = json.nullableDouble("odometerKm"),
    income = json.optBoolean("income", false),
    recurring = json.optBoolean("recurring", false),
    reminderDate = json.nullableString("reminderDate"),
    createdAt = json.optLong("createdAt", System.currentTimeMillis()),
)

private fun parseMaintenance(json: JSONObject) = MaintenanceEntity(
    id = json.requiredString("id"),
    vehicleId = json.requiredString("vehicleId"),
    name = json.requiredString("name"),
    category = json.optString("category", "บำรุงรักษา"),
    nextDate = json.nullableString("nextDate"),
    nextOdometerKm = json.nullableDouble("nextOdometerKm"),
    warningDays = json.optInt("warningDays", 30),
    warningOdometerKm = json.optDouble("warningOdometerKm", 1_000.0),
    repeatMonths = if (json.has("repeatMonths") && !json.isNull("repeatMonths")) json.getInt("repeatMonths") else null,
    repeatOdometerKm = json.nullableDouble("repeatOdometerKm"),
    provider = json.optString("provider"),
    referenceNumber = json.optString("referenceNumber"),
    note = json.optString("note"),
    createdAt = json.optLong("createdAt", System.currentTimeMillis()),
)

private fun parseTrip(json: JSONObject) = TripEntity(
    id = json.requiredString("id"),
    vehicleId = json.requiredString("vehicleId"),
    name = json.requiredString("name"),
    date = json.requiredString("date"),
    distanceKm = json.optDouble("distanceKm", 0.0),
    fuelCost = json.optDouble("fuelCost", 0.0),
    tollCost = json.optDouble("tollCost", 0.0),
    parkingCost = json.optDouble("parkingCost", 0.0),
    foodCost = json.optDouble("foodCost", 0.0),
    otherCost = json.optDouble("otherCost", 0.0),
    createdAt = json.optLong("createdAt", System.currentTimeMillis()),
)
