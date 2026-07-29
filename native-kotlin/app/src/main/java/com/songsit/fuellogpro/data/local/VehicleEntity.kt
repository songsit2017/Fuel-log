package com.songsit.fuellogpro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val registration: String,
    val fuelType: String,
    val createdAt: Long,
    val imageUri: String? = null,
    val distanceUnit: String = "km",
    val volumeUnit: String = "L",
    val consumptionUnit: String = "km/l",
    val hasDualTank: Boolean = false,
    val tankCapacity: Double? = null,
    val vin: String = "",
    val insurance: String = "",
    val isActive: Boolean = true,
    val brand: String = "",
    val model: String = "",
    val modelYear: Int? = null,
    val brandLogoUrl: String? = null,
)
