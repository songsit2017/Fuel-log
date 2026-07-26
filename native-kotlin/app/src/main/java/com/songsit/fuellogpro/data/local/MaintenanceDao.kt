package com.songsit.fuellogpro.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_tasks WHERE vehicleId = :vehicleId ORDER BY createdAt ASC")
    fun observeForVehicle(vehicleId: String): Flow<List<MaintenanceEntity>>

    @Upsert
    suspend fun upsert(task: MaintenanceEntity)

    @Query("DELETE FROM maintenance_tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM maintenance_tasks WHERE vehicleId = :vehicleId")
    suspend fun deleteForVehicle(vehicleId: String)
}
