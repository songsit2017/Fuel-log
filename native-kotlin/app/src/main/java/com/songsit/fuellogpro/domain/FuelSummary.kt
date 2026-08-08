package com.songsit.fuellogpro.domain

import com.songsit.fuellogpro.domain.model.FuelEntry

data class FuelSummary(
    val totalSpent: Double,
    val totalLiters: Double,
    val latestOdometerKm: Double?,
    val averageKmPerLiter: Double?,
)

private fun orderedEntries(entries: List<FuelEntry>): List<FuelEntry> =
    entries.sortedWith(compareBy({ it.date }, { it.time }, { it.odometerKm }))

/**
 * Full-to-full km/L intervals: distance between two full-tank fill-ups, but liters summed
 * across EVERY fill-up in between (partial fill-ups included), not just the closing full
 * tank's own liters. The full-to-full method only works because a partial fill-up's liters
 * still went into the tank and got burned over that distance — dropping them understates
 * liters used and inflates km/L (e.g. a partial fill-up skipped, then a full one right after,
 * used to report an implausibly high figure).
 */
private fun fullTankIntervals(entries: List<FuelEntry>): List<Triple<FuelEntry, Double, Double>> {
    val ordered = orderedEntries(entries)
    val fullIndices = ordered.withIndex().filter { it.value.fullTank }.map { it.index }
    val result = mutableListOf<Triple<FuelEntry, Double, Double>>()
    for (i in 1 until fullIndices.size) {
        val previous = ordered[fullIndices[i - 1]]
        val current = ordered[fullIndices[i]]
        val segment = ordered.subList(fullIndices[i - 1] + 1, fullIndices[i] + 1)
        if (segment.any { it.missedPreviousFillUp }) continue
        val intervalDistance = current.odometerKm - previous.odometerKm
        val segmentLiters = segment.sumOf { it.liters }
        if (intervalDistance > 0 && segmentLiters > 0) {
            result += Triple(current, intervalDistance, segmentLiters)
        }
    }
    return result
}

fun calculateFuelSummary(entries: List<FuelEntry>): FuelSummary {
    val intervals = fullTankIntervals(entries)
    val distance = intervals.sumOf { it.second }
    val liters = intervals.sumOf { it.third }
    return FuelSummary(
        totalSpent = entries.sumOf { it.amount },
        totalLiters = entries.sumOf { it.liters },
        latestOdometerKm = entries.maxOfOrNull { it.odometerKm },
        averageKmPerLiter = if (liters > 0) distance / liters else null,
    )
}

/**
 * Item A (per-entry km/L): keyed by the closing full-tank fill-up's id, using the same
 * full-to-full interval calculateFuelSummary() aggregates above — so a fuel-list row can show
 * its own figure without reimplementing the math.
 */
fun calculatePerEntryKmPerLiter(entries: List<FuelEntry>): Map<String, Double> =
    fullTankIntervals(entries).associate { (current, distance, liters) -> current.id to distance / liters }

data class FuelEfficiencyAlert(
    val latestKmPerLiter: Double,
    val averageKmPerLiter: Double,
    val dropPercent: Double,
)

/**
 * Flags a fill-up whose per-entry km/L (see [calculatePerEntryKmPerLiter] above) sits well
 * below the vehicle's own recent trend — a relative comparison rather than a fixed threshold,
 * since "normal" km/L varies hugely by vehicle. Needs [minPriorSamples] prior full-tank
 * data points before it will fire at all, so a nearly-new vehicle's first few fill-ups (still
 * settling into a baseline) don't trip a false alarm.
 */
fun checkFuelEfficiencyDrop(
    priorEntries: List<FuelEntry>,
    newEntry: FuelEntry,
    minPriorSamples: Int = 3,
    dropThreshold: Double = 0.15,
): FuelEfficiencyAlert? {
    if (!newEntry.fullTank || newEntry.missedPreviousFillUp) return null
    val kmPerLiterById = calculatePerEntryKmPerLiter(priorEntries + newEntry)
    val latest = kmPerLiterById[newEntry.id] ?: return null
    val priorValues = orderedEntries(priorEntries)
        .filter { it.fullTank }
        .mapNotNull { kmPerLiterById[it.id] }
        .takeLast(5)
    if (priorValues.size < minPriorSamples) return null
    val average = priorValues.average()
    if (average <= 0) return null
    val drop = (average - latest) / average
    if (drop < dropThreshold) return null
    return FuelEfficiencyAlert(
        latestKmPerLiter = latest,
        averageKmPerLiter = average,
        dropPercent = drop * 100,
    )
}
