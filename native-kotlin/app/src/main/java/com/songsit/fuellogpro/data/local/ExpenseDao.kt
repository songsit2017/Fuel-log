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

    @Query("SELECT * FROM expenses")
    suspend fun getAll(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId")
    suspend fun getForVehicle(vehicleId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: String): ExpenseEntity?

    @Query("SELECT DISTINCT category FROM expenses WHERE vehicleId = :vehicleId AND category != '' ORDER BY createdAt DESC")
    fun observeDistinctCategories(vehicleId: String): Flow<List<String>>

    @Upsert
    suspend fun upsert(expense: ExpenseEntity)

    @Upsert
    suspend fun upsertAll(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM expenses WHERE vehicleId = :vehicleId")
    suspend fun deleteForVehicle(vehicleId: String)
}
