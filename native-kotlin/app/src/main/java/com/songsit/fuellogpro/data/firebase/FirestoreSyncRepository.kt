package com.songsit.fuellogpro.data.firebase

import androidx.room.withTransaction
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.songsit.fuellogpro.data.local.DeletionTombstoneEntity
import com.songsit.fuellogpro.data.local.ExpenseEntity
import com.songsit.fuellogpro.data.local.FuelEntryEntity
import com.songsit.fuellogpro.data.local.FuelLogDatabase
import com.songsit.fuellogpro.data.local.MaintenanceEntity
import com.songsit.fuellogpro.data.local.TripEntity
import com.songsit.fuellogpro.data.local.VehicleEntity
import com.songsit.fuellogpro.data.local.SyncConflictEntity
import com.songsit.fuellogpro.data.local.deletionTombstoneKey
import kotlinx.coroutines.tasks.await
import com.songsit.fuellogpro.domain.SyncDecision
import com.songsit.fuellogpro.domain.decideSync
import java.util.UUID

data class CloudSyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val conflicts: Int,
    val vehicles: Int,
)

class FirestoreSyncRepository(
    private val database: FuelLogDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun sync(uid: String, email: String?, displayName: String?): CloudSyncResult {
        normalizeLegacyVehicleId()
        val cloudVehicles = discoverVehicles(uid)
        val localVehicles = database.vehicleDao().getAll().associateBy(VehicleEntity::id)
        var uploaded = 0
        var downloaded = 0
        var conflicts = 0

        for ((id, local) in localVehicles) {
            try {
                val cloud = cloudVehicles[id]
                val equal = cloud?.let(::parseVehicle)?.let(::vehicleCanonical) == vehicleCanonical(local)
                when (decideSync(localExists = true, cloudExists = cloud != null, contentEqual = equal)) {
                    SyncDecision.UPLOAD -> {
                        firestore.collection("vehicles").document(id).set(
                            vehicleCloudMap(local, uid, email, displayName),
                            SetOptions.merge(),
                        ).await()
                        uploaded++
                        clearConflict(VEHICLES_COLLECTION, id, id)
                    }
                    SyncDecision.CONFLICT -> {
                        conflicts++
                        recordConflict(id, VEHICLES_COLLECTION, id)
                    }
                    else -> clearConflict(VEHICLES_COLLECTION, id, id)
                }
            } catch (e: Exception) {
                throw Exception("Sync failed on vehicle doc '$id' (${local.name}): ${e.message}", e)
            }
        }
        val cloudOnlyVehicles = cloudVehicles
            .filterKeys { it !in localVehicles }
            .values
            .map(::parseVehicle)
        if (cloudOnlyVehicles.isNotEmpty()) {
            database.vehicleDao().upsertAll(cloudOnlyVehicles)
            cloudOnlyVehicles.forEach { clearConflict(VEHICLES_COLLECTION, it.id, it.id) }
            downloaded += cloudOnlyVehicles.size
        }

        val vehicleIds = localVehicles.keys + cloudVehicles.keys
        for (vehicleId in vehicleIds) {
            try {
                syncDeletionTombstones(vehicleId).also {
                    uploaded += it.uploaded
                    downloaded += it.downloaded
                }
            } catch (e: Exception) {
                throw Exception("Sync failed on vehicle '$vehicleId' collection '_deletions': ${e.message}", e)
            }
            try {
                syncCollection(
                    vehicleId = vehicleId,
                    collection = "entries",
                    local = database.fuelEntryDao().getAll().filter { it.vehicleId == vehicleId },
                    idOf = FuelEntryEntity::id,
                    encode = ::fuelCloudMap,
                    decode = { parseFuel(vehicleId, it) },
                    upsert = { database.fuelEntryDao().upsertAll(it) },
                ).also {
                    uploaded += it.uploaded
                    downloaded += it.downloaded
                    conflicts += it.conflicts
                }
            } catch (e: Exception) {
                throw Exception("Sync failed on vehicle '$vehicleId' collection 'entries': ${e.message}", e)
            }
            try {
                syncCollection(
                    vehicleId = vehicleId,
                    collection = "expenses",
                    local = database.expenseDao().getAll().filter { it.vehicleId == vehicleId },
                    idOf = ExpenseEntity::id,
                    encode = ::expenseCloudMap,
                    decode = { parseExpense(vehicleId, it) },
                    upsert = { database.expenseDao().upsertAll(it) },
                ).also {
                    uploaded += it.uploaded
                    downloaded += it.downloaded
                    conflicts += it.conflicts
                }
            } catch (e: Exception) {
                throw Exception("Sync failed on vehicle '$vehicleId' collection 'expenses': ${e.message}", e)
            }
            try {
                syncCollection(
                    vehicleId = vehicleId,
                    collection = "reminders",
                    local = database.maintenanceDao().getAll().filter { it.vehicleId == vehicleId },
                    idOf = MaintenanceEntity::id,
                    encode = ::maintenanceCloudMap,
                    decode = { parseMaintenance(vehicleId, it) },
                    upsert = { database.maintenanceDao().upsertAll(it) },
                ).also {
                    uploaded += it.uploaded
                    downloaded += it.downloaded
                    conflicts += it.conflicts
                }
            } catch (e: Exception) {
                throw Exception("Sync failed on vehicle '$vehicleId' collection 'reminders': ${e.message}", e)
            }
            try {
                syncCollection(
                    vehicleId = vehicleId,
                    collection = "trips",
                    local = database.tripDao().getAll().filter { it.vehicleId == vehicleId },
                    idOf = TripEntity::id,
                    encode = ::tripCloudMap,
                    decode = { parseTrip(vehicleId, it) },
                    upsert = { database.tripDao().upsertAll(it) },
                ).also {
                    uploaded += it.uploaded
                    downloaded += it.downloaded
                    conflicts += it.conflicts
                }
            } catch (e: Exception) {
                throw Exception("Sync failed on vehicle '$vehicleId' collection 'trips': ${e.message}", e)
            }
        }
        return CloudSyncResult(uploaded, downloaded, conflicts, vehicleIds.size)
    }

    suspend fun resolveConflict(key: String, useLocal: Boolean) {
        val conflict = database.syncConflictDao().getByKey(key)
            ?: error("ไม่พบรายการขัดแย้งนี้แล้ว")
        if (conflict.collectionName == VEHICLES_COLLECTION) {
            val reference = firestore.collection("vehicles").document(conflict.recordId)
            if (useLocal) {
                val local = database.vehicleDao().getById(conflict.recordId)
                    ?: error("ไม่พบข้อมูลรถในเครื่อง")
                reference.set(
                    vehicleEditableCloudMap(local) + ("updatedAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge(),
                ).await()
            } else {
                val cloud = reference.get().await()
                require(cloud.exists()) { "ไม่พบข้อมูลรถบน Cloud" }
                database.vehicleDao().upsert(parseVehicle(cloud))
            }
            database.syncConflictDao().deleteByKey(key)
            return
        }

        val reference = firestore.collection("vehicles").document(conflict.vehicleId)
            .collection(conflict.collectionName).document(conflict.recordId)
        if (useLocal) {
            val payload = when (conflict.collectionName) {
                "entries" -> database.fuelEntryDao().getById(conflict.recordId)?.let(::fuelCloudMap)
                "expenses" -> database.expenseDao().getById(conflict.recordId)?.let(::expenseCloudMap)
                "reminders" -> database.maintenanceDao().getById(conflict.recordId)?.let(::maintenanceCloudMap)
                "trips" -> database.tripDao().getById(conflict.recordId)?.let(::tripCloudMap)
                else -> error("ไม่รู้จักประเภทข้อมูล ${conflict.collectionName}")
            } ?: error("ไม่พบข้อมูลในเครื่อง")
            reference.set(
                payload + ("updatedAt" to FieldValue.serverTimestamp()),
                SetOptions.merge(),
            ).await()
        } else {
            val cloud = reference.get().await()
            require(cloud.exists()) { "ไม่พบข้อมูลบน Cloud" }
            when (conflict.collectionName) {
                "entries" -> database.fuelEntryDao().upsert(parseFuel(conflict.vehicleId, cloud))
                "expenses" -> database.expenseDao().upsert(parseExpense(conflict.vehicleId, cloud))
                "reminders" -> database.maintenanceDao().upsert(parseMaintenance(conflict.vehicleId, cloud))
                "trips" -> database.tripDao().upsert(parseTrip(conflict.vehicleId, cloud))
                else -> error("ไม่รู้จักประเภทข้อมูล ${conflict.collectionName}")
            }
        }
        database.syncConflictDao().deleteByKey(key)
    }

    private suspend fun normalizeLegacyVehicleId() {
        val legacy = database.vehicleDao().getAll().firstOrNull { it.id == LEGACY_VEHICLE_ID } ?: return
        val newId = UUID.randomUUID().toString()
        database.withTransaction {
            database.vehicleDao().upsert(legacy.copy(id = newId))
            database.fuelEntryDao().upsertAll(
                database.fuelEntryDao().getAll()
                    .filter { it.vehicleId == LEGACY_VEHICLE_ID }
                    .map { it.copy(vehicleId = newId) },
            )
            database.expenseDao().upsertAll(
                database.expenseDao().getAll()
                    .filter { it.vehicleId == LEGACY_VEHICLE_ID }
                    .map { it.copy(vehicleId = newId) },
            )
            database.maintenanceDao().upsertAll(
                database.maintenanceDao().getAll()
                    .filter { it.vehicleId == LEGACY_VEHICLE_ID }
                    .map { it.copy(vehicleId = newId) },
            )
            database.tripDao().upsertAll(
                database.tripDao().getAll()
                    .filter { it.vehicleId == LEGACY_VEHICLE_ID }
                    .map { it.copy(vehicleId = newId) },
            )
            database.vehicleDao().deleteById(LEGACY_VEHICLE_ID)
        }
    }

    private suspend fun discoverVehicles(uid: String): Map<String, DocumentSnapshot> {
        val found = linkedMapOf<String, DocumentSnapshot>()
        firestore.collection("vehicles").whereEqualTo("ownerUid", uid).get().await()
            .documents.forEach { found[it.id] = it }
        firestore.collection("vehicles").whereArrayContains("memberUids", uid).get().await()
            .documents.forEach { found[it.id] = it }
        return found
    }

    companion object {
        private const val LEGACY_VEHICLE_ID = "local-default"
        private const val VEHICLES_COLLECTION = "vehicles"
        private const val DELETIONS_COLLECTION = "_deletions"
        private val RECORD_COLLECTIONS = setOf("entries", "expenses", "reminders", "trips")
    }

    private suspend fun <T> syncCollection(
        vehicleId: String,
        collection: String,
        local: List<T>,
        idOf: (T) -> String,
        encode: (T) -> Map<String, Any?>,
        decode: (DocumentSnapshot) -> T,
        upsert: suspend (List<T>) -> Unit,
    ): RecordSyncCount {
        val localById = local.associateBy(idOf)
        val cloudById = firestore.collection("vehicles").document(vehicleId)
            .collection(collection).get().await().documents.associateBy { it.id }
        var uploaded = 0
        var conflicts = 0
        val imported = mutableListOf<T>()

        for ((id, localItem) in localById) {
            val cloudDocument = cloudById[id]
            val cloudItem = cloudDocument?.let { runCatching { decode(it) }.getOrNull() }
            val equal = cloudItem?.let(encode) == encode(localItem)
            when (decideSync(localExists = true, cloudExists = cloudDocument != null, contentEqual = equal)) {
                SyncDecision.UPLOAD -> {
                    val upload = encode(localItem).toMutableMap().apply {
                        put("updatedAt", FieldValue.serverTimestamp())
                    }
                    firestore.collection("vehicles").document(vehicleId).collection(collection)
                        .document(id).set(upload, SetOptions.merge()).await()
                    uploaded++
                    clearConflict(collection, vehicleId, id)
                }
                SyncDecision.CONFLICT -> {
                    conflicts++
                    recordConflict(vehicleId, collection, id)
                }
                else -> clearConflict(collection, vehicleId, id)
            }
        }
        for ((id, cloudDocument) in cloudById) {
            if (id !in localById) {
                try {
                    imported.add(decode(cloudDocument))
                    clearConflict(collection, vehicleId, id)
                } catch (_: Exception) {
                    conflicts++
                    recordConflict(vehicleId, collection, id)
                }
            }
        }
        if (imported.isNotEmpty()) database.withTransaction { upsert(imported) }
        return RecordSyncCount(uploaded, imported.size, conflicts)
    }

    private suspend fun syncDeletionTombstones(vehicleId: String): RecordSyncCount {
        val localByKey = database.deletionTombstoneDao().getForVehicle(vehicleId)
            .associateBy(DeletionTombstoneEntity::key)
        val cloudByKey = firestore.collection(VEHICLES_COLLECTION).document(vehicleId)
            .collection(DELETIONS_COLLECTION).get().await().documents
            .mapNotNull { document ->
                val collectionName = document.getString("collectionName")
                val recordId = document.getString("recordId")
                if (collectionName == null || collectionName !in RECORD_COLLECTIONS || recordId.isNullOrBlank()) {
                    return@mapNotNull null
                }
                val item = DeletionTombstoneEntity(
                    key = deletionTombstoneKey(collectionName, vehicleId, recordId),
                    vehicleId = vehicleId,
                    collectionName = collectionName,
                    recordId = recordId,
                    deletedAt = document.getLong("deletedAt") ?: 0L,
                )
                item.key to item
            }
            .toMap()

        var uploaded = 0
        for ((key, item) in localByKey) {
            if (key !in cloudByKey) {
                firestore.collection(VEHICLES_COLLECTION).document(vehicleId)
                    .collection(DELETIONS_COLLECTION).document(key)
                    .set(
                        mapOf(
                            "collectionName" to item.collectionName,
                            "recordId" to item.recordId,
                            "deletedAt" to item.deletedAt,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    ).await()
                uploaded++
            }
        }

        val merged = (localByKey.keys + cloudByKey.keys).mapNotNull { key ->
            listOfNotNull(localByKey[key], cloudByKey[key]).maxByOrNull { it.deletedAt }
        }
        if (merged.isNotEmpty()) {
            database.withTransaction {
                database.deletionTombstoneDao().upsertAll(merged)
                for (item in merged) {
                    deleteLocalRecord(item.collectionName, item.recordId)
                    database.syncConflictDao().deleteByKey(
                        conflictKey(item.collectionName, vehicleId, item.recordId),
                    )
                }
            }
            for (item in merged) {
                firestore.collection(VEHICLES_COLLECTION).document(vehicleId)
                    .collection(item.collectionName).document(item.recordId)
                    .delete().await()
            }
        }
        return RecordSyncCount(
            uploaded = uploaded,
            downloaded = cloudByKey.keys.count { it !in localByKey },
            conflicts = 0,
        )
    }

    private suspend fun deleteLocalRecord(collectionName: String, recordId: String) {
        when (collectionName) {
            "entries" -> database.fuelEntryDao().deleteById(recordId)
            "expenses" -> database.expenseDao().deleteById(recordId)
            "reminders" -> database.maintenanceDao().deleteById(recordId)
            "trips" -> database.tripDao().deleteById(recordId)
        }
    }

    private suspend fun recordConflict(vehicleId: String, collection: String, recordId: String) {
        database.syncConflictDao().upsert(
            SyncConflictEntity(
                key = conflictKey(collection, vehicleId, recordId),
                vehicleId = vehicleId,
                collectionName = collection,
                recordId = recordId,
                detectedAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun clearConflict(collection: String, vehicleId: String, recordId: String) {
        database.syncConflictDao().deleteByKey(conflictKey(collection, vehicleId, recordId))
    }
}

private data class RecordSyncCount(val uploaded: Int, val downloaded: Int, val conflicts: Int)

private fun vehicleCloudMap(
    item: VehicleEntity,
    uid: String,
    email: String?,
    displayName: String?,
): Map<String, Any?> = mapOf(
    "name" to item.name,
    "registration" to item.registration,
    "fuelType" to item.fuelType,
    "ownerUid" to uid,
    "memberUids" to listOf(uid),
    "members" to mapOf(
        uid to mapOf("role" to "owner", "email" to email.orEmpty(), "displayName" to displayName.orEmpty()),
    ),
    "createdAt" to FieldValue.serverTimestamp(),
)

private fun vehicleCanonical(item: VehicleEntity) =
    listOf(item.name, item.registration, item.fuelType)

private fun vehicleEditableCloudMap(item: VehicleEntity): Map<String, Any?> = mapOf(
    "name" to item.name,
    "registration" to item.registration,
    "fuelType" to item.fuelType,
)

private fun conflictKey(collection: String, vehicleId: String, recordId: String) =
    "$collection|$vehicleId|$recordId"

private fun fuelCloudMap(item: FuelEntryEntity): Map<String, Any?> = mapOf(
    "id" to item.id,
    "vehicleId" to item.vehicleId,
    "date" to item.date,
    "time" to item.time,
    "odometer" to item.odometerKm,
    "liters" to item.liters,
    "price" to item.pricePerLiter,
    "total" to item.amount,
    "full" to item.fullTank,
    "station" to item.station,
)

private fun expenseCloudMap(item: ExpenseEntity): Map<String, Any?> = mapOf(
    "id" to item.id,
    "vehicleId" to item.vehicleId,
    "date" to item.date,
    "category" to item.category,
    "title" to item.description,
    "amount" to item.amount,
    "odometer" to item.odometerKm,
    "income" to item.income,
    "recurrence" to if (item.recurring) "recurring" else "once",
    "reminderDate" to item.reminderDate,
)

private fun maintenanceCloudMap(item: MaintenanceEntity): Map<String, Any?> = mapOf(
    "id" to item.id,
    "vehicleId" to item.vehicleId,
    "name" to item.name,
    "category" to item.category,
    "nextDate" to item.nextDate,
    "nextOdo" to item.nextOdometerKm,
    "warningDays" to item.warningDays.toLong(),
    "warningOdo" to item.warningOdometerKm,
    "repeatMonths" to item.repeatMonths?.toLong(),
    "repeatOdo" to item.repeatOdometerKm,
    "provider" to item.provider,
    "policyNumber" to item.referenceNumber,
    "note" to item.note,
)

private fun tripCloudMap(item: TripEntity): Map<String, Any?> = mapOf(
    "id" to item.id,
    "vehicleId" to item.vehicleId,
    "name" to item.name,
    "date" to item.date,
    "distance" to item.distanceKm,
    "fuel" to item.fuelCost,
    "toll" to item.tollCost,
    "parking" to item.parkingCost,
    "food" to item.foodCost,
    "other" to item.otherCost,
)

private fun parseVehicle(document: DocumentSnapshot) = VehicleEntity(
    id = document.id,
    name = document.getString("name") ?: "รถจาก Cloud",
    registration = document.getString("registration").orEmpty(),
    fuelType = document.getString("fuelType").orEmpty(),
    createdAt = document.timestampMillis("createdAt"),
)

private fun parseFuel(vehicleId: String, document: DocumentSnapshot) = FuelEntryEntity(
    id = document.id,
    vehicleId = vehicleId,
    date = document.getString("date").orEmpty(),
    time = document.getString("time").orEmpty(),
    odometerKm = document.number("odometer", "odometerKm"),
    liters = document.number("liters"),
    pricePerLiter = document.number("price", "pricePerLiter"),
    amount = document.number("total", "amount"),
    fullTank = document.getBoolean("full") ?: document.getBoolean("fullTank") ?: true,
    station = document.getString("station").orEmpty(),
    createdAt = document.timestampMillis("createdAt"),
)

private fun parseExpense(vehicleId: String, document: DocumentSnapshot) = ExpenseEntity(
    id = document.id,
    vehicleId = vehicleId,
    date = document.getString("date").orEmpty(),
    category = document.getString("category") ?: "อื่น ๆ",
    description = document.getString("title") ?: document.getString("description").orEmpty(),
    amount = document.number("amount"),
    odometerKm = document.optionalNumber("odometer", "odometerKm"),
    income = document.getBoolean("income") ?: false,
    recurring = document.getString("recurrence") == "recurring" || document.getBoolean("recurring") == true,
    reminderDate = document.getString("reminderDate")?.takeIf(String::isNotBlank),
    createdAt = document.timestampMillis("createdAt"),
)

private fun parseMaintenance(vehicleId: String, document: DocumentSnapshot) = MaintenanceEntity(
    id = document.id,
    vehicleId = vehicleId,
    name = document.getString("name") ?: "รายการดูแลรถ",
    category = document.getString("category") ?: "บำรุงรักษา",
    nextDate = document.getString("nextDate")?.takeIf(String::isNotBlank),
    nextOdometerKm = document.optionalNumber("nextOdo", "nextOdometerKm"),
    warningDays = document.optionalNumber("warningDays")?.toInt() ?: 30,
    warningOdometerKm = document.optionalNumber("warningOdo", "warningOdometerKm") ?: 1_000.0,
    repeatMonths = document.optionalNumber("repeatMonths")?.toInt(),
    repeatOdometerKm = document.optionalNumber("repeatOdo", "repeatOdometerKm"),
    provider = document.getString("provider").orEmpty(),
    referenceNumber = document.getString("policyNumber") ?: document.getString("referenceNumber").orEmpty(),
    note = document.getString("note").orEmpty(),
    createdAt = document.timestampMillis("createdAt"),
)

private fun parseTrip(vehicleId: String, document: DocumentSnapshot) = TripEntity(
    id = document.id,
    vehicleId = vehicleId,
    name = document.getString("name") ?: "ทริป",
    date = document.getString("date").orEmpty(),
    distanceKm = document.number("distance", "distanceKm"),
    fuelCost = document.number("fuel", "fuelCost"),
    tollCost = document.number("toll", "tollCost"),
    parkingCost = document.number("parking", "parkingCost"),
    foodCost = document.number("food", "foodCost"),
    otherCost = document.number("other", "otherCost"),
    createdAt = document.timestampMillis("createdAt"),
)

private fun DocumentSnapshot.number(vararg names: String): Double =
    optionalNumber(*names) ?: 0.0

private fun DocumentSnapshot.optionalNumber(vararg names: String): Double? =
    names.firstNotNullOfOrNull { name -> (get(name) as? Number)?.toDouble() }

private fun DocumentSnapshot.timestampMillis(name: String): Long =
    (get(name) as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
