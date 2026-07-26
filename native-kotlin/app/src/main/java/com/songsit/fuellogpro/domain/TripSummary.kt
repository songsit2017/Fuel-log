package com.songsit.fuellogpro.domain

import com.songsit.fuellogpro.domain.model.Trip
import com.songsit.fuellogpro.domain.model.totalCost

data class TripSummary(
    val tripCount: Int,
    val totalDistanceKm: Double,
    val totalCost: Double,
    val costPerKm: Double?,
)

fun calculateTripSummary(trips: List<Trip>): TripSummary {
    val distance = trips.sumOf(Trip::distanceKm)
    val cost = trips.sumOf { it.totalCost }
    return TripSummary(
        tripCount = trips.size,
        totalDistanceKm = distance,
        totalCost = cost,
        costPerKm = if (distance > 0) cost / distance else null,
    )
}
