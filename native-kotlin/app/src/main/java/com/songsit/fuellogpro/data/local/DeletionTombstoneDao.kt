package com.songsit.fuellogpro.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DeletionTombstoneDao {
    @Query("SELECT * FROM deletion_tombstones WHERE vehicleId = :vehicleId")
    suspend fun getForVehicle(vehicleId: String): List<DeletionTombstoneEntity>

    @Query("SELECT * FROM deletion_tombstones WHERE collectionName = :collectionName")
    suspend fun getByCollection(collectionName: String): List<DeletionTombstoneEntity>

    @Upsert
    suspend fun upsert(item: DeletionTombstoneEntity)

    @Upsert
    suspend fun upsertAll(items: List<DeletionTombstoneEntity>)
}
