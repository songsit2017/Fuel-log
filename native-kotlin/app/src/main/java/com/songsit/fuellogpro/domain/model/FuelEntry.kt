package com.songsit.fuellogpro.domain.model

data class FuelEntry(
    val id: String,
    val vehicleId: String,
    val date: String,
    val time: String,
    val odometerKm: Double,
    val liters: Double,
    val pricePerLiter: Double,
    val amount: Double,
    val fullTank: Boolean,
    val station: String = "",
    val driver: String = "",
    val photoUrls: List<String> = emptyList(),
    val photoUri: String? = null,
)

