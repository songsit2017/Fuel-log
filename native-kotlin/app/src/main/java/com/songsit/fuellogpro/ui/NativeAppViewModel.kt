package com.songsit.fuellogpro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.songsit.fuellogpro.data.LocalFuelRepository
import com.songsit.fuellogpro.data.LocalExpenseRepository
import com.songsit.fuellogpro.data.LocalVehicleRepository
import com.songsit.fuellogpro.data.LocalMaintenanceRepository
import com.songsit.fuellogpro.data.LocalTripRepository
import com.songsit.fuellogpro.domain.FuelSummary
import com.songsit.fuellogpro.domain.calculateFuelSummary
import com.songsit.fuellogpro.domain.calculateExpenseSummary
import com.songsit.fuellogpro.domain.model.FuelEntry
import com.songsit.fuellogpro.domain.model.Expense
import com.songsit.fuellogpro.domain.model.Vehicle
import com.songsit.fuellogpro.domain.model.MaintenanceTask
import com.songsit.fuellogpro.domain.TripSummary
import com.songsit.fuellogpro.domain.calculateTripSummary
import com.songsit.fuellogpro.domain.model.Trip
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
    val trips: List<Trip> = emptyList(),
    val summary: FuelSummary = calculateFuelSummary(emptyList()),
    val expenseCategorySuggestions: List<String> = emptyList(),
    val maintenanceCategorySuggestions: List<String> = emptyList(),
    val saving: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedVehicle: Vehicle?
        get() = vehicles.firstOrNull { it.id == selectedVehicleId } ?: vehicles.firstOrNull()
    val totalExpenses: Double
        get() = calculateExpenseSummary(expenses).totalExpense
    val totalIncome: Double
        get() = calculateExpenseSummary(expenses).totalIncome
    val netExpense: Double
        get() = calculateExpenseSummary(expenses).netExpense
    val tripSummary: TripSummary
        get() = calculateTripSummary(trips)
}

private data class VehicleRecords(
    val fuelEntries: List<FuelEntry>,
    val expenses: List<Expense>,
    val maintenanceTasks: List<MaintenanceTask>,
    val trips: List<Trip>,
)

private data class Suggestions(
    val expenseCategories: List<String>,
    val maintenanceCategories: List<String>,
)

class NativeAppViewModel(
    private val fuelRepository: LocalFuelRepository,
    private val vehicleRepository: LocalVehicleRepository,
    private val expenseRepository: LocalExpenseRepository,
    private val maintenanceRepository: LocalMaintenanceRepository,
    private val tripRepository: LocalTripRepository,
    private val onReminderDataChanged: () -> Unit,
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
    private val trips = selectedVehicleId.flatMapLatest { vehicleId ->
        vehicleId?.let(tripRepository::observe) ?: flowOf(emptyList())
    }
    private val records = combine(entries, expenses, maintenanceTasks, trips) {
            fuelEntries, vehicleExpenses, tasks, vehicleTrips ->
        VehicleRecords(fuelEntries, vehicleExpenses, tasks, vehicleTrips)
    }
    private val expenseCategorySuggestions = selectedVehicleId.flatMapLatest { vehicleId ->
        vehicleId?.let(expenseRepository::observeCategorySuggestions) ?: flowOf(emptyList())
    }
    private val maintenanceCategorySuggestions = selectedVehicleId.flatMapLatest { vehicleId ->
        vehicleId?.let(maintenanceRepository::observeCategorySuggestions) ?: flowOf(emptyList())
    }
    private val suggestions = combine(
        expenseCategorySuggestions,
        maintenanceCategorySuggestions,
    ) { expenseCategories, maintenanceCategories ->
        Suggestions(expenseCategories, maintenanceCategories)
    }

    val state: StateFlow<NativeAppState> = combine(
        vehicles,
        selectedVehicleId,
        records,
        suggestions,
        saving,
        error,
    ) { values ->
        val vehicleList = values[0] as List<Vehicle>
        val selectedId = values[1] as String?
        val recordsForVehicle = values[2] as VehicleRecords
        val suggestionsForVehicle = values[3] as Suggestions
        val isSaving = values[4] as Boolean
        val message = values[5] as String?
        val (fuelEntries, vehicleExpenses, tasks, vehicleTrips) = recordsForVehicle
        val validId = selectedId?.takeIf { id -> vehicleList.any { it.id == id } }
            ?: vehicleList.firstOrNull()?.id
        if (validId != selectedId) selectedVehicleId.value = validId
        NativeAppState(
            vehicles = vehicleList,
            selectedVehicleId = validId,
            entries = if (validId == selectedId) fuelEntries else emptyList(),
            expenses = if (validId == selectedId) vehicleExpenses else emptyList(),
            maintenanceTasks = if (validId == selectedId) tasks else emptyList(),
            trips = if (validId == selectedId) vehicleTrips else emptyList(),
            summary = calculateFuelSummary(if (validId == selectedId) fuelEntries else emptyList()),
            expenseCategorySuggestions = if (validId == selectedId) suggestionsForVehicle.expenseCategories else emptyList(),
            maintenanceCategorySuggestions = if (validId == selectedId) suggestionsForVehicle.maintenanceCategories else emptyList(),
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
                tripRepository.deleteForVehicle(id)
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
        photoUri: String? = null,
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
                    photoUri = photoUri,
                )
            }.onSuccess {
                onReminderDataChanged()
                onSaved()
            }
                .onFailure { error.value = it.message ?: "บันทึกไม่สำเร็จ" }
            saving.value = false
        }
    }

    fun updateFuel(
        id: String,
        date: String,
        time: String,
        odometerKm: Double,
        liters: Double,
        pricePerLiter: Double,
        fullTank: Boolean,
        station: String,
        photoUri: String? = null,
        onSaved: () -> Unit,
    ) {
        val vehicleId = state.value.entries.firstOrNull { it.id == id }?.vehicleId
            ?: state.value.selectedVehicle?.id
        if (vehicleId == null) {
            error.value = "ไม่พบรถสำหรับรายการนี้"
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
                fuelRepository.update(id, vehicleId, date, time, odometerKm, liters, pricePerLiter, fullTank, station, photoUri)
            }.onSuccess {
                onReminderDataChanged()
                onSaved()
            }.onFailure { error.value = it.message ?: "แก้ไขไม่สำเร็จ" }
            saving.value = false
        }
    }

    fun deleteFuel(id: String) {
        viewModelScope.launch {
            runCatching { fuelRepository.delete(id) }
                .onSuccess { onReminderDataChanged() }
                .onFailure { error.value = it.message ?: "ลบไม่สำเร็จ" }
        }
    }

    fun addExpense(
        date: String,
        category: String,
        description: String,
        amount: Double,
        odometerKm: Double?,
        income: Boolean,
        recurring: Boolean,
        reminderDate: String?,
        photoUri: String? = null,
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
                expenseRepository.add(
                    vehicleId,
                    date,
                    category,
                    description,
                    amount,
                    odometerKm,
                    income,
                    recurring,
                    reminderDate,
                    photoUri,
                )
            }.onSuccess {
                onReminderDataChanged()
                onSaved()
            }
                .onFailure { error.value = it.message ?: "บันทึกค่าใช้จ่ายไม่สำเร็จ" }
            saving.value = false
        }
    }

    fun updateExpense(
        id: String,
        date: String,
        category: String,
        description: String,
        amount: Double,
        odometerKm: Double?,
        income: Boolean,
        recurring: Boolean,
        reminderDate: String?,
        photoUri: String? = null,
        onSaved: () -> Unit,
    ) {
        val vehicleId = state.value.expenses.firstOrNull { it.id == id }?.vehicleId
            ?: state.value.selectedVehicle?.id
        if (vehicleId == null) {
            error.value = "ไม่พบรถสำหรับรายการนี้"
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
                expenseRepository.update(id, vehicleId, date, category, description, amount, odometerKm, income, recurring, reminderDate, photoUri)
            }.onSuccess {
                onReminderDataChanged()
                onSaved()
            }.onFailure { error.value = it.message ?: "แก้ไขค่าใช้จ่ายไม่สำเร็จ" }
            saving.value = false
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            runCatching { expenseRepository.delete(id) }
                .onSuccess { onReminderDataChanged() }
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
            }.onSuccess {
                onReminderDataChanged()
                onSaved()
            }
                .onFailure { error.value = it.message ?: "บันทึกรายการเตือนไม่สำเร็จ" }
            saving.value = false
        }
    }

    fun updateMaintenance(
        id: String,
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
        val vehicleId = state.value.maintenanceTasks.firstOrNull { it.id == id }?.vehicleId
            ?: state.value.selectedVehicle?.id
        if (vehicleId == null) {
            error.value = "ไม่พบรถสำหรับรายการนี้"
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
                maintenanceRepository.update(
                    id = id,
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
            }.onSuccess {
                onReminderDataChanged()
                onSaved()
            }.onFailure { error.value = it.message ?: "แก้ไขรายการเตือนไม่สำเร็จ" }
            saving.value = false
        }
    }

    fun completeMaintenance(id: String) {
        val task = state.value.maintenanceTasks.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            runCatching {
                maintenanceRepository.markDone(task, state.value.summary.latestOdometerKm)
            }.onSuccess { onReminderDataChanged() }
                .onFailure { error.value = it.message ?: "อัปเดตรายการไม่สำเร็จ" }
        }
    }

    fun deleteMaintenance(id: String) {
        viewModelScope.launch {
            runCatching { maintenanceRepository.delete(id) }
                .onSuccess { onReminderDataChanged() }
                .onFailure { error.value = it.message ?: "ลบรายการเตือนไม่สำเร็จ" }
        }
    }

    fun addTrip(
        name: String,
        date: String,
        distanceKm: Double,
        fuelCost: Double,
        tollCost: Double,
        parkingCost: Double,
        foodCost: Double,
        otherCost: Double,
        onSaved: () -> Unit,
    ) {
        val vehicleId = state.value.selectedVehicle?.id
        if (vehicleId == null) {
            error.value = "กรุณาเพิ่มรถก่อนบันทึกทริป"
            return
        }
        if (name.isBlank() || distanceKm < 0) {
            error.value = "กรุณาระบุชื่อทริปและระยะทางให้ถูกต้อง"
            return
        }
        val costs = listOf(fuelCost, tollCost, parkingCost, foodCost, otherCost)
        if (costs.any { it < 0 }) {
            error.value = "ค่าใช้จ่ายของทริปต้องไม่ติดลบ"
            return
        }
        viewModelScope.launch {
            saving.value = true
            error.value = null
            runCatching {
                tripRepository.add(
                    vehicleId, name, date, distanceKm, fuelCost,
                    tollCost, parkingCost, foodCost, otherCost,
                )
            }.onSuccess { onSaved() }
                .onFailure { error.value = it.message ?: "บันทึกทริปไม่สำเร็จ" }
            saving.value = false
        }
    }

    fun updateTrip(
        id: String,
        name: String,
        date: String,
        distanceKm: Double,
        fuelCost: Double,
        tollCost: Double,
        parkingCost: Double,
        foodCost: Double,
        otherCost: Double,
        onSaved: () -> Unit,
    ) {
        val vehicleId = state.value.trips.firstOrNull { it.id == id }?.vehicleId
            ?: state.value.selectedVehicle?.id
        if (vehicleId == null) {
            error.value = "ไม่พบรถสำหรับทริปนี้"
            return
        }
        if (name.isBlank() || distanceKm < 0) {
            error.value = "กรุณาระบุชื่อทริปและระยะทางให้ถูกต้อง"
            return
        }
        viewModelScope.launch {
            saving.value = true
            error.value = null
            runCatching {
                tripRepository.update(id, vehicleId, name, date, distanceKm, fuelCost, tollCost, parkingCost, foodCost, otherCost)
            }.onSuccess { onSaved() }
                .onFailure { error.value = it.message ?: "แก้ไขทริปไม่สำเร็จ" }
            saving.value = false
        }
    }

    fun deleteTrip(id: String) {
        viewModelScope.launch {
            runCatching { tripRepository.delete(id) }
                .onFailure { error.value = it.message ?: "ลบทริปไม่สำเร็จ" }
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
    private val tripRepository: LocalTripRepository,
    private val onReminderDataChanged: () -> Unit,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NativeAppViewModel(
            fuelRepository,
            vehicleRepository,
            expenseRepository,
            maintenanceRepository,
            tripRepository,
            onReminderDataChanged,
        ) as T
}
