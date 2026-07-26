package com.songsit.fuellogpro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.songsit.fuellogpro.data.LocalFuelRepository
import com.songsit.fuellogpro.domain.FuelSummary
import com.songsit.fuellogpro.domain.calculateFuelSummary
import com.songsit.fuellogpro.domain.model.FuelEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NativeAppState(
    val vehicleName: String = "รถของฉัน",
    val entries: List<FuelEntry> = emptyList(),
    val summary: FuelSummary = calculateFuelSummary(emptyList()),
    val saving: Boolean = false,
    val errorMessage: String? = null,
)

class NativeAppViewModel(
    private val repository: LocalFuelRepository,
) : ViewModel() {
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val vehicleId = "local-default"

    val state: StateFlow<NativeAppState> = combine(
        repository.observe(vehicleId),
        saving,
        error,
    ) { entries, isSaving, message ->
        NativeAppState(
            entries = entries,
            summary = calculateFuelSummary(entries),
            saving = isSaving,
            errorMessage = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NativeAppState())

    fun addFuel(
        date: String,
        time: String,
        odometerKm: Double,
        liters: Double,
        pricePerLiter: Double,
        fullTank: Boolean,
        station: String,
        onSaved: () -> Unit,
    ) {
        if (odometerKm <= 0 || liters <= 0 || pricePerLiter <= 0) {
            error.value = "กรุณากรอกเลขไมล์ ลิตร และราคาให้ถูกต้อง"
            return
        }
        viewModelScope.launch {
            saving.value = true
            error.value = null
            runCatching {
                repository.add(
                    vehicleId = vehicleId,
                    date = date,
                    time = time,
                    odometerKm = odometerKm,
                    liters = liters,
                    pricePerLiter = pricePerLiter,
                    fullTank = fullTank,
                    station = station,
                )
            }.onSuccess { onSaved() }
                .onFailure { error.value = it.message ?: "บันทึกไม่สำเร็จ" }
            saving.value = false
        }
    }

    fun deleteFuel(id: String) {
        viewModelScope.launch {
            runCatching { repository.delete(id) }
                .onFailure { error.value = it.message ?: "ลบไม่สำเร็จ" }
        }
    }

    fun clearError() {
        error.value = null
    }
}

class NativeAppViewModelFactory(
    private val repository: LocalFuelRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NativeAppViewModel(repository) as T
}
