package com.songsit.fuellogpro.data

import com.songsit.fuellogpro.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    fun observeVehicles(): Flow<List<Vehicle>>
    suspend fun restoreOwnedVehicles(uid: String): Int
}

