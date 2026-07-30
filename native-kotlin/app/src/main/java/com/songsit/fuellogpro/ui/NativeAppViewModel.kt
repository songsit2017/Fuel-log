package com.songsit.fuellogpro.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.songsit.fuellogpro.R
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
import com.songsit.fuellogpro.domain.calculateMaintenanceStatus
import com.songsit.fuellogpro.domain.checkFuelEfficiencyDrop
import com.songsit.fuellogpro.domain.DueLevel
import com.songsit.fuellogpro.domain.model.FuelEntry
import com.songsit.fuellogpro.domain.model.FuelEntryFormValues
import com.songsit.fuellogpro.domain.model.Expense
import com.songsit.fuellogpro.domain.model.Vehicle
import com.songsit.fuellogpro.domain.model.VehicleFormValues
import com.songsit.fuellogpro.domain.model.MaintenanceTask
import com.songsit.fuellogpro.domain.TripSummary
import com.songsit.fuellogpro.domain.calculateTripSummary
import com.songsit.fuellogpro.domain.model.Trip
import com.songsit.fuellogpro.notifications.FuelEfficiencyNotifier
import com.songsit.fuellogpro.notifications.NotificationPreferences
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
    private val context: Context,
    private val onReminderDataChanged: () -> Unit,
) : ViewModel() {
    private fun getString(resId: Int) = context.getString(resId)
    private val notificationPreferences = NotificationPreferences(context)
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

    fun addVehicle(values: VehicleFormValues, onSaved: () -> Unit) {
        if (values.name.isBlank()) {
            error.value = getString(R.string.error_vehicle_name_required)
            return
        }
        viewModelScope.launch {
            saving.value = true
            error.value = null
            runCatching { vehicleRepository.add(values) }
                .onSuccess {
                    selectedVehicleId.value = it
                    onSaved()
                }
                .onFailure { error.value = it.message ?: getString(R.string.error_save_vehicle_failed) }
            saving.value = false
        }
    }

    fun updateVehicle(id: String, values: VehicleFormValues, onSaved: () -> Unit) {
        if (values.name.isBlank()) {
            error.value = getString(R.string.error_vehicle_name_required)
            return
        }
        viewModelScope.launch {
            saving.value = true
            error.value = null
            runCatching { vehicleRepository.update(id, values) }
                .onSuccess { onSaved() }
                .onFailure { error.value = it.message ?: getString(R.string.error_save_vehicle_failed) }
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
            }.onFailure { error.value = it.message ?: getString(R.string.error_delete_vehicle_failed) }
        }
    }

    fun addFuel(values: FuelEntryFormValues, onSaved: () -> Unit) {
        val vehicleId = state.value.selectedVehicle?.id
        if (vehicleId == null) {
            error.value = getString(R.string.error_add_vehicle_before_fuel)
            return
        }
        if (values.odometerKm <= 0 || values.liters <= 0 || values.pricePerLiter <= 0) {
            error.value = getString(R.string.error_invalid_fuel_fields)
            return
        }
        val priorEntries = state.value.entries
        val maintenanceTasks = state.value.maintenanceTasks
        viewModelScope.launch {
            saving.value = true
            error.value = null
            runCatching { fuelRepository.add(vehicleId, values) }
                .onSuccess {
                    onReminderDataChanged()
                    checkFuelEfficiencyAlert(vehicleId, priorEntries, values, maintenanceTasks)
                    onSaved()
                }
                .onFailure { error.value = it.message ?: getString(R.string.error_save_failed) }
            saving.value = false
        }
    }

    // Fires right after a fill-up is saved rather than on a schedule (see FuelEfficiencyNotifier) —
    // there's nothing to check periodically, only right after a new per-entry km/L becomes
    // available. Uses the just-submitted form values directly instead of waiting on the
    // repository's Flow to refresh, since the new entry's generated id isn't returned by add().
    private fun checkFuelEfficiencyAlert(
        vehicleId: String,
        priorEntries: List<FuelEntry>,
        values: FuelEntryFormValues,
        maintenanceTasks: List<MaintenanceTask>,
    ) {
        if (!notificationPreferences.load().fuelEfficiencyAlerts) return
        val newEntry = FuelEntry(
            id = "pending-efficiency-check",
            vehicleId = vehicleId,
            date = values.date,
            time = values.time,
            odometerKm = values.odometerKm,
            liters = values.liters,
            pricePerLiter = values.pricePerLiter,
            amount = values.grossAmount,
            fullTank = values.fullTank,
            missedPreviousFillUp = values.missedPreviousFillUp,
        )
        val alert = checkFuelEfficiencyDrop(priorEntries, newEntry) ?: return
        val maintenanceOverdue = maintenanceTasks.any {
            calculateMaintenanceStatus(it, values.odometerKm).level == DueLevel.OVERDUE
        }
        FuelEfficiencyNotifier.notify(context, alert, maintenanceOverdue)
    }

    fun updateFuel(id: String, values: FuelEntryFormValues, onSaved: () -> Unit) {
        val vehicleId = state.value.entries.firstOrNull { it.id == id }?.vehicleId
            ?: state.value.selectedVehicle?.id
        if (vehicleId == null) {
            error.value = getString(R.string.error_vehicle_not_found_for_record)
            return
        }
        if (values.odometerKm <= 0 || values.liters <= 0 || values.pricePerLiter <= 0) {
            error.value = getString(R.string.error_invalid_fuel_fields)
            return
        }
        viewModelScope.launch {
            saving.value = true
            error.value = null
            runCatching { fuelRepository.update(id, vehicleId, values) }
                .onSuccess {
                    onReminderDataChanged()
                    onSaved()
                }.onFailure { error.value = it.message ?: getString(R.string.error_update_failed) }
            saving.value = false
        }
    }

    fun deleteFuel(id: String) {
        viewModelScope.launch {
            runCatching { fuelRepository.delete(id) }
                .onSuccess { onReminderDataChanged() }
                .onFailure { error.value = it.message ?: getString(R.string.error_delete_failed) }
        }
    }

    fun addExpense(
        date: String,
        time: String,
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
            error.value = getString(R.string.error_add_vehicle_before_expense)
            return
        }
        if (amount <= 0 || category.isBlank()) {
            error.value = getString(R.string.error_invalid_expense_fields)
            return
        }
        viewModelScope.launch {
            saving.value = true
            error.value = null
            runCatching {
                expenseRepository.add(
                    vehicleId,
                    date,
                    time,
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
                .onFailure { error.value = it.message ?: getString(R.string.error_save_expense_failed) }
            saving.value = false
        }
    }

    fun updateExpense(
        id: String,
        date: String,
        time: String,
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
            error.value = getString(R.string.error_vehicle_not_found_for_record)
            return
        }
        if (amount <= 0 || category.isBlank()) {
            error.value = getString(R.string.error_invalid_expense_fields)
            return
        }
        viewModelScope.launch {
            saving.value = true
            error.value = null
            runCatching {
                expenseRepository.update(id, vehicleId, date, time, category, description, amount, odometerKm, income, recurring, reminderDate, photoUri)
            }.onSuccess {
                onReminderDataChanged()
                onSaved()
            }.onFailure { error.value = it.message ?: getString(R.string.error_update_expense_failed) }
            saving.value = false
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            runCatching { expenseRepository.delete(id) }
                .onSuccess { onReminderDataChanged() }
                .onFailure { error.value = it.message ?: getString(R.string.error_delete_expense_failed) }
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
            error.value = getString(R.string.error_add_vehicle_before_maintenance)
            return
        }
        if (name.isBlank() || (nextDate.isNullOrBlank() && nextOdometerKm == null)) {
            error.value = getString(R.string.error_invalid_maintenance_fields)
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
                .onFailure { error.value = it.message ?: getString(R.string.error_save_maintenance_failed) }
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
            error.value = getString(R.string.error_vehicle_not_found_for_record)
            return
        }
        if (name.isBlank() || (nextDate.isNullOrBlank() && nextOdometerKm == null)) {
            error.value = getString(R.string.error_invalid_maintenance_fields)
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
            }.onFailure { error.value = it.message ?: getString(R.string.error_update_maintenance_failed) }
            saving.value = false
        }
    }

    fun completeMaintenance(id: String) {
        val task = state.value.maintenanceTasks.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            runCatching {
                maintenanceRepository.markDone(task, state.value.summary.latestOdometerKm)
            }.onSuccess { onReminderDataChanged() }
                .onFailure { error.value = it.message ?: getString(R.string.error_complete_maintenance_failed) }
        }
    }

    fun deleteMaintenance(id: String) {
        viewModelScope.launch {
            runCatching { maintenanceRepository.delete(id) }
                .onSuccess { onReminderDataChanged() }
                .onFailure { error.value = it.message ?: getString(R.string.error_delete_maintenance_failed) }
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
            error.value = getString(R.string.error_add_vehicle_before_trip)
            return
        }
        if (name.isBlank() || distanceKm < 0) {
            error.value = getString(R.string.error_invalid_trip_fields)
            return
        }
        val costs = listOf(fuelCost, tollCost, parkingCost, foodCost, otherCost)
        if (costs.any { it < 0 }) {
            error.value = getString(R.string.error_trip_costs_negative)
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
                .onFailure { error.value = it.message ?: getString(R.string.error_save_trip_failed) }
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
            error.value = getString(R.string.error_vehicle_not_found_for_trip)
            return
        }
        if (name.isBlank() || distanceKm < 0) {
            error.value = getString(R.string.error_invalid_trip_fields)
            return
        }
        viewModelScope.launch {
            saving.value = true
            error.value = null
            runCatching {
                tripRepository.update(id, vehicleId, name, date, distanceKm, fuelCost, tollCost, parkingCost, foodCost, otherCost)
            }.onSuccess { onSaved() }
                .onFailure { error.value = it.message ?: getString(R.string.error_update_trip_failed) }
            saving.value = false
        }
    }

    fun deleteTrip(id: String) {
        viewModelScope.launch {
            runCatching { tripRepository.delete(id) }
                .onFailure { error.value = it.message ?: getString(R.string.error_delete_trip_failed) }
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
    private val context: Context,
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
            context,
            onReminderDataChanged,
        ) as T
}
