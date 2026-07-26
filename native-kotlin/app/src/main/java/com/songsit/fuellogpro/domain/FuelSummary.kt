package com.songsit.fuellogpro.domain

import com.songsit.fuellogpro.domain.model.FuelEntry

data class FuelSummary(
    val totalSpent: Double,
    val totalLiters: Double,
    val latestOdometerKm: Double?,
    val averageKmPerLiter: Double?,
)

fun calculateFuelSummary(entries: List<FuelEntry>): FuelSummary {
    val ordered = entries.sortedWith(compareBy<FuelEntry>({ it.date }, { it.time }, { it.odometerKm }))
    val fullEntries = ordered.filter { it.fullTank }
    var distance = 0.0
    var liters = 0.0
    for (index in 1 until fullEntries.size) {
        val previous = fullEntries[index - 1]
        val current = fullEntries[index]
        val intervalDistance = current.odometerKm - previous.odometerKm
        if (intervalDistance > 0 && current.liters > 0) {
            distance += intervalDistance
            liters += current.liters
        }
    }
    return FuelSummary(
        totalSpent = entries.sumOf { it.amount },
        totalLiters = entries.sumOf { it.liters },
        latestOdometerKm = entries.maxOfOrNull { it.odometerKm },
        averageKmPerLiter = if (liters > 0) distance / liters else null,
    )
}
