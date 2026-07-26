package com.songsit.fuellogpro.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_tasks WHERE vehicleId = :vehicleId ORDER BY createdAt ASC")
    fun observeForVehicle(vehicleId: String): Flow<List<MaintenanceEntity>>

    @Query("SELECT * FROM maintenance_tasks WHERE nextDate IS NOT NULL")
    suspend fun getTasksWithDates(): List<MaintenanceEntity>

    @Query("SELECT * FROM maintenance_tasks")
    suspend fun getAll(): List<MaintenanceEntity>

    @Query("SELECT * FROM maintenance_tasks WHERE id = :id")
    suspend fun getById(id: String): MaintenanceEntity?

    @Upsert
    suspend fun upsert(task: MaintenanceEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<MaintenanceEntity>)

    @Query("DELETE FROM maintenance_tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM maintenance_tasks WHERE vehicleId = :vehicleId")
    suspend fun deleteForVehicle(vehicleId: String)
}
