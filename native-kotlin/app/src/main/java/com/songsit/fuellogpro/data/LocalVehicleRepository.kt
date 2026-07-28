package com.songsit.fuellogpro.data

import com.songsit.fuellogpro.data.local.VehicleDao
import com.songsit.fuellogpro.data.local.VehicleEntity
import com.songsit.fuellogpro.domain.model.Vehicle
import com.songsit.fuellogpro.domain.model.VehicleFormValues
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalVehicleRepository(
    private val dao: VehicleDao,
) {
    fun observe(): Flow<List<Vehicle>> =
        dao.observeAll().map { vehicles -> vehicles.map(VehicleEntity::toDomain) }

    suspend fun add(values: VehicleFormValues): String {
        val id = UUID.randomUUID().toString()
        dao.upsert(values.toEntity(id, createdAt = System.currentTimeMillis()))
        return id
    }

    suspend fun update(id: String, values: VehicleFormValues) {
        val createdAt = dao.getById(id)?.createdAt ?: System.currentTimeMillis()
        dao.upsert(values.toEntity(id, createdAt))
    }

    suspend fun delete(id: String) = dao.deleteById(id)
}

private fun VehicleFormValues.toEntity(id: String, createdAt: Long) = VehicleEntity(
    id = id,
    name = name.trim(),
    registration = registration.trim(),
    fuelType = fuelType.trim(),
    createdAt = createdAt,
    imageUri = imageUri,
    distanceUnit = distanceUnit,
    volumeUnit = volumeUnit,
    consumptionUnit = consumptionUnit,
    hasDualTank = hasDualTank,
    tankCapacity = tankCapacity,
    vin = vin.trim(),
    insurance = insurance.trim(),
    isActive = isActive,
    brand = brand.trim(),
    model = model.trim(),
    modelYear = modelYear,
)

private fun VehicleEntity.toDomain() = Vehicle(
    id = id,
    name = name,
    registration = registration,
    fuelType = fuelType,
    imageUri = imageUri,
    distanceUnit = distanceUnit,
    volumeUnit = volumeUnit,
    consumptionUnit = consumptionUnit,
    hasDualTank = hasDualTank,
    tankCapacity = tankCapacity,
    vin = vin,
    insurance = insurance,
    isActive = isActive,
    brand = brand,
    model = model,
    modelYear = modelYear,
)
