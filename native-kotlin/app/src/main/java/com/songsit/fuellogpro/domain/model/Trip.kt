package com.songsit.fuellogpro.domain.model

data class Trip(
    val id: String,
    val vehicleId: String,
    val name: String,
    val date: String,
    val distanceKm: Double,
    val fuelCost: Double,
    val tollCost: Double,
    val parkingCost: Double,
    val foodCost: Double,
    val otherCost: Double,
)

val Trip.totalCost: Double
    get() = fuelCost + tollCost + parkingCost + foodCost + otherCost
