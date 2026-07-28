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
import com.songsit.fuellogpro.data.OilPriceInfo
import com.songsit.fuellogpro.data.OilPriceRepository
import com.songsit.fuellogpro.data.OcrRepository
import com.songsit.fuellogpro.data.ClaudeOcrRepository
import com.songsit.fuellogpro.data.WeatherRepository
import com.songsit.fuellogpro.data.ReceiptScanResult
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
import com.songsit.fuellogpro.settings.DisplayPreferences
import com.songsit.fuellogpro.settings.DisplaySettings
import android.widget.Toast
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope

// Item C (multi-photo): cap matches PhotoAttachmentRow's MAX_PHOTOS in ui/FuelLogApp.kt.
private const val MAX_PICK_PHOTOS = 3

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

    // Same permission gate, for the "capture weather at fill-up" lookup.
    private var pendingWeatherResult: ((com.songsit.fuellogpro.data.WeatherInfo) -> Unit)? = null
    private var pendingWeatherError: ((String) -> Unit)? = null
    private val weatherRepository = WeatherRepository()

    // Item 2/3/C (photo attachment + OCR + multi-photo): each picked image is copied into
    // app-private storage (context.filesDir/photos) so it survives even if the source
    // content:// URI's permission grant is later revoked, then the first picked photo is run
    // through on-device OCR to try to pre-fill an amount. PickMultipleVisualMedia (a stable,
    // non-experimental androidx.activity contract, available since minSdk 26 here) lets the
    // user pick up to MAX_PICK_PHOTOS at once instead of one at a time.
    private var pendingPhotoResult: ((uris: List<String>, scanResult: ReceiptScanResult?) -> Unit)? = null
    private var pendingPhotoType: String? = null
    private val ocrRepository = OcrRepository()
    private val claudeOcrRepository = ClaudeOcrRepository()

    // Tries the Claude/Anthropic-backed scanReceipt Cloud Function (functions/index.js) first —
    // it needs the user signed in (the function itself rejects anonymous calls) — falling back
    // to on-device ML Kit amount-only extraction otherwise. `type` null means "don't scan at
    // all" (used for vehicle photos, which aren't receipts).
    private suspend fun scanFirstPhoto(path: String, type: String?): ReceiptScanResult? {
        if (type == null) return null
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
            claudeOcrRepository.scanReceipt(path, type)?.let { return it }
        }
        val amount = runCatching { ocrRepository.extractAmount(path) }.getOrNull()
        return amount?.let { ReceiptScanResult(amount = it) }
    }

    private val pickPhoto = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK_PHOTOS),
    ) { uris ->
        val onPicked = pendingPhotoResult
        val type = pendingPhotoType
        pendingPhotoResult = null
        if (uris.isEmpty() || onPicked == null) return@registerForActivityResult
        lifecycleScope.launch {
            runCatching {
                val photosDir = java.io.File(filesDir, "photos").apply { mkdirs() }
                uris.map { uri ->
                    val destFile = java.io.File(photosDir, "${java.util.UUID.randomUUID()}.jpg")
                    contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("ไม่สามารถเปิดไฟล์รูปได้")
                    destFile.absolutePath
                }
            }.onSuccess { paths ->
                lifecycleScope.launch {
                    val scanResult = paths.firstOrNull()?.let { path -> scanFirstPhoto(path, type) }
                    onPicked(paths, scanResult)
                }
            }.onFailure {
                Toast.makeText(this@MainActivity, it.message ?: "แนบรูปไม่สำเร็จ", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Camera counterpart to pickPhoto above: the user chooses "ถ่ายรูป" instead of "เลือกจาก
    // แกลอรี่" in PhotoAttachmentRow's source menu. TakePicture writes straight into a
    // filesDir/photos file we hand it via FileProvider, so the result path already matches the
    // gallery flow's storage convention with no extra copy step.
    private var pendingCameraFile: java.io.File? = null
    private var pendingCameraResult: ((uris: List<String>, scanResult: ReceiptScanResult?) -> Unit)? = null
    private var pendingCameraType: String? = null
    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val onCaptured = pendingCameraResult
        val type = pendingCameraType
        val destFile = pendingCameraFile
        pendingCameraResult = null
        pendingCameraFile = null
        if (!success || onCaptured == null || destFile == null) return@registerForActivityResult
        lifecycleScope.launch {
            val scanResult = scanFirstPhoto(destFile.absolutePath, type)
            onCaptured(listOf(destFile.absolutePath), scanResult)
        }
    }

    private fun launchCameraCapture(type: String?, onCaptured: (uris: List<String>, scanResult: ReceiptScanResult?) -> Unit) {
        val photosDir = java.io.File(filesDir, "photos").apply { mkdirs() }
        val destFile = java.io.File(photosDir, "${java.util.UUID.randomUUID()}.jpg")
        pendingCameraFile = destFile
        pendingCameraResult = onCaptured
        pendingCameraType = type
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", destFile)
        takePicture.launch(uri)
    }
    // The "add fill-up" dialog can request nearby-station lookup and weather capture back-to-back
    // on open, both needing location permission — this flag stops the second request from calling
    // launch() on the shared registerForActivityResult launcher while the first is still pending.
    private var locationPermissionRequestInFlight = false
    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        locationPermissionRequestInFlight = false
        val hasLocation = granted.values.any { it }
        if (hasLocation) {
            if (pendingNearbyResult != null) performNearbyStationLookup()
            if (pendingWeatherResult != null) performWeatherFetch()
        } else {
            pendingNearbyResult = null
            pendingNearbyError?.invoke("กรุณาอนุญาตตำแหน่งเพื่อค้นหาปั๊มใกล้ฉัน")
            pendingNearbyError = null
            pendingWeatherResult = null
            pendingWeatherError?.invoke("กรุณาอนุญาตตำแหน่งเพื่อดึงสภาพอากาศ")
            pendingWeatherError = null
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

    // Drives the "กำลังนำเข้าข้อมูล... N%" progress dialog while openBackup below is running —
    // null means no import in progress. A plain class field (not Compose state) because
    // registerForActivityResult callbacks live outside setContent; setContent collects this
    // as state and passes it down to FuelLogApp.
    private val importProgress = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)
    private val pendingImportSummaryResult = kotlinx.coroutines.flow.MutableStateFlow<com.songsit.fuellogpro.data.BackupImportResult?>(null)

    private fun java.io.InputStream.readTextLimited(maxBytes: Int = 20_000_000): String {
        val bytes = readBytes()
        require(bytes.size <= maxBytes) { "ไฟล์ใหญ่เกินขนาดที่รองรับ" }
        return bytes.toString(Charsets.UTF_8)
    }

    private fun queryFileSize(uri: android.net.Uri): Long? =
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (sizeIndex >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
        }

    // Handles both the native JSON backup format and a Fuelio .fuelio/.zip/.csv export
    // (item 3). Format is sniffed by peeking the first bytes, not the extension, since
    // Android's OpenDocument picker doesn't reliably surface .fuelio as a distinct mime
    // type — and the zip branch below streams straight from the picked Uri (re-opening it
    // as needed) instead of buffering the whole backup into memory, so multi-hundred-MB
    // .fuelio exports full of photos import without hitting an OutOfMemoryError.
    private val openBackup = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            importProgress.value = 0
            runCatching {
                val header = contentResolver.openInputStream(uri)?.use { stream ->
                    val prefix = ByteArray(4)
                    val read = stream.read(prefix)
                    prefix.copyOf(if (read < 0) 0 else read)
                } ?: error("ไม่สามารถเปิดไฟล์สำรองได้")
                val isZip = header.size >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
                if (isZip) {
                    val totalBytes = queryFileSize(uri)
                    fuelioImportRepository.importFuelioZip(
                        openStream = { contentResolver.openInputStream(uri) ?: error("ไม่สามารถเปิดไฟล์สำรองได้") },
                        totalBytes = totalBytes,
                        onProgress = { percent -> importProgress.value = percent },
                    )
                } else {
                    val text = contentResolver.openInputStream(uri)?.use { it.readTextLimited(20_000_000) }
                        ?: error("ไม่สามารถเปิดไฟล์สำรองได้")
                    val looksLikeFuelioCsv = text.contains("##Log") || text.contains("##Costs")
                    if (looksLikeFuelioCsv) {
                        fuelioImportRepository.importFuelioCsv(text)
                    } else {
                        backupRepository.importJson(text)
                    }
                }
            }.onSuccess {
                importProgress.value = null
                pendingImportSummaryResult.value = it
            }.onFailure {
                importProgress.value = null
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
        val displayPreferences = DisplayPreferences(this)
        backupRepository = LocalBackupRepository(database)
        csvExportRepository = LocalCsvExportRepository(database)
        fuelioImportRepository = FuelioImportRepository(applicationContext, database)
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
            var displaySettings by remember { mutableStateOf(displayPreferences.load()) }
            val syncConflicts by database.syncConflictDao().observeAll().collectAsState(emptyList())
            var oilPriceInfo by remember { mutableStateOf<OilPriceInfo?>(null) }
            LaunchedEffect(Unit) {
                oilPriceInfo = oilPriceRepository.fetchTodayPrices()
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
            val importProgressPercent by importProgress.collectAsState()
            val importSummaryResult by pendingImportSummaryResult.collectAsState()
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
                importProgressPercent = importProgressPercent,
                importSummaryResult = importSummaryResult,
                onDismissImportSummary = { pendingImportSummaryResult.value = null },
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
                displaySettings = displaySettings,
                onDisplaySettingsChange = { settings: DisplaySettings ->
                    displaySettings = settings
                    displayPreferences.save(settings)
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
                    } else if (!locationPermissionRequestInFlight) {
                        locationPermissionRequestInFlight = true
                        locationPermission.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    }
                },
                onFetchWeather = { onResult, onError ->
                    pendingWeatherResult = onResult
                    pendingWeatherError = onError
                    val hasPermission = hasLocationPermission()
                    if (hasPermission) {
                        performWeatherFetch()
                    } else if (!locationPermissionRequestInFlight) {
                        locationPermissionRequestInFlight = true
                        locationPermission.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    }
                },
                onPickPhoto = { type, onPicked ->
                    pendingPhotoResult = onPicked
                    pendingPhotoType = type
                    pickPhoto.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                        ),
                    )
                },
                onPickCameraPhoto = { type, onPicked -> launchCameraCapture(type, onPicked) },
                oilPriceInfo = oilPriceInfo,
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
                            val resId = resources.getIdentifier("default_web_client_id", "string", packageName)
                            val webClientId = if (resId != 0) getString(resId) else ""
                            authRepository.signIn(
                                this@MainActivity,
                                webClientId,
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
                onUpdateVehicle = viewModel::updateVehicle,
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
                NearbyStationRepository().fetchNearbyStations(applicationContext, location.latitude, location.longitude)
            }.onSuccess { stations -> onResult(stations) }
                .onFailure { onError?.invoke(it.message ?: "ค้นหาไม่ได้ กรุณาอนุญาตตำแหน่งหรือพิมพ์ชื่อปั๊มเอง") }
        }
    }

    // Weather Integration: captures conditions at the device's current location for the "add
    // fill-up" form, same permission/location flow as performNearbyStationLookup() above.
    private fun performWeatherFetch() {
        val onResult = pendingWeatherResult
        val onError = pendingWeatherError
        pendingWeatherResult = null
        pendingWeatherError = null
        if (onResult == null) return
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        lifecycleScope.launch {
            runCatching {
                val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                    ?: error("ไม่สามารถอ่านตำแหน่งได้ กรุณาเปิด GPS")
                weatherRepository.fetchCurrent(location.latitude, location.longitude)
            }.onSuccess { weather -> onResult(weather) }
                .onFailure { onError?.invoke(it.message ?: "ดึงข้อมูลสภาพอากาศไม่สำเร็จ") }
        }
    }
}
