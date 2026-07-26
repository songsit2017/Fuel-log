package com.songsit.fuellogpro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.songsit.fuellogpro.data.LocalFuelRepository
import com.songsit.fuellogpro.data.LocalVehicleRepository
import com.songsit.fuellogpro.domain.FuelSummary
import com.songsit.fuellogpro.domain.calculateFuelSummary
import com.songsit.fuellogpro.domain.model.FuelEntry
import com.songsit.fuellogpro.domain.model.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NativeAppState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleId: String? = null,
    val entries: List<FuelEntry> = emptyList(),
    val summary: FuelSummary = calculateFuelSummary(emptyList()),
    val saving: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedVehicle: Vehicle?
        get() = vehicles.firstOrNull { it.id == selectedVehicleId } ?: vehicles.firstOrNull()
}

class NativeAppViewModel(
    private val fuelRepository: LocalFuelRepository,
    private val vehicleRepository: LocalVehicleRepository,
) : ViewModel() {
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val selectedVehicleId = MutableStateFlow<String?>(null)
    private val vehicles = vehicleRepository.observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val entries = selectedVehicleId.flatMapLatest { vehicleId ->
        vehicleId?.let(fuelRepository::observe) ?: flowOf(emptyList())
    }

    val state: StateFlow<NativeAppState> = combine(
        vehicles,
        selectedVehicleId,
        entries,
        saving,
        error,
    ) { vehicleList, selectedId, fuelEntries, isSaving, message ->
        val validId = selectedId?.takeIf { id -> vehicleList.any { it.id == id } }
            ?: vehicleList.firstOrNull()?.id
        if (validId != selectedId) selectedVehicleId.value = validId
        NativeAppState(
            vehicles = vehicleList,
            selectedVehicleId = validId,
            entries = if (validId == selectedId) fuelEntries else emptyList(),
            summary = calculateFuelSummary(if (validId == selectedId) fuelEntries else emptyList()),
            saving = isSaving,
            errorMessage = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NativeAppState())

    fun selectVehicle(id: String) {
        selectedVehicleId.value = id
    }

    fun addVehicle(name: String, registration: String, fuelType: String, onSaved: () -> Unit) {
        if (name.isBlank()) {
            error.value = "กรุณาระบุชื่อรถ"
            return
        }
        viewModelScope.launch {
            saving.value = true
            error.value = null
            runCatching { vehicleRepository.add(name, registration, fuelType) }
                .onSuccess {
                    selectedVehicleId.value = it
                    onSaved()
                }
                .onFailure { error.value = it.message ?: "บันทึกรถไม่สำเร็จ" }
            saving.value = false
        }
    }

    fun deleteVehicle(id: String) {
        viewModelScope.launch {
            runCatching {
                fuelRepository.deleteForVehicle(id)
                vehicleRepository.delete(id)
            }.onFailure { error.value = it.message ?: "ลบรถไม่สำเร็จ" }
        }
    }

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
        val vehicleId = state.value.selectedVehicle?.id
        if (vehicleId == null) {
            error.value = "กรุณาเพิ่มรถก่อนบันทึกการเติมน้ำมัน"
            return
        }
        if (odometerKm <= 0 || liters <= 0 || pricePerLiter <= 0) {
            error.value = "กรุณากรอกเลขไมล์ ลิตร และราคาให้ถูกต้อง"
            return
        }
        viewModelScope.launch {
            saving.value = true
            error.value = null
            runCatching {
                fuelRepository.add(
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
            runCatching { fuelRepository.delete(id) }
                .onFailure { error.value = it.message ?: "ลบไม่สำเร็จ" }
        }
    }

    fun clearError() {
        error.value = null
    }
}

class NativeAppViewModelFactory(
    private val fuelRepository: LocalFuelRepository,
    private val vehicleRepository: LocalVehicleRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NativeAppViewModel(fuelRepository, vehicleRepository) as T
}
