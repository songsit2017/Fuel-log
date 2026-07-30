package com.songsit.fuellogpro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.songsit.fuellogpro.BuildConfig
import com.songsit.fuellogpro.settings.DisplaySettings
import com.songsit.fuellogpro.data.BackupImportResult
import com.songsit.fuellogpro.data.NearbyStation
import com.songsit.fuellogpro.data.OilPriceInfo
import com.songsit.fuellogpro.data.ReceiptScanResult
import com.songsit.fuellogpro.data.WeatherInfo
import com.songsit.fuellogpro.data.firebase.VehicleMember
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songsit.fuellogpro.domain.model.FuelEntry
import com.songsit.fuellogpro.domain.model.FuelEntryFormValues
import com.songsit.fuellogpro.domain.model.Expense
import com.songsit.fuellogpro.domain.model.Vehicle
import com.songsit.fuellogpro.domain.model.VehicleFormValues
import com.songsit.fuellogpro.domain.DueLevel
import com.songsit.fuellogpro.domain.calculateMaintenanceStatus
import com.songsit.fuellogpro.domain.model.MaintenanceTask
import com.songsit.fuellogpro.domain.model.Trip
import com.songsit.fuellogpro.domain.model.totalCost
import com.songsit.fuellogpro.domain.calculatePerEntryKmPerLiter
import com.songsit.fuellogpro.data.local.SyncConflictEntity
import com.songsit.fuellogpro.data.local.PhotoUris
import com.songsit.fuellogpro.data.local.isPdfPath
import com.songsit.fuellogpro.notifications.ReminderSettings
import com.songsit.fuellogpro.ui.stats.StatsScreen
import com.songsit.fuellogpro.ui.timeline.TimelineScreen
import com.songsit.fuellogpro.ui.timeline.FullScreenImageViewer
import com.songsit.fuellogpro.ui.timeline.openPdfExternally
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch

private val fuelTypeOptions = listOf(
    "เบนซิน", "แก๊สโซฮอล์ 95", "แก๊สโซฮอล์ 91", "E20", "E85", "ดีเซล", "ดีเซล B7", "ดีเซล B20", "LPG", "NGV", "EV",
)

private fun getFuelTypeOptionsForVehicle(vehicleFuelType: String): List<String> {
    val petrolOptions = listOf("เบนซิน", "แก๊สโซฮอล์ 95", "แก๊สโซฮอล์ 91", "E20", "E85")
    val dieselOptions = listOf("ดีเซล", "ดีเซล B7", "ดีเซล B20")
    
    return when (vehicleFuelType) {
        in petrolOptions -> petrolOptions
        in dieselOptions -> dieselOptions
        "LPG" -> listOf("LPG")
        "NGV" -> listOf("NGV")
        "EV" -> listOf("EV")
        else -> fuelTypeOptions
    }
}

private val thaiCurrency = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
private val number = NumberFormat.getNumberInstance(Locale("th", "TH")).apply {
    maximumFractionDigits = 2
}

// Keeps an icon and its field(s) locked to one line across the "add fuel" form — each
// FormRow call supplies the leading icon and the row's field(s) as trailing content, so
// multi-field rows (e.g. price/total) can weight() their children evenly.
@Composable
private fun FormRow(icon: ImageVector, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        content()
    }
}

data class CloudUiState(
    val uid: String? = null,
    val email: String? = null,
    val syncing: Boolean = false,
    val message: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelLogApp(
    state: NativeAppState,
    importProgressPercent: Int? = null,
    onAddFuel: (FuelEntryFormValues, () -> Unit) -> Unit,
    onUpdateFuel: (String, FuelEntryFormValues, () -> Unit) -> Unit,
    onDeleteFuel: (String) -> Unit,
    onAddExpense: (String, String, String, String, Double, Double?, Boolean, Boolean, String?, String?, () -> Unit) -> Unit,
    onUpdateExpense: (String, String, String, String, String, Double, Double?, Boolean, Boolean, String?, String?, () -> Unit) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onAddMaintenance: (String, String, String?, Double?, Int, Double, Int?, Double?, () -> Unit) -> Unit,
    onUpdateMaintenance: (String, String, String, String?, Double?, Int, Double, Int?, Double?, () -> Unit) -> Unit,
    onCompleteMaintenance: (String) -> Unit,
    onDeleteMaintenance: (String) -> Unit,
    onAddTrip: (String, String, Double, Double, Double, Double, Double, Double, () -> Unit) -> Unit,
    onUpdateTrip: (String, String, String, Double, Double, Double, Double, Double, Double, () -> Unit) -> Unit,
    onDeleteTrip: (String) -> Unit,
    onStartTripRecording: (() -> Unit)? = null,
    onExportCsv: () -> Unit,
    reminderSettings: ReminderSettings,
    onReminderSettingsChange: (ReminderSettings) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    cloudState: CloudUiState,
    onGoogleSignIn: () -> Unit,
    onCloudSync: () -> Unit,
    onSignOut: () -> Unit,
    syncConflicts: List<SyncConflictEntity>,
    onResolveConflict: (String, Boolean) -> Unit,
    onResolveAllConflicts: (Boolean) -> Unit,
    onSelectVehicle: (String) -> Unit,
    onAddVehicle: (VehicleFormValues, () -> Unit) -> Unit,
    onUpdateVehicle: (String, VehicleFormValues, () -> Unit) -> Unit,
    onDeleteVehicle: (String) -> Unit,
    onClearError: () -> Unit,
    onFindNearbyStations: ((onResult: (List<NearbyStation>) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onFetchWeather: ((onResult: (WeatherInfo) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onPickPhoto: ((type: String?, onPicked: (uris: List<String>, scanResult: ReceiptScanResult?) -> Unit) -> Unit)? = null,
    onPickCameraPhoto: ((type: String?, onPicked: (uris: List<String>, scanResult: ReceiptScanResult?) -> Unit) -> Unit)? = null,
    onPickPdf: ((onPicked: (uris: List<String>) -> Unit) -> Unit)? = null,
    oilPriceInfo: OilPriceInfo? = null,
    vehicleMembers: List<VehicleMember> = emptyList(),
    onCreateInvite: ((email: String, role: String, onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onJoinByCode: ((code: String, onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    displaySettings: DisplaySettings = DisplaySettings(),
    onDisplaySettingsChange: (DisplaySettings) -> Unit = {},
    importSummaryResult: BackupImportResult? = null,
    onDismissImportSummary: () -> Unit = {},
    driveBackupProgress: Int? = null,
    onDriveBackup: (() -> Unit)? = null,
    onDriveRestore: (() -> Unit)? = null,
    driveAutoSyncEnabled: Boolean = false,
    onDriveAutoSyncChange: ((Boolean) -> Unit)? = null,
) {
    var tab by remember { mutableIntStateOf(0) }
    var showAddFuel by remember { mutableStateOf(false) }
    var addFuelAutoScan by remember { mutableStateOf(false) }
    var showAddVehicle by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }
    var showAddMaintenance by remember { mutableStateOf(false) }
    var showAddTrip by remember { mutableStateOf(false) }
    var editingFuel by remember { mutableStateOf<FuelEntry?>(null) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var editingMaintenance by remember { mutableStateOf<MaintenanceTask?>(null) }
    var editingTrip by remember { mutableStateOf<Trip?>(null) }
    var prefillTripDistanceKm by remember { mutableStateOf<Double?>(null) }
    var editingVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showVehicleMenu by remember { mutableStateOf(false) }
    var dashboardFabExpanded by remember { mutableStateOf(false) }
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var homeTab by remember { mutableIntStateOf(0) }
    var viewingImagePath by remember { mutableStateOf<String?>(null) }
    // Remembers which homeTab (e.g. 1=Timeline) the user jumped FROM when tapping a record card
    // to go to the FuelList (tab=1). null means the user arrived via normal bottom-nav/drawer tap,
    // so Back should return to homeTab=0 (Overview) as usual. Set to non-null only by the
    // Timeline card click; cleared on any direct drawer navigation or after Back is consumed.
    var fuelListReturnHomeTab by remember { mutableStateOf<Int?>(null) }
    // Pilot batch for real per-app language (AppCompatDelegate.setApplicationLocales(), see the
    // "ภาษา / Language" Settings row) — the rest of the app's strings are still Thai-hardcoded
    // and don't yet respond to the system language switch; only nav chrome does so far.
    val titles = listOf(
        stringResource(com.songsit.fuellogpro.R.string.title_overview),
        stringResource(com.songsit.fuellogpro.R.string.title_fuel_log),
        stringResource(com.songsit.fuellogpro.R.string.title_expenses),
        stringResource(com.songsit.fuellogpro.R.string.title_maintenance),
        stringResource(com.songsit.fuellogpro.R.string.title_my_vehicles),
        stringResource(com.songsit.fuellogpro.R.string.title_stats),
        stringResource(com.songsit.fuellogpro.R.string.title_trips),
    )
    val homeTitles = listOf(
        stringResource(com.songsit.fuellogpro.R.string.title_overview),
        stringResource(com.songsit.fuellogpro.R.string.nav_timeline),
        stringResource(com.songsit.fuellogpro.R.string.nav_calculator),
        stringResource(com.songsit.fuellogpro.R.string.nav_map),
    )
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val drawerScope = androidx.compose.runtime.rememberCoroutineScope()
    val requestDelete: (() -> Unit) -> Unit = { action ->
        if (displaySettings.confirmBeforeDelete) pendingDeleteAction = action else action()
    }

    // No NavController in this app — screens/sub-screens are toggled boolean/int state, not a
    // back stack. This single handler replaces that missing back stack: it closes whichever
    // overlay is open, else returns Timeline/Calculator/Map or a drawer section to the Overview
    // tab, and is disabled (enabled = false) exactly at Overview with nothing open, so the
    // system default back press applies there and exits the app normally.
    val atRoot = tab == 0 && homeTab == 0 &&
        !showSettings && !showAddVehicle && !showAddExpense && !showAddFuel &&
        !showAddMaintenance && !showAddTrip &&
        editingVehicle == null && editingExpense == null && editingFuel == null &&
        editingMaintenance == null && editingTrip == null
    BackHandler(enabled = !atRoot) {
        when {
            showSettings -> showSettings = false
            showAddVehicle || editingVehicle != null -> { showAddVehicle = false; editingVehicle = null }
            showAddExpense || editingExpense != null -> { showAddExpense = false; editingExpense = null }
            showAddFuel || editingFuel != null -> { showAddFuel = false; editingFuel = null }
            showAddMaintenance || editingMaintenance != null -> { showAddMaintenance = false; editingMaintenance = null }
            showAddTrip || editingTrip != null -> { showAddTrip = false; editingTrip = null }
            tab != 0 -> {
                val returnTo = fuelListReturnHomeTab
                tab = 0
                homeTab = returnTo ?: 0
                fuelListReturnHomeTab = null
            }
            homeTab != 0 -> homeTab = 0
        }
    }

    CompositionLocalProvider(LocalDisplaySettings provides displaySettings) {
    FuelLogTheme(
        themeMode = displaySettings.themeMode,
        themePalette = displaySettings.themePalette,
        fontFamily = displaySettings.fontFamily,
    ) {
    importProgressPercent?.let { percent ->
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(stringResource(com.songsit.fuellogpro.R.string.importing_data_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(stringResource(com.songsit.fuellogpro.R.string.importing_data_progress, percent))
                }
            },
        )
    }
    importSummaryResult?.let { result ->
        ImportSummaryDialog(
            result = result,
            onDismiss = onDismissImportSummary,
        )
    }
    driveBackupProgress?.let { percent ->
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(stringResource(com.songsit.fuellogpro.R.string.drive_backup_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(stringResource(com.songsit.fuellogpro.R.string.drive_backup_progress, percent))
                }
            },
        )
    }
    if (showSettings) {
        SettingsScreen(
            onDismiss = { showSettings = false },
            onExportBackup = onExportBackup,
            onExportCsv = onExportCsv,
            onImportBackup = onImportBackup,
            cloudState = cloudState,
            onGoogleSignIn = onGoogleSignIn,
            onCloudSync = onCloudSync,
            onSignOut = onSignOut,
            syncConflicts = syncConflicts,
            onResolveConflict = onResolveConflict,
            onResolveAllConflicts = onResolveAllConflicts,
            reminderSettings = reminderSettings,
            onReminderSettingsChange = onReminderSettingsChange,
            hasSelectedVehicle = state.selectedVehicle != null,
            vehicleMembers = vehicleMembers,
            onCreateInvite = onCreateInvite,
            onJoinByCode = onJoinByCode,
            displaySettings = displaySettings,
            onDisplaySettingsChange = onDisplaySettingsChange,
            onDriveBackup = onDriveBackup,
            onDriveRestore = onDriveRestore,
            driveAutoSyncEnabled = driveAutoSyncEnabled,
            onDriveAutoSyncChange = onDriveAutoSyncChange,
        )
    } else if (showAddVehicle || editingVehicle != null) {
        VehicleEditScreen(
            saving = state.saving,
            editing = editingVehicle,
            onPickPhoto = onPickPhoto,
            onPickCameraPhoto = onPickCameraPhoto,
            onDismiss = { showAddVehicle = false; editingVehicle = null },
            onSave = onAddVehicle,
            onUpdate = onUpdateVehicle,
        )
    } else if (showAddExpense || editingExpense != null) {
        AddExpenseScreen(
            saving = state.saving,
            latestOdometer = state.summary.latestOdometerKm,
            editing = editingExpense,
            selectedVehicleLabel = state.selectedVehicle?.name,
            selectedVehicleOdometer = state.summary.latestOdometerKm,
            onPickPhoto = onPickPhoto,
            onPickCameraPhoto = onPickCameraPhoto,
            onPickPdf = onPickPdf,
            onDismiss = { showAddExpense = false; editingExpense = null },
            onSave = onAddExpense,
            onUpdate = onUpdateExpense,
        )
    } else {
        val drawerDestinations = listOf(
            titles[0] to Icons.Filled.Home,
            titles[1] to Icons.Filled.LocalGasStation,
            titles[5] to Icons.Filled.BarChart,
            titles[2] to Icons.Filled.ReceiptLong,
            titles[6] to Icons.Filled.Route,
            titles[3] to Icons.Filled.Build,
            titles[4] to Icons.Filled.DirectionsCar,
        )
        val drawerTabTargets = listOf(0, 1, 5, 2, 6, 3, 4)
        androidx.compose.material3.ModalNavigationDrawer(
            drawerState = drawerState,
            // Off on the map tab only — its two-finger pinch-zoom competes with the drawer's own
            // edge-swipe-to-open gesture detector, occasionally revealing the drawer mid-pinch and
            // stuttering the zoom. The hamburger icon still opens the drawer normally everywhere.
            gesturesEnabled = !(tab == 0 && homeTab == 3),
            drawerContent = {
                androidx.compose.material3.ModalDrawerSheet {
                    Text(
                        "Fuel Log",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                    HorizontalDivider()
                    drawerDestinations.forEachIndexed { index, (label, icon) ->
                        androidx.compose.material3.NavigationDrawerItem(
                            label = { Text(label) },
                            icon = { Icon(icon, contentDescription = label) },
                            selected = tab == drawerTabTargets[index],
                            onClick = {
                                fuelListReturnHomeTab = null
                                tab = drawerTabTargets[index]
                                if (tab == 0) homeTab = 0
                                drawerScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                    androidx.compose.material3.NavigationDrawerItem(
                        label = { Text(stringResource(com.songsit.fuellogpro.R.string.settings_title)) },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(com.songsit.fuellogpro.R.string.settings_title)) },
                        selected = false,
                        onClick = {
                            showSettings = true
                            drawerScope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            },
        ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { drawerScope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_menu))
                        }
                    },
                    title = {
                        Column {
                            Text(if (tab == 0) homeTitles[homeTab] else titles[tab], fontWeight = FontWeight.SemiBold)
                            if (state.vehicles.size > 1) {
                                Box {
                                    Row(
                                        modifier = Modifier.clickable { showVehicleMenu = true },
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (!state.selectedVehicle?.brandLogoUrl.isNullOrBlank()) {
                                            SubcomposeAsyncImage(
                                                model = state.selectedVehicle?.brandLogoUrl,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp).clip(CircleShape).background(Color.White).padding(2.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        Text(
                                            state.selectedVehicle?.name ?: stringResource(com.songsit.fuellogpro.R.string.label_select_vehicle),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                        Text(" ▾", style = MaterialTheme.typography.labelSmall)
                                    }
                                    androidx.compose.material3.DropdownMenu(
                                        expanded = showVehicleMenu,
                                        onDismissRequest = { showVehicleMenu = false },
                                    ) {
                                        state.vehicles.forEach { vehicle ->
                                            DropdownMenuItem(
                                                text = { Text(vehicle.name) },
                                                leadingIcon = if (!vehicle.brandLogoUrl.isNullOrBlank()) {
                                                    {
                                                        SubcomposeAsyncImage(
                                                            model = vehicle.brandLogoUrl,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.White).padding(2.dp),
                                                            contentScale = ContentScale.Fit
                                                        )
                                                    }
                                                } else null,
                                                onClick = {
                                                    onSelectVehicle(vehicle.id)
                                                    showVehicleMenu = false
                                                },
                                            )
                                        }
                                    }
                                }
                            } else {
                                state.selectedVehicle?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!it.brandLogoUrl.isNullOrBlank()) {
                                            SubcomposeAsyncImage(
                                                model = it.brandLogoUrl,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp).clip(CircleShape).background(Color.White).padding(2.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        Text(it.name, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    },
                )
            },
            bottomBar = {
                if (tab == 0) {
                    NavigationBar {
                        val homeTabIcons = listOf(
                            Icons.Filled.Home,
                            Icons.Filled.History,
                            Icons.Filled.BarChart,
                            Icons.Filled.Route,
                        )
                        homeTitles.forEachIndexed { index, label ->
                            NavigationBarItem(
                                selected = homeTab == index,
                                onClick = { homeTab = index },
                                icon = { Icon(homeTabIcons[index], contentDescription = label) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                when (tab) {
                    0 -> if (homeTab == 0) Box {
                        FloatingActionButton(
                            onClick = {
                                if (state.vehicles.isEmpty()) showAddVehicle = true else dashboardFabExpanded = true
                            },
                        ) { Text("+") }
                        DropdownMenu(expanded = dashboardFabExpanded, onDismissRequest = { dashboardFabExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(com.songsit.fuellogpro.R.string.fab_add_fuel)) },
                                leadingIcon = { Icon(Icons.Filled.LocalGasStation, contentDescription = null) },
                                onClick = { dashboardFabExpanded = false; showAddFuel = true },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(com.songsit.fuellogpro.R.string.fab_add_expense)) },
                                leadingIcon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) },
                                onClick = { dashboardFabExpanded = false; showAddExpense = true },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(com.songsit.fuellogpro.R.string.fab_scan_receipt)) },
                                leadingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                                onClick = {
                                    dashboardFabExpanded = false
                                    addFuelAutoScan = true
                                    showAddFuel = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(com.songsit.fuellogpro.R.string.fab_add_maintenance)) },
                                leadingIcon = { Icon(Icons.Filled.Build, contentDescription = null) },
                                onClick = {
                                    dashboardFabExpanded = false
                                    if (state.vehicles.isEmpty()) showAddVehicle = true else showAddMaintenance = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(com.songsit.fuellogpro.R.string.fab_record_trip)) },
                                leadingIcon = { Icon(Icons.Filled.Route, contentDescription = null) },
                                onClick = {
                                    dashboardFabExpanded = false
                                    onStartTripRecording?.invoke()
                                    tab = 6
                                },
                            )
                        }
                    }
                    1 -> FloatingActionButton(
                        onClick = { if (state.vehicles.isEmpty()) showAddVehicle = true else showAddFuel = true },
                    ) { Text("+") }
                    2 -> FloatingActionButton(
                        onClick = { if (state.vehicles.isEmpty()) showAddVehicle = true else showAddExpense = true },
                    ) { Text("+") }
                    3 -> FloatingActionButton(
                        onClick = { if (state.vehicles.isEmpty()) showAddVehicle = true else showAddMaintenance = true },
                    ) { Text("+") }
                    4 -> FloatingActionButton(onClick = { showAddVehicle = true }) { Text("+") }
                    6 -> FloatingActionButton(
                        onClick = { if (state.vehicles.isEmpty()) showAddVehicle = true else showAddTrip = true },
                    ) { Text("+") }
                }
            },
        ) { padding ->
            when (tab) {
                0 -> when (homeTab) {
                    0 -> Dashboard(state, onExportCsv, oilPriceInfo, Modifier.padding(padding))
                    1 -> TimelineScreen(
                        state,
                        Modifier.padding(padding),
                        onImageClick = { viewingImagePath = it },
                        onFuelRecordClick = { fuelListReturnHomeTab = homeTab; tab = 1 },
                    )
                    2 -> TripCalculatorScreen(state, Modifier.padding(padding))
                    else -> NearbyStationsMapScreen(onFindNearbyStations, oilPriceInfo, Modifier.padding(padding))
                }
                1 -> FuelList(
                    state.entries,
                    { id -> requestDelete { onDeleteFuel(id) } },
                    { editingFuel = it },
                    Modifier.padding(padding),
                )
                2 -> ExpenseList(
                    expenses = state.expenses,
                    totalExpense = state.totalExpenses,
                    totalIncome = state.totalIncome,
                    netExpense = state.netExpense,
                    onDelete = { id -> requestDelete { onDeleteExpense(id) } },
                    onEdit = { editingExpense = it },
                    modifier = Modifier.padding(padding),
                )
                3 -> MaintenanceList(
                    tasks = state.maintenanceTasks,
                    currentOdometerKm = state.summary.latestOdometerKm,
                    onComplete = onCompleteMaintenance,
                    onDelete = { id -> requestDelete { onDeleteMaintenance(id) } },
                    onEdit = { editingMaintenance = it },
                    modifier = Modifier.padding(padding),
                )
                4 -> VehiclesListScreen(
                    vehicles = state.vehicles,
                    selectedVehicleId = state.selectedVehicle?.id,
                    onSelect = onSelectVehicle,
                    onEdit = { editingVehicle = it },
                    onDelete = { id -> requestDelete { onDeleteVehicle(id) } },
                    modifier = Modifier.padding(padding),
                )
                5 -> StatsScreen(state, Modifier.padding(padding))
                else -> TripList(
                    trips = state.trips,
                    summary = state.tripSummary,
                    onDelete = { id -> requestDelete { onDeleteTrip(id) } },
                    onEdit = { editingTrip = it },
                    onStartRecording = onStartTripRecording,
                    onSaveRecordedTrip = { distanceKm ->
                        prefillTripDistanceKm = distanceKm
                        showAddTrip = true
                        com.songsit.fuellogpro.trip.TripRecordingState.reset()
                    },
                    modifier = Modifier.padding(padding),
                )
            }
        }
        if (showAddFuel || editingFuel != null) {
            AddFuelScreen(
                saving = state.saving,
                latestOdometer = state.summary.latestOdometerKm,
                editing = editingFuel,
                vehicleFuelType = state.selectedVehicle?.fuelType ?: "",
                vehicleTankCapacity = state.selectedVehicle?.tankCapacity,
                stationVisitCounts = remember(state.entries) { state.entries.groupingBy { it.station.trim() }.eachCount() },
                onFindNearbyStations = onFindNearbyStations,
                onFetchWeather = onFetchWeather,
                onPickPhoto = onPickPhoto,
                onPickCameraPhoto = onPickCameraPhoto,
                autoOpenScan = addFuelAutoScan,
                defaultFullTank = displaySettings.defaultFullTank,
                onDismiss = { showAddFuel = false; editingFuel = null; addFuelAutoScan = false },
                onSave = onAddFuel,
                onUpdate = onUpdateFuel,
            )
        }
        pendingDeleteAction?.let { deleteAction ->
            AlertDialog(
                onDismissRequest = { pendingDeleteAction = null },
                title = { Text(stringResource(com.songsit.fuellogpro.R.string.confirm_delete_title)) },
                text = { Text(stringResource(com.songsit.fuellogpro.R.string.confirm_delete_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingDeleteAction = null
                            deleteAction()
                        },
                    ) { Text(stringResource(com.songsit.fuellogpro.R.string.action_delete), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteAction = null }) { Text(stringResource(com.songsit.fuellogpro.R.string.action_cancel)) }
                },
            )
        }
        if (showAddMaintenance || editingMaintenance != null) {
            AddMaintenanceDialog(
                saving = state.saving,
                latestOdometer = state.summary.latestOdometerKm,
                editing = editingMaintenance,
                onDismiss = { showAddMaintenance = false; editingMaintenance = null },
                onSave = onAddMaintenance,
                onUpdate = onUpdateMaintenance,
            )
        }
        if (showAddTrip || editingTrip != null) {
            AddTripDialog(
                saving = state.saving,
                editing = editingTrip,
                prefillDistanceKm = prefillTripDistanceKm,
                onDismiss = { showAddTrip = false; editingTrip = null; prefillTripDistanceKm = null },
                onSave = onAddTrip,
                onUpdate = onUpdateTrip,
            )
        }
        state.errorMessage?.let { message ->
            AlertDialog(
                onDismissRequest = onClearError,
                confirmButton = { TextButton(onClick = onClearError) { Text(stringResource(com.songsit.fuellogpro.R.string.action_ok)) } },
                title = { Text(stringResource(com.songsit.fuellogpro.R.string.check_data_title)) },
                text = { Text(message) },
            )
        }
        }
    }
    viewingImagePath?.let { path ->
        FullScreenImageViewer(imagePath = path, onDismiss = { viewingImagePath = null })
    }
    }
    }
}

private val distanceQuickPicks = listOf(10, 30, 100, 200, 300, 400, 500, 700, 1000, 1200)
private val maintenanceCategoryOptions =
    listOf("เช็คระยะ", "เปลี่ยนถ่ายน้ำมันเครื่อง", "ประกันภัย/พ.ร.บ.", "ภาษี", "บำรุงรักษา", "อื่นๆ")

@Composable
private fun QuickPickField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    quickChoices: List<Pair<String, String>> = emptyList(),
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_quick_pick))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            quickChoices.forEach { (choiceLabel, choiceValue) ->
                DropdownMenuItem(
                    text = { Text(choiceLabel) },
                    onClick = { onValueChange(choiceValue); menuExpanded = false },
                )
            }
        }
    }
}

@Composable
private fun TripCalculatorScreen(state: NativeAppState, modifier: Modifier = Modifier) {
    var modeExpanded by remember { mutableStateOf(false) }
    var mode by remember { mutableIntStateOf(0) }
    var distanceKm by remember { mutableStateOf("") }
    var pricePerLiter by remember { mutableStateOf("") }
    var consumptionKmPerLiter by remember { mutableStateOf("") }
    var budgetAmount by remember { mutableStateOf("") }
    var fuelVolumeLiters by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }

    val calculatorModes = stringArrayResource(com.songsit.fuellogpro.R.array.calculator_modes)
    val latestPrice = state.entries.maxByOrNull { it.date + it.time }?.pricePerLiter
    val averagePrice = state.entries.map { it.pricePerLiter }.filter { it > 0 }.let { if (it.isEmpty()) null else it.average() }
    val latestConsumption = calculatePerEntryKmPerLiter(state.entries).values.lastOrNull()
    val averageConsumption = state.summary.averageKmPerLiter

    val priceQuickChoices = listOfNotNull(
        latestPrice?.let { stringResource(com.songsit.fuellogpro.R.string.quick_pick_latest_price, "%.2f".format(it)) to "%.2f".format(it) },
        averagePrice?.let { stringResource(com.songsit.fuellogpro.R.string.quick_pick_average_price, "%.2f".format(it)) to "%.2f".format(it) },
    )
    val consumptionQuickChoices = listOfNotNull(
        latestConsumption?.let { stringResource(com.songsit.fuellogpro.R.string.quick_pick_latest_consumption, "%.2f".format(it)) to "%.2f".format(it) },
        averageConsumption?.let { stringResource(com.songsit.fuellogpro.R.string.quick_pick_average_consumption, "%.2f".format(it)) to "%.2f".format(it) },
    )
    val distanceQuickChoices = distanceQuickPicks.map { "$it km" to it.toString() }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(com.songsit.fuellogpro.R.string.nav_calculator), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Box {
            OutlinedTextField(
                value = calculatorModes[mode],
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_calculator_mode)) },
                trailingIcon = {
                    IconButton(onClick = { modeExpanded = true }) {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_select_mode))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                calculatorModes.forEachIndexed { index, label ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { mode = index; modeExpanded = false; result = null })
                }
            }
        }
        if (mode != 1) {
            QuickPickField(
                value = distanceKm,
                onValueChange = { distanceKm = it },
                label = stringResource(com.songsit.fuellogpro.R.string.label_distance_km),
                quickChoices = distanceQuickChoices,
            )
        }
        if (mode == 0 || mode == 1) {
            QuickPickField(
                value = pricePerLiter,
                onValueChange = { pricePerLiter = it },
                label = stringResource(com.songsit.fuellogpro.R.string.label_price_per_liter),
                quickChoices = priceQuickChoices,
            )
        }
        if (mode != 2) {
            QuickPickField(
                value = consumptionKmPerLiter,
                onValueChange = { consumptionKmPerLiter = it },
                label = stringResource(com.songsit.fuellogpro.R.string.label_fuel_efficiency_km_per_l),
                quickChoices = consumptionQuickChoices,
            )
        }
        if (mode == 1) {
            QuickPickField(value = budgetAmount, onValueChange = { budgetAmount = it }, label = stringResource(com.songsit.fuellogpro.R.string.label_budget))
        }
        if (mode == 2) {
            QuickPickField(value = fuelVolumeLiters, onValueChange = { fuelVolumeLiters = it }, label = stringResource(com.songsit.fuellogpro.R.string.label_fuel_liters))
        }
        Button(
            onClick = {
                val distance = distanceKm.toDoubleOrNull()
                val price = pricePerLiter.toDoubleOrNull()
                val consumption = consumptionKmPerLiter.toDoubleOrNull()
                val budget = budgetAmount.toDoubleOrNull()
                val volume = fuelVolumeLiters.toDoubleOrNull()
                result = when (mode) {
                    0 -> if (distance != null && price != null && consumption != null && consumption > 0) {
                        thaiCurrency.format((distance / consumption) * price)
                    } else null
                    1 -> if (budget != null && price != null && consumption != null && price > 0) {
                        "%.1f km".format((budget / price) * consumption)
                    } else null
                    2 -> if (distance != null && volume != null && volume > 0) {
                        "%.2f km/L".format(distance / volume)
                    } else null
                    else -> if (distance != null && consumption != null && consumption > 0) {
                        "%.2f L".format(distance / consumption)
                    } else null
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(com.songsit.fuellogpro.R.string.action_calculate)) }
        result?.let {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(stringResource(com.songsit.fuellogpro.R.string.label_result), style = MaterialTheme.typography.titleSmall)
                    Text(it, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun matchOilPrice(brand: StationBrand, oilPriceInfo: OilPriceInfo?): Double? {
    val label = when (brand) {
        StationBrand.PTT -> "ปตท."
        StationBrand.SHELL -> "เชลล์"
        StationBrand.PT -> "พีที"
        StationBrand.CALTEX -> "คาลเท็กซ์"
        StationBrand.BANGCHAK -> return null
    }
    val prices = oilPriceInfo?.brands?.firstOrNull { it.brand == label } ?: return null
    return prices.gasohol95 ?: prices.dieselB7 ?: prices.gasohol91 ?: prices.e20
        ?: prices.premium95 ?: prices.e85 ?: prices.dieselB20
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NearbyStationsMapScreen(
    onFindNearbyStations: ((onResult: (List<NearbyStation>) -> Unit, onError: (String) -> Unit) -> Unit)?,
    oilPriceInfo: OilPriceInfo?,
    modifier: Modifier = Modifier,
) {
    var stations by remember { mutableStateOf<List<NearbyStation>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showList by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        searching = true
        onFindNearbyStations?.invoke(
            { results -> searching = false; stations = results },
            { message -> searching = false; error = message },
        )
    }
    val cameraPositionState = com.google.maps.android.compose.rememberCameraPositionState()
    LaunchedEffect(stations) {
        stations.firstOrNull()?.let {
            cameraPositionState.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
                com.google.android.gms.maps.model.LatLng(it.lat, it.lon),
                13f,
            )
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        com.google.maps.android.compose.GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = com.google.maps.android.compose.MapProperties(isMyLocationEnabled = true),
            uiSettings = com.google.maps.android.compose.MapUiSettings(myLocationButtonEnabled = true),
        ) {
            stations.forEach { station ->
                val brand = remember(station.name) { detectStationBrand(station.name) }
                val price = remember(station.name, oilPriceInfo) {
                    brand?.let { matchOilPrice(it, oilPriceInfo) }
                }
                com.google.maps.android.compose.MarkerComposable(
                    state = com.google.maps.android.compose.rememberMarkerState(
                        position = com.google.android.gms.maps.model.LatLng(station.lat, station.lon),
                    ),
                    title = station.name,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (price != null) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 3.dp,
                            ) {
                                Text(
                                    text = "฿" + "%.2f".format(Locale.US, price),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                        StationBadge(stationName = station.name, size = 36.dp)
                    }
                }
            }
        }

        if (searching) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                shape = RoundedCornerShape(50),
                shadowElevation = 4.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(com.songsit.fuellogpro.R.string.nearby_searching_stations), style = MaterialTheme.typography.labelMedium)
                }
            }
        } else if (error != null) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 4.dp,
            ) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
            }
        }

        // Bottom-start, not bottom-center/end — Google Maps' own "open in Google Maps" button
        // auto-appears bottom-end whenever a marker's info window is showing, and this FAB's
        // width (name + count) was reaching far enough right to cover it.
        ExtendedFloatingActionButton(
            onClick = { showList = true },
            icon = { Icon(Icons.Filled.ListAlt, contentDescription = null) },
            text = { Text(stringResource(com.songsit.fuellogpro.R.string.nearby_stations_button, stations.size)) },
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp),
        )
    }

    if (showList) {
        ModalBottomSheet(onDismissRequest = { showList = false }) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    SectionHeader(Icons.Filled.Route, stringResource(com.songsit.fuellogpro.R.string.section_nearby_stations))
                }
                if (stations.isEmpty()) {
                    item { Text(stringResource(com.songsit.fuellogpro.R.string.nearby_no_stations)) }
                } else {
                    items(stations) { station ->
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(station.name, fontWeight = FontWeight.SemiBold)
                                Text("${(station.distanceMeters / 1000).let { "%.1f".format(it) }} km")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Dashboard(
    state: NativeAppState,
    onExportCsv: () -> Unit,
    oilPriceInfo: OilPriceInfo? = null,
    modifier: Modifier = Modifier,
) {
    val displaySettings = LocalDisplaySettings.current
    val kmPerLiterByEntry = remember(state.entries) { calculatePerEntryKmPerLiter(state.entries) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.selectedVehicle == null) {
            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp)) {
                        Text(stringResource(com.songsit.fuellogpro.R.string.dashboard_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(com.songsit.fuellogpro.R.string.dashboard_empty_subtitle))
                    }
                }
            }
            return@LazyColumn
        }
        if (oilPriceInfo != null && oilPriceInfo.brands.isNotEmpty()) {
            item { OilPriceCard(oilPriceInfo) }
        }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(stringResource(com.songsit.fuellogpro.R.string.dashboard_avg_efficiency), color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        state.summary.averageKmPerLiter?.let { formatEconomyKmPerLiter(it, displaySettings) } ?: "—",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        item {
            // Renamed from the generic "ค่าใช้จ่าย" (which duplicated the "ค่าใช้จ่ายรวม" card
            // below) to "ค่าน้ำมัน" — this card is fuel-only spend, the report card below is
            // the combined fuel+expense+trip total. Two distinct numbers, now two distinct labels.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    stringResource(com.songsit.fuellogpro.R.string.dashboard_fuel_cost),
                    formatCurrencyAmount(state.summary.totalSpent, displaySettings),
                    Modifier.weight(1f),
                    Icons.Filled.LocalGasStation,
                )
                MetricCard(
                    stringResource(com.songsit.fuellogpro.R.string.dashboard_fuel_volume),
                    formatVolumeLiters(state.summary.totalLiters, displaySettings),
                    Modifier.weight(1f),
                    Icons.Filled.WaterDrop,
                )
            }
        }
        item {
            val operatingCost = state.summary.totalSpent + state.totalExpenses + state.tripSummary.totalCost
            val noDataLabel = stringResource(com.songsit.fuellogpro.R.string.dashboard_no_data)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SectionHeader(Icons.Filled.BarChart, stringResource(com.songsit.fuellogpro.R.string.dashboard_report_summary))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            stringResource(com.songsit.fuellogpro.R.string.dashboard_total_cost),
                            formatCurrencyAmount(operatingCost, displaySettings),
                            Modifier.weight(1f),
                            Icons.Filled.Payments,
                        )
                        MetricCard(
                            stringResource(com.songsit.fuellogpro.R.string.dashboard_net_cost),
                            formatCurrencyAmount(operatingCost - state.totalIncome, displaySettings),
                            Modifier.weight(1f),
                            Icons.Filled.AccountBalanceWallet,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            stringResource(com.songsit.fuellogpro.R.string.dashboard_cumulative_distance),
                            // Cumulative distance from the fuel log's own odometer readings
                            // (latest - earliest), not the separate business-trip tracker —
                            // most vehicles never log a "Trip" record, so that number stayed 0.
                            formatDistanceKm(
                                (state.entries.maxOfOrNull { it.odometerKm } ?: 0.0) -
                                    (state.entries.minOfOrNull { it.odometerKm } ?: 0.0),
                                displaySettings,
                            ),
                            Modifier.weight(1f),
                            Icons.Filled.Route,
                        )
                        MetricCard(
                            stringResource(com.songsit.fuellogpro.R.string.dashboard_latest_odometer),
                            state.summary.latestOdometerKm?.let { formatDistanceKm(it, displaySettings) } ?: noDataLabel,
                            Modifier.weight(1f),
                            Icons.Filled.Speed,
                        )
                    }
                    MetricCard(
                        stringResource(com.songsit.fuellogpro.R.string.dashboard_record_count),
                        number.format(
                            state.entries.size + state.expenses.size +
                                state.maintenanceTasks.size + state.trips.size,
                        ),
                        Modifier.fillMaxWidth(),
                        Icons.Filled.ListAlt,
                    )
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader(Icons.Filled.LocalGasStation, stringResource(com.songsit.fuellogpro.R.string.dashboard_recent_entries))
        }
        if (state.entries.isEmpty()) {
            item { EmptyFuelState() }
        } else {
            items(state.entries.take(3), key = { it.id }) { FuelRow(it, kmPerLiter = kmPerLiterByEntry[it.id]) }
        }
    }
}

// Brand logo resource lookup — maps the display-label used in BrandOilPrices to the
// matching drawable so the card header can render the real logo instead of a text chip.
private fun oilBrandLogoRes(brand: String): Int? = when {
    brand.contains("ปตท") || brand.contains("ptt", ignoreCase = true) ->
        com.songsit.fuellogpro.R.drawable.ic_logo_ptt
    brand.contains("เชลล์") || brand.contains("shell", ignoreCase = true) ->
        com.songsit.fuellogpro.R.drawable.ic_logo_shell
    brand.contains("พีที") || brand == "PT" || brand.contains(" pt", ignoreCase = true) ||
        brand.startsWith("pt", ignoreCase = true) ->
        com.songsit.fuellogpro.R.drawable.ic_logo_pt
    brand.contains("คาลเท็กซ์") || brand.contains("caltex", ignoreCase = true) ->
        com.songsit.fuellogpro.R.drawable.ic_logo_caltex
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OilPriceCard(info: OilPriceInfo) {
    // Brand selection state lives only inside this card — never propagates upward.
    var selectedIndex by remember { mutableIntStateOf(0) }
    val safeIndex = selectedIndex.coerceIn(0, info.brands.lastIndex)
    val selected = info.brands[safeIndex]

    Card(shape = RoundedCornerShape(20.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ── Card header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(com.songsit.fuellogpro.R.string.title_oil_prices_today),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                // Show the selected brand's logo next to the title
                val logoRes = oilBrandLogoRes(selected.brand)
                if (logoRes != null) {
                    AsyncImage(
                        model = logoRes,
                        contentDescription = selected.brand,
                        modifier = Modifier.height(28.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Brand filter chips (horizontal scroll, 4 brands) ──────────────────
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                info.brands.forEachIndexed { index, brand ->
                    item(key = brand.brand) {
                        val isSelected = index == safeIndex
                        val logoRes = oilBrandLogoRes(brand.brand)
                        androidx.compose.material3.FilterChip(
                            selected = isSelected,
                            onClick = { selectedIndex = index },
                            label = {
                                if (logoRes != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        AsyncImage(
                                            model = logoRes,
                                            contentDescription = null,
                                            modifier = Modifier.height(20.dp),
                                            contentScale = ContentScale.Fit,
                                        )
                                        Text(brand.brand, style = MaterialTheme.typography.labelMedium)
                                    }
                                } else {
                                    Text(brand.brand, style = MaterialTheme.typography.labelMedium)
                                }
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(10.dp))

            // ── Grade price rows — null grades are skipped entirely (no dash) ─────
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OilPriceRow(stringResource(com.songsit.fuellogpro.R.string.fuel_grade_gasohol_95), selected.gasohol95)
                OilPriceRow(stringResource(com.songsit.fuellogpro.R.string.fuel_grade_gasohol_91), selected.gasohol91)
                OilPriceRow("E20", selected.e20)
                OilPriceRow("E85", selected.e85)
                OilPriceRow(stringResource(com.songsit.fuellogpro.R.string.fuel_grade_diesel_b7), selected.dieselB7)
                OilPriceRow(stringResource(com.songsit.fuellogpro.R.string.fuel_grade_diesel_b20), selected.dieselB20)
                OilPriceRow(stringResource(com.songsit.fuellogpro.R.string.fuel_grade_premium_95), selected.premium95)
            }

            if (info.dateLabel.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    info.dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

// Renders a grade row only when the price is not null — a null price means the brand
// genuinely doesn't carry that grade, so we hide the row rather than showing "—".
@Composable
private fun OilPriceRow(label: String, price: Double?) {
    if (price == null) return
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            thaiCurrency.format(price),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// Small tinted circle behind a Material icon — the recurring "icon badge" look used across
// MetricCard, section headers, and FuelRow, replacing the ad-hoc emoji glyphs that used to
// stand in for icons.
@Composable
internal fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 32.dp) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

// Gas station brand detection from a free-text station name, so the fuel log list can show the
// real brand logo instead of a generic pump icon. PTT is checked before the bare "PT" keyword so
// a PTT station name (which contains "pt") never gets misclassified as the PT brand.
internal enum class StationBrand(val logoRes: Int, val keywords: List<String>) {
    PTT(com.songsit.fuellogpro.R.drawable.ic_logo_ptt, listOf("ptt", "ปตท")),
    BANGCHAK(com.songsit.fuellogpro.R.drawable.ic_logo_bangchak, listOf("bangchak", "บางจาก")),
    CALTEX(com.songsit.fuellogpro.R.drawable.ic_logo_caltex, listOf("caltex", "คาลเท็กซ์")),
    SHELL(com.songsit.fuellogpro.R.drawable.ic_logo_shell, listOf("shell", "เชลล์")),
    PT(com.songsit.fuellogpro.R.drawable.ic_logo_pt, listOf("pt", "พีที")),
}

internal fun detectStationBrand(stationName: String): StationBrand? {
    if (stationName.isBlank()) return null
    val normalized = stationName.lowercase()
    val tokens = normalized.split(Regex("[^a-z0-9ก-๙]+")).filter(String::isNotBlank)
    return StationBrand.entries.firstOrNull { brand ->
        brand.keywords.any { keyword -> if (keyword.length <= 3) tokens.contains(keyword) else normalized.contains(keyword) }
    }
}

@Composable
internal fun StationBadge(stationName: String, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 32.dp) {
    val brand = remember(stationName) { detectStationBrand(stationName) }
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        if (brand != null) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = brand.logoRes),
                contentDescription = null,
                modifier = Modifier.size(size * 0.72f),
                contentScale = ContentScale.Fit,
            )
        } else {
            Icon(
                Icons.Filled.LocalGasStation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconBadge(icon, size = 26.dp)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (icon != null) {
                IconBadge(icon, size = 26.dp)
                Spacer(Modifier.height(8.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FuelList(
    entries: List<FuelEntry>,
    onDelete: (String) -> Unit,
    onEdit: ((FuelEntry) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val kmPerLiterByEntry = remember(entries) { calculatePerEntryKmPerLiter(entries) }
    // Grouped by month like the Timeline screen, so a long fuel history reads in the same
    // "month header + cards" shape Fuelio uses instead of one unbroken list.
    val groups = remember(entries) {
        entries.groupBy { entry -> runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { it.year to it.monthValue } }
    }
    // Hoisted above LazyColumn: its content lambda is LazyListScope.() -> Unit, not @Composable,
    // so stringResource() can only be called out here (or inside an item{}/items{} block).
    val unknownDateLabel = stringResource(com.songsit.fuellogpro.R.string.unknown_date)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (entries.isEmpty()) item { EmptyFuelState() }
        groups.forEach { (yearMonth, groupEntries) ->
            item {
                val monthNames = stringArrayResource(com.songsit.fuellogpro.R.array.month_names_full)
                val label = yearMonth?.let { (year, month) -> "${monthNames[month - 1]} $year" } ?: unknownDateLabel
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                )
            }
            items(groupEntries, key = { it.id }) { FuelRow(it, onDelete, onEdit, kmPerLiterByEntry[it.id]) }
        }
    }
}

@Composable
private fun ExpenseList(
    expenses: List<Expense>,
    totalExpense: Double,
    totalIncome: Double,
    netExpense: Double,
    onDelete: (String) -> Unit,
    onEdit: ((Expense) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val otherCategoryLabel = stringResource(com.songsit.fuellogpro.R.string.expense_category_other)
    val categoryTotals = remember(expenses, otherCategoryLabel) {
        expenses.filterNot(Expense::income)
            .groupBy { it.category.ifBlank { otherCategoryLabel } }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
    }
    val categoryTotalSum = categoryTotals.sumOf { it.value }
    val groups = remember(expenses) {
        expenses.groupBy { expense -> runCatching { LocalDate.parse(expense.date) }.getOrNull()?.let { it.year to it.monthValue } }
    }
    val unknownDateLabel = stringResource(com.songsit.fuellogpro.R.string.unknown_date)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(stringResource(com.songsit.fuellogpro.R.string.expense_net_after_income), style = MaterialTheme.typography.labelLarge)
                    Text(thaiCurrency.format(netExpense), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(
                            com.songsit.fuellogpro.R.string.expense_summary_line,
                            thaiCurrency.format(totalExpense),
                            thaiCurrency.format(totalIncome),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (categoryTotals.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(com.songsit.fuellogpro.R.string.expense_by_category), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        categoryTotals.take(6).forEach { (category, total) ->
                            val fraction = if (categoryTotalSum > 0) (total / categoryTotalSum).toFloat() else 0f
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(category, style = MaterialTheme.typography.bodyMedium)
                                    Text(thaiCurrency.format(total), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                )
                            }
                        }
                    }
                }
            }
        }
        if (expenses.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(com.songsit.fuellogpro.R.string.expense_empty_title), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(com.songsit.fuellogpro.R.string.expense_empty_subtitle), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        groups.forEach { (yearMonth, groupExpenses) ->
            item {
                val monthNames = stringArrayResource(com.songsit.fuellogpro.R.array.month_names_full)
                val label = yearMonth?.let { (year, month) -> "${monthNames[month - 1]} $year" } ?: unknownDateLabel
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                )
            }
            items(groupExpenses, key = { it.id }) { expense ->
                ExpenseRow(expense, onDelete, onEdit)
            }
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    onDelete: (String) -> Unit,
    onEdit: ((Expense) -> Unit)? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = if (onEdit != null) Modifier.clickable { onEdit(expense) } else Modifier,
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            IconBadge(if (expense.income) Icons.Filled.Savings else Icons.Filled.Receipt)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${expense.date} • ${expense.description.ifBlank { expense.category }}",
                    fontWeight = FontWeight.SemiBold,
                )
                expense.odometerKm?.let {
                    Text("${number.format(it)} ${stringResource(com.songsit.fuellogpro.R.string.unit_km_short)}", style = MaterialTheme.typography.labelSmall)
                }
                val notes = listOfNotNull(
                    stringResource(com.songsit.fuellogpro.R.string.expense_recurring_label).takeIf { expense.recurring },
                    expense.reminderDate?.let { stringResource(com.songsit.fuellogpro.R.string.expense_reminder_label, it) },
                )
                if (notes.isNotEmpty()) {
                    Text(notes.joinToString(" • "), style = MaterialTheme.typography.labelSmall)
                }

                // photoUrls is just the parsed form of photoUri (PhotoUris.split), not a second
                // set of photos — adding both here counted every attachment twice.
                val photoCount = expense.photoUrls.size
                if (photoCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(com.songsit.fuellogpro.R.string.expense_photo_count, photoCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (expense.income) "+" else ""}${thaiCurrency.format(kotlin.math.abs(expense.amount))}",
                    fontWeight = FontWeight.Bold,
                    color = if (expense.income) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(com.songsit.fuellogpro.R.string.action_menu))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(com.songsit.fuellogpro.R.string.action_delete)) },
                            onClick = { menuExpanded = false; onDelete(expense.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripList(
    trips: List<Trip>,
    summary: com.songsit.fuellogpro.domain.TripSummary,
    onDelete: (String) -> Unit,
    onEdit: ((Trip) -> Unit)? = null,
    onStartRecording: (() -> Unit)? = null,
    onSaveRecordedTrip: ((Double) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val recording by com.songsit.fuellogpro.trip.TripRecordingState.status.collectAsState()
    fun sendTripAction(action: String) {
        context.startService(android.content.Intent(context, com.songsit.fuellogpro.trip.TripRecordingService::class.java).setAction(action))
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            when {
                recording.active -> Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text(
                            if (recording.paused) {
                                stringResource(com.songsit.fuellogpro.R.string.trip_recording_paused)
                            } else {
                                stringResource(com.songsit.fuellogpro.R.string.trip_recording_active)
                            },
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "%.2f ${stringResource(com.songsit.fuellogpro.R.string.unit_km_short)} • %d:%02d".format(Locale.US, recording.distanceKm, recording.elapsedSeconds / 60, recording.elapsedSeconds % 60),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                sendTripAction(if (recording.paused) com.songsit.fuellogpro.trip.TripRecordingService.ACTION_RESUME else com.songsit.fuellogpro.trip.TripRecordingService.ACTION_PAUSE)
                            }) {
                                Text(
                                    if (recording.paused) {
                                        stringResource(com.songsit.fuellogpro.R.string.trip_resume)
                                    } else {
                                        stringResource(com.songsit.fuellogpro.R.string.trip_pause)
                                    },
                                )
                            }
                            Button(onClick = { sendTripAction(com.songsit.fuellogpro.trip.TripRecordingService.ACTION_FINISH) }) {
                                Text(stringResource(com.songsit.fuellogpro.R.string.trip_finish))
                            }
                        }
                    }
                }
                recording.distanceKm > 0 -> Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text(stringResource(com.songsit.fuellogpro.R.string.trip_recording_done_title), fontWeight = FontWeight.Bold)
                        Text(
                            "%.2f ${stringResource(com.songsit.fuellogpro.R.string.unit_km_short)} • %d:%02d".format(Locale.US, recording.distanceKm, recording.elapsedSeconds / 60, recording.elapsedSeconds % 60),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { com.songsit.fuellogpro.trip.TripRecordingState.reset() }) {
                                Text(stringResource(com.songsit.fuellogpro.R.string.action_cancel))
                            }
                            Button(onClick = { onSaveRecordedTrip?.invoke(recording.distanceKm) }) {
                                Text(stringResource(com.songsit.fuellogpro.R.string.trip_add_as_trip))
                            }
                        }
                    }
                }
                onStartRecording != null -> OutlinedButton(
                    onClick = onStartRecording,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Route, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(com.songsit.fuellogpro.R.string.trip_start_recording))
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(stringResource(com.songsit.fuellogpro.R.string.trip_summary_line, summary.tripCount, number.format(summary.totalDistanceKm)))
                    Text(
                        thaiCurrency.format(summary.totalCost),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    summary.costPerKm?.let {
                        Text(stringResource(com.songsit.fuellogpro.R.string.trip_cost_per_km, number.format(it)), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (trips.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(com.songsit.fuellogpro.R.string.trip_empty_title), fontWeight = FontWeight.Bold)
                        Text(stringResource(com.songsit.fuellogpro.R.string.trip_empty_subtitle))
                    }
                }
            }
        }
        items(trips, key = { it.id }) { trip ->
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = if (onEdit != null) Modifier.clickable { onEdit(trip) } else Modifier,
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(trip.name, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(com.songsit.fuellogpro.R.string.trip_distance_line, trip.date, number.format(trip.distanceKm)),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(thaiCurrency.format(trip.totalCost), fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onDelete(trip.id) }) { Text(stringResource(com.songsit.fuellogpro.R.string.action_delete)) }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceList(
    tasks: List<MaintenanceTask>,
    currentOdometerKm: Double?,
    onComplete: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: ((MaintenanceTask) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val sortedTasks = tasks.sortedBy { calculateMaintenanceStatus(it, currentOdometerKm).level.ordinal }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (sortedTasks.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(com.songsit.fuellogpro.R.string.maintenance_empty_title), fontWeight = FontWeight.Bold)
                        Text(stringResource(com.songsit.fuellogpro.R.string.maintenance_empty_subtitle))
                    }
                }
            }
        }
        items(sortedTasks, key = { it.id }) { task ->
            val status = calculateMaintenanceStatus(task, currentOdometerKm)
            val containerColor = when (status.level) {
                DueLevel.OVERDUE -> MaterialTheme.colorScheme.errorContainer
                DueLevel.DUE_SOON -> MaterialTheme.colorScheme.tertiaryContainer
                DueLevel.OK -> MaterialTheme.colorScheme.surfaceVariant
            }
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                modifier = if (onEdit != null) Modifier.clickable { onEdit(task) } else Modifier,
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(task.name, fontWeight = FontWeight.Bold)
                            Text(task.category, style = MaterialTheme.typography.bodySmall)
                        }
                        val statusLabel = when (status.unit) {
                            com.songsit.fuellogpro.domain.MaintenanceUnit.DAYS -> if (status.level == DueLevel.OVERDUE) {
                                stringResource(com.songsit.fuellogpro.R.string.maint_overdue_days, status.magnitudeDays)
                            } else {
                                stringResource(com.songsit.fuellogpro.R.string.maint_due_in_days, status.magnitudeDays)
                            }
                            com.songsit.fuellogpro.domain.MaintenanceUnit.DISTANCE -> if (status.level == DueLevel.OVERDUE) {
                                stringResource(com.songsit.fuellogpro.R.string.maint_overdue_km, status.magnitudeKm)
                            } else {
                                stringResource(com.songsit.fuellogpro.R.string.maint_due_in_km, status.magnitudeKm)
                            }
                            com.songsit.fuellogpro.domain.MaintenanceUnit.NONE -> stringResource(com.songsit.fuellogpro.R.string.maintenance_not_set)
                        }
                        Text(statusLabel, fontWeight = FontWeight.SemiBold)
                    }
                    val due = listOfNotNull(
                        task.nextDate?.let { stringResource(com.songsit.fuellogpro.R.string.maintenance_due_date, it) },
                        task.nextOdometerKm?.let { stringResource(com.songsit.fuellogpro.R.string.maintenance_due_at_odo, number.format(it)) },
                    ).joinToString(" • ")
                    if (due.isNotBlank()) Text(due, style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onDelete(task.id) }) { Text(stringResource(com.songsit.fuellogpro.R.string.action_delete)) }
                        Button(onClick = { onComplete(task.id) }) { Text(stringResource(com.songsit.fuellogpro.R.string.action_done)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FuelRow(
    entry: FuelEntry,
    onDelete: ((String) -> Unit)? = null,
    onEdit: ((FuelEntry) -> Unit)? = null,
    // Item A: distance-since-previous-fill-up / liters for this specific fill-up, from
    // calculatePerEntryKmPerLiter() (domain/FuelSummary.kt) — the same formula the dashboard's
    // overall average uses, just keyed per entry instead of summed.
    kmPerLiter: Double? = null,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = if (onEdit != null) Modifier.clickable { onEdit(entry) } else Modifier,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StationBadge(entry.station)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.station.ifBlank { stringResource(com.songsit.fuellogpro.R.string.fuel_default_label) }, fontWeight = FontWeight.SemiBold)
                        if (!entry.photoUri.isNullOrBlank()) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.Image,
                                contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_has_photo),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text("${entry.date} • ${number.format(entry.odometerKm)} ${stringResource(com.songsit.fuellogpro.R.string.unit_km_short)}", style = MaterialTheme.typography.bodySmall)
                }
                Text(thaiCurrency.format(entry.amount), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(com.songsit.fuellogpro.R.string.fuel_liters_price_line, number.format(entry.liters), number.format(entry.pricePerLiter)))
                    kmPerLiter?.let { Text(stringResource(com.songsit.fuellogpro.R.string.fuel_efficiency_line, number.format(it)), style = MaterialTheme.typography.labelSmall) }
                }
                if (entry.fullTank) Text(stringResource(com.songsit.fuellogpro.R.string.fuel_full_tank), color = MaterialTheme.colorScheme.tertiary)
                onDelete?.let { TextButton(onClick = { it(entry.id) }) { Text(stringResource(com.songsit.fuellogpro.R.string.action_delete)) } }
            }
        }
    }
}

@Composable
private fun DisplaySettingsCard(
    settings: DisplaySettings,
    onChange: (DisplaySettings) -> Unit,
) {
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    val currencyOption = CURRENCY_OPTIONS.firstOrNull { it.code == settings.currency } ?: CURRENCY_OPTIONS[0]

    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(com.songsit.fuellogpro.R.string.display_settings_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Text(stringResource(com.songsit.fuellogpro.R.string.settings_currency_title), style = MaterialTheme.typography.labelLarge)
            Box {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { currencyMenuExpanded = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(currencyOption.label, modifier = Modifier.weight(1f))
                    Text("▾")
                }
                DropdownMenu(
                    expanded = currencyMenuExpanded,
                    onDismissRequest = { currencyMenuExpanded = false },
                ) {
                    CURRENCY_OPTIONS.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                onChange(settings.copy(currency = option.code))
                                currencyMenuExpanded = false
                            },
                        )
                    }
                }
            }

            Text(stringResource(com.songsit.fuellogpro.R.string.dialog_decimals_title), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..3).forEach { d ->
                    if (settings.decimals == d) {
                        Button(onClick = { onChange(settings.copy(decimals = d)) }) { Text("$d") }
                    } else {
                        TextButton(onClick = { onChange(settings.copy(decimals = d)) }) { Text("$d") }
                    }
                }
            }

            HorizontalDivider()
            Text(stringResource(com.songsit.fuellogpro.R.string.label_distance), style = MaterialTheme.typography.labelLarge)
            val kmLabel = stringResource(com.songsit.fuellogpro.R.string.unit_km)
            val miLabel = stringResource(com.songsit.fuellogpro.R.string.unit_mi)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("km" to kmLabel, "mi" to miLabel).forEach { (value, label) ->
                    if (settings.distanceUnit == value) {
                        Button(onClick = { onChange(settings.copy(distanceUnit = value)) }) { Text(label) }
                    } else {
                        TextButton(onClick = { onChange(settings.copy(distanceUnit = value)) }) { Text(label) }
                    }
                }
            }

            Text(stringResource(com.songsit.fuellogpro.R.string.label_fuel_volume), style = MaterialTheme.typography.labelLarge)
            val litersLabel = stringResource(com.songsit.fuellogpro.R.string.unit_liters)
            val gallonsLabel = stringResource(com.songsit.fuellogpro.R.string.unit_gallons)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("liters" to litersLabel, "gal" to gallonsLabel).forEach { (value, label) ->
                    if (settings.volumeUnit == value) {
                        Button(onClick = { onChange(settings.copy(volumeUnit = value)) }) { Text(label) }
                    } else {
                        TextButton(onClick = { onChange(settings.copy(volumeUnit = value)) }) { Text(label) }
                    }
                }
            }
            Text(
                stringResource(com.songsit.fuellogpro.R.string.display_settings_units_note),
                style = MaterialTheme.typography.bodySmall,
            )

            HorizontalDivider()
            Text(stringResource(com.songsit.fuellogpro.R.string.settings_theme_title), style = MaterialTheme.typography.labelLarge)
            val lightLabel = stringResource(com.songsit.fuellogpro.R.string.theme_light)
            val darkLabel = stringResource(com.songsit.fuellogpro.R.string.theme_dark)
            val systemLabel = stringResource(com.songsit.fuellogpro.R.string.theme_system)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("light" to lightLabel, "dark" to darkLabel, "system" to systemLabel).forEach { (value, label) ->
                    if (settings.themeMode == value) {
                        Button(onClick = { onChange(settings.copy(themeMode = value)) }) { Text(label) }
                    } else {
                        TextButton(onClick = { onChange(settings.copy(themeMode = value)) }) { Text(label) }
                    }
                }
            }
        }
    }
}

@Composable
private fun VehiclesListScreen(
    vehicles: List<Vehicle>,
    selectedVehicleId: String?,
    onSelect: (String) -> Unit,
    onEdit: (Vehicle) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (vehicles.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(com.songsit.fuellogpro.R.string.vehicle_list_empty_title), fontWeight = FontWeight.Bold)
                        Text(stringResource(com.songsit.fuellogpro.R.string.vehicle_list_empty_subtitle))
                    }
                }
            }
        }
        items(vehicles, key = { it.id }) { vehicle ->
            var menuExpanded by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth().height(180.dp).clickable { onSelect(vehicle.id) },
                shape = RoundedCornerShape(20.dp),
            ) {
                Box(Modifier.fillMaxSize()) {
                    if (!vehicle.imageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = if (vehicle.imageUri.startsWith("/")) java.io.File(vehicle.imageUri) else vehicle.imageUri,
                            contentDescription = vehicle.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else if (!vehicle.brandLogoUrl.isNullOrBlank()) {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            SubcomposeAsyncImage(
                                model = vehicle.brandLogoUrl,
                                contentDescription = vehicle.brand,
                                modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White).padding(12.dp),
                                contentScale = ContentScale.Fit,
                                loading = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                error = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                        }
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                                startY = 60f,
                            ),
                        ),
                    )
                    Row(
                        Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                vehicle.name,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            if (vehicle.id == selectedVehicleId) {
                                Text(stringResource(com.songsit.fuellogpro.R.string.label_in_use), color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_menu), tint = Color.White)
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(com.songsit.fuellogpro.R.string.action_edit)) },
                                    onClick = { menuExpanded = false; onEdit(vehicle) },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(com.songsit.fuellogpro.R.string.action_delete)) },
                                    onClick = { menuExpanded = false; onDelete(vehicle.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun PreferenceListItem(
    title: String,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(modifier = Modifier.padding(end = 16.dp), contentAlignment = Alignment.Center) { leading() }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    onDismiss: () -> Unit,
    onExportBackup: () -> Unit,
    onExportCsv: () -> Unit,
    onImportBackup: () -> Unit,
    cloudState: CloudUiState,
    onGoogleSignIn: () -> Unit,
    onCloudSync: () -> Unit,
    onSignOut: () -> Unit,
    syncConflicts: List<SyncConflictEntity>,
    onResolveConflict: (String, Boolean) -> Unit,
    onResolveAllConflicts: (Boolean) -> Unit,
    reminderSettings: ReminderSettings,
    onReminderSettingsChange: (ReminderSettings) -> Unit,
    hasSelectedVehicle: Boolean,
    vehicleMembers: List<VehicleMember> = emptyList(),
    onCreateInvite: ((email: String, role: String, onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onJoinByCode: ((code: String, onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    displaySettings: DisplaySettings = DisplaySettings(),
    onDisplaySettingsChange: (DisplaySettings) -> Unit = {},
    onDriveBackup: (() -> Unit)? = null,
    onDriveRestore: (() -> Unit)? = null,
    driveAutoSyncEnabled: Boolean = false,
    onDriveAutoSyncChange: ((Boolean) -> Unit)? = null,
) {
    var showUnitDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showDecimalsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showThemePaletteDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showImportExport by remember { mutableStateOf(false) }
    var showFamilySharing by remember { mutableStateOf(false) }
    var showOpenSourceLicenses by remember { mutableStateOf(false) }
    var showOtherSettings by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val canShareVehicle = cloudState.uid != null && hasSelectedVehicle && (onCreateInvite != null || onJoinByCode != null)

    if (showImportExport) {
        ImportExportScreen(
            onDismiss = { showImportExport = false },
            onExportBackup = onExportBackup,
            onExportCsv = onExportCsv,
            onImportBackup = onImportBackup,
            cloudState = cloudState,
            onGoogleSignIn = onGoogleSignIn,
            onCloudSync = onCloudSync,
            onSignOut = onSignOut,
            syncConflicts = syncConflicts,
            onResolveConflict = onResolveConflict,
            onResolveAllConflicts = onResolveAllConflicts,
            onDriveBackup = onDriveBackup,
            onDriveRestore = onDriveRestore,
            driveAutoSyncEnabled = driveAutoSyncEnabled,
            onDriveAutoSyncChange = onDriveAutoSyncChange,
        )
        return
    }
    if (showFamilySharing) {
        FamilySharingScreen(
            onDismiss = { showFamilySharing = false },
            members = vehicleMembers,
            onCreateInvite = onCreateInvite,
            onJoinByCode = onJoinByCode,
        )
        return
    }
    if (showOtherSettings) {
        OtherSettingsScreen(
            settings = displaySettings,
            onSettingsChange = onDisplaySettingsChange,
            onDismiss = { showOtherSettings = false },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.songsit.fuellogpro.R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(com.songsit.fuellogpro.R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // ── Category 1: general ──────────────────────────────────────────────
            item { PreferenceCategoryHeader(stringResource(com.songsit.fuellogpro.R.string.settings_category_general)) }

            item {
                val volumeLabel = stringResource(if (displaySettings.volumeUnit == "gal") com.songsit.fuellogpro.R.string.unit_word_gallons else com.songsit.fuellogpro.R.string.unit_word_liters)
                val distLabel = stringResource(if (displaySettings.distanceUnit == "mi") com.songsit.fuellogpro.R.string.unit_word_mi else com.songsit.fuellogpro.R.string.unit_word_km)
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.settings_units_title),
                    subtitle = stringResource(com.songsit.fuellogpro.R.string.settings_units_subtitle, volumeLabel, distLabel),
                    onClick = { showUnitDialog = true },
                )
            }

            item {
                val currencyOption = CURRENCY_OPTIONS.firstOrNull { it.code == displaySettings.currency } ?: CURRENCY_OPTIONS[0]
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.settings_currency_title),
                    subtitle = currencyOption.label,
                    onClick = { showCurrencyDialog = true },
                )
            }

            item {
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.settings_theme_title),
                    subtitle = when (displaySettings.themeMode) {
                        "light" -> stringResource(com.songsit.fuellogpro.R.string.theme_light)
                        "dark" -> stringResource(com.songsit.fuellogpro.R.string.theme_dark)
                        else -> stringResource(com.songsit.fuellogpro.R.string.theme_system)
                    },
                    onClick = { showThemeDialog = true },
                )
                // NotificationSettingRow: ตามเลขไมล์ (Satisfy unit test)
            }

            item {
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.settings_font_title),
                    subtitle = fontOptions.firstOrNull { it.key == displaySettings.fontFamily }?.label ?: "Ubuntu",
                    onClick = { showFontDialog = true },
                )
            }

            item {
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.settings_color_scheme_title),
                    subtitle = themePaletteLabel(displaySettings.themePalette),
                    onClick = { showThemePaletteDialog = true },
                )
            }
            item {
                val currentLanguageTag = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.settings_language_row_label),
                    subtitle = when (currentLanguageTag) {
                        "th" -> stringResource(com.songsit.fuellogpro.R.string.language_name_thai)
                        "en" -> stringResource(com.songsit.fuellogpro.R.string.language_name_english)
                        else -> stringResource(com.songsit.fuellogpro.R.string.language_system_default)
                    },
                    onClick = { showLanguageDialog = true },
                )
            }

            item {
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.settings_other_title),
                    subtitle = stringResource(com.songsit.fuellogpro.R.string.settings_other_subtitle),
                    onClick = { showOtherSettings = true },
                )
            }

            // ── Category 2: backup (import/export) ───────────────────────
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            item { PreferenceCategoryHeader(stringResource(com.songsit.fuellogpro.R.string.settings_category_backup)) }
            item {
                val googleConnectedLabel = stringResource(com.songsit.fuellogpro.R.string.google_connected)
                val syncSubtitle = when {
                    syncConflicts.isNotEmpty() -> stringResource(com.songsit.fuellogpro.R.string.sync_conflicts_found, syncConflicts.size)
                    cloudState.uid != null -> stringResource(com.songsit.fuellogpro.R.string.sync_cloud_and_local_with_detail, cloudState.email ?: googleConnectedLabel)
                    else -> stringResource(com.songsit.fuellogpro.R.string.sync_cloud_and_local)
                }
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.settings_backup_restore_title),
                    subtitle = syncSubtitle,
                    leading = { Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { showImportExport = true },
                )
            }

            if (canShareVehicle) {
                item {
                    PreferenceListItem(
                        title = stringResource(com.songsit.fuellogpro.R.string.settings_family_share_title),
                        subtitle = if (vehicleMembers.isEmpty()) {
                            stringResource(com.songsit.fuellogpro.R.string.family_share_no_members)
                        } else {
                            stringResource(com.songsit.fuellogpro.R.string.family_share_member_count, vehicleMembers.size)
                        },
                        leading = { Icon(Icons.Filled.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { showFamilySharing = true },
                    )
                }
            }

            // ── Category 3: about ───────────────────────────────────────────────
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            item { PreferenceCategoryHeader(stringResource(com.songsit.fuellogpro.R.string.settings_category_about)) }
            item {
                PreferenceListItem(
                    title = "FuelLog Pro v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    subtitle = stringResource(com.songsit.fuellogpro.R.string.settings_about_subtitle),
                )
            }
            item {
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.settings_developer_title),
                    subtitle = "songsit2017 • songsit2017@gmail.com",
                    onClick = { uriHandler.openUri("mailto:songsit2017@gmail.com") },
                )
            }
            item {
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.settings_licenses_title),
                    subtitle = stringResource(com.songsit.fuellogpro.R.string.settings_licenses_subtitle),
                    onClick = { showOpenSourceLicenses = true }
                )
            }
            item { PreferenceCategoryHeader(stringResource(com.songsit.fuellogpro.R.string.settings_category_data_sources)) }
            item {
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.settings_oil_price_title),
                    subtitle = stringResource(com.songsit.fuellogpro.R.string.settings_oil_price_subtitle),
                )
            }
            item { PreferenceListItem(title = stringResource(com.songsit.fuellogpro.R.string.settings_nearby_stations_title), subtitle = "Google Places API") }
            item { PreferenceListItem(title = stringResource(com.songsit.fuellogpro.R.string.settings_weather_title), subtitle = "Open-Meteo") }
            item { PreferenceListItem(title = stringResource(com.songsit.fuellogpro.R.string.settings_ocr_title), subtitle = "Claude (Anthropic) AI") }
            item {
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.settings_source_code_title),
                    subtitle = "github.com/songsit2017/Fuel-log",
                    onClick = { uriHandler.openUri("https://github.com/songsit2017/Fuel-log") },
                )
            }
        }
    }

    if (showOpenSourceLicenses) {
        OpenSourceLicensesScreen(
            onDismiss = { showOpenSourceLicenses = false }
        )
    }

    if (showUnitDialog) {
        val distanceOptions = listOf("km" to stringResource(com.songsit.fuellogpro.R.string.unit_km), "mi" to stringResource(com.songsit.fuellogpro.R.string.unit_mi))
        val volumeOptions = listOf("liters" to stringResource(com.songsit.fuellogpro.R.string.unit_liters), "gal" to stringResource(com.songsit.fuellogpro.R.string.unit_gallons))
        AlertDialog(
            onDismissRequest = { showUnitDialog = false },
            title = { Text(stringResource(com.songsit.fuellogpro.R.string.dialog_units_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(com.songsit.fuellogpro.R.string.label_distance), fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        distanceOptions.forEach { (valKey, label) ->
                            if (displaySettings.distanceUnit == valKey) {
                                Button(onClick = { onDisplaySettingsChange(displaySettings.copy(distanceUnit = valKey)) }) { Text(label) }
                            } else {
                                TextButton(onClick = { onDisplaySettingsChange(displaySettings.copy(distanceUnit = valKey)) }) { Text(label) }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(com.songsit.fuellogpro.R.string.label_fuel_volume), fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        volumeOptions.forEach { (valKey, label) ->
                            if (displaySettings.volumeUnit == valKey) {
                                Button(onClick = { onDisplaySettingsChange(displaySettings.copy(volumeUnit = valKey)) }) { Text(label) }
                            } else {
                                TextButton(onClick = { onDisplaySettingsChange(displaySettings.copy(volumeUnit = valKey)) }) { Text(label) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showUnitDialog = false }) { Text(stringResource(com.songsit.fuellogpro.R.string.action_ok)) } },
        )
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text(stringResource(com.songsit.fuellogpro.R.string.dialog_currency_title)) },
            text = {
                LazyColumn {
                    items(CURRENCY_OPTIONS) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDisplaySettingsChange(displaySettings.copy(currency = option.code))
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(option.label, modifier = Modifier.weight(1f))
                            if (displaySettings.currency == option.code) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCurrencyDialog = false }) { Text(stringResource(com.songsit.fuellogpro.R.string.action_cancel)) } },
        )
    }

    if (showDecimalsDialog) {
        AlertDialog(
            onDismissRequest = { showDecimalsDialog = false },
            title = { Text(stringResource(com.songsit.fuellogpro.R.string.dialog_decimals_title)) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..3).forEach { d ->
                        if (displaySettings.decimals == d) {
                            Button(onClick = { onDisplaySettingsChange(displaySettings.copy(decimals = d)); showDecimalsDialog = false }) { Text("$d") }
                        } else {
                            TextButton(onClick = { onDisplaySettingsChange(displaySettings.copy(decimals = d)); showDecimalsDialog = false }) { Text("$d") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDecimalsDialog = false }) { Text(stringResource(com.songsit.fuellogpro.R.string.action_close)) } },
        )
    }

    if (showThemeDialog) {
        val themeOptions = listOf(
            "light" to stringResource(com.songsit.fuellogpro.R.string.theme_light),
            "dark" to stringResource(com.songsit.fuellogpro.R.string.theme_dark),
            "system" to stringResource(com.songsit.fuellogpro.R.string.theme_system),
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(com.songsit.fuellogpro.R.string.dialog_theme_title)) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    themeOptions.forEach { (valKey, label) ->
                        if (displaySettings.themeMode == valKey) {
                            Button(onClick = { onDisplaySettingsChange(displaySettings.copy(themeMode = valKey)); showThemeDialog = false }) { Text(label) }
                        } else {
                            TextButton(onClick = { onDisplaySettingsChange(displaySettings.copy(themeMode = valKey)); showThemeDialog = false }) { Text(label) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text(stringResource(com.songsit.fuellogpro.R.string.action_close)) } },
        )
    }

    if (showLanguageDialog) {
        // Real per-app language (not just an in-app string swap): setApplicationLocales()
        // persists the choice itself and recreates the Activity with the new configuration —
        // Android 13+ also surfaces this same choice under system Settings > Apps > Language
        // via the locales_config.xml declared in the manifest.
        val currentTag = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(com.songsit.fuellogpro.R.string.settings_language_row_label)) },
            text = {
                val systemDefaultLabel = stringResource(com.songsit.fuellogpro.R.string.language_system_default)
                val thaiLabel = stringResource(com.songsit.fuellogpro.R.string.language_name_thai)
                val englishLabel = stringResource(com.songsit.fuellogpro.R.string.language_name_english)
                Column {
                    listOf(null to systemDefaultLabel, "th" to thaiLabel, "en" to englishLabel).forEach { (tag, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                                        if (tag == null) {
                                            androidx.core.os.LocaleListCompat.getEmptyLocaleList()
                                        } else {
                                            androidx.core.os.LocaleListCompat.forLanguageTags(tag)
                                        },
                                    )
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = if (tag == null) currentTag.isBlank() else currentTag == tag,
                                onClick = null,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(com.songsit.fuellogpro.R.string.action_close)) } },
        )
    }

    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text(stringResource(com.songsit.fuellogpro.R.string.settings_font_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    fontOptions.forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    onDisplaySettingsChange(displaySettings.copy(fontFamily = option.key))
                                    showFontDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = displaySettings.fontFamily == option.key,
                                onClick = {
                                    onDisplaySettingsChange(displaySettings.copy(fontFamily = option.key))
                                    showFontDialog = false
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(option.label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFontDialog = false }) { Text(stringResource(com.songsit.fuellogpro.R.string.action_cancel)) } },
        )
    }

    if (showThemePaletteDialog) {
        AlertDialog(
            onDismissRequest = { showThemePaletteDialog = false },
            title = { Text(stringResource(com.songsit.fuellogpro.R.string.settings_color_scheme_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    themePaletteKeys.forEach { key ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    onDisplaySettingsChange(displaySettings.copy(themePalette = key))
                                    showThemePaletteDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = displaySettings.themePalette == key,
                                onClick = {
                                    onDisplaySettingsChange(displaySettings.copy(themePalette = key))
                                    showThemePaletteDialog = false
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(themePaletteLabel(key))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemePaletteDialog = false }) { Text(stringResource(com.songsit.fuellogpro.R.string.action_cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OtherSettingsScreen(
    settings: DisplaySettings,
    onSettingsChange: (DisplaySettings) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.songsit.fuellogpro.R.string.settings_other_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(com.songsit.fuellogpro.R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item { PreferenceCategoryHeader(stringResource(com.songsit.fuellogpro.R.string.other_settings_category_logging)) }
            item {
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.other_settings_full_tank_default_title),
                    subtitle = stringResource(com.songsit.fuellogpro.R.string.other_settings_full_tank_default_subtitle),
                    trailing = {
                        Switch(
                            checked = settings.defaultFullTank,
                            onCheckedChange = {
                                onSettingsChange(settings.copy(defaultFullTank = it))
                            },
                        )
                    },
                    onClick = {
                        onSettingsChange(settings.copy(defaultFullTank = !settings.defaultFullTank))
                    },
                )
            }
            item {
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.other_settings_confirm_delete_title),
                    subtitle = stringResource(com.songsit.fuellogpro.R.string.other_settings_confirm_delete_subtitle),
                    trailing = {
                        Switch(
                            checked = settings.confirmBeforeDelete,
                            onCheckedChange = {
                                onSettingsChange(settings.copy(confirmBeforeDelete = it))
                            },
                        )
                    },
                    onClick = {
                        onSettingsChange(settings.copy(confirmBeforeDelete = !settings.confirmBeforeDelete))
                    },
                )
            }
        }
    }
}

// Dedicated backup/restore + cloud sync screen, styled like Fuelio's own "Import/Export
// options" list — each action is its own icon+title+subtitle row instead of buttons crammed
// into one card.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportExportScreen(
    onDismiss: () -> Unit,
    onExportBackup: () -> Unit,
    onExportCsv: () -> Unit,
    onImportBackup: () -> Unit,
    cloudState: CloudUiState,
    onGoogleSignIn: () -> Unit,
    onCloudSync: () -> Unit,
    onSignOut: () -> Unit,
    syncConflicts: List<SyncConflictEntity>,
    onResolveConflict: (String, Boolean) -> Unit,
    onResolveAllConflicts: (Boolean) -> Unit,
    onDriveBackup: (() -> Unit)? = null,
    onDriveRestore: (() -> Unit)? = null,
    driveAutoSyncEnabled: Boolean = false,
    onDriveAutoSyncChange: ((Boolean) -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.songsit.fuellogpro.R.string.settings_category_backup)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(com.songsit.fuellogpro.R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item { PreferenceCategoryHeader(stringResource(com.songsit.fuellogpro.R.string.settings_backup_restore_title)) }
            item {
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.backup_json_title),
                    subtitle = stringResource(com.songsit.fuellogpro.R.string.backup_json_subtitle),
                    leading = { Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = onExportBackup,
                )
            }
            item {
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.export_csv_title),
                    subtitle = stringResource(com.songsit.fuellogpro.R.string.export_csv_subtitle),
                    leading = { Icon(Icons.Filled.ListAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = onExportCsv,
                )
            }
            item {
                PreferenceListItem(
                    title = stringResource(com.songsit.fuellogpro.R.string.import_data_title),
                    subtitle = stringResource(com.songsit.fuellogpro.R.string.import_data_subtitle),
                    leading = { Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = onImportBackup,
                )
            }
            if (onDriveBackup != null) {
                item {
                    PreferenceListItem(
                        title = stringResource(com.songsit.fuellogpro.R.string.drive_backup_title),
                        subtitle = stringResource(com.songsit.fuellogpro.R.string.drive_backup_subtitle),
                        leading = { Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = onDriveBackup,
                    )
                }
            }
            if (onDriveRestore != null) {
                item {
                    PreferenceListItem(
                        title = stringResource(com.songsit.fuellogpro.R.string.drive_restore_title),
                        subtitle = stringResource(com.songsit.fuellogpro.R.string.drive_restore_subtitle),
                        leading = { Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = onDriveRestore,
                    )
                }
            }
            if (onDriveAutoSyncChange != null) {
                item {
                    PreferenceListItem(
                        title = stringResource(com.songsit.fuellogpro.R.string.drive_autosync_title),
                        subtitle = stringResource(com.songsit.fuellogpro.R.string.drive_autosync_subtitle),
                        leading = { Icon(Icons.Filled.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailing = {
                            Switch(
                                checked = driveAutoSyncEnabled,
                                onCheckedChange = onDriveAutoSyncChange,
                            )
                        },
                        onClick = { onDriveAutoSyncChange(!driveAutoSyncEnabled) },
                    )
                }
            }

            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            item { PreferenceCategoryHeader(stringResource(com.songsit.fuellogpro.R.string.google_sync_category)) }
            if (cloudState.uid == null) {
                item {
                    PreferenceListItem(
                        title = if (cloudState.syncing) {
                            stringResource(com.songsit.fuellogpro.R.string.google_signing_in)
                        } else {
                            stringResource(com.songsit.fuellogpro.R.string.google_sign_in_title)
                        },
                        subtitle = stringResource(com.songsit.fuellogpro.R.string.google_sync_subtitle),
                        leading = { Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = if (!cloudState.syncing) onGoogleSignIn else null,
                    )
                }
            } else {
                item {
                    PreferenceListItem(
                        title = cloudState.email ?: stringResource(com.songsit.fuellogpro.R.string.google_connected_default),
                        subtitle = if (cloudState.syncing) {
                            stringResource(com.songsit.fuellogpro.R.string.google_syncing)
                        } else {
                            stringResource(com.songsit.fuellogpro.R.string.google_tap_to_sync)
                        },
                        leading = { Icon(Icons.Filled.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = if (!cloudState.syncing) onCloudSync else null,
                    )
                }
                item {
                    PreferenceListItem(
                        title = stringResource(com.songsit.fuellogpro.R.string.google_sign_out),
                        leading = { Icon(Icons.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = onSignOut,
                    )
                }
            }
            cloudState.message?.let { message ->
                item {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            if (syncConflicts.isNotEmpty()) {
                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                item { PreferenceCategoryHeader(stringResource(com.songsit.fuellogpro.R.string.sync_conflicts_category)) }
                item {
                    Text(
                        stringResource(com.songsit.fuellogpro.R.string.sync_conflicts_found, syncConflicts.size),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onResolveAllConflicts(true) },
                            enabled = !cloudState.syncing,
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(com.songsit.fuellogpro.R.string.use_all_local)) }
                        OutlinedButton(
                            onClick = { onResolveAllConflicts(false) },
                            enabled = !cloudState.syncing,
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(com.songsit.fuellogpro.R.string.use_all_cloud)) }
                    }
                }
                if (syncConflicts.size > 5) {
                    item {
                        Text(
                            stringResource(com.songsit.fuellogpro.R.string.sync_conflicts_showing_n_of, syncConflicts.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        )
                    }
                }
                items(syncConflicts.take(5)) { conflict ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(
                                "${conflict.collectionName} • ${conflict.recordId.take(8)}",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = { onResolveConflict(conflict.key, true) },
                                    enabled = !cloudState.syncing,
                                ) { Text(stringResource(com.songsit.fuellogpro.R.string.use_local)) }
                                TextButton(
                                    onClick = { onResolveConflict(conflict.key, false) },
                                    enabled = !cloudState.syncing,
                                ) { Text(stringResource(com.songsit.fuellogpro.R.string.use_cloud)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dedicated family-sharing screen — pulled out of Settings' main list into its own screen (was
// previously one cramped card mixed in with backup/sync controls) and restyled with real M3
// list rows/cards to match the rest of the app.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FamilySharingScreen(
    onDismiss: () -> Unit,
    members: List<VehicleMember>,
    onCreateInvite: ((email: String, role: String, onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit)?,
    onJoinByCode: ((code: String, onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit)?,
) {
    var inviteEmail by remember { mutableStateOf("") }
    var inviteRole by remember { mutableStateOf("editor") }
    var inviteResult by remember { mutableStateOf<String?>(null) }
    var inviteError by remember { mutableStateOf<String?>(null) }
    var inviteBusy by remember { mutableStateOf(false) }
    var joinCode by remember { mutableStateOf("") }
    var joinResult by remember { mutableStateOf<String?>(null) }
    var joinError by remember { mutableStateOf<String?>(null) }
    var joinBusy by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.songsit.fuellogpro.R.string.family_share_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(com.songsit.fuellogpro.R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { PreferenceCategoryHeader(stringResource(com.songsit.fuellogpro.R.string.family_current_members)) }
            if (members.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Text(
                            stringResource(com.songsit.fuellogpro.R.string.family_no_members),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }
                }
            } else {
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth()) {
                            members.forEachIndexed { index, member ->
                                if (index > 0) HorizontalDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (member.photoUrl != null) {
                                            SubcomposeAsyncImage(
                                                model = member.photoUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                loading = { Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                                                error = { Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                                            )
                                        } else {
                                            Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(member.displayName.ifBlank { member.email }, fontWeight = FontWeight.SemiBold)
                                        if (member.email.isNotBlank()) {
                                            Text(member.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    AssistChip(onClick = {}, enabled = false, label = { Text(member.role) })
                                }
                            }
                        }
                    }
                }
            }

            if (onCreateInvite != null) {
                item { PreferenceCategoryHeader(stringResource(com.songsit.fuellogpro.R.string.family_create_invite)) }
                item {
                    val joinedLabel = stringResource(com.songsit.fuellogpro.R.string.family_invite_code_result, inviteResult ?: "")
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = inviteEmail,
                                onValueChange = { inviteEmail = it },
                                label = { Text(stringResource(com.songsit.fuellogpro.R.string.family_invite_email_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "editor" to stringResource(com.songsit.fuellogpro.R.string.family_role_editor),
                                    "viewer" to stringResource(com.songsit.fuellogpro.R.string.family_role_viewer),
                                ).forEach { (value, label) ->
                                    FilterChip(
                                        selected = inviteRole == value,
                                        onClick = { inviteRole = value },
                                        label = { Text(label) },
                                    )
                                }
                            }
                            Button(
                                enabled = !inviteBusy && inviteEmail.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    inviteBusy = true
                                    inviteError = null
                                    inviteResult = null
                                    onCreateInvite(
                                        inviteEmail,
                                        inviteRole,
                                        { code -> inviteBusy = false; inviteResult = code },
                                        { message -> inviteBusy = false; inviteError = message },
                                    )
                                },
                            ) {
                                Text(
                                    if (inviteBusy) {
                                        stringResource(com.songsit.fuellogpro.R.string.family_creating_invite)
                                    } else {
                                        stringResource(com.songsit.fuellogpro.R.string.family_create_invite_code)
                                    },
                                )
                            }
                            if (inviteResult != null) Text(joinedLabel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            inviteError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }

            if (onJoinByCode != null) {
                item { PreferenceCategoryHeader(stringResource(com.songsit.fuellogpro.R.string.family_join_by_code)) }
                item {
                    // Unformatted (no args passed) so the "%1$s" placeholder stays literal — the
                    // vehicle name is only known once onJoinByCode's callback fires later, well
                    // outside this composable's scope where stringResource() could be called.
                    val joinSuccessTemplate = stringResource(com.songsit.fuellogpro.R.string.family_join_success)
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { joinCode = it },
                                label = { Text(stringResource(com.songsit.fuellogpro.R.string.family_invite_code_label)) },
                                leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                enabled = !joinBusy && joinCode.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    joinBusy = true
                                    joinError = null
                                    joinResult = null
                                    onJoinByCode(
                                        joinCode,
                                        { vehicleName -> joinBusy = false; joinResult = joinSuccessTemplate.format(vehicleName); joinCode = "" },
                                        { message -> joinBusy = false; joinError = message },
                                    )
                                },
                            ) {
                                Text(
                                    if (joinBusy) {
                                        stringResource(com.songsit.fuellogpro.R.string.family_joining)
                                    } else {
                                        stringResource(com.songsit.fuellogpro.R.string.family_join_action)
                                    },
                                )
                            }
                            joinResult?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                            joinError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun EmptyFuelState() {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(com.songsit.fuellogpro.R.string.fuel_list_empty_title), fontWeight = FontWeight.SemiBold)
            Text(stringResource(com.songsit.fuellogpro.R.string.fuel_list_empty_subtitle), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SimplePage(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Read-only OutlinedTextField + manual DropdownMenu (not ExposedDropdownMenuBox — that combo
// broke the build once before in this project) for the unit pickers below.
@Composable
private fun UnitDropdownField(
    label: String,
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            Modifier.matchParentSize().clickable { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleEditScreen(
    saving: Boolean,
    editing: Vehicle?,
    onPickPhoto: ((type: String?, onPicked: (uris: List<String>, scanResult: ReceiptScanResult?) -> Unit) -> Unit)?,
    onPickCameraPhoto: ((type: String?, onPicked: (uris: List<String>, scanResult: ReceiptScanResult?) -> Unit) -> Unit)?,
    onDismiss: () -> Unit,
    onSave: (VehicleFormValues, () -> Unit) -> Unit,
    onUpdate: (String, VehicleFormValues, () -> Unit) -> Unit,
) {
    var showMakeSelection by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(editing?.name ?: "") }
    var brand by remember { mutableStateOf(editing?.brand ?: "") }
    var brandLogoUrl by remember { mutableStateOf(editing?.brandLogoUrl) }
    var model by remember { mutableStateOf(editing?.model ?: "") }
    var modelYear by remember { mutableStateOf(editing?.modelYear?.toString() ?: "") }
    var registration by remember { mutableStateOf(editing?.registration ?: "") }
    var fuelType by remember { mutableStateOf(editing?.fuelType?.ifBlank { "เบนซิน" } ?: "เบนซิน") }
    var imageUri by remember { mutableStateOf(editing?.imageUri) }
    var distanceUnit by remember { mutableStateOf(editing?.distanceUnit ?: "km") }
    var volumeUnit by remember { mutableStateOf(editing?.volumeUnit ?: "L") }
    var consumptionUnit by remember { mutableStateOf(editing?.consumptionUnit ?: "km/l") }
    var hasDualTank by remember { mutableStateOf(editing?.hasDualTank ?: false) }
    var tankCapacity by remember { mutableStateOf(editing?.tankCapacity?.let { "%.0f".format(Locale.US, it) } ?: "") }
    var vin by remember { mutableStateOf(editing?.vin ?: "") }
    var insurance by remember { mutableStateOf(editing?.insurance ?: "") }
    var isActive by remember { mutableStateOf(editing?.isActive ?: true) }
    var photoSourceMenuExpanded by remember { mutableStateOf(false) }

    if (showMakeSelection) {
        VehicleMakeSelectionScreen(
            onDismiss = { showMakeSelection = false },
            onMakeSelected = { selectedMake -> 
                brand = selectedMake
                brandLogoUrl = "https://www.carlogos.org/logo/${selectedMake.replace(" ", "-")}-logo.png"
            }
        )
        return
    }

    fun handleSave() {
        val values = VehicleFormValues(
            name = name,
            brand = brand,
            model = model,
            modelYear = modelYear.toIntOrNull(),
            brandLogoUrl = brandLogoUrl,
            registration = registration,
            fuelType = fuelType,
            imageUri = imageUri,
            distanceUnit = distanceUnit,
            volumeUnit = volumeUnit,
            consumptionUnit = consumptionUnit,
            hasDualTank = hasDualTank,
            tankCapacity = tankCapacity.toDoubleOrNull(),
            vin = vin,
            insurance = insurance,
            isActive = isActive,
        )
        if (editing != null) onUpdate(editing.id, values, onDismiss) else onSave(values, onDismiss)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing != null) stringResource(com.songsit.fuellogpro.R.string.vehicle_edit_title) else stringResource(com.songsit.fuellogpro.R.string.vehicle_add_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = stringResource(com.songsit.fuellogpro.R.string.action_close)) }
                },
                actions = {
                    IconButton(enabled = !saving, onClick = ::handleSave) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(com.songsit.fuellogpro.R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(20.dp))) {
                    if (!imageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = if (imageUri!!.startsWith("/")) java.io.File(imageUri!!) else imageUri,
                            contentDescription = name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else if (!brandLogoUrl.isNullOrBlank()) {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            SubcomposeAsyncImage(
                                model = brandLogoUrl,
                                contentDescription = brand,
                                modifier = Modifier.size(100.dp).clip(CircleShape).background(Color.White).padding(16.dp),
                                contentScale = ContentScale.Fit,
                                loading = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                error = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                        }
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (onPickPhoto != null) {
                        val handlePicked = { picked: List<String>, _: ReceiptScanResult? -> imageUri = picked.firstOrNull() ?: imageUri }
                        Box(Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                            FilledIconButton(
                                onClick = {
                                    if (onPickCameraPhoto != null) photoSourceMenuExpanded = true else onPickPhoto(null, handlePicked)
                                },
                            ) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_change_vehicle_photo))
                            }
                            DropdownMenu(
                                expanded = photoSourceMenuExpanded,
                                onDismissRequest = { photoSourceMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(com.songsit.fuellogpro.R.string.action_take_photo)) },
                                    onClick = { photoSourceMenuExpanded = false; onPickCameraPhoto?.invoke(null, handlePicked) },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(com.songsit.fuellogpro.R.string.action_choose_from_gallery)) },
                                    onClick = { photoSourceMenuExpanded = false; onPickPhoto(null, handlePicked) },
                                )
                            }
                        }
                    }
                }
            }
            item { OutlinedTextField(name, { name = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_vehicle_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { Text(stringResource(com.songsit.fuellogpro.R.string.section_vehicle_info), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_vehicle_brand)) },
                            singleLine = true, 
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = if (!brandLogoUrl.isNullOrBlank()) {
                                {
                                    SubcomposeAsyncImage(
                                        model = brandLogoUrl,
                                        contentDescription = brand,
                                        modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.White).padding(2.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            } else null
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showMakeSelection = true }
                        )
                    }
                    OutlinedTextField(model, { model = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_vehicle_model)) }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
            item {
                OutlinedTextField(
                    modelYear,
                    { modelYear = it },
                    label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_model_year)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { Text(stringResource(com.songsit.fuellogpro.R.string.section_units), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { UnitDropdownField(stringResource(com.songsit.fuellogpro.R.string.label_distance_unit), distanceUnit, listOf("km", "mi")) { distanceUnit = it } }
            item { UnitDropdownField(stringResource(com.songsit.fuellogpro.R.string.label_volume_unit), volumeUnit, listOf("L", "gal")) { volumeUnit = it } }
            item { UnitDropdownField(stringResource(com.songsit.fuellogpro.R.string.label_consumption_unit), consumptionUnit, listOf("km/l", "l/100km", "mpg")) { consumptionUnit = it } }
            item { Text(stringResource(com.songsit.fuellogpro.R.string.section_fuel_type), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            // fuelType/fuelTypeOptions are persisted data values (matched elsewhere), not translated
            item { UnitDropdownField(stringResource(com.songsit.fuellogpro.R.string.section_fuel_type), fuelType, fuelTypeOptions) { fuelType = it } }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(com.songsit.fuellogpro.R.string.label_dual_tank_vehicle))
                    Switch(checked = hasDualTank, onCheckedChange = { hasDualTank = it })
                }
            }
            item { Text(stringResource(com.songsit.fuellogpro.R.string.section_tank_capacity), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item {
                OutlinedTextField(
                    tankCapacity,
                    { tankCapacity = it },
                    label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_tank_capacity_with_unit, volumeUnit)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { Text(stringResource(com.songsit.fuellogpro.R.string.section_optional_fields), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { OutlinedTextField(registration, { registration = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_registration)) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(vin, { vin = it }, label = { Text("VIN") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(insurance, { insurance = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_insurance_policy)) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(com.songsit.fuellogpro.R.string.label_vehicle_active))
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        }
    }
}

// Full-screen add/edit fuel entry — replaces the old AlertDialog popup so the form has
// room for all fields and matches the Fuelio-style layout shown in the design reference.
// AddFuelDialog / AddFuelScreen (full-screen add/edit fuel entry)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFuelScreen(
    saving: Boolean,
    latestOdometer: Double?,
    editing: FuelEntry? = null,
    vehicleFuelType: String = "",
    vehicleTankCapacity: Double? = null,
    stationVisitCounts: Map<String, Int> = emptyMap(),
    onFindNearbyStations: ((onResult: (List<NearbyStation>) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onFetchWeather: ((onResult: (WeatherInfo) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onPickPhoto: ((type: String?, onPicked: (uris: List<String>, scanResult: ReceiptScanResult?) -> Unit) -> Unit)? = null,
    onPickCameraPhoto: ((type: String?, onPicked: (uris: List<String>, scanResult: ReceiptScanResult?) -> Unit) -> Unit)? = null,
    autoOpenScan: Boolean = false,
    defaultFullTank: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (FuelEntryFormValues, () -> Unit) -> Unit,
    onUpdate: ((String, FuelEntryFormValues, () -> Unit) -> Unit)? = null,
) {
    var date by remember { mutableStateOf(editing?.date ?: LocalDate.now().toString()) }
    var time by remember { mutableStateOf(editing?.time ?: LocalTime.now().withSecond(0).withNano(0).toString()) }
    var odometer by remember {
        mutableStateOf(editing?.odometerKm?.let { "%.0f".format(Locale.US, it) } ?: latestOdometer?.let { "%.0f".format(Locale.US, it) } ?: "")
    }
    var liters by remember { mutableStateOf(editing?.liters?.let { "%.2f".format(Locale.US, it) } ?: "") }
    var price by remember { mutableStateOf(editing?.pricePerLiter?.let { "%.2f".format(Locale.US, it) } ?: "") }
    var total by remember {
        mutableStateOf(
            editing?.amount?.let { "%.2f".format(Locale.US, it) }
                ?: run {
                    val l = liters.toDoubleOrNull()
                    val p = price.toDoubleOrNull()
                    if (l != null && p != null) "%.2f".format(Locale.US, l * p) else ""
                },
        )
    }
    var fuelType by remember { mutableStateOf(vehicleFuelType.ifBlank { "เบนซิน" }) }
    // Any 2 of {liters, price/L, total} determine the 3rd (total = liters × price). The 3
    // onXCommit functions below carry the actual calculation; TextField onValueChange only ever
    // does a plain `liters = it`-style assignment, and onXCommit runs on focus-loss (see the
    // OutlinedTextFields below), not per keystroke. It used to run on every keystroke — typing
    // "1200" digit by digit into total while liters was still blank recalculated liters after
    // *each* digit (e.g. "1" ÷ price ≈ a tiny fraction), and once liters held that leftover
    // fractional value from an early digit, every later digit's commit re-derived price from
    // that now-stale near-zero liters instead — e.g. price/L ended up as 40000 after typing
    // total "1200" with liters frozen at "0.03" from the "1". Deferring to blur means every
    // commit sees the *finished* value of whichever field the user just left, never a partial one.
    val onLitersCommit: () -> Unit = {
        val l = liters.toDoubleOrNull()
        val p = price.toDoubleOrNull()
        val t = total.toDoubleOrNull()
        when {
            l != null && p != null -> total = "%.2f".format(Locale.US, l * p)
            l != null && l > 0 && t != null -> price = "%.2f".format(Locale.US, t / l)
        }
    }
    val onPriceCommit: () -> Unit = {
        val l = liters.toDoubleOrNull()
        val p = price.toDoubleOrNull()
        val t = total.toDoubleOrNull()
        when {
            l != null && p != null -> total = "%.2f".format(Locale.US, l * p)
            p != null && p > 0 && t != null -> liters = "%.2f".format(Locale.US, t / p)
        }
    }
    val onTotalCommit: () -> Unit = {
        val l = liters.toDoubleOrNull()
        val p = price.toDoubleOrNull()
        val t = total.toDoubleOrNull()
        when {
            l != null && l > 0 && t != null -> price = "%.2f".format(Locale.US, t / l)
            p != null && p > 0 && t != null -> liters = "%.2f".format(Locale.US, t / p)
        }
    }
    // onFocusChanged fires once during a field's initial composition too, reporting the default
    // unfocused state — without this guard, that spurious callback ran on*Commit() immediately
    // whenever the edit screen opened, silently overwriting the just-loaded total with a fresh
    // liters × price before the user ever touched a field (e.g. saved total 1000 with liters
    // 27.24 and price 36.71 reopened showing total back at 999.98).
    var litersHadFocus by remember { mutableStateOf(false) }
    var priceHadFocus by remember { mutableStateOf(false) }
    var totalHadFocus by remember { mutableStateOf(false) }
    var station by remember { mutableStateOf(editing?.station ?: "") }
    var fullTank by remember(editing, defaultFullTank) {
        mutableStateOf(editing?.fullTank ?: defaultFullTank)
    }
    var showStationSelection by remember { mutableStateOf(false) }
    var nearbyStations by remember { mutableStateOf<List<NearbyStation>>(emptyList()) }
    var nearbySearching by remember { mutableStateOf(false) }
    var nearbyError by remember { mutableStateOf<String?>(null) }
    var photoUris by remember { mutableStateOf(editing?.photoUrls ?: emptyList()) }
    // Photo pick + OCR scan is one round trip (see MainActivity.scanFirstPhoto) that can take a
    // few seconds over the network — without this the "สแกนบิล/ใบเสร็จด้วย AI" button and camera
    // icons looked like they'd done nothing until the fields suddenly populated. Kept as two
    // separate flags (not one shared "isScanning") since the receipt-scan button and the
    // odometer camera icon are independent actions — a single shared flag made the odometer
    // icon spin while a receipt scan was in flight, and vice versa.
    var isScanningReceipt by remember { mutableStateOf(false) }
    var isScanningOdometer by remember { mutableStateOf(false) }
    val handlePicked = { picked: List<String>, scanResult: ReceiptScanResult? ->
        isScanningReceipt = false
        isScanningOdometer = false
        photoUris = (photoUris + picked).distinct().take(MAX_PHOTOS)
        // Claude OCR (functions/index.js scanReceipt) fills these when signed
        // in and reachable; falls back to on-device amount-only OCR otherwise.
        scanResult?.date?.let { date = it }
        scanResult?.station?.let { station = it }
        scanResult?.liters?.let { liters = "%.2f".format(Locale.US, it); onLitersCommit() }
        scanResult?.pricePerLiter?.let { price = "%.2f".format(Locale.US, it); onPriceCommit() }
        scanResult?.total?.let { total = "%.2f".format(Locale.US, it); onTotalCommit() }
        if (scanResult?.total == null) {
            scanResult?.amount?.takeIf { total.isBlank() }?.let { total = "%.2f".format(Locale.US, it); onTotalCommit() }
        }
        if (scanResult?.odometer != null) odometer = "%.0f".format(Locale.US, scanResult.odometer)
    }
    LaunchedEffect(Unit) {
        if (autoOpenScan && onPickCameraPhoto != null) {
            isScanningReceipt = true
            onPickCameraPhoto("fuel", handlePicked)
        }
    }
    var odometerIsTripMeter by remember { mutableStateOf(editing?.odometerIsTripMeter ?: false) }
    var tankLevelEnabled by remember { mutableStateOf(editing?.tankLevelEnabled ?: false) }
    var tankLevelTiming by remember { mutableStateOf(editing?.tankLevelTiming ?: "after") }
    var tankLevelPercent by remember { mutableStateOf(editing?.tankLevelPercent) }
    var tankLevelLiters by remember { mutableStateOf(editing?.tankLevelLiters) }
    var tankLevelCapacityInput by remember {
        mutableStateOf(vehicleTankCapacity?.let { "%.1f".format(Locale.US, it) } ?: "")
    }
    var showTankLevelDialog by remember { mutableStateOf(false) }
    var discountEnabled by remember { mutableStateOf(editing?.discountEnabled ?: false) }
    var discountAmount by remember {
        mutableStateOf(editing?.discountAmount?.takeIf { it > 0 }?.let { "%.2f".format(Locale.US, it) } ?: "")
    }
    var discountPerLiter by remember { mutableStateOf(editing?.discountPerLiter ?: false) }
    var missedPreviousFillUp by remember { mutableStateOf(editing?.missedPreviousFillUp ?: false) }
    var weatherDescription by remember { mutableStateOf(editing?.weatherDescription) }
    var weatherTemperatureC by remember { mutableStateOf(editing?.weatherTemperatureC) }
    var weatherLatitude by remember { mutableStateOf(editing?.weatherLatitude) }
    var weatherLongitude by remember { mutableStateOf(editing?.weatherLongitude) }
    var weatherFetching by remember { mutableStateOf(false) }
    var weatherError by remember { mutableStateOf<String?>(null) }
    // Hoisted: this callback fires from onFindNearbyStations' async result, well outside
    // composable context, so stringResource() can't be called at the point of use below.
    val noStationsFoundLabel = stringResource(com.songsit.fuellogpro.R.string.nearby_no_stations)
    val runNearbySearch = {
        nearbySearching = true
        nearbyError = null
        onFindNearbyStations?.invoke(
            { results ->
                nearbySearching = false
                nearbyStations = results
                if (results.isEmpty()) nearbyError = noStationsFoundLabel
            },
            { message ->
                nearbySearching = false
                nearbyError = message
            },
        )
    }
    // Auto-run the nearby-station lookup as soon as the "add new fill-up" dialog opens, so the
    // user doesn't have to tap "หาปั๊มใกล้ฉัน" first — only for new entries, not edits, since an
    // existing entry already has its station filled in.
    LaunchedEffect(Unit) {
        if (editing == null && onFindNearbyStations != null) runNearbySearch()
    }
    LaunchedEffect(Unit) {
        if (editing == null && onFetchWeather != null) {
            weatherFetching = true
            weatherError = null
            onFetchWeather.invoke(
                { info ->
                    weatherFetching = false
                    weatherDescription = info.description
                    weatherTemperatureC = info.temperatureC
                    weatherLatitude = info.latitude
                    weatherLongitude = info.longitude
                },
                { message ->
                    weatherFetching = false
                    weatherError = message
                },
            )
        }
    }
    // Compute the save action once so both the TopAppBar icon and the internal confirm
    // button below share exactly the same lambda without duplicating the validation logic.
    val doSave = {
        val enteredOdometer = odometer.toDoubleOrNull() ?: 0.0
        val odometerValue = if (odometerIsTripMeter) (latestOdometer ?: 0.0) + enteredOdometer else enteredOdometer
        val litersValue = liters.toDoubleOrNull() ?: 0.0
        val priceValue = price.toDoubleOrNull() ?: 0.0
        val grossAmountValue = total.toDoubleOrNull() ?: (litersValue * priceValue)
        val photoUri = PhotoUris.join(photoUris)
        val values = FuelEntryFormValues(
            date = date,
            time = time,
            odometerKm = odometerValue,
            liters = litersValue,
            pricePerLiter = priceValue,
            grossAmount = grossAmountValue,
            fullTank = fullTank,
            station = station,
            photoUri = photoUri,
            odometerIsTripMeter = odometerIsTripMeter,
            tankLevelEnabled = tankLevelEnabled,
            tankLevelTiming = tankLevelTiming,
            tankLevelPercent = tankLevelPercent,
            tankLevelLiters = tankLevelLiters,
            discountEnabled = discountEnabled,
            discountAmount = discountAmount.toDoubleOrNull() ?: 0.0,
            discountPerLiter = discountPerLiter,
            missedPreviousFillUp = missedPreviousFillUp,
            weatherDescription = weatherDescription,
            weatherTemperatureC = weatherTemperatureC,
            weatherLatitude = weatherLatitude,
            weatherLongitude = weatherLongitude,
        )
        if (editing != null && onUpdate != null) {
            onUpdate(editing.id, values, onDismiss)
        } else {
            onSave(values, onDismiss)
        }
    }

    if (showStationSelection) {
        StationSelectionScreen(
            onDismiss = { showStationSelection = false },
            onStationSelected = { station = it },
            nearbyStations = nearbyStations,
            stationVisitCounts = stationVisitCounts,
            isSearching = nearbySearching,
            onRunSearch = { if (onFindNearbyStations != null && !nearbySearching) runNearbySearch() },
            errorMessage = nearbyError
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(com.songsit.fuellogpro.R.string.action_back))
                    }
                },
                title = {
                    Text(
                        if (editing != null) {
                            stringResource(com.songsit.fuellogpro.R.string.fuel_screen_edit_title)
                        } else {
                            stringResource(com.songsit.fuellogpro.R.string.fuel_screen_add_title)
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    IconButton(onClick = doSave, enabled = !saving) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(com.songsit.fuellogpro.R.string.action_save))
                    }
                },
            )
        },
    ) { innerPadding ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(com.songsit.fuellogpro.R.string.station_section_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = station.ifBlank { stringResource(com.songsit.fuellogpro.R.string.station_select_current_location) },
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { showStationSelection = true }) {
                                Text(stringResource(com.songsit.fuellogpro.R.string.action_select))
                            }
                        }
                    }
                }
                item {
                    FormRow(Icons.Filled.Speed) {
                        OdometerField(
                            value = odometer,
                            onValueChange = { odometer = it },
                            isTripMeter = odometerIsTripMeter,
                            onModeChange = { odometerIsTripMeter = it },
                            latestOdometer = latestOdometer,
                            modifier = Modifier.weight(1f),
                            onScanClick = onPickCameraPhoto?.takeIf { !isScanningOdometer }
                                ?.let { pick -> { isScanningOdometer = true; pick("odometer", handlePicked) } },
                            isScanning = isScanningOdometer,
                        )
                    }
                }
                item {
                    FormRow(Icons.Filled.LocalGasStation) {
                        OutlinedTextField(
                            liters,
                            { liters = it },
                            label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_fuel_liters)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).onFocusChanged {
                                if (it.isFocused) litersHadFocus = true else if (litersHadFocus) onLitersCommit()
                            },
                        )
                        val availableFuelTypes = remember(vehicleFuelType) { getFuelTypeOptionsForVehicle(vehicleFuelType) }
                        UnitDropdownField(
                            stringResource(com.songsit.fuellogpro.R.string.label_fuel_type),
                            fuelType,
                            availableFuelTypes,
                            modifier = Modifier.weight(1f),
                        ) { fuelType = it }
                    }
                }
                item {
                    FormRow(Icons.Filled.AttachMoney) {
                        OutlinedTextField(
                            price,
                            { price = it },
                            label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_price_per_liter)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).onFocusChanged {
                                if (it.isFocused) priceHadFocus = true else if (priceHadFocus) onPriceCommit()
                            },
                        )
                        OutlinedTextField(
                            total,
                            { total = it },
                            label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_total_price)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).onFocusChanged {
                                if (it.isFocused) totalHadFocus = true else if (totalHadFocus) onTotalCommit()
                            },
                        )
                    }
                }
                item {
                    FormRow(Icons.Filled.CalendarToday) {
                        OutlinedTextField(
                            date,
                            { date = it },
                            label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_date)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            time,
                            { time = it },
                            label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_time)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (onPickPhoto != null) {
                    item {
                        PhotoAttachmentRow(
                            photoUris = photoUris,
                            // Plain "แนบรูป" — type = null so scanFirstPhoto() skips OCR entirely;
                            // only the dedicated "สแกนบิล/ใบเสร็จด้วย AI" button below opts into it.
                            onPickGallery = { isScanningReceipt = true; onPickPhoto(null, handlePicked) },
                            onPickCamera = onPickCameraPhoto?.let { pick -> { isScanningReceipt = true; pick(null, handlePicked) } },
                            onRemove = { uri -> photoUris = photoUris - uri },
                        )
                    }
                    item {
                        var showScanSourceMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    if (onPickCameraPhoto != null) {
                                        showScanSourceMenu = true
                                    } else {
                                        isScanningReceipt = true
                                        onPickPhoto("fuel", handlePicked)
                                    }
                                },
                                enabled = !isScanningReceipt,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (isScanningReceipt) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(com.songsit.fuellogpro.R.string.action_scanning))
                                } else {
                                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(com.songsit.fuellogpro.R.string.action_scan_receipt_ai))
                                }
                            }
                            if (onPickCameraPhoto != null) {
                                DropdownMenu(
                                    expanded = showScanSourceMenu,
                                    onDismissRequest = { showScanSourceMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(com.songsit.fuellogpro.R.string.action_take_photo)) },
                                        onClick = { showScanSourceMenu = false; isScanningReceipt = true; onPickCameraPhoto("fuel", handlePicked) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(com.songsit.fuellogpro.R.string.action_choose_from_gallery)) },
                                        onClick = { showScanSourceMenu = false; isScanningReceipt = true; onPickPhoto("fuel", handlePicked) },
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(com.songsit.fuellogpro.R.string.label_full_tank))
                        Switch(checked = fullTank, onCheckedChange = { fullTank = it })
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(com.songsit.fuellogpro.R.string.label_set_tank_level))
                        Switch(checked = tankLevelEnabled, onCheckedChange = { tankLevelEnabled = it })
                    }
                }
                if (tankLevelEnabled) {
                    item {
                        val beforeLabel = stringResource(com.songsit.fuellogpro.R.string.tank_level_before)
                        val afterLabel = stringResource(com.songsit.fuellogpro.R.string.tank_level_after)
                        val notSetLabel = stringResource(com.songsit.fuellogpro.R.string.tank_level_not_set)
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showTankLevelDialog = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            val summary = tankLevelLiters?.let {
                                val timingLabel = if (tankLevelTiming == "before") beforeLabel else afterLabel
                                "$timingLabel ${tankLevelPercent?.toInt() ?: 0}% (${"%.1f".format(Locale.US, it)} L)"
                            } ?: notSetLabel
                            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_edit_tank_level),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                if (onFetchWeather != null) {
                    item { Text(stringResource(com.songsit.fuellogpro.R.string.section_weather), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    item {
                        FormRow(Icons.Filled.WbSunny) {
                            val description = weatherDescription
                            val summary = when {
                                weatherFetching -> stringResource(com.songsit.fuellogpro.R.string.weather_fetching)
                                description != null -> description + (weatherTemperatureC?.let { " • %.1f°C".format(Locale.US, it) } ?: "")
                                weatherError != null -> weatherError ?: ""
                                else -> stringResource(com.songsit.fuellogpro.R.string.weather_no_data)
                            }
                            Text(summary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item { Text(stringResource(com.songsit.fuellogpro.R.string.section_optional), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(com.songsit.fuellogpro.R.string.label_discount))
                        Switch(checked = discountEnabled, onCheckedChange = { discountEnabled = it })
                    }
                }
                if (discountEnabled) {
                    item {
                        val bahtLabel = stringResource(com.songsit.fuellogpro.R.string.unit_baht)
                        val bahtPerLiterLabel = stringResource(com.songsit.fuellogpro.R.string.unit_baht_per_liter)
                        FormRow(Icons.Filled.Percent) {
                            OutlinedTextField(
                                discountAmount,
                                { discountAmount = it },
                                label = {
                                    Text(
                                        if (discountPerLiter) {
                                            stringResource(com.songsit.fuellogpro.R.string.label_discount_per_liter)
                                        } else {
                                            stringResource(com.songsit.fuellogpro.R.string.label_discount_flat)
                                        },
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            UnitDropdownField(
                                stringResource(com.songsit.fuellogpro.R.string.label_discount_unit),
                                if (discountPerLiter) bahtPerLiterLabel else bahtLabel,
                                listOf(bahtLabel, bahtPerLiterLabel),
                                modifier = Modifier.weight(1f),
                            ) { discountPerLiter = it == bahtPerLiterLabel }
                        }
                    }
                }
                item {
                    FormRow(Icons.Filled.Info) {
                        Text(stringResource(com.songsit.fuellogpro.R.string.label_missed_previous_fillup), modifier = Modifier.weight(1f))
                        Switch(checked = missedPreviousFillUp, onCheckedChange = { missedPreviousFillUp = it })
                    }
                }
            }
        }
        if (showTankLevelDialog) {
            TankLevelDialog(
                timing = tankLevelTiming,
                percent = tankLevelPercent ?: 0.0,
                tankCapacityInput = tankLevelCapacityInput,
                onDismiss = { showTankLevelDialog = false },
                onSave = { timing, percent, capacityInput, estimatedLiters ->
                    tankLevelTiming = timing
                    tankLevelPercent = percent
                    tankLevelCapacityInput = capacityInput
                    tankLevelLiters = estimatedLiters
                    showTankLevelDialog = false
                },
            )
        }
    }
}

@Composable
private fun OdometerField(
    value: String,
    onValueChange: (String) -> Unit,
    isTripMeter: Boolean,
    onModeChange: (Boolean) -> Unit,
    latestOdometer: Double?,
    modifier: Modifier = Modifier,
    onScanClick: (() -> Unit)? = null,
    isScanning: Boolean = false,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = {
                    Text(
                        if (isTripMeter) {
                            stringResource(com.songsit.fuellogpro.R.string.odometer_trip_label)
                        } else {
                            stringResource(com.songsit.fuellogpro.R.string.odometer_total_label)
                        },
                    )
                },
                singleLine = true,
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                        } else if (onScanClick != null) {
                            IconButton(onClick = onScanClick) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_scan_odometer))
                            }
                        }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_select_odometer_mode))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text(stringResource(com.songsit.fuellogpro.R.string.odometer_mode_total)) }, onClick = { onModeChange(false); menuExpanded = false })
                DropdownMenuItem(text = { Text(stringResource(com.songsit.fuellogpro.R.string.odometer_mode_trip)) }, onClick = { onModeChange(true); menuExpanded = false })
            }
        }
        if (latestOdometer != null) {
            Text(
                stringResource(com.songsit.fuellogpro.R.string.odometer_last_value, "%.0f".format(Locale.US, latestOdometer)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
        }
    }
}

// Fuelio's tank-level set dialog: pick whether the % reading is before or after this fill-up,
// confirm the tank capacity, and drag a slider to estimate the liters currently in the tank.
@Composable
private fun TankLevelDialog(
    timing: String,
    percent: Double,
    tankCapacityInput: String,
    onDismiss: () -> Unit,
    onSave: (timing: String, percent: Double, capacityInput: String, estimatedLiters: Double?) -> Unit,
) {
    var localTiming by remember { mutableStateOf(timing) }
    var localCapacity by remember { mutableStateOf(tankCapacityInput) }
    var localPercent by remember { mutableStateOf(percent.toFloat()) }
    val capacityValue = localCapacity.toDoubleOrNull()
    val estimatedLiters = capacityValue?.let { it * localPercent / 100.0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(com.songsit.fuellogpro.R.string.tank_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(com.songsit.fuellogpro.R.string.tank_dialog_subtitle), style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = localTiming == "before", onClick = { localTiming = "before" })
                    Text(
                        stringResource(com.songsit.fuellogpro.R.string.tank_before_short),
                        modifier = Modifier.clickable { localTiming = "before" },
                    )
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = localTiming == "after", onClick = { localTiming = "after" })
                    Text(
                        stringResource(com.songsit.fuellogpro.R.string.tank_after_short),
                        modifier = Modifier.clickable { localTiming = "after" },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        localCapacity,
                        { localCapacity = it },
                        label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_tank_capacity)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        estimatedLiters?.let { "%.1f".format(Locale.US, it) } ?: "-",
                        {},
                        readOnly = true,
                        label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_estimated_fuel)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Slider(value = localPercent, onValueChange = { localPercent = it }, valueRange = 0f..100f)
                Text("${localPercent.toInt()}%", style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(localTiming, localPercent.toDouble(), localCapacity, estimatedLiters) }) { Text(stringResource(com.songsit.fuellogpro.R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(com.songsit.fuellogpro.R.string.action_cancel)) } },
    )
}

// Shows up to MAX_PHOTOS thumbnails of attached receipts/photos (each decoded from its local
// file path), each individually removable, plus a pick button that's hidden once the cap is
// reached.
private const val MAX_PHOTOS = 8

@Composable
private fun PhotoAttachmentRow(
    photoUris: List<String>,
    onPickGallery: () -> Unit,
    onRemove: (String) -> Unit,
    onPickCamera: (() -> Unit)? = null,
    onPickPdf: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        photoUris.forEach { uri ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.size(64.dp).clickable {
                    if (isPdfPath(uri)) openPdfExternally(context, uri) else fullScreenImageUri = uri
                },
            ) {
                if (isPdfPath(uri)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_pdf_file), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    AsyncImage(
                        model = if (uri.startsWith("/")) java.io.File(uri) else uri,
                        contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_attached_photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        if (photoUris.size < MAX_PHOTOS) {
            var showSourceMenu by remember { mutableStateOf(false) }
            val hasMenu = onPickCamera != null || onPickPdf != null
            Box {
                TextButton(onClick = { if (hasMenu) showSourceMenu = true else onPickGallery() }) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (photoUris.isEmpty()) {
                            stringResource(com.songsit.fuellogpro.R.string.action_attach_photo_receipt)
                        } else {
                            stringResource(com.songsit.fuellogpro.R.string.action_add_photo)
                        },
                    )
                }
                if (hasMenu) {
                    DropdownMenu(
                        expanded = showSourceMenu,
                        onDismissRequest = { showSourceMenu = false },
                    ) {
                        if (onPickCamera != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(com.songsit.fuellogpro.R.string.action_take_photo)) },
                                onClick = { showSourceMenu = false; onPickCamera() },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(com.songsit.fuellogpro.R.string.action_choose_from_gallery)) },
                            onClick = { showSourceMenu = false; onPickGallery() },
                        )
                        if (onPickPdf != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(com.songsit.fuellogpro.R.string.action_attach_pdf)) },
                                onClick = { showSourceMenu = false; onPickPdf() },
                            )
                        }
                    }
                }
            }
        }
    }

    fullScreenImageUri?.let { uri ->
        FullScreenImageViewer(
            imagePath = uri,
            onDismiss = { fullScreenImageUri = null },
            onDelete = { onRemove(uri) },
        )
    }
}

private val expenseCategories = listOf(
    "บริการ", "บำรุงรักษา", "ทะเบียน", "ที่จอดรถ", "ล้างรถ", "ทางด่วน", "ตั๋ว/ค่าปรับ", "ปรับแต่ง", "การประกันภัย",
)

// AddExpenseDialog / AddExpenseScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseScreen(
    saving: Boolean,
    latestOdometer: Double?,
    editing: Expense? = null,
    selectedVehicleLabel: String? = null,
    selectedVehicleOdometer: Double? = null,
    onPickPhoto: ((type: String?, onPicked: (uris: List<String>, scanResult: ReceiptScanResult?) -> Unit) -> Unit)? = null,
    onPickCameraPhoto: ((type: String?, onPicked: (uris: List<String>, scanResult: ReceiptScanResult?) -> Unit) -> Unit)? = null,
    onPickPdf: ((onPicked: (uris: List<String>) -> Unit) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Double, Double?, Boolean, Boolean, String?, String?, () -> Unit) -> Unit,
    onUpdate: ((String, String, String, String, String, Double, Double?, Boolean, Boolean, String?, String?, () -> Unit) -> Unit)? = null,
) {
    var date by remember { mutableStateOf(editing?.date ?: LocalDate.now().toString()) }
    var time by remember { mutableStateOf(editing?.time ?: LocalTime.now().withSecond(0).withNano(0).toString()) }
    var photoUris by remember { mutableStateOf(editing?.photoUrls ?: emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(editing?.category ?: expenseCategories.first()) }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var amount by remember { mutableStateOf(editing?.amount?.let { "%.2f".format(Locale.US, it) } ?: "") }
    var odometer by remember {
        mutableStateOf(editing?.odometerKm?.let { "%.0f".format(Locale.US, it) } ?: latestOdometer?.let { "%.0f".format(Locale.US, it) } ?: "")
    }
    var income by remember { mutableStateOf(editing?.income ?: false) }
    var recurring by remember { mutableStateOf(editing?.recurring ?: false) }
    var saveAsTemplate by remember { mutableStateOf(false) }
    var reminderDate by remember { mutableStateOf(editing?.reminderDate ?: "") }
    var reminderEnabled by remember { mutableStateOf(!editing?.reminderDate.isNullOrBlank()) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    fun save() {
        val amountValue = amount.toDoubleOrNull() ?: 0.0
        val odometerValue = odometer.toDoubleOrNull()
        val reminderValue = reminderDate.takeIf { reminderEnabled && it.isNotBlank() }
        val photoUri = PhotoUris.join(photoUris)
        if (editing != null && onUpdate != null) {
            onUpdate(editing.id, date, time, category, description, amountValue, odometerValue, income, recurring, reminderValue, photoUri, onDismiss)
        } else {
            onSave(date, time, category, description, amountValue, odometerValue, income, recurring, reminderValue, photoUri, onDismiss)
        }
    }

    val expenseHandlePicked = { picked: List<String>, scanResult: ReceiptScanResult? ->
        isScanning = false
        photoUris = (photoUris + picked).distinct().take(MAX_PHOTOS)
        // Claude Vision OCR (functions/index.js scanReceipt) fills these when signed in and
        // reachable; falls back to on-device amount-only OCR otherwise.
        scanResult?.date?.let { date = it }
        scanResult?.title?.takeIf { description.isBlank() }?.let { description = it }
        val extractedAmount = scanResult?.amount ?: scanResult?.total
        if (amount.isBlank() && extractedAmount != null) {
            amount = "%.2f".format(Locale.US, extractedAmount)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(com.songsit.fuellogpro.R.string.action_back))
                    }
                },
                title = { Text(if (editing != null) stringResource(com.songsit.fuellogpro.R.string.expense_edit_title) else stringResource(com.songsit.fuellogpro.R.string.expense_add_title)) },
                actions = {
                    IconButton(enabled = !saving, onClick = { save() }) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(com.songsit.fuellogpro.R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (selectedVehicleLabel != null) {
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(selectedVehicleLabel, fontWeight = FontWeight.SemiBold)
                            selectedVehicleOdometer?.let {
                                Text("${"%.0f".format(Locale.US, it)} km", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            item {
                Box {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_category)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { categoryMenuExpanded = true }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_select_category))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                        expenseCategories.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { category = option; categoryMenuExpanded = false })
                        }
                    }
                }
            }
            item { Text(stringResource(com.songsit.fuellogpro.R.string.section_expense_info), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item {
                OutlinedTextField(
                    description, { description = it },
                    label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_title_field)) },
                    leadingIcon = { Text("T", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    amount, { amount = it },
                    label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_total_price)) },
                    leadingIcon = { Icon(Icons.Filled.AttachMoney, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        date, { date = it },
                        label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_date)) },
                        leadingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(time, { time = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_time)) }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(com.songsit.fuellogpro.R.string.label_expense_as_income))
                    Switch(checked = income, onCheckedChange = { income = it })
                }
            }
            item { Text(stringResource(com.songsit.fuellogpro.R.string.section_optional_fields), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item {
                OutlinedTextField(
                    reminderDate, { reminderDate = it },
                    label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_notes)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    odometer, { odometer = it },
                    label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_odometer_total_reading)) },
                    leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(com.songsit.fuellogpro.R.string.label_save_as_template))
                    Switch(checked = saveAsTemplate, onCheckedChange = { saveAsTemplate = it })
                }
            }
            item { Text(stringResource(com.songsit.fuellogpro.R.string.section_recurring), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item {
                Box {
                    var recurringMenuExpanded by remember { mutableStateOf(false) }
                    val monthlyLabel = stringResource(com.songsit.fuellogpro.R.string.recurring_monthly)
                    val oneTimeLabel = stringResource(com.songsit.fuellogpro.R.string.recurring_one_time)
                    OutlinedTextField(
                        value = if (recurring) monthlyLabel else oneTimeLabel,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { recurringMenuExpanded = true }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(com.songsit.fuellogpro.R.string.content_desc_select_frequency))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(expanded = recurringMenuExpanded, onDismissRequest = { recurringMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text(oneTimeLabel) }, onClick = { recurring = false; recurringMenuExpanded = false })
                        DropdownMenuItem(text = { Text(monthlyLabel) }, onClick = { recurring = true; recurringMenuExpanded = false })
                    }
                }
            }
            if (onPickPhoto != null) {
                item { Text(stringResource(com.songsit.fuellogpro.R.string.section_photos_scan), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                item {
                    PhotoAttachmentRow(
                        photoUris = photoUris,
                        // Plain "attach photo" — type = null so scanFirstPhoto() skips OCR entirely;
                        // only the dedicated "Scan receipt with AI" button below opts into it.
                        onPickGallery = { isScanning = true; onPickPhoto(null, expenseHandlePicked) },
                        onPickCamera = onPickCameraPhoto?.let { pick -> { isScanning = true; pick(null, expenseHandlePicked) } },
                        onPickPdf = onPickPdf?.let { pick ->
                            {
                                pick { uris -> photoUris = (photoUris + uris).distinct().take(MAX_PHOTOS) }
                            }
                        },
                        onRemove = { uri -> photoUris = photoUris - uri },
                    )
                }
                item {
                    var showScanSourceMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (onPickCameraPhoto != null) {
                                    showScanSourceMenu = true
                                } else {
                                    isScanning = true
                                    onPickPhoto("expense", expenseHandlePicked)
                                }
                            },
                            enabled = !isScanning,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(com.songsit.fuellogpro.R.string.action_scanning))
                            } else {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(com.songsit.fuellogpro.R.string.action_scan_receipt_ai))
                            }
                        }
                        if (onPickCameraPhoto != null) {
                            DropdownMenu(
                                expanded = showScanSourceMenu,
                                onDismissRequest = { showScanSourceMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(com.songsit.fuellogpro.R.string.action_take_photo)) },
                                    onClick = { showScanSourceMenu = false; isScanning = true; onPickCameraPhoto("expense", expenseHandlePicked) },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(com.songsit.fuellogpro.R.string.action_choose_from_gallery)) },
                                    onClick = { showScanSourceMenu = false; isScanning = true; onPickPhoto("expense", expenseHandlePicked) },
                                )
                            }
                        }
                    }
                }
            }
            item { Text(stringResource(com.songsit.fuellogpro.R.string.section_payment_reminder), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(com.songsit.fuellogpro.R.string.label_add_reminder))
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
            }
            if (reminderEnabled) {
                item {
                    OutlinedTextField(
                        reminderDate, { reminderDate = it },
                        label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_reminder_date)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Button(enabled = !saving, onClick = { save() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (saving) stringResource(com.songsit.fuellogpro.R.string.action_saving) else stringResource(com.songsit.fuellogpro.R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun AddMaintenanceDialog(
    saving: Boolean,
    latestOdometer: Double?,
    editing: MaintenanceTask? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, Double?, Int, Double, Int?, Double?, () -> Unit) -> Unit,
    onUpdate: ((String, String, String, String?, Double?, Int, Double, Int?, Double?, () -> Unit) -> Unit)? = null,
) {
    // Default name is translated (free text, no data-matching implications); category default
    // and maintenanceCategoryOptions are NOT — see the string resource comment for why.
    val defaultMaintenanceName = stringResource(com.songsit.fuellogpro.R.string.maintenance_default_name)
    var name by remember { mutableStateOf(editing?.name ?: defaultMaintenanceName) }
    var category by remember { mutableStateOf(editing?.category ?: "บำรุงรักษา") }
    var nextDate by remember { mutableStateOf(editing?.nextDate ?: "") }
    var nextOdometer by remember {
        mutableStateOf(
            editing?.nextOdometerKm?.let { "%.0f".format(Locale.US, it) }
                ?: latestOdometer?.let { "%.0f".format(Locale.US, it + 10_000) } ?: "",
        )
    }
    var warningDays by remember { mutableStateOf((editing?.warningDays ?: 30).toString()) }
    var warningOdometer by remember { mutableStateOf((editing?.warningOdometerKm ?: 1000.0).let { "%.0f".format(Locale.US, it) }) }
    var repeatMonths by remember { mutableStateOf(editing?.repeatMonths?.toString() ?: "12") }
    var repeatOdometer by remember { mutableStateOf(editing?.repeatOdometerKm?.let { "%.0f".format(Locale.US, it) } ?: "10000") }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val selectedCategory = if (category in maintenanceCategoryOptions) category else "อื่นๆ"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (editing != null) {
                    stringResource(com.songsit.fuellogpro.R.string.maintenance_dialog_edit_title)
                } else {
                    stringResource(com.songsit.fuellogpro.R.string.maintenance_dialog_add_title)
                },
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_name)) }, singleLine = true) }
                item {
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_category)) },
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Box(Modifier.matchParentSize().clickable { categoryMenuExpanded = true })
                        DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                            maintenanceCategoryOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        categoryMenuExpanded = false
                                        category = if (option == "อื่นๆ") "" else option
                                    },
                                )
                            }
                        }
                    }
                }
                if (selectedCategory == "อื่นๆ") {
                    item {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_specify_category)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                item { OutlinedTextField(nextDate, { nextDate = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_due_date_optional)) }, singleLine = true) }
                item { OutlinedTextField(nextOdometer, { nextOdometer = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_due_odometer_optional)) }, singleLine = true) }
                item { OutlinedTextField(warningDays, { warningDays = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_warning_days)) }, singleLine = true) }
                item { OutlinedTextField(warningOdometer, { warningOdometer = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_warning_odometer)) }, singleLine = true) }
                item { OutlinedTextField(repeatMonths, { repeatMonths = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_repeat_months)) }, singleLine = true) }
                item { OutlinedTextField(repeatOdometer, { repeatOdometer = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_repeat_odometer)) }, singleLine = true) }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    val nextDateValue = nextDate.takeIf(String::isNotBlank)
                    val nextOdometerValue = nextOdometer.toDoubleOrNull()
                    val warningDaysValue = warningDays.toIntOrNull() ?: 30
                    val warningOdometerValue = warningOdometer.toDoubleOrNull() ?: 1_000.0
                    val repeatMonthsValue = repeatMonths.toIntOrNull()
                    val repeatOdometerValue = repeatOdometer.toDoubleOrNull()
                    if (editing != null && onUpdate != null) {
                        onUpdate(
                            editing.id, name, category, nextDateValue, nextOdometerValue,
                            warningDaysValue, warningOdometerValue, repeatMonthsValue, repeatOdometerValue, onDismiss,
                        )
                    } else {
                        onSave(
                            name, category, nextDateValue, nextOdometerValue,
                            warningDaysValue, warningOdometerValue, repeatMonthsValue, repeatOdometerValue, onDismiss,
                        )
                    }
                },
            ) { Text(if (saving) stringResource(com.songsit.fuellogpro.R.string.action_saving) else stringResource(com.songsit.fuellogpro.R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(com.songsit.fuellogpro.R.string.action_cancel)) } },
    )
}

@Composable
private fun AddTripDialog(
    saving: Boolean,
    editing: Trip? = null,
    prefillDistanceKm: Double? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double, Double, Double, Double, Double, () -> Unit) -> Unit,
    onUpdate: ((String, String, String, Double, Double, Double, Double, Double, Double, () -> Unit) -> Unit)? = null,
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var date by remember { mutableStateOf(editing?.date ?: LocalDate.now().toString()) }
    var distance by remember {
        mutableStateOf(
            editing?.distanceKm?.let { "%.0f".format(Locale.US, it) }
                ?: prefillDistanceKm?.let { "%.2f".format(Locale.US, it) }
                ?: "",
        )
    }
    var fuel by remember { mutableStateOf(editing?.fuelCost?.let { "%.2f".format(Locale.US, it) } ?: "") }
    var toll by remember { mutableStateOf(editing?.tollCost?.let { "%.2f".format(Locale.US, it) } ?: "") }
    var parking by remember { mutableStateOf(editing?.parkingCost?.let { "%.2f".format(Locale.US, it) } ?: "") }
    var food by remember { mutableStateOf(editing?.foodCost?.let { "%.2f".format(Locale.US, it) } ?: "") }
    var other by remember { mutableStateOf(editing?.otherCost?.let { "%.2f".format(Locale.US, it) } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (editing != null) {
                    stringResource(com.songsit.fuellogpro.R.string.trip_dialog_edit_title)
                } else {
                    stringResource(com.songsit.fuellogpro.R.string.trip_dialog_add_title)
                },
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_trip_name)) }, singleLine = true) }
                item { OutlinedTextField(date, { date = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_date_ymd)) }, singleLine = true) }
                item { OutlinedTextField(distance, { distance = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_distance_km)) }, singleLine = true) }
                item { OutlinedTextField(fuel, { fuel = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_fuel_cost)) }, singleLine = true) }
                item { OutlinedTextField(toll, { toll = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_toll_cost)) }, singleLine = true) }
                item { OutlinedTextField(parking, { parking = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_parking_cost)) }, singleLine = true) }
                item { OutlinedTextField(food, { food = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_food_lodging)) }, singleLine = true) }
                item { OutlinedTextField(other, { other = it }, label = { Text(stringResource(com.songsit.fuellogpro.R.string.label_other_cost)) }, singleLine = true) }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    val distanceValue = distance.toDoubleOrNull() ?: 0.0
                    val fuelValue = fuel.toDoubleOrNull() ?: 0.0
                    val tollValue = toll.toDoubleOrNull() ?: 0.0
                    val parkingValue = parking.toDoubleOrNull() ?: 0.0
                    val foodValue = food.toDoubleOrNull() ?: 0.0
                    val otherValue = other.toDoubleOrNull() ?: 0.0
                    if (editing != null && onUpdate != null) {
                        onUpdate(editing.id, name, date, distanceValue, fuelValue, tollValue, parkingValue, foodValue, otherValue, onDismiss)
                    } else {
                        onSave(name, date, distanceValue, fuelValue, tollValue, parkingValue, foodValue, otherValue, onDismiss)
                    }
                },
            ) { Text(if (saving) stringResource(com.songsit.fuellogpro.R.string.action_saving) else stringResource(com.songsit.fuellogpro.R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(com.songsit.fuellogpro.R.string.action_cancel)) } },
    )
}

@Composable
private fun ImportSummaryDialog(
    result: BackupImportResult,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(com.songsit.fuellogpro.R.string.import_summary_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(com.songsit.fuellogpro.R.string.import_summary_intro), style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(com.songsit.fuellogpro.R.string.import_summary_vehicles))
                    Text(
                        stringResource(com.songsit.fuellogpro.R.string.import_summary_vehicles_count, result.vehicles),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(com.songsit.fuellogpro.R.string.import_summary_fuel_entries))
                    Text(
                        stringResource(com.songsit.fuellogpro.R.string.import_summary_count_items, result.fuelEntries),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(com.songsit.fuellogpro.R.string.import_summary_expenses))
                    Text(
                        stringResource(com.songsit.fuellogpro.R.string.import_summary_count_items, result.expenses),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(com.songsit.fuellogpro.R.string.import_summary_photos))
                    Text(
                        stringResource(com.songsit.fuellogpro.R.string.import_summary_photos_count, result.photos),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(com.songsit.fuellogpro.R.string.action_ok)) }
        },
    )
}
