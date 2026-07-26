package com.songsit.fuellogpro.ui.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songsit.fuellogpro.data.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VehiclesViewModel(
    private val repository: VehicleRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(VehiclesUiState())
    val uiState: StateFlow<VehiclesUiState> = mutableUiState.asStateFlow()
    private val restoredUsers = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            repository.observeVehicles().collectLatest { vehicles ->
                mutableUiState.update { it.copy(vehicles = vehicles) }
            }
        }
    }

    fun onSignedIn(uid: String) {
        if (!restoredUsers.add(uid)) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(loading = true, errorMessage = null) }
            runCatching { repository.restoreOwnedVehicles(uid) }
                .onSuccess { count ->
                    mutableUiState.update {
                        it.copy(loading = false, restoredCount = count)
                    }
                }
                .onFailure { error ->
                    restoredUsers.remove(uid)
                    mutableUiState.update {
                        it.copy(loading = false, errorMessage = error.message)
                    }
                }
        }
    }
}

