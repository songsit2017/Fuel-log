package com.songsit.fuellogpro.domain

import com.songsit.fuellogpro.domain.model.Trip
import org.junit.Assert.assertEquals
import org.junit.Test

class TripSummaryTest {
    @Test
    fun aggregatesDistanceAndAllCostCategories() {
        val summary = calculateTripSummary(
            listOf(
                trip("a", 100.0, fuel = 500.0, toll = 100.0),
                trip("b", 50.0, parking = 50.0, food = 150.0, other = 200.0),
            ),
        )

        assertEquals(2, summary.tripCount)
        assertEquals(150.0, summary.totalDistanceKm, 0.001)
        assertEquals(1_000.0, summary.totalCost, 0.001)
        assertEquals(6.666, summary.costPerKm!!, 0.001)
    }

    @Test
    fun zeroDistanceDoesNotProduceInvalidCostPerKm() {
        assertEquals(null, calculateTripSummary(listOf(trip("a", 0.0, fuel = 100.0))).costPerKm)
    }
}

private fun trip(
    id: String,
    distance: Double,
    fuel: Double = 0.0,
    toll: Double = 0.0,
    parking: Double = 0.0,
    food: Double = 0.0,
    other: Double = 0.0,
) = Trip(
    id = id,
    vehicleId = "car",
    name = id,
    date = "2026-07-27",
    distanceKm = distance,
    fuelCost = fuel,
    tollCost = toll,
    parkingCost = parking,
    foodCost = food,
    otherCost = other,
)
