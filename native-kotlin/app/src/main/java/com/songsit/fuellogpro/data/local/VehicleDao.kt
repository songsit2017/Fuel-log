package com.songsit.fuellogpro.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles ORDER BY createdAt ASC")
    suspend fun getAll(): List<VehicleEntity>

    @Upsert
    suspend fun upsert(vehicle: VehicleEntity)

    @Upsert
    suspend fun upsertAll(vehicles: List<VehicleEntity>)

    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun deleteById(id: String)
}
