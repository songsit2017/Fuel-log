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
    val photoUri: String? = null,
    val odometerIsTripMeter: Boolean = false,
    val tankLevelEnabled: Boolean = false,
    val tankLevelTiming: String = "after",
    val tankLevelPercent: Double? = null,
    val tankLevelLiters: Double? = null,
    val discountEnabled: Boolean = false,
    val discountAmount: Double = 0.0,
    val discountPerLiter: Boolean = false,
    val missedPreviousFillUp: Boolean = false,
    val weatherDescription: String? = null,
    val weatherTemperatureC: Double? = null,
    val weatherLatitude: Double? = null,
    val weatherLongitude: Double? = null,
)
