package com.songsit.fuellogpro.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY date DESC, createdAt DESC")
    fun observeForVehicle(vehicleId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE reminderDate IS NOT NULL")
    suspend fun getItemsWithReminderDates(): List<ExpenseEntity>

    @Upsert
    suspend fun upsert(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM expenses WHERE vehicleId = :vehicleId")
    suspend fun deleteForVehicle(vehicleId: String)
}
