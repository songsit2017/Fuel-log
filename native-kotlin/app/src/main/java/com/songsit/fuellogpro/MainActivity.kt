package com.songsit.fuellogpro

import android.os.Bundle
import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.songsit.fuellogpro.data.LocalFuelRepository
import com.songsit.fuellogpro.data.LocalExpenseRepository
import com.songsit.fuellogpro.data.LocalVehicleRepository
import com.songsit.fuellogpro.data.LocalMaintenanceRepository
import com.songsit.fuellogpro.data.LocalTripRepository
import com.songsit.fuellogpro.data.LocalBackupRepository
import com.songsit.fuellogpro.data.local.FuelLogDatabase
import com.songsit.fuellogpro.ui.FuelLogApp
import com.songsit.fuellogpro.ui.NativeAppViewModel
import com.songsit.fuellogpro.ui.NativeAppViewModelFactory
import com.songsit.fuellogpro.notifications.MaintenanceReminderWorker
import android.widget.Toast
import java.time.LocalDate
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var backupRepository: LocalBackupRepository

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    private val createBackup = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            runCatching {
                val json = backupRepository.exportJson()
                contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(json) }
                    ?: error("ไม่สามารถเปิดไฟล์ปลายทางได้")
            }.onSuccess {
                Toast.makeText(this@MainActivity, "สำรองข้อมูลเรียบร้อย", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this@MainActivity, it.message ?: "สำรองข้อมูลไม่สำเร็จ", Toast.LENGTH_LONG).show()
            }
        }
    }
    private val openBackup = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            runCatching {
                val json = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                    reader.readTextLimited(20_000_000)
                } ?: error("ไม่สามารถเปิดไฟล์สำรองได้")
                backupRepository.importJson(json)
            }.onSuccess {
                Toast.makeText(
                    this@MainActivity,
                    "นำเข้าแบบไม่ลบข้อมูลเดิม ${it.totalRecords} รายการ",
                    Toast.LENGTH_LONG,
                ).show()
            }.onFailure {
                Toast.makeText(this@MainActivity, it.message ?: "นำเข้าไม่สำเร็จ", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MaintenanceReminderWorker.schedule(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val database = FuelLogDatabase.get(this)
        backupRepository = LocalBackupRepository(database)
        val fuelRepository = LocalFuelRepository(database.fuelEntryDao())
        val expenseRepository = LocalExpenseRepository(database.expenseDao())
        val vehicleRepository = LocalVehicleRepository(database.vehicleDao())
        val maintenanceRepository = LocalMaintenanceRepository(database.maintenanceDao())
        val tripRepository = LocalTripRepository(database.tripDao())
        setContent {
            val viewModel: NativeAppViewModel = viewModel(
                factory = NativeAppViewModelFactory(
                    fuelRepository,
                    vehicleRepository,
                    expenseRepository,
                    maintenanceRepository,
                    tripRepository,
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
                onAddTrip = viewModel::addTrip,
                onDeleteTrip = viewModel::deleteTrip,
                onExportBackup = {
                    createBackup.launch("FuelLog-Native-${LocalDate.now()}.json")
                },
                onImportBackup = {
                    openBackup.launch(arrayOf("application/json", "text/json", "text/plain"))
                },
                onSelectVehicle = viewModel::selectVehicle,
                onAddVehicle = viewModel::addVehicle,
                onDeleteVehicle = viewModel::deleteVehicle,
                onClearError = viewModel::clearError,
            )
        }
    }
}

private fun java.io.Reader.readTextLimited(maxChars: Int): String {
    val result = StringBuilder()
    val buffer = CharArray(8_192)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(result.length + count <= maxChars) { "ไฟล์สำรองมีขนาดใหญ่เกิน 20 MB" }
        result.append(buffer, 0, count)
    }
    return result.toString()
}
