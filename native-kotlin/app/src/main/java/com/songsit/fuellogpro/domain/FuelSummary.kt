package com.songsit.fuellogpro.domain

import com.songsit.fuellogpro.domain.model.FuelEntry

data class FuelSummary(
    val totalSpent: Double,
    val totalLiters: Double,
    val latestOdometerKm: Double?,
    val averageKmPerLiter: Double?,
)

private fun orderedFullTankEntries(entries: List<FuelEntry>): List<FuelEntry> =
    entries.sortedWith(compareBy<FuelEntry>({ it.date }, { it.time }, { it.odometerKm })).filter { it.fullTank }

fun calculateFuelSummary(entries: List<FuelEntry>): FuelSummary {
    val fullEntries = orderedFullTankEntries(entries)
    var distance = 0.0
    var liters = 0.0
    for (index in 1 until fullEntries.size) {
        val previous = fullEntries[index - 1]
        val current = fullEntries[index]
        if (current.missedPreviousFillUp) continue
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

/**
 * Item A (per-entry km/L): keyed by fill-up id instead of summed across all fill-ups, using
 * the exact same "distance since previous full-tank fill-up / liters of the current fill-up"
 * formula calculateFuelSummary() aggregates above — so a fuel-list row can show its own figure
 * without reimplementing the math.
 */
fun calculatePerEntryKmPerLiter(entries: List<FuelEntry>): Map<String, Double> {
    val fullEntries = orderedFullTankEntries(entries)
    val result = mutableMapOf<String, Double>()
    for (index in 1 until fullEntries.size) {
        val previous = fullEntries[index - 1]
        val current = fullEntries[index]
        if (current.missedPreviousFillUp) continue
        val intervalDistance = current.odometerKm - previous.odometerKm
        if (intervalDistance > 0 && current.liters > 0) {
            result[current.id] = intervalDistance / current.liters
        }
    }
    return result
}
