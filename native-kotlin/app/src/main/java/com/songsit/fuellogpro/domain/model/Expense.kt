package com.songsit.fuellogpro.domain.model

data class Expense(
    val id: String,
    val vehicleId: String,
    val date: String,
    val category: String,
    val description: String,
    val amount: Double,
    val odometerKm: Double?,
    val income: Boolean,
    val recurring: Boolean,
    val reminderDate: String?,
    val photoUri: String? = null,
    val photoUrls: List<String> = emptyList(),
)
