package com.songsit.fuellogpro.data

import com.songsit.fuellogpro.data.local.VehicleDao
import com.songsit.fuellogpro.data.local.VehicleEntity
import com.songsit.fuellogpro.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalVehicleRepository(
    private val dao: VehicleDao,
) {
    fun observe(): Flow<List<Vehicle>> =
        dao.observeAll().map { vehicles -> vehicles.map(VehicleEntity::toDomain) }

    suspend fun add(name: String, registration: String, fuelType: String): String {
        val id = UUID.randomUUID().toString()
        dao.upsert(
            VehicleEntity(
                id = id,
                name = name.trim(),
                registration = registration.trim(),
                fuelType = fuelType.trim(),
                createdAt = System.currentTimeMillis(),
            ),
        )
        return id
    }

    suspend fun delete(id: String) = dao.deleteById(id)
}

private fun VehicleEntity.toDomain() = Vehicle(
    id = id,
    name = name,
    registration = registration,
    fuelType = fuelType,
)
