package com.songsit.fuellogpro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val name: String,
    val date: String,
    val distanceKm: Double,
    val fuelCost: Double,
    val tollCost: Double,
    val parkingCost: Double,
    val foodCost: Double,
    val otherCost: Double,
    val createdAt: Long,
)
