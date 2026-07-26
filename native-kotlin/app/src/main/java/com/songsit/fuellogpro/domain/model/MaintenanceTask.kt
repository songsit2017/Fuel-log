package com.songsit.fuellogpro.domain.model

data class MaintenanceTask(
    val id: String,
    val vehicleId: String,
    val name: String,
    val category: String,
    val nextDate: String?,
    val nextOdometerKm: Double?,
    val warningDays: Int = 30,
    val warningOdometerKm: Double = 1_000.0,
    val repeatMonths: Int?,
    val repeatOdometerKm: Double?,
    val provider: String = "",
    val referenceNumber: String = "",
    val note: String = "",
)
