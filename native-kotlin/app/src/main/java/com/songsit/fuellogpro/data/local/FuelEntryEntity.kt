package com.songsit.fuellogpro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_entries")
data class FuelEntryEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val date: String,
    val time: String,
    val odometerKm: Double,
    val liters: Double,
    val pricePerLiter: Double,
    val amount: Double,
    val fullTank: Boolean,
    val station: String,
    val createdAt: Long,
)
