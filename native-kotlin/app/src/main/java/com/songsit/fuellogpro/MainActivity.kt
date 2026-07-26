package com.songsit.fuellogpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.songsit.fuellogpro.data.LocalFuelRepository
import com.songsit.fuellogpro.data.LocalExpenseRepository
import com.songsit.fuellogpro.data.LocalVehicleRepository
import com.songsit.fuellogpro.data.LocalMaintenanceRepository
import com.songsit.fuellogpro.data.local.FuelLogDatabase
import com.songsit.fuellogpro.ui.FuelLogApp
import com.songsit.fuellogpro.ui.NativeAppViewModel
import com.songsit.fuellogpro.ui.NativeAppViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = FuelLogDatabase.get(this)
        val fuelRepository = LocalFuelRepository(database.fuelEntryDao())
        val expenseRepository = LocalExpenseRepository(database.expenseDao())
        val vehicleRepository = LocalVehicleRepository(database.vehicleDao())
        val maintenanceRepository = LocalMaintenanceRepository(database.maintenanceDao())
        setContent {
            val viewModel: NativeAppViewModel = viewModel(
                factory = NativeAppViewModelFactory(
                    fuelRepository,
                    vehicleRepository,
                    expenseRepository,
                    maintenanceRepository,
                ),
            )
            val state by viewModel.state.collectAsState()
            FuelLogApp(
                state = state,
                onAddFuel = viewModel::addFuel,
                onDeleteFuel = viewModel::deleteFuel,
                onAddExpense = viewModel::addExpense,
                onDeleteExpense = viewModel::deleteExpense,
                onAddMaintenance = viewModel::addMaintenance,
                onCompleteMaintenance = viewModel::completeMaintenance,
                onDeleteMaintenance = viewModel::deleteMaintenance,
                onSelectVehicle = viewModel::selectVehicle,
                onAddVehicle = viewModel::addVehicle,
                onDeleteVehicle = viewModel::deleteVehicle,
                onClearError = viewModel::clearError,
            )
        }
    }
}
