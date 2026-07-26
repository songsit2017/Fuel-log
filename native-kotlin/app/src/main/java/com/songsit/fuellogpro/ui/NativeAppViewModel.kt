package com.songsit.fuellogpro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.songsit.fuellogpro.data.LocalFuelRepository
import com.songsit.fuellogpro.data.LocalExpenseRepository
import com.songsit.fuellogpro.data.LocalVehicleRepository
import com.songsit.fuellogpro.data.LocalMaintenanceRepository
import com.songsit.fuellogpro.domain.FuelSummary
import com.songsit.fuellogpro.domain.calculateFuelSummary
import com.songsit.fuellogpro.domain.model.FuelEntry
import com.songsit.fuellogpro.domain.model.Expense
import com.songsit.fuellogpro.domain.model.Vehicle
import com.songsit.fuellogpro.domain.model.MaintenanceTask
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
    val expenses: List<Expense> = emptyList(),
    val maintenanceTasks: List<MaintenanceTask> = emptyList(),
    val summary: FuelSummary = calculateFuelSummary(emptyList()),
    val saving: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedVehicle: Vehicle?
        get() = vehicles.firstOrNull { it.id == selectedVehicleId } ?: vehicles.firstOrNull()
    val totalExpenses: Double
        get() = expenses.sumOf(Expense::amount)
}

class NativeAppViewModel(
    private val fuelRepository: LocalFuelRepository,
    private val vehicleRepository: LocalVehicleRepository,
    private val expenseRepository: LocalExpenseRepository,
    private val maintenanceRepository: LocalMaintenanceRepository,
) : ViewModel() {
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val selectedVehicleId = MutableStateFlow<String?>(null)
    private val vehicles = vehicleRepository.observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val entries = selectedVehicleId.flatMapLatest { vehicleId ->
        vehicleId?.let(fuelRepository::observe) ?: flowOf(emptyList())
    }
    private val expenses = selectedVehicleId.flatMapLatest { vehicleId ->
        vehicleId?.let(expenseRepository::observe) ?: flowOf(emptyList())
    }
    private val maintenanceTasks = selectedVehicleId.flatMapLatest { vehicleId ->
        vehicleId?.let(maintenanceRepository::observe) ?: flowOf(emptyList())
    }
    private val records = combine(entries, expenses, maintenanceTasks) { fuelEntries, vehicleExpenses, tasks ->
        Triple(fuelEntries, vehicleExpenses, tasks)
    }

    val state: StateFlow<NativeAppState> = combine(
        vehicles,
        selectedVehicleId,
        records,
        saving,
        error,
    ) { vehicleList, selectedId, recordsForVehicle, isSaving, message ->
        val (fuelEntries, vehicleExpenses, tasks) = recordsForVehicle
        val validId = selectedId?.takeIf { id -> vehicleList.any { it.id == id } }
            ?: vehicleList.firstOrNull()?.id
        if (validId != selectedId) selectedVehicleId.value = validId
        NativeAppState(
            vehicles = vehicleList,
            selectedVehicleId = validId,
            entries = if (validId == selectedId) fuelEntries else emptyList(),
            expenses = if (validId == selectedId) vehicleExpenses else emptyList(),
            maintenanceTasks = if (validId == selectedId) tasks else emptyList(),
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
                expenseRepository.deleteForVehicle(id)
                maintenanceRepository.deleteForVehicle(id)
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

    fun addExpense(
        date: String,
        category: String,
        description: String,
        amount: Double,
        odometerKm: Double?,
        onSaved: () -> Unit,
    ) {
        val vehicleId = state.value.selectedVehicle?.id
        if (vehicleId == null) {
            error.value = "กรุณาเพิ่มรถก่อนบันทึกค่าใช้จ่าย"
            return
        }
        if (amount <= 0 || category.isBlank()) {
            error.value = "กรุณาระบุหมวดหมู่และจำนวนเงินให้ถูกต้อง"
            return
        }
        viewModelScope.launch {
            saving.value = true
            error.value = null
            runCatching {
                expenseRepository.add(vehicleId, date, category, description, amount, odometerKm)
            }.onSuccess { onSaved() }
                .onFailure { error.value = it.message ?: "บันทึกค่าใช้จ่ายไม่สำเร็จ" }
            saving.value = false
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            runCatching { expenseRepository.delete(id) }
                .onFailure { error.value = it.message ?: "ลบค่าใช้จ่ายไม่สำเร็จ" }
        }
    }

    fun addMaintenance(
        name: String,
        category: String,
        nextDate: String?,
        nextOdometerKm: Double?,
        warningDays: Int,
        warningOdometerKm: Double,
        repeatMonths: Int?,
        repeatOdometerKm: Double?,
        onSaved: () -> Unit,
    ) {
        val vehicleId = state.value.selectedVehicle?.id
        if (vehicleId == null) {
            error.value = "กรุณาเพิ่มรถก่อนสร้างรายการเตือน"
            return
        }
        if (name.isBlank() || (nextDate.isNullOrBlank() && nextOdometerKm == null)) {
            error.value = "กรุณาระบุรายการและกำหนดวันที่หรือเลขไมล์"
            return
        }
        viewModelScope.launch {
            saving.value = true
            error.value = null
            runCatching {
                maintenanceRepository.add(
                    vehicleId = vehicleId,
                    name = name,
                    category = category.ifBlank { "บำรุงรักษา" },
                    nextDate = nextDate,
                    nextOdometerKm = nextOdometerKm,
                    warningDays = warningDays.coerceAtLeast(0),
                    warningOdometerKm = warningOdometerKm.coerceAtLeast(0.0),
                    repeatMonths = repeatMonths?.takeIf { it > 0 },
                    repeatOdometerKm = repeatOdometerKm?.takeIf { it > 0 },
                )
            }.onSuccess { onSaved() }
                .onFailure { error.value = it.message ?: "บันทึกรายการเตือนไม่สำเร็จ" }
            saving.value = false
        }
    }

    fun completeMaintenance(id: String) {
        val task = state.value.maintenanceTasks.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            runCatching {
                maintenanceRepository.markDone(task, state.value.summary.latestOdometerKm)
            }.onFailure { error.value = it.message ?: "อัปเดตรายการไม่สำเร็จ" }
        }
    }

    fun deleteMaintenance(id: String) {
        viewModelScope.launch {
            runCatching { maintenanceRepository.delete(id) }
                .onFailure { error.value = it.message ?: "ลบรายการเตือนไม่สำเร็จ" }
        }
    }

    fun clearError() {
        error.value = null
    }
}

class NativeAppViewModelFactory(
    private val fuelRepository: LocalFuelRepository,
    private val vehicleRepository: LocalVehicleRepository,
    private val expenseRepository: LocalExpenseRepository,
    private val maintenanceRepository: LocalMaintenanceRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NativeAppViewModel(
            fuelRepository,
            vehicleRepository,
            expenseRepository,
            maintenanceRepository,
        ) as T
}
