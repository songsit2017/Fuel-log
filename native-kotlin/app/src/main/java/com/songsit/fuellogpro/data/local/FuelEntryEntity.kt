// ============================================================================
// AI AGENT WARNING: SOURCE MODEL FOR THE PU POCKET PROJECTION
// Contract fields are encoded to Firestore and then mapped into PU Pocket.
// Read /ARCHITECTURE.md before renaming fields, changing units, ID stability,
// money precision, date/time meaning, deletion behavior, or photo representation.
// ============================================================================
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
    val paymentMethod: String = "",
    val driver: String = "",
    val recordedByUid: String? = null,
    val recordedByName: String? = null,
)
