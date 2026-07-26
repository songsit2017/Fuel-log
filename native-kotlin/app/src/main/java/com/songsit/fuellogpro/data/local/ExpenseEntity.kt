package com.songsit.fuellogpro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val date: String,
    val category: String,
    val description: String,
    val amount: Double,
    val odometerKm: Double?,
    val createdAt: Long,
)
