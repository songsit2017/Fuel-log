package com.songsit.fuellogpro.ui.vehicles

import com.songsit.fuellogpro.domain.model.Vehicle

data class VehiclesUiState(
    val loading: Boolean = false,
    val vehicles: List<Vehicle> = emptyList(),
    val restoredCount: Int = 0,
    val errorMessage: String? = null,
)

