package com.songsit.fuellogpro.domain.model

data class FuelEntry(
    val id: String,
    val vehicleId: String,
    val date: String,
    val time: String,
    val odometerKm: Double,
    val liters: Double,
    val pricePerLiter: Double,
    val amount: Double,
    val fullTank: Boolean,
    val station: String = "",
    val driver: String = "",
    val paymentMethod: String = "",
    val photoUrls: List<String> = emptyList(),
    val photoUri: String? = null,
    val odometerIsTripMeter: Boolean = false,
    val tankLevelEnabled: Boolean = false,
    val tankLevelTiming: String = "after",
    val tankLevelPercent: Double? = null,
    val tankLevelLiters: Double? = null,
    val discountEnabled: Boolean = false,
    val discountAmount: Double = 0.0,
    val discountPerLiter: Boolean = false,
    val missedPreviousFillUp: Boolean = false,
    val weatherDescription: String? = null,
    val weatherTemperatureC: Double? = null,
    val weatherLatitude: Double? = null,
    val weatherLongitude: Double? = null,
)

// Bundles the "add/edit fill-up" form's editable fields into one value so add/update calls
// don't need to keep growing an already-long positional lambda (mirrors VehicleFormValues).
data class FuelEntryFormValues(
    val date: String,
    val time: String,
    val odometerKm: Double,
    val liters: Double,
    val pricePerLiter: Double,
    // The form's "ราคารวม" field as the user last committed it — trusted as-is rather than
    // re-derived from liters * pricePerLiter, since both of those are independently rounded to
    // 2 decimals and multiplying them back out drifts a few satang from what was actually typed
    // (e.g. typing total "1000" landed as 999.98 after a liters/price round-trip).
    val grossAmount: Double,
    val fullTank: Boolean,
    val station: String,
    val paymentMethod: String = "",
    val photoUri: String?,
    val odometerIsTripMeter: Boolean = false,
    val tankLevelEnabled: Boolean = false,
    val tankLevelTiming: String = "after",
    val tankLevelPercent: Double? = null,
    val tankLevelLiters: Double? = null,
    val discountEnabled: Boolean = false,
    val discountAmount: Double = 0.0,
    val discountPerLiter: Boolean = false,
    val missedPreviousFillUp: Boolean = false,
    val weatherDescription: String? = null,
    val weatherTemperatureC: Double? = null,
    val weatherLatitude: Double? = null,
    val weatherLongitude: Double? = null,
)
