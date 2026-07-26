package com.songsit.fuellogpro.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelEntryDao {
    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY date DESC, time DESC, odometerKm DESC")
    fun observeForVehicle(vehicleId: String): Flow<List<FuelEntryEntity>>

    @Upsert
    suspend fun upsert(entry: FuelEntryEntity)

    @Query("DELETE FROM fuel_entries WHERE id = :id")
    suspend fun deleteById(id: String)
}
