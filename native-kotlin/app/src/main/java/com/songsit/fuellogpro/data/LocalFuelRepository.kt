package com.songsit.fuellogpro.data

import com.songsit.fuellogpro.data.local.FuelEntryDao
import com.songsit.fuellogpro.data.local.FuelEntryEntity
import com.songsit.fuellogpro.data.local.PhotoUris
import com.songsit.fuellogpro.domain.model.FuelEntry
import com.songsit.fuellogpro.domain.model.FuelEntryFormValues
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
        values: FuelEntryFormValues,
        recordedByUid: String? = null,
        recordedByName: String? = null,
    ): String {
        val id = UUID.randomUUID().toString()
        dao.upsert(
            values.toEntity(
                id = id,
                vehicleId = vehicleId,
                createdAt = System.currentTimeMillis(),
                recordedByUid = recordedByUid,
                recordedByName = recordedByName,
            ),
        )
        return id
    }

    // recordedBy is never overwritten on edit — it stays whoever originally created the entry,
    // carried forward from the existing row same as fallbackPhotoUri below.
    suspend fun update(id: String, vehicleId: String, values: FuelEntryFormValues) {
        val existing = dao.getById(id)
        dao.upsert(
            values.toEntity(
                id = id,
                vehicleId = vehicleId,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                fallbackPhotoUri = existing?.photoUri,
                recordedByUid = existing?.recordedByUid,
                recordedByName = existing?.recordedByName,
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

// Discount can be a flat amount or a per-liter rate; net "amount" (used everywhere else in the
// app as the fill-up's spend) is always the gross total minus whichever discount was entered.
private fun FuelEntryFormValues.toEntity(
    id: String,
    vehicleId: String,
    createdAt: Long,
    fallbackPhotoUri: String? = null,
    recordedByUid: String? = null,
    recordedByName: String? = null,
): FuelEntryEntity {
    val discountTotal = if (discountEnabled) {
        if (discountPerLiter) discountAmount * liters else discountAmount
    } else {
        0.0
    }
    return FuelEntryEntity(
        id = id,
        vehicleId = vehicleId,
        date = date,
        time = time,
        odometerKm = odometerKm,
        liters = liters,
        pricePerLiter = pricePerLiter,
        amount = (grossAmount - discountTotal).coerceAtLeast(0.0),
        fullTank = fullTank,
        station = station.trim(),
        paymentMethod = paymentMethod.trim(),
        createdAt = createdAt,
        photoUri = photoUri ?: fallbackPhotoUri,
        odometerIsTripMeter = odometerIsTripMeter,
        tankLevelEnabled = tankLevelEnabled,
        tankLevelTiming = tankLevelTiming,
        tankLevelPercent = tankLevelPercent,
        tankLevelLiters = tankLevelLiters,
        discountEnabled = discountEnabled,
        discountAmount = discountAmount,
        discountPerLiter = discountPerLiter,
        missedPreviousFillUp = missedPreviousFillUp,
        weatherDescription = weatherDescription,
        weatherTemperatureC = weatherTemperatureC,
        weatherLatitude = weatherLatitude,
        weatherLongitude = weatherLongitude,
        driver = driver.trim(),
        recordedByUid = recordedByUid,
        recordedByName = recordedByName,
    )
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
    paymentMethod = paymentMethod,
    photoUri = photoUri,
    photoUrls = PhotoUris.split(photoUri),
    odometerIsTripMeter = odometerIsTripMeter,
    tankLevelEnabled = tankLevelEnabled,
    tankLevelTiming = tankLevelTiming,
    tankLevelPercent = tankLevelPercent,
    tankLevelLiters = tankLevelLiters,
    discountEnabled = discountEnabled,
    discountAmount = discountAmount,
    discountPerLiter = discountPerLiter,
    missedPreviousFillUp = missedPreviousFillUp,
    weatherDescription = weatherDescription,
    weatherTemperatureC = weatherTemperatureC,
    weatherLatitude = weatherLatitude,
    weatherLongitude = weatherLongitude,
    driver = driver,
    recordedByUid = recordedByUid,
    recordedByName = recordedByName,
)
