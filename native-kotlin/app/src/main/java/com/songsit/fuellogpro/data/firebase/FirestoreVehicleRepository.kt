package com.songsit.fuellogpro.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.songsit.fuellogpro.data.VehicleRepository
import com.songsit.fuellogpro.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await

class FirestoreVehicleRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : VehicleRepository {
    private val vehicles = MutableStateFlow<List<Vehicle>>(emptyList())

    override fun observeVehicles(): Flow<List<Vehicle>> = vehicles

    override suspend fun restoreOwnedVehicles(uid: String): Int {
        val snapshot = firestore.collection("vehicles")
            .whereEqualTo("ownerUid", uid)
            .get()
            .await()
        vehicles.value = snapshot.documents.map { document ->
            Vehicle(
                id = document.id,
                name = document.getString("name") ?: "รถจาก Cloud",
                registration = document.getString("registration").orEmpty(),
                fuelType = document.getString("fuelType").orEmpty(),
                imageUri = document.getString("imageUri"),
                distanceUnit = document.getString("distanceUnit") ?: "km",
                volumeUnit = document.getString("volumeUnit") ?: "L",
                consumptionUnit = document.getString("consumptionUnit") ?: "km/l",
                hasDualTank = document.getBoolean("hasDualTank") ?: false,
                tankCapacity = document.getDouble("tankCapacity"),
                vin = document.getString("vin").orEmpty(),
                insurance = document.getString("insurance").orEmpty(),
                isActive = document.getBoolean("isActive") ?: true,
            )
        }
        return vehicles.value.size
    }
}
