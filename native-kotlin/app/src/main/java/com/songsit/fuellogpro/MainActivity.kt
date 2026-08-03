package com.songsit.fuellogpro

import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
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

// Cap matches PhotoAttachmentRow's MAX_PHOTOS in ui/FuelLogApp.kt.
private const val MAX_PICK_PHOTOS = 8

class MainActivity : AppCompatActivity() {
    private lateinit var backupRepository: LocalBackupRepository
    private lateinit var csvExportRepository: LocalCsvExportRepository
    private val driveBackupRepository = com.songsit.fuellogpro.data.GoogleDriveBackupRepository(this)
    private val driveBackupProgress = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

    // Google Drive backup (drive.file scope, app-created files only): incremental authorization
    // via the Identity Services Authorization API, requested separately from the Firebase
    // sign-in above (Credential Manager's ID-token flow has no way to carry OAuth scopes).
    private var pendingDriveAuthResult: ((String?) -> Unit)? = null
    private val driveAuthorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val callback = pendingDriveAuthResult
        pendingDriveAuthResult = null
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            runCatching {
                com.google.android.gms.auth.api.identity.Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(result.data!!)
                    .accessToken
            }.onSuccess { token -> callback?.invoke(token) }
                .onFailure { callback?.invoke(null) }
        } else {
            callback?.invoke(null)
        }
    }

    private fun requestDriveAccessToken(onResult: (String?) -> Unit) {
        val scope = com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file")
        val request = com.google.android.gms.auth.api.identity.AuthorizationRequest.builder()
            .setRequestedScopes(listOf(scope))
            .build()
        com.google.android.gms.auth.api.identity.Identity.getAuthorizationClient(this)
            .authorize(request)
            .addOnSuccessListener { authResult ->
                if (authResult.hasResolution()) {
                    pendingDriveAuthResult = onResult
                    val intentSenderRequest = androidx.activity.result.IntentSenderRequest
                        .Builder(authResult.pendingIntent!!.intentSender)
                        .build()
                    driveAuthorizationLauncher.launch(intentSenderRequest)
                } else {
                    onResult(authResult.accessToken)
                }
            }
            .addOnFailureListener { onResult(null) }
    }

    // Uploads a fresh JSON export plus every photo under filesDir/photos and
    // filesDir/fuelio_images to Drive (see GoogleDriveBackupRepository for the folder layout).
    // `silent` is used by the auto-sync hook in onReminderDataChanged below — it skips the
    // blocking progress dialog and success toast (only a failure is surfaced) so saving a
    // fill-up/expense doesn't interrupt the user with a sync popup every time; the very first
    // authorization still needs a one-time consent screen regardless of this flag, since that
    // part is Google's UI, not ours.
    private fun performDriveBackup(silent: Boolean = false) {
        requestDriveAccessToken { token ->
            if (token == null) {
                if (!silent) Toast.makeText(this, getString(R.string.drive_auth_failed), Toast.LENGTH_LONG).show()
                return@requestDriveAccessToken
            }
            lifecycleScope.launch {
                if (!silent) driveBackupProgress.value = 0
                runCatching {
                    val json = backupRepository.exportJson()
                    val photosDir = java.io.File(filesDir, "photos")
                    val fuelioImagesDir = java.io.File(filesDir, "fuelio_images")
                    val photos = (photosDir.listFiles()?.toList().orEmpty() + fuelioImagesDir.listFiles()?.toList().orEmpty())
                        .filter { it.isFile }
                    driveBackupRepository.backup(token, json, photos) { percent ->
                        if (!silent) driveBackupProgress.value = percent
                    }
                }.onSuccess { result ->
                    driveBackupProgress.value = null
                    if (!silent) {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.drive_backup_success, result.photosUploaded, result.photosSkipped),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }.onFailure {
                    driveBackupProgress.value = null
                    Toast.makeText(this@MainActivity, it.message ?: getString(R.string.drive_backup_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Downloads the latest JSON backup from Drive and merges it in via the same
    // LocalBackupRepository.importJson() used for local-file JSON import — an id-keyed upsert,
    // so this updates matching records in place and adds new ones without deleting or
    // duplicating anything already on the device. Reuses the same progress/summary UI as the
    // local import flow (importProgress/pendingImportSummaryResult).
    private fun performDriveRestore() {
        requestDriveAccessToken { token ->
            if (token == null) {
                Toast.makeText(this, getString(R.string.drive_auth_failed), Toast.LENGTH_LONG).show()
                return@requestDriveAccessToken
            }
            lifecycleScope.launch {
                importProgress.value = 0
                runCatching {
                    val json = driveBackupRepository.downloadLatestBackup(token)
                        ?: error(getString(R.string.drive_backup_not_found))
                    importProgress.value = 50
                    backupRepository.importJson(json)
                }.onSuccess {
                    importProgress.value = null
                    pendingImportSummaryResult.value = it
                }.onFailure {
                    importProgress.value = null
                    Toast.makeText(this@MainActivity, it.message ?: getString(R.string.drive_restore_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Permission gate for the nearby-fuel-station finder (item 1). We store the pending
    // result/error callbacks and re-run the location lookup once permission is granted.
    private var pendingNearbyResult: ((List<com.songsit.fuellogpro.data.NearbyStation>) -> Unit)? = null
    private var pendingNearbyError: ((String) -> Unit)? = null

    // Same permission gate, for the "capture weather at fill-up" lookup.
    private var pendingWeatherResult: ((com.songsit.fuellogpro.data.WeatherInfo) -> Unit)? = null
    private var pendingWeatherError: ((String) -> Unit)? = null
    private val weatherRepository = WeatherRepository(this)

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
        return if (type == "odometer") {
            val odometer = runCatching { ocrRepository.extractOdometer(path) }.getOrNull()
            odometer?.let { ReceiptScanResult(odometer = it) }
        } else {
            val amount = runCatching { ocrRepository.extractAmount(path) }.getOrNull()
            amount?.let { ReceiptScanResult(amount = it) }
        }
    }

    private val pickPhoto = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK_PHOTOS),
    ) { uris ->
        val onPicked = pendingPhotoResult
        val type = pendingPhotoType
        pendingPhotoResult = null
        if (onPicked == null) return@registerForActivityResult
        // Still call back on an empty pick (user backed out of the chooser) — the caller sets a
        // "scanning..." flag before launching and only clears it in this callback, so skipping
        // it here left the button spinning forever with nothing to show for it.
        if (uris.isEmpty()) { onPicked(emptyList(), null); return@registerForActivityResult }
        lifecycleScope.launch {
            runCatching {
                val photosDir = java.io.File(filesDir, "photos").apply { mkdirs() }
                uris.map { uri ->
                    val destFile = java.io.File(photosDir, "${java.util.UUID.randomUUID()}.jpg")
                    contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: error(getString(R.string.error_open_photo_file))
                    destFile.absolutePath
                }
            }.onSuccess { paths ->
                lifecycleScope.launch {
                    val scanResult = paths.firstOrNull()?.let { path -> scanFirstPhoto(path, type) }
                    onPicked(paths, scanResult)
                }
            }.onFailure {
                Toast.makeText(this@MainActivity, it.message ?: getString(R.string.error_attach_photo_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    // Expense attachments can also be a PDF (e.g. an emailed e-receipt) — the Android Photo
    // Picker used by pickPhoto above only surfaces images/videos, so a PDF needs the separate
    // system document picker instead. Same copy-into-app-storage reasoning as pickPhoto: the
    // source content:// URI's permission grant isn't guaranteed to outlive this picker session.
    private var pendingDocumentResult: ((uris: List<String>) -> Unit)? = null
    private val pickDocument = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val onPicked = pendingDocumentResult
        pendingDocumentResult = null
        if (onPicked == null) return@registerForActivityResult
        if (uris.isEmpty()) { onPicked(emptyList()); return@registerForActivityResult }
        lifecycleScope.launch {
            runCatching {
                val photosDir = java.io.File(filesDir, "photos").apply { mkdirs() }
                uris.map { uri ->
                    val destFile = java.io.File(photosDir, "${java.util.UUID.randomUUID()}.pdf")
                    contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: error(getString(R.string.error_open_pdf_file))
                    destFile.absolutePath
                }
            }.onSuccess { paths -> onPicked(paths) }
                .onFailure {
                    Toast.makeText(this@MainActivity, it.message ?: getString(R.string.error_attach_pdf_failed), Toast.LENGTH_LONG).show()
                    onPicked(emptyList())
                }
        }
    }

    private fun launchPdfPicker(onPicked: (uris: List<String>) -> Unit) {
        pendingDocumentResult = onPicked
        pickDocument.launch(arrayOf("application/pdf"))
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
        if (onCaptured == null) return@registerForActivityResult
        // Same reasoning as pickPhoto above: call back even on cancel so the "scanning..."
        // flag the caller set before launching the camera always gets cleared.
        if (!success || destFile == null) { onCaptured(emptyList(), null); return@registerForActivityResult }
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

    // Receipt/expense-bill scans go through Google Play services' Document Scanner instead of a
    // plain camera capture — it auto-detects the paper's edges, crops to just the document, and
    // applies a scan-style filter, matching what a dedicated document-scanner app produces.
    // launchCameraCapture above (odometer photos, plain "แนบรูป" attach) stays a plain photo
    // since those aren't documents and edge-detection would just fail on a car dashboard.
    private var pendingScanResult: ((uris: List<String>, scanResult: ReceiptScanResult?) -> Unit)? = null
    private var pendingScanType: String? = null
    private val documentScanLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val onCaptured = pendingScanResult
        val type = pendingScanType
        pendingScanResult = null
        pendingScanType = null
        if (onCaptured == null) return@registerForActivityResult
        val pageUri = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages?.firstOrNull()?.imageUri
        if (pageUri == null) { onCaptured(emptyList(), null); return@registerForActivityResult }
        lifecycleScope.launch {
            runCatching {
                val photosDir = java.io.File(filesDir, "photos").apply { mkdirs() }
                val destFile = java.io.File(photosDir, "${java.util.UUID.randomUUID()}.jpg")
                contentResolver.openInputStream(pageUri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                } ?: error(getString(R.string.error_open_photo_file))
                destFile.absolutePath
            }.onSuccess { path ->
                val scanResult = scanFirstPhoto(path, type)
                onCaptured(listOf(path), scanResult)
            }.onFailure {
                Toast.makeText(this@MainActivity, it.message ?: getString(R.string.error_attach_photo_failed), Toast.LENGTH_LONG).show()
                onCaptured(emptyList(), null)
            }
        }
    }

    private fun launchDocumentScan(type: String, onCaptured: (uris: List<String>, scanResult: ReceiptScanResult?) -> Unit) {
        pendingScanResult = onCaptured
        pendingScanType = type
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE_WITH_FILTER)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setPageLimit(1)
            .setGalleryImportAllowed(true)
            .build()
        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                documentScanLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                pendingScanResult = null
                pendingScanType = null
                Toast.makeText(this, e.message ?: getString(R.string.error_open_doc_scanner_failed), Toast.LENGTH_LONG).show()
                onCaptured(emptyList(), null)
            }
    }

    // The "add fill-up" dialog can request nearby-station lookup and weather capture back-to-back
    // on open, both needing location permission — this flag stops the second request from calling
    // launch() on the shared registerForActivityResult launcher while the first is still pending.
    private var locationPermissionRequestInFlight = false
    private var pendingTripRecordingStart = false
    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        locationPermissionRequestInFlight = false
        val hasLocation = granted.values.any { it }
        if (hasLocation) {
            if (pendingNearbyResult != null) performNearbyStationLookup()
            if (pendingWeatherResult != null) performWeatherFetch()
            if (pendingTripRecordingStart) { pendingTripRecordingStart = false; startTripRecordingService() }
        } else {
            pendingNearbyResult = null
            pendingNearbyError?.invoke(getString(R.string.error_location_permission_nearby))
            pendingNearbyError = null
            pendingWeatherResult = null
            pendingWeatherError?.invoke(getString(R.string.error_location_permission_weather))
            pendingWeatherError = null
            if (pendingTripRecordingStart) {
                pendingTripRecordingStart = false
                Toast.makeText(this, getString(R.string.error_location_permission_trip), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startTripRecordingService() {
        if (!hasFineLocationPermission()) {
            Toast.makeText(this, getString(R.string.error_trip_coarse_location_warning), Toast.LENGTH_LONG).show()
        }
        val intent = Intent(this, com.songsit.fuellogpro.trip.TripRecordingService::class.java)
            .setAction(com.songsit.fuellogpro.trip.TripRecordingService.ACTION_START)
        androidx.core.content.ContextCompat.startForegroundService(this, intent)
    }
    private val createBackup = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            runCatching {
                val json = backupRepository.exportJson()
                contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(json) }
                    ?: error(getString(R.string.error_open_destination_file))
            }.onSuccess {
                Toast.makeText(this@MainActivity, getString(R.string.backup_success), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this@MainActivity, it.message ?: getString(R.string.backup_failed), Toast.LENGTH_LONG).show()
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
        require(bytes.size <= maxBytes) { getString(R.string.error_file_too_large) }
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
                } ?: error(getString(R.string.error_open_backup_file))
                val isZip = header.size >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
                if (isZip) {
                    val totalBytes = queryFileSize(uri)
                    fuelioImportRepository.importFuelioZip(
                        openStream = { contentResolver.openInputStream(uri) ?: error(getString(R.string.error_open_backup_file)) },
                        totalBytes = totalBytes,
                        onProgress = { percent -> importProgress.value = percent },
                    )
                } else {
                    val text = contentResolver.openInputStream(uri)?.use { it.readTextLimited(20_000_000) }
                        ?: error(getString(R.string.error_open_backup_file))
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
                Toast.makeText(this@MainActivity, it.message ?: getString(R.string.import_failed), Toast.LENGTH_LONG).show()
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
                    ?: error(getString(R.string.error_open_destination_file))
            }.onSuccess {
                Toast.makeText(this@MainActivity, getString(R.string.csv_export_success), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(
                    this@MainActivity,
                    it.message ?: getString(R.string.csv_export_failed),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate() — installSplashScreen() reads the
        // Theme.FuelLogPro.Starting splash theme set in AndroidManifest.xml, shows it, then
        // switches back to Theme.FuelLogPro (postSplashScreenTheme) automatically.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        MaintenanceReminderWorker.schedule(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val database = FuelLogDatabase.get(this)
        val notificationPreferences = NotificationPreferences(this)
        val displayPreferences = DisplayPreferences(this)
        val backupPreferences = com.songsit.fuellogpro.settings.BackupPreferences(this)
        backupRepository = LocalBackupRepository(database, this)
        csvExportRepository = LocalCsvExportRepository(database)
        fuelioImportRepository = FuelioImportRepository(applicationContext, database)
        val authRepository = GoogleAuthRepository()
        val cloudRepository = FirestoreSyncRepository(database, applicationContext)
        val deletionRecorder = LocalDeletionRecorder(database)
        val fuelRepository = LocalFuelRepository(database.fuelEntryDao(), deletionRecorder)
        val expenseRepository = LocalExpenseRepository(database.expenseDao(), deletionRecorder)
        val vehicleRepository = LocalVehicleRepository(database.vehicleDao(), deletionRecorder)
        val maintenanceRepository = LocalMaintenanceRepository(database.maintenanceDao(), deletionRecorder)
        val tripRepository = LocalTripRepository(database.tripDao(), deletionRecorder)
        val oilPriceRepository = OilPriceRepository()
        val vehicleSharingRepository = VehicleSharingRepository(applicationContext)
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
            var backupSettings by remember { mutableStateOf(backupPreferences.load()) }
            val syncConflicts by database.syncConflictDao().observeAll().collectAsState(emptyList())
            var oilPriceInfo by remember { mutableStateOf<OilPriceInfo?>(null) }
            LaunchedEffect(Unit) {
                oilPriceInfo = oilPriceRepository.fetchTodayPrices()
            }
            // Pull the latest family-shared data as soon as the app opens (if already signed
            // in), so a vehicle another member edited elsewhere shows up without the user
            // having to remember to tap "ซิงก์ตอนนี้" first. Runs once per process launch.
            LaunchedEffect(Unit) {
                val uid = authRepository.currentUid
                if (uid != null) {
                    cloudState = cloudState.copy(syncing = true, message = null)
                    runCatching {
                        cloudRepository.sync(uid, authRepository.currentEmail, authRepository.currentDisplayName, authRepository.currentPhotoUrl)
                    }.onSuccess { result ->
                        cloudState = cloudState.copy(
                            syncing = false,
                            message = getString(R.string.auto_sync_success, result.uploaded, result.downloaded, result.vehicles),
                        )
                    }.onFailure {
                        cloudState = cloudState.copy(syncing = false, message = it.message ?: getString(R.string.auto_sync_failed))
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
                    applicationContext,
                    onReminderDataChanged = {
                        MaintenanceReminderWorker.refresh(applicationContext)
                        // Fuelio-style auto-sync: mirror every add/edit to Drive in the
                        // background, matching the ("ซิงค์อัตโนมัติ ในขณะเพิ่ม/แก้ไขใหม่") checkbox.
                        if (backupSettings.driveAutoSyncEnabled) performDriveBackup(silent = true)
                        // Family sharing: push every add/edit to Firestore too, silently, so
                        // other members see the change without anyone tapping "ซิงก์ตอนนี้".
                        val uid = authRepository.currentUid
                        if (uid != null) {
                            composeScope.launch {
                                runCatching {
                                    cloudRepository.sync(uid, authRepository.currentEmail, authRepository.currentDisplayName, authRepository.currentPhotoUrl)
                                }
                            }
                        }
                    },
                ),
            )
            val state by viewModel.state.collectAsState()
            val importProgressPercent by importProgress.collectAsState()
            val importSummaryResult by pendingImportSummaryResult.collectAsState()
            val driveBackupPercent by driveBackupProgress.collectAsState()
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
                driveBackupProgress = driveBackupPercent,
                onDriveBackup = { performDriveBackup() },
                onDriveRestore = { performDriveRestore() },
                driveAutoSyncEnabled = backupSettings.driveAutoSyncEnabled,
                onDriveAutoSyncChange = { enabled ->
                    backupSettings = backupSettings.copy(driveAutoSyncEnabled = enabled)
                    backupPreferences.save(backupSettings)
                },
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
                onStartTripRecording = {
                    if (hasLocationPermission()) {
                        startTripRecordingService()
                    } else if (!locationPermissionRequestInFlight) {
                        pendingTripRecordingStart = true
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
                onPickCameraPhoto = { type, onPicked ->
                    if (type == "fuel" || type == "expense") launchDocumentScan(type, onPicked) else launchCameraCapture(type, onPicked)
                },
                onPickPdf = { onPicked -> launchPdfPicker(onPicked) },
                oilPriceInfo = oilPriceInfo,
                vehicleMembers = vehicleMembers,
                onCreateInvite = { email, role, onResult, onError ->
                    val vehicleId = state.selectedVehicleId
                    val ownerUid = authRepository.currentUid
                    val vehicleName = state.selectedVehicle?.name ?: ""
                    if (vehicleId == null || ownerUid == null) {
                        onError(getString(R.string.error_sign_in_and_select_vehicle))
                    } else {
                        composeScope.launch {
                            runCatching {
                                vehicleSharingRepository.createInvite(vehicleId, vehicleName, ownerUid, email, role)
                            }.onSuccess { onResult(it.code) }
                                .onFailure { onError(it.message ?: getString(R.string.error_create_invite_failed)) }
                        }
                    }
                },
                onJoinByCode = { code, onResult, onError ->
                    val uid = authRepository.currentUid
                    val email = authRepository.currentEmail
                    if (uid == null || email == null) {
                        onError(getString(R.string.error_sign_in_first))
                    } else {
                        composeScope.launch {
                            runCatching {
                                vehicleSharingRepository.joinByCode(code, uid, email, authRepository.currentDisplayName ?: "", authRepository.currentPhotoUrl)
                            }.onSuccess { vehicleName ->
                                onResult(vehicleName)
                                runCatching { cloudRepository.sync(uid, email, authRepository.currentDisplayName, authRepository.currentPhotoUrl) }
                            }.onFailure { onError(it.message ?: getString(R.string.error_join_failed)) }
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
                                message = getString(R.string.signed_in_tap_sync),
                            )
                        }.onFailure {
                            cloudState = cloudState.copy(
                                syncing = false,
                                message = it.message ?: getString(R.string.sign_in_failed),
                            )
                        }
                    }
                },
                onCloudSync = {
                    val uid = authRepository.currentUid
                    if (uid == null) {
                        cloudState = cloudState.copy(message = getString(R.string.error_sign_in_first))
                    } else {
                        composeScope.launch {
                            cloudState = cloudState.copy(syncing = true, message = null)
                            runCatching {
                                cloudRepository.sync(
                                    uid,
                                    authRepository.currentEmail,
                                    authRepository.currentDisplayName,
                                    authRepository.currentPhotoUrl,
                                )
                            }.onSuccess { result ->
                                MaintenanceReminderWorker.refresh(applicationContext)
                                cloudState = cloudState.copy(
                                    syncing = false,
                                    message = getString(R.string.sync_result, result.uploaded, result.downloaded, result.vehicles),
                                )
                            }.onFailure {
                                cloudState = cloudState.copy(
                                    syncing = false,
                                    message = it.message ?: getString(R.string.sync_failed),
                                )
                            }
                        }
                    }
                },
                onSignOut = {
                    authRepository.signOut()
                    cloudState = CloudUiState(message = getString(R.string.signed_out_local_data_intact))
                },
                syncConflicts = syncConflicts,
                onResolveConflict = { key, useLocal ->
                    composeScope.launch {
                        cloudState = cloudState.copy(syncing = true, message = null)
                        runCatching { cloudRepository.resolveConflict(key, useLocal) }
                            .onSuccess {
                                cloudState = cloudState.copy(
                                    syncing = false,
                                    message = if (useLocal) getString(R.string.conflict_used_local) else getString(R.string.conflict_used_cloud),
                                )
                            }
                            .onFailure {
                                cloudState = cloudState.copy(
                                    syncing = false,
                                    message = it.message ?: getString(R.string.conflict_resolve_failed),
                                )
                            }
                    }
                },
                onResolveAllConflicts = { useLocal ->
                    composeScope.launch {
                        cloudState = cloudState.copy(syncing = true, message = null)
                        runCatching { cloudRepository.resolveAllConflicts(useLocal) }
                            .onSuccess { count ->
                                cloudState = cloudState.copy(
                                    syncing = false,
                                    message = if (useLocal) getString(R.string.conflict_used_local_count, count) else getString(R.string.conflict_used_cloud_count, count),
                                )
                            }
                            .onFailure {
                                cloudState = cloudState.copy(
                                    syncing = false,
                                    message = it.message ?: getString(R.string.conflict_resolve_failed),
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

    private fun hasFineLocationPermission(): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // Gets the device location, then queries the Overpass API for nearby amenity=fuel points.
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
                    ?: error(getString(R.string.error_read_location_gps))
                NearbyStationRepository().fetchNearbyStations(applicationContext, location.latitude, location.longitude)
            }.onSuccess { stations -> onResult(stations) }
                .onFailure { onError?.invoke(it.message ?: getString(R.string.error_nearby_search_failed)) }
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
                    ?: error(getString(R.string.error_read_location_gps))
                weatherRepository.fetchCurrent(location.latitude, location.longitude)
            }.onSuccess { weather -> onResult(weather) }
                .onFailure { onError?.invoke(it.message ?: getString(R.string.error_weather_fetch_failed)) }
        }
    }
}
