package com.songsit.fuellogpro.data

import androidx.room.withTransaction
import com.songsit.fuellogpro.data.local.DeletionTombstoneEntity
import com.songsit.fuellogpro.data.local.FuelLogDatabase
import com.songsit.fuellogpro.data.local.deletionTombstoneKey

class LocalDeletionRecorder(
    private val database: FuelLogDatabase,
) {
    suspend fun delete(
        collectionName: String,
        recordId: String,
        vehicleId: suspend () -> String?,
        deleteLocal: suspend () -> Unit,
    ) {
        database.withTransaction {
            val ownerVehicleId = vehicleId() ?: return@withTransaction
            database.deletionTombstoneDao().upsert(
                DeletionTombstoneEntity(
                    key = deletionTombstoneKey(collectionName, ownerVehicleId, recordId),
                    vehicleId = ownerVehicleId,
                    collectionName = collectionName,
                    recordId = recordId,
                    deletedAt = System.currentTimeMillis(),
                ),
            )
            deleteLocal()
        }
    }
}
