package com.songsit.fuellogpro.domain.model

data class Vehicle(
    val id: String,
    val name: String,
    val registration: String = "",
    val fuelType: String = "",
    val imageUri: String? = null,
    val distanceUnit: String = "km",
    val volumeUnit: String = "L",
    val consumptionUnit: String = "km/l",
    val hasDualTank: Boolean = false,
    val tankCapacity: Double? = null,
    val vin: String = "",
    val insurance: String = "",
    val isActive: Boolean = true,
    val brand: String = "",
    val model: String = "",
    val modelYear: Int? = null,
)

// Bundles the vehicle form's editable fields into one value so add/update calls don't need
// a 12-parameter positional lambda.
data class VehicleFormValues(
    val name: String,
    val registration: String,
    val fuelType: String,
    val imageUri: String?,
    val distanceUnit: String,
    val volumeUnit: String,
    val consumptionUnit: String,
    val hasDualTank: Boolean,
    val tankCapacity: Double?,
    val vin: String,
    val insurance: String,
    val isActive: Boolean,
    val brand: String = "",
    val model: String = "",
    val modelYear: Int? = null,
)
