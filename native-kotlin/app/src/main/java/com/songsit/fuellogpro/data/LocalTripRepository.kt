package com.songsit.fuellogpro.data

import com.songsit.fuellogpro.data.local.TripDao
import com.songsit.fuellogpro.data.local.TripEntity
import com.songsit.fuellogpro.domain.model.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalTripRepository(
    private val dao: TripDao,
) {
    fun observe(vehicleId: String): Flow<List<Trip>> =
        dao.observeForVehicle(vehicleId).map { trips -> trips.map(TripEntity::toDomain) }

    suspend fun add(
        vehicleId: String,
        name: String,
        date: String,
        distanceKm: Double,
        fuelCost: Double,
        tollCost: Double,
        parkingCost: Double,
        foodCost: Double,
        otherCost: Double,
    ) {
        dao.upsert(
            TripEntity(
                id = UUID.randomUUID().toString(),
                vehicleId = vehicleId,
                name = name.trim(),
                date = date,
                distanceKm = distanceKm,
                fuelCost = fuelCost,
                tollCost = tollCost,
                parkingCost = parkingCost,
                foodCost = foodCost,
                otherCost = otherCost,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun delete(id: String) = dao.deleteById(id)
    suspend fun deleteForVehicle(vehicleId: String) = dao.deleteForVehicle(vehicleId)
}

private fun TripEntity.toDomain() = Trip(
    id, vehicleId, name, date, distanceKm, fuelCost, tollCost, parkingCost, foodCost, otherCost,
)
