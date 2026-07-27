package com.songsit.fuellogpro.data

import com.songsit.fuellogpro.data.local.MaintenanceDao
import com.songsit.fuellogpro.data.local.MaintenanceEntity
import com.songsit.fuellogpro.domain.model.MaintenanceTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class LocalMaintenanceRepository(
    private val dao: MaintenanceDao,
    private val deletionRecorder: LocalDeletionRecorder,
) {
    fun observe(vehicleId: String): Flow<List<MaintenanceTask>> =
        dao.observeForVehicle(vehicleId).map { tasks -> tasks.map(MaintenanceEntity::toDomain) }

    fun observeCategorySuggestions(vehicleId: String): Flow<List<String>> =
        dao.observeDistinctCategories(vehicleId)

    suspend fun add(
        vehicleId: String,
        name: String,
        category: String,
        nextDate: String?,
        nextOdometerKm: Double?,
        warningDays: Int,
        warningOdometerKm: Double,
        repeatMonths: Int?,
        repeatOdometerKm: Double?,
    ) {
        dao.upsert(
            MaintenanceEntity(
                id = UUID.randomUUID().toString(),
                vehicleId = vehicleId,
                name = name.trim(),
                category = category.trim(),
                nextDate = nextDate?.takeIf(String::isNotBlank),
                nextOdometerKm = nextOdometerKm,
                warningDays = warningDays,
                warningOdometerKm = warningOdometerKm,
                repeatMonths = repeatMonths,
                repeatOdometerKm = repeatOdometerKm,
                provider = "",
                referenceNumber = "",
                note = "",
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun update(
        id: String,
        vehicleId: String,
        name: String,
        category: String,
        nextDate: String?,
        nextOdometerKm: Double?,
        warningDays: Int,
        warningOdometerKm: Double,
        repeatMonths: Int?,
        repeatOdometerKm: Double?,
    ) {
        val existing = dao.getById(id)
        dao.upsert(
            MaintenanceEntity(
                id = id,
                vehicleId = vehicleId,
                name = name.trim(),
                category = category.trim(),
                nextDate = nextDate?.takeIf(String::isNotBlank),
                nextOdometerKm = nextOdometerKm,
                warningDays = warningDays,
                warningOdometerKm = warningOdometerKm,
                repeatMonths = repeatMonths,
                repeatOdometerKm = repeatOdometerKm,
                provider = existing?.provider ?: "",
                referenceNumber = existing?.referenceNumber ?: "",
                note = existing?.note ?: "",
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            ),
        )
    }

    suspend fun markDone(task: MaintenanceTask, currentOdometerKm: Double?, today: LocalDate = LocalDate.now()) {
        if (task.repeatMonths == null && task.repeatOdometerKm == null) {
            delete(task.id)
            return
        }
        val nextDate = task.repeatMonths?.let { months ->
            val base = task.nextDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
            val step = months.coerceAtLeast(1).toLong()
            generateSequence(base.plusMonths(step)) { it.plusMonths(step) }
                .first { it.isAfter(today) }
                .toString()
        } ?: task.nextDate
        val nextOdometer = task.repeatOdometerKm?.let { interval ->
            val base = maxOf(task.nextOdometerKm ?: 0.0, currentOdometerKm ?: 0.0)
            base + interval
        } ?: task.nextOdometerKm
        dao.upsert(task.toEntity(nextDate, nextOdometer))
    }

    suspend fun delete(id: String) = deletionRecorder.delete(
        collectionName = "reminders",
        recordId = id,
        vehicleId = { dao.getById(id)?.vehicleId },
        deleteLocal = { dao.deleteById(id) },
    )
    suspend fun deleteForVehicle(vehicleId: String) = dao.deleteForVehicle(vehicleId)
}

private fun MaintenanceEntity.toDomain() = MaintenanceTask(
    id, vehicleId, name, category, nextDate, nextOdometerKm, warningDays,
    warningOdometerKm, repeatMonths, repeatOdometerKm, provider, referenceNumber, note,
)

private fun MaintenanceTask.toEntity(nextDate: String?, nextOdometerKm: Double?) = MaintenanceEntity(
    id, vehicleId, name, category, nextDate, nextOdometerKm, warningDays,
    warningOdometerKm, repeatMonths, repeatOdometerKm, provider, referenceNumber, note,
    createdAt = System.currentTimeMillis(),
)
