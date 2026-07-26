package com.songsit.fuellogpro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenance_tasks")
data class MaintenanceEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val name: String,
    val category: String,
    val nextDate: String?,
    val nextOdometerKm: Double?,
    val warningDays: Int,
    val warningOdometerKm: Double,
    val repeatMonths: Int?,
    val repeatOdometerKm: Double?,
    val provider: String,
    val referenceNumber: String,
    val note: String,
    val createdAt: Long,
)
