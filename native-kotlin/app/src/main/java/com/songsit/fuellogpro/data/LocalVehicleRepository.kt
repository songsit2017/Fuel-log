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
    private val deletionRecorder: LocalDeletionRecorder,
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

    // Tombstoned like every other record type (see LocalDeletionRecorder) — the vehicle IS its
    // own "parent", so vehicleId and recordId are the same id. Without this, a deleted vehicle
    // was only removed from the local Room row; the next sync() still found it in Firestore
    // (untouched) and re-downloaded it as a "cloud-only" vehicle, resurrecting it after every
    // app restart.
    suspend fun delete(id: String) = deletionRecorder.delete(
        collectionName = "vehicles",
        recordId = id,
        vehicleId = { id },
        deleteLocal = { dao.deleteById(id) },
    )
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
    brandLogoUrl = brandLogoUrl,
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
    brandLogoUrl = brandLogoUrl,
)
