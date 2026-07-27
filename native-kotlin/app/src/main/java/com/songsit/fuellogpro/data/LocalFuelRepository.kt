package com.songsit.fuellogpro.data

import com.songsit.fuellogpro.data.local.FuelEntryDao
import com.songsit.fuellogpro.data.local.FuelEntryEntity
import com.songsit.fuellogpro.data.local.PhotoUris
import com.songsit.fuellogpro.domain.model.FuelEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalFuelRepository(
    private val dao: FuelEntryDao,
    private val deletionRecorder: LocalDeletionRecorder,
) {
    fun observe(vehicleId: String): Flow<List<FuelEntry>> =
        dao.observeForVehicle(vehicleId).map { entries -> entries.map(FuelEntryEntity::toDomain) }

    suspend fun add(
        vehicleId: String,
        date: String,
        time: String,
        odometerKm: Double,
        liters: Double,
        pricePerLiter: Double,
        fullTank: Boolean,
        station: String,
        photoUri: String? = null,
    ) {
        dao.upsert(
            FuelEntryEntity(
                id = UUID.randomUUID().toString(),
                vehicleId = vehicleId,
                date = date,
                time = time,
                odometerKm = odometerKm,
                liters = liters,
                pricePerLiter = pricePerLiter,
                amount = liters * pricePerLiter,
                fullTank = fullTank,
                station = station.trim(),
                createdAt = System.currentTimeMillis(),
                photoUri = photoUri,
            ),
        )
    }

    suspend fun update(
        id: String,
        vehicleId: String,
        date: String,
        time: String,
        odometerKm: Double,
        liters: Double,
        pricePerLiter: Double,
        fullTank: Boolean,
        station: String,
        photoUri: String? = null,
    ) {
        val existing = dao.getById(id)
        dao.upsert(
            FuelEntryEntity(
                id = id,
                vehicleId = vehicleId,
                date = date,
                time = time,
                odometerKm = odometerKm,
                liters = liters,
                pricePerLiter = pricePerLiter,
                amount = liters * pricePerLiter,
                fullTank = fullTank,
                station = station.trim(),
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                photoUri = photoUri ?: existing?.photoUri,
            ),
        )
    }

    suspend fun delete(id: String) = deletionRecorder.delete(
        collectionName = "entries",
        recordId = id,
        vehicleId = { dao.getById(id)?.vehicleId },
        deleteLocal = { dao.deleteById(id) },
    )

    suspend fun deleteForVehicle(vehicleId: String) = dao.deleteForVehicle(vehicleId)
}

private fun FuelEntryEntity.toDomain() = FuelEntry(
    id = id,
    vehicleId = vehicleId,
    date = date,
    time = time,
    odometerKm = odometerKm,
    liters = liters,
    pricePerLiter = pricePerLiter,
    amount = amount,
    fullTank = fullTank,
    station = station,
    photoUri = photoUri,
    photoUrls = PhotoUris.split(photoUri),
)
