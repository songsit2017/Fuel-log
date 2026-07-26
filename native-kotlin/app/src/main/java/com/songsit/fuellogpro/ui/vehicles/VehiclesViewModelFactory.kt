package com.songsit.fuellogpro.ui.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.songsit.fuellogpro.data.VehicleRepository

class VehiclesViewModelFactory(
    private val repository: VehicleRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        VehiclesViewModel(repository) as T
}

