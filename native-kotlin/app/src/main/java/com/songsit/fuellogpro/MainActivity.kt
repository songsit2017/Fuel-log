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
import com.songsit.fuellogpro.data.LocalDeletionRecorder
import com.songsit.fuellogpro.data.LocalExpenseRepository
import com.songsit.fuellogpro.data.LocalVehicleRepository
import com.songsit.fuellogpro.data.LocalMaintenanceRepository
import com.songsit.fuellogpro.data.LocalTripRepository
import com.songsit.fuellogpro.data.LocalBackupRepository
import com.songsit.fuellogpro.data.firebase.FirestoreSyncRepository
import com.songsit.fuellogpro.auth.GoogleAuthRepository
import com.songsit.fuellogpro.data.local.FuelLogDatabase
import com.songsit.fuellogpro.ui.FuelLogApp
import com.songsit.fuellogpro.ui.NativeAppViewModel
import com.songsit.fuellogpro.ui.NativeAppViewModelFactory
import com.songsit.fuellogpro.ui.CloudUiState
import com.songsit.fuellogpro.notifications.MaintenanceReminderWorker
import android.widget.Toast
import java.time.LocalDate
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope

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
        val authRepository = GoogleAuthRepository()
        val cloudRepository = FirestoreSyncRepository(database)
        val deletionRecorder = LocalDeletionRecorder(database)
        val fuelRepository = LocalFuelRepository(database.fuelEntryDao(), deletionRecorder)
        val expenseRepository = LocalExpenseRepository(database.expenseDao(), deletionRecorder)
        val vehicleRepository = LocalVehicleRepository(database.vehicleDao())
        val maintenanceRepository = LocalMaintenanceRepository(database.maintenanceDao(), deletionRecorder)
        val tripRepository = LocalTripRepository(database.tripDao(), deletionRecorder)
        setContent {
            var cloudState by remember {
                mutableStateOf(
                    CloudUiState(
                        uid = authRepository.currentUid,
                        email = authRepository.currentEmail,
                    ),
                )
            }
            val composeScope = rememberCoroutineScope()
            val syncConflicts by database.syncConflictDao().observeAll().collectAsState(emptyList())
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
                cloudState = cloudState,
                onGoogleSignIn = {
                    composeScope.launch {
                        cloudState = cloudState.copy(syncing = true, message = null)
                        runCatching {
                            authRepository.signIn(
                                this@MainActivity,
                                getString(R.string.default_web_client_id),
                            )
                        }.onSuccess { uid ->
                            cloudState = CloudUiState(
                                uid = uid,
                                email = authRepository.currentEmail,
                                message = "เข้าสู่ระบบแล้ว แตะซิงก์ตอนนี้เพื่อรวมข้อมูล",
                            )
                        }.onFailure {
                            cloudState = cloudState.copy(
                                syncing = false,
                                message = it.message ?: "เข้าสู่ระบบไม่สำเร็จ",
                            )
                        }
                    }
                },
                onCloudSync = {
                    val uid = authRepository.currentUid
                    if (uid == null) {
                        cloudState = cloudState.copy(message = "กรุณาเข้าสู่ระบบก่อน")
                    } else {
                        composeScope.launch {
                            cloudState = cloudState.copy(syncing = true, message = null)
                            runCatching {
                                cloudRepository.sync(
                                    uid,
                                    authRepository.currentEmail,
                                    authRepository.currentDisplayName,
                                )
                            }.onSuccess { result ->
                                cloudState = cloudState.copy(
                                    syncing = false,
                                    message = "อัปโหลด ${result.uploaded} • ดาวน์โหลด ${result.downloaded} • รถ ${result.vehicles}",
                                )
                            }.onFailure {
                                cloudState = cloudState.copy(
                                    syncing = false,
                                    message = it.message ?: "ซิงก์ไม่สำเร็จ",
                                )
                            }
                        }
                    }
                },
                onSignOut = {
                    authRepository.signOut()
                    cloudState = CloudUiState(message = "ออกจากระบบแล้ว ข้อมูลในเครื่องยังอยู่ครบ")
                },
                syncConflicts = syncConflicts,
                onResolveConflict = { key, useLocal ->
                    composeScope.launch {
                        cloudState = cloudState.copy(syncing = true, message = null)
                        runCatching { cloudRepository.resolveConflict(key, useLocal) }
                            .onSuccess {
                                cloudState = cloudState.copy(
                                    syncing = false,
                                    message = if (useLocal) "ใช้ข้อมูลในเครื่องแล้ว" else "ใช้ข้อมูล Cloud แล้ว",
                                )
                            }
                            .onFailure {
                                cloudState = cloudState.copy(
                                    syncing = false,
                                    message = it.message ?: "แก้รายการขัดแย้งไม่สำเร็จ",
                                )
                            }
                    }
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
