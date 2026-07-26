package com.songsit.fuellogpro.domain

import com.songsit.fuellogpro.domain.model.FuelEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class FuelSummaryTest {
    @Test
    fun fullTankIntervalsProduceExpectedEfficiency() {
        val entries = listOf(
            fuel(id = "1", odometer = 10_000.0, liters = 30.0, amount = 1_000.0),
            fuel(id = "2", odometer = 10_450.0, liters = 30.0, amount = 1_050.0),
        )

        val summary = calculateFuelSummary(entries)

        assertEquals(15.0, summary.averageKmPerLiter!!, 0.001)
        assertEquals(60.0, summary.totalLiters, 0.001)
        assertEquals(2_050.0, summary.totalSpent, 0.001)
        assertEquals(10_450.0, summary.latestOdometerKm!!, 0.001)
    }

    @Test
    fun partialTanksAreExcludedFromEfficiencyIntervals() {
        val entries = listOf(
            fuel(id = "1", odometer = 1_000.0, liters = 20.0, amount = 700.0),
            fuel(id = "2", odometer = 1_200.0, liters = 10.0, amount = 350.0, full = false),
        )

        assertEquals(null, calculateFuelSummary(entries).averageKmPerLiter)
    }
}

private fun fuel(
    id: String,
    odometer: Double,
    liters: Double,
    amount: Double,
    full: Boolean = true,
) = FuelEntry(
    id = id,
    vehicleId = "car",
    date = "2026-07-27",
    time = "08:00",
    odometerKm = odometer,
    liters = liters,
    pricePerLiter = amount / liters,
    amount = amount,
    fullTank = full,
)
