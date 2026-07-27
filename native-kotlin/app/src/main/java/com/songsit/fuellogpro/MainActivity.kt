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
import com.songsit.fuellogpro.data.LocalCsvExportRepository
import com.songsit.fuellogpro.data.FuelioImportRepository
import com.songsit.fuellogpro.data.NearbyStationRepository
import com.songsit.fuellogpro.data.OilPriceRepository
import com.songsit.fuellogpro.data.firebase.FirestoreSyncRepository
import com.songsit.fuellogpro.data.firebase.VehicleSharingRepository
import com.songsit.fuellogpro.data.firebase.VehicleMember
import com.songsit.fuellogpro.auth.GoogleAuthRepository
import com.songsit.fuellogpro.data.local.FuelLogDatabase
import com.songsit.fuellogpro.ui.FuelLogApp
import com.songsit.fuellogpro.ui.NativeAppViewModel
import com.songsit.fuellogpro.ui.NativeAppViewModelFactory
import com.songsit.fuellogpro.ui.CloudUiState
import com.songsit.fuellogpro.notifications.MaintenanceReminderWorker
import com.songsit.fuellogpro.notifications.NotificationPreferences
import com.songsit.fuellogpro.notifications.ReminderSettings
import android.widget.Toast
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope

class MainActivity : ComponentActivity() {
    private lateinit var backupRepository: LocalBackupRepository
    private lateinit var csvExportRepository: LocalCsvExportRepository

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

    // Permission gate for the nearby-fuel-station finder (item 1). We store the pending
    // result/error callbacks and re-run the location lookup once permission is granted.
    private var pendingNearbyResult: ((List<com.songsit.fuellogpro.data.NearbyStation>) -> Unit)? = null
    private var pendingNearbyError: ((String) -> Unit)? = null
    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        val hasLocation = granted.values.any { it }
        if (hasLocation) {
            performNearbyStationLookup()
        } else {
            pendingNearbyResult = null
            pendingNearbyError?.invoke("กรุณาอนุญาตตำแหน่งเพื่อค้นหาปั๊มใกล้ฉัน")
            pendingNearbyError = null
        }
    }
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
    private lateinit var fuelioImportRepository: FuelioImportRepository

    // Handles both the native JSON backup format and a Fuelio .fuelio/.zip/.csv export
    // (item 3). Format is sniffed from the file bytes, not the extension, since Android's
    // OpenDocument picker doesn't reliably surface .fuelio as a distinct mime type.
    private val openBackup = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            runCatching {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytesLimited(20_000_000) }
                    ?: error("ไม่สามารถเปิดไฟล์สำรองได้")
                val isZip = bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
                if (isZip) {
                    fuelioImportRepository.importFuelioZip(bytes)
                } else {
                    val text = bytes.toString(Charsets.UTF_8)
                    val looksLikeFuelioCsv = text.contains("##Log") || text.contains("##Costs")
                    if (looksLikeFuelioCsv) {
                        fuelioImportRepository.importFuelioCsv(text)
                    } else {
                        backupRepository.importJson(text)
                    }
                }
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
    private val createCsvExport = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            runCatching {
                val csv = csvExportRepository.exportCsv()
                contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(csv) }
                    ?: error("ไม่สามารถเปิดไฟล์ปลายทางได้")
            }.onSuccess {
                Toast.makeText(this@MainActivity, "ส่งออก CSV เรียบร้อย", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(
                    this@MainActivity,
                    it.message ?: "ส่งออก CSV ไม่สำเร็จ",
                    Toast.LENGTH_LONG,
                ).show()
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
        val notificationPreferences = NotificationPreferences(this)
        backupRepository = LocalBackupRepository(database)
        csvExportRepository = LocalCsvExportRepository(database)
        fuelioImportRepository = FuelioImportRepository(database)
        val authRepository = GoogleAuthRepository()
        val cloudRepository = FirestoreSyncRepository(database)
        val deletionRecorder = LocalDeletionRecorder(database)
        val fuelRepository = LocalFuelRepository(database.fuelEntryDao(), deletionRecorder)
        val expenseRepository = LocalExpenseRepository(database.expenseDao(), deletionRecorder)
        val vehicleRepository = LocalVehicleRepository(database.vehicleDao())
        val maintenanceRepository = LocalMaintenanceRepository(database.maintenanceDao(), deletionRecorder)
        val tripRepository = LocalTripRepository(database.tripDao(), deletionRecorder)
        val oilPriceRepository = OilPriceRepository()
        val vehicleSharingRepository = VehicleSharingRepository()
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
            var reminderSettings by remember { mutableStateOf(notificationPreferences.load()) }
            val syncConflicts by database.syncConflictDao().observeAll().collectAsState(emptyList())
            var oilPriceSummary by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                val info = oilPriceRepository.fetchTodayPrices()
                if (info != null) {
                    val currency = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
                    val parts = buildList {
                        info.gasohol95?.let { add("แก๊สโซฮอล์ 95: ${currency.format(it)}") }
                        info.gasohol91?.let { add("แก๊สโซฮอล์ 91: ${currency.format(it)}") }
                        info.dieselB7?.let { add("ดีเซล B7: ${currency.format(it)}") }
                    }
                    if (parts.isNotEmpty()) {
                        oilPriceSummary = parts.joinToString(" • ") +
                            if (info.dateLabel.isNotBlank()) "\n${info.dateLabel}" else ""
                    }
                }
            }
            val viewModel: NativeAppViewModel = viewModel(
                factory = NativeAppViewModelFactory(
                    fuelRepository,
                    vehicleRepository,
                    expenseRepository,
                    maintenanceRepository,
                    tripRepository,
                    onReminderDataChanged = {
                        MaintenanceReminderWorker.refresh(applicationContext)
                    },
                ),
            )
            val state by viewModel.state.collectAsState()
            var vehicleMembers by remember { mutableStateOf<List<VehicleMember>>(emptyList()) }
            LaunchedEffect(state.selectedVehicleId, cloudState.uid) {
                val vehicleId = state.selectedVehicleId
                vehicleMembers = if (vehicleId != null && cloudState.uid != null) {
                    runCatching { vehicleSharingRepository.loadMembers(vehicleId) }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }
            }
            FuelLogApp(
                state = state,
                onAddFuel = viewModel::addFuel,
                onUpdateFuel = viewModel::updateFuel,
                onDeleteFuel = viewModel::deleteFuel,
                onAddExpense = viewModel::addExpense,
                onUpdateExpense = viewModel::updateExpense,
                onDeleteExpense = viewModel::deleteExpense,
                onAddMaintenance = viewModel::addMaintenance,
                onUpdateMaintenance = viewModel::updateMaintenance,
                onCompleteMaintenance = viewModel::completeMaintenance,
                onDeleteMaintenance = viewModel::deleteMaintenance,
                onAddTrip = viewModel::addTrip,
                onUpdateTrip = viewModel::updateTrip,
                onDeleteTrip = viewModel::deleteTrip,
                onExportCsv = {
                    createCsvExport.launch("FuelLog-Report-${LocalDate.now()}.csv")
                },
                reminderSettings = reminderSettings,
                onReminderSettingsChange = { settings: ReminderSettings ->
                    reminderSettings = settings
                    notificationPreferences.save(settings)
                    MaintenanceReminderWorker.refresh(applicationContext)
                },
                onExportBackup = {
                    createBackup.launch("FuelLog-Native-${LocalDate.now()}.json")
                },
                onImportBackup = {
                    openBackup.launch(arrayOf("application/json", "text/json", "text/plain", "application/zip", "application/octet-stream", "*/*"))
                },
                onFindNearbyStations = { onResult, onError ->
                    pendingNearbyResult = onResult
                    pendingNearbyError = onError
                    val hasPermission = hasLocationPermission()
                    if (hasPermission) {
                        performNearbyStationLookup()
                    } else {
                        locationPermission.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    }
                },
                oilPriceSummary = oilPriceSummary,
                vehicleMembers = vehicleMembers,
                onCreateInvite = { email, role, onResult, onError ->
                    val vehicleId = state.selectedVehicleId
                    val ownerUid = authRepository.currentUid
                    val vehicleName = state.selectedVehicle?.name ?: ""
                    if (vehicleId == null || ownerUid == null) {
                        onError("กรุณาเข้าสู่ระบบและเลือกรถก่อน")
                    } else {
                        composeScope.launch {
                            runCatching {
                                vehicleSharingRepository.createInvite(vehicleId, vehicleName, ownerUid, email, role)
                            }.onSuccess { onResult(it.code) }
                                .onFailure { onError(it.message ?: "สร้างคำเชิญไม่สำเร็จ") }
                        }
                    }
                },
                onJoinByCode = { code, onResult, onError ->
                    val uid = authRepository.currentUid
                    val email = authRepository.currentEmail
                    if (uid == null || email == null) {
                        onError("กรุณาเข้าสู่ระบบก่อน")
                    } else {
                        composeScope.launch {
                            runCatching {
                                vehicleSharingRepository.joinByCode(code, uid, email, authRepository.currentDisplayName ?: "")
                            }.onSuccess { vehicleName ->
                                onResult(vehicleName)
                                runCatching { cloudRepository.sync(uid, email, authRepository.currentDisplayName) }
                            }.onFailure { onError(it.message ?: "เข้าร่วมไม่สำเร็จ") }
                        }
                    }
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
                                MaintenanceReminderWorker.refresh(applicationContext)
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

    private fun hasLocationPermission(): Boolean {
        val fine = androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            coarse == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // Item 1: ports V8's fetchNearbyStations()/autoNearby() (app.js:675-691) — get device
    // location, then query the Overpass API for nearby amenity=fuel points.
    private fun performNearbyStationLookup() {
        val onResult = pendingNearbyResult
        val onError = pendingNearbyError
        pendingNearbyResult = null
        pendingNearbyError = null
        if (onResult == null) return
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        lifecycleScope.launch {
            runCatching {
                val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                    ?: error("ไม่สามารถอ่านตำแหน่งได้ กรุณาเปิด GPS")
                NearbyStationRepository().fetchNearbyStations(location.latitude, location.longitude)
            }.onSuccess { stations -> onResult(stations) }
                .onFailure { onError?.invoke(it.message ?: "ค้นหาไม่ได้ กรุณาอนุญาตตำแหน่งหรือพิมพ์ชื่อปั๊มเอง") }
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

private fun java.io.InputStream.readBytesLimited(maxBytes: Int): ByteArray {
    val buffer = java.io.ByteArrayOutputStream()
    val chunk = ByteArray(8_192)
    while (true) {
        val count = read(chunk)
        if (count < 0) break
        require(buffer.size() + count <= maxBytes) { "ไฟล์สำรองมีขนาดใหญ่เกิน 20 MB" }
        buffer.write(chunk, 0, count)
    }
    return buffer.toByteArray()
}
