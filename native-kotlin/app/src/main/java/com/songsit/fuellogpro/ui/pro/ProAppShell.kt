package com.songsit.fuellogpro.ui.pro

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.songsit.fuellogpro.R
import com.songsit.fuellogpro.data.BackupImportResult
import com.songsit.fuellogpro.data.NearbyStation
import com.songsit.fuellogpro.data.OilPriceInfo
import com.songsit.fuellogpro.data.ReceiptScanResult
import com.songsit.fuellogpro.data.WeatherInfo
import com.songsit.fuellogpro.data.firebase.VehicleMember
import com.songsit.fuellogpro.data.local.SyncConflictEntity
import com.songsit.fuellogpro.domain.model.Expense
import com.songsit.fuellogpro.domain.model.FuelEntry
import com.songsit.fuellogpro.domain.model.FuelEntryFormValues
import com.songsit.fuellogpro.domain.model.MaintenanceTask
import com.songsit.fuellogpro.domain.model.Trip
import com.songsit.fuellogpro.domain.model.Vehicle
import com.songsit.fuellogpro.domain.model.VehicleFormValues
import com.songsit.fuellogpro.notifications.ReminderSettings
import com.songsit.fuellogpro.settings.DisplaySettings
import com.songsit.fuellogpro.settings.ProOnboardingPreferences
import com.songsit.fuellogpro.ui.AddExpenseScreen
import com.songsit.fuellogpro.ui.AddFuelScreen
import com.songsit.fuellogpro.ui.AddMaintenanceDialog
import com.songsit.fuellogpro.ui.AddTripDialog
import com.songsit.fuellogpro.ui.CloudUiState
import com.songsit.fuellogpro.ui.ExpenseList
import com.songsit.fuellogpro.ui.LocalDisplaySettings
import com.songsit.fuellogpro.ui.MaintenanceList
import com.songsit.fuellogpro.ui.NativeAppState
import com.songsit.fuellogpro.ui.NearbyStationsMapScreen
import com.songsit.fuellogpro.ui.SettingsScreen
import com.songsit.fuellogpro.ui.TripCalculatorScreen
import com.songsit.fuellogpro.ui.TripList
import com.songsit.fuellogpro.ui.VehicleEditScreen
import com.songsit.fuellogpro.ui.VehiclesListScreen
import com.songsit.fuellogpro.ui.stats.StatsScreen
import com.songsit.fuellogpro.ui.timeline.FullScreenImageViewer
import com.songsit.fuellogpro.ui.timeline.TimelineScreen

private enum class ProTab { DASHBOARD, TIMELINE, STATS, TRIPS, MORE }

/**
 * App shell for the "Pro" theme (FuelLog Pro Redesign concept) — a 5-tab bottom nav
 * (Dashboard/Timeline/Stats/Trips/More) replacing the default drawer, per the approved redesign
 * plan. Only reachable when displaySettings.themePalette == "pro" (see the gate at the top of
 * FuelLogApp()). Mirrors FuelLogApp's full parameter surface so the two shells stay wired to
 * the exact same callbacks/state — this file owns navigation and a bespoke Dashboard/More/
 * Onboarding; every other screen (Timeline/Stats/Trips/Expenses/Maintenance/Vehicles/Map/
 * Settings/Add Fuel/Add Expense/Add Maintenance/Add Trip/Edit Vehicle) reuses the existing
 * composable as-is so it inherits the Pro palette automatically and keeps its real business
 * logic (OCR scan, nearby stations, weather, sync, etc.) untouched.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProAppShell(
    state: NativeAppState,
    onAddFuel: (FuelEntryFormValues, () -> Unit) -> Unit,
    onUpdateFuel: (String, FuelEntryFormValues, () -> Unit) -> Unit,
    onDeleteFuel: (String) -> Unit,
    onAddExpense: (String, String, String, String, Double, Double?, Boolean, Boolean, String?, String?, String, () -> Unit) -> Unit,
    onUpdateExpense: (String, String, String, String, String, Double, Double?, Boolean, Boolean, String?, String?, String, () -> Unit) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onAddMaintenance: (String, String, String?, Double?, Int, Double, Int?, Double?, () -> Unit) -> Unit,
    onUpdateMaintenance: (String, String, String, String?, Double?, Int, Double, Int?, Double?, () -> Unit) -> Unit,
    onCompleteMaintenance: (String) -> Unit,
    onDeleteMaintenance: (String) -> Unit,
    onAddTrip: (String, String, Double, Double, Double, Double, Double, Double, () -> Unit) -> Unit,
    onUpdateTrip: (String, String, String, Double, Double, Double, Double, Double, Double, () -> Unit) -> Unit,
    onDeleteTrip: (String) -> Unit,
    onStartTripRecording: (() -> Unit)?,
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
    onFindNearbyStations: ((onResult: (List<NearbyStation>) -> Unit, onError: (String) -> Unit) -> Unit)?,
    onFetchWeather: ((onResult: (WeatherInfo) -> Unit, onError: (String) -> Unit) -> Unit)?,
    onPickPhoto: ((type: String?, onPicked: (uris: List<String>, scanResult: ReceiptScanResult?) -> Unit) -> Unit)?,
    onPickCameraPhoto: ((type: String?, onPicked: (uris: List<String>, scanResult: ReceiptScanResult?) -> Unit) -> Unit)?,
    onPickPdf: ((onPicked: (uris: List<String>) -> Unit) -> Unit)?,
    // Runs OCR on a photo that's already attached (e.g. shared in) instead of only right after a
    // fresh pick/capture — see AddFuelScreen's param of the same name for the full explanation.
    onScanExistingPhoto: ((path: String, type: String, onResult: (ReceiptScanResult?) -> Unit) -> Unit)? = null,
    oilPriceInfo: OilPriceInfo?,
    vehicleMembers: List<VehicleMember>,
    onCreateInvite: ((email: String, role: String, onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit)?,
    onJoinByCode: ((code: String, onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit)?,
    displaySettings: DisplaySettings,
    onDisplaySettingsChange: (DisplaySettings) -> Unit,
    importSummaryResult: BackupImportResult?,
    onDismissImportSummary: () -> Unit,
    onDriveBackup: (() -> Unit)?,
    onDriveRestore: (() -> Unit)?,
    driveAutoSyncEnabled: Boolean,
    onDriveAutoSyncChange: ((Boolean) -> Unit)?,
    // Photo(s) shared in from another app (Gallery/Camera/LINE/etc. via Android's share sheet —
    // dev build only, see MainActivity's ACTION_SEND/SEND_MULTIPLE handling and
    // src/debug/AndroidManifest.xml) already copied into app storage. Non-empty triggers the
    // "save as" chooser below; onSharedPhotoConsumed clears it in MainActivity once handled (or
    // dismissed) so it doesn't reappear on the next recomposition.
    sharedPhotoPaths: List<String> = emptyList(),
    onSharedPhotoConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    var showOnboarding by remember { mutableStateOf(!ProOnboardingPreferences(context).hasSeenOnboarding()) }
    var tab by remember { mutableIntStateOf(0) }
    var moreDestination by remember { mutableStateOf<ProMoreDestination?>(null) }
    var showAddFuel by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }
    var dashboardFabExpanded by remember { mutableStateOf(false) }
    var showAddMaintenance by remember { mutableStateOf(false) }
    var showAddTrip by remember { mutableStateOf(false) }
    var showAddVehicle by remember { mutableStateOf(false) }
    var editingFuel by remember { mutableStateOf<FuelEntry?>(null) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var editingMaintenance by remember { mutableStateOf<MaintenanceTask?>(null) }
    var editingTrip by remember { mutableStateOf<Trip?>(null) }
    var editingVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var prefillTripDistanceKm by remember { mutableStateOf<Double?>(null) }
    var viewingImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var viewingImageIndex by remember { mutableIntStateOf(0) }
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val requestDelete: (() -> Unit) -> Unit = { action ->
        if (displaySettings.confirmBeforeDelete) pendingDeleteAction = action else action()
    }
    var showVehicleMenu by remember { mutableStateOf(false) }
    var pendingPhotoPathsForForm by remember { mutableStateOf<List<String>>(emptyList()) }

    if (showOnboarding) {
        ProOnboardingScreen(
            averageKmPerLiterLabel = state.summary.averageKmPerLiter?.let {
                com.songsit.fuellogpro.ui.formatEconomyKmPerLiter(it, displaySettings)
            },
            onGoogleSignIn = {
                ProOnboardingPreferences(context).markOnboardingSeen()
                showOnboarding = false
                onGoogleSignIn()
            },
            onSkip = {
                ProOnboardingPreferences(context).markOnboardingSeen()
                showOnboarding = false
            },
        )
        return
    }

    val anyDialogOpen = showAddFuel || showAddExpense || showAddMaintenance || showAddTrip || showAddVehicle ||
        editingFuel != null || editingExpense != null || editingMaintenance != null || editingTrip != null || editingVehicle != null
    val atRoot = tab == 0 && moreDestination == null && !anyDialogOpen
    BackHandler(enabled = !atRoot) {
        when {
            anyDialogOpen -> {
                showAddFuel = false; showAddExpense = false; showAddMaintenance = false; showAddTrip = false; showAddVehicle = false
                editingFuel = null; editingExpense = null; editingMaintenance = null; editingTrip = null; editingVehicle = null
            }
            moreDestination != null -> moreDestination = null
            else -> tab = 0
        }
    }

    CompositionLocalProvider(LocalDisplaySettings provides displaySettings) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(proTabTitle(tab, moreDestination), fontWeight = FontWeight.SemiBold) },
                    // Was previously hardcoded to WindowInsets(0, 0, 0, 0) on the assumption that
                    // MainActivity's setDecorFitsSystemWindows(window, true) always keeps the
                    // decor non-edge-to-edge, making TopAppBar's own status-bar inset reservation
                    // redundant. That assumption breaks on Android 15+ (targetSdk 37): apps
                    // targeting SDK 35+ are edge-to-edge by default and setDecorFitsSystemWindows
                    // no longer opts out, so the decor DOES extend under the status bar there —
                    // with insets zeroed, the header rendered with no top padding and overlapped
                    // the phone's own status bar/clock. Leaving windowInsets at its default lets
                    // TopAppBar ask WindowInsets.statusBars for the real value each time, which is
                    // 0 on pre-15 devices (matching the old behavior) and the actual bar height on
                    // 15+ (fixing the overlap) — self-adjusting instead of hardcoded either way.
                    navigationIcon = {
                        if (moreDestination != null) {
                            IconButton(onClick = { moreDestination = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                            }
                        }
                    },
                    actions = {
                        // Same vehicle switcher the default shell's TopAppBar has (FuelLogApp.kt)
                        // — tap the vehicle name to swap which vehicle's data the whole app is
                        // showing. Placed on the right (mockup puts secondary header controls
                        // there, e.g. the avatar/theme-toggle circle) instead of stacked under
                        // the page title, so it doesn't compete with it for space.
                        if (state.vehicles.size > 1) {
                            Box {
                                Row(
                                    modifier = Modifier.clickable { showVehicleMenu = true }.padding(end = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (!state.selectedVehicle?.brandLogoUrl.isNullOrBlank()) {
                                        SubcomposeAsyncImage(
                                            model = state.selectedVehicle?.brandLogoUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp).clip(CircleShape).background(Color.White).padding(2.dp),
                                            contentScale = ContentScale.Fit,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(
                                        state.selectedVehicle?.name ?: stringResource(R.string.label_select_vehicle),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    Text(" ▾", style = MaterialTheme.typography.labelMedium)
                                }
                                DropdownMenu(expanded = showVehicleMenu, onDismissRequest = { showVehicleMenu = false }) {
                                    state.vehicles.forEach { vehicle ->
                                        DropdownMenuItem(
                                            text = { Text(vehicle.name) },
                                            leadingIcon = if (!vehicle.brandLogoUrl.isNullOrBlank()) {
                                                {
                                                    SubcomposeAsyncImage(
                                                        model = vehicle.brandLogoUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.White).padding(2.dp),
                                                        contentScale = ContentScale.Fit,
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
                            state.selectedVehicle?.let { vehicle ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                                    if (!vehicle.brandLogoUrl.isNullOrBlank()) {
                                        SubcomposeAsyncImage(
                                            model = vehicle.brandLogoUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp).clip(CircleShape).background(Color.White).padding(2.dp),
                                            contentScale = ContentScale.Fit,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(vehicle.name, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    },
                )
            },
            bottomBar = {
                if (moreDestination == null) {
                    NavigationBar {
                        val items = listOf(
                            Triple(ProTab.DASHBOARD, stringResource(R.string.nav_dashboard), Icons.Filled.Home),
                            Triple(ProTab.TIMELINE, stringResource(R.string.nav_timeline), Icons.Filled.History),
                            Triple(ProTab.STATS, stringResource(R.string.title_stats), Icons.Filled.BarChart),
                            Triple(ProTab.TRIPS, stringResource(R.string.title_trips), Icons.Filled.Route),
                            Triple(ProTab.MORE, stringResource(R.string.nav_more), Icons.Filled.GridView),
                        )
                        items.forEachIndexed { index, (_, label, icon) ->
                            // Mockup nav items just recolor icon+label on selection (t.amber vs
                            // t.textMuted) — no pill/indicator behind the selected item. M3's
                            // NavigationBarItem defaults to a secondaryContainer indicator pill
                            // and a secondary-tinted icon, so both are overridden explicitly.
                            NavigationBarItem(
                                selected = tab == index,
                                onClick = { tab = index },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = Color.Transparent,
                                ),
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                // Primary (electric blue) marks the FAB — M3's FloatingActionButton default
                // washed-out primaryContainer/onPrimaryContainer pair is overridden to the fully
                // saturated primary + white icon.
                val fabColors = @Composable {
                    Pair(MaterialTheme.colorScheme.primary, Color.White)
                }
                when {
                    moreDestination == ProMoreDestination.EXPENSES -> {
                        val (bg, fg) = fabColors()
                        FloatingActionButton(
                            onClick = { if (state.vehicles.isEmpty()) showAddVehicle = true else showAddExpense = true },
                            containerColor = bg,
                            contentColor = fg,
                        ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.fab_add_expense)) }
                    }
                    moreDestination == ProMoreDestination.MAINTENANCE -> {
                        val (bg, fg) = fabColors()
                        FloatingActionButton(
                            onClick = { if (state.vehicles.isEmpty()) showAddVehicle = true else showAddMaintenance = true },
                            containerColor = bg,
                            contentColor = fg,
                        ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.fab_add_maintenance)) }
                    }
                    moreDestination == ProMoreDestination.VEHICLES -> {
                        val (bg, fg) = fabColors()
                        FloatingActionButton(
                            onClick = { showAddVehicle = true },
                            containerColor = bg,
                            contentColor = fg,
                        ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.title_my_vehicles)) }
                    }
                    moreDestination == null && tab == 0 -> {
                        val (bg, fg) = fabColors()
                        Box {
                            FloatingActionButton(
                                onClick = { if (state.vehicles.isEmpty()) showAddVehicle = true else dashboardFabExpanded = true },
                                containerColor = bg,
                                contentColor = fg,
                            ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.fab_add_fuel)) }
                            DropdownMenu(expanded = dashboardFabExpanded, onDismissRequest = { dashboardFabExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.fab_add_fuel)) },
                                    leadingIcon = { Icon(Icons.Filled.LocalGasStation, contentDescription = null) },
                                    onClick = { dashboardFabExpanded = false; showAddFuel = true },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.fab_add_expense)) },
                                    leadingIcon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) },
                                    onClick = { dashboardFabExpanded = false; showAddExpense = true },
                                )
                            }
                        }
                    }
                }
            },
        ) { padding ->
            val contentModifier = Modifier.padding(padding)
            if (moreDestination != null) {
                when (moreDestination) {
                    ProMoreDestination.EXPENSES -> ExpenseList(
                        expenses = state.expenses,
                        totalExpense = state.totalExpenses,
                        totalIncome = state.totalIncome,
                        netExpense = state.netExpense,
                        onDelete = { id -> requestDelete { onDeleteExpense(id) } },
                        onEdit = { editingExpense = it },
                        modifier = contentModifier,
                    )
                    ProMoreDestination.MAINTENANCE -> MaintenanceList(
                        tasks = state.maintenanceTasks,
                        currentOdometerKm = state.summary.latestOdometerKm,
                        onComplete = onCompleteMaintenance,
                        onDelete = { id -> requestDelete { onDeleteMaintenance(id) } },
                        onEdit = { editingMaintenance = it },
                        modifier = contentModifier,
                    )
                    ProMoreDestination.VEHICLES -> VehiclesListScreen(
                        vehicles = state.vehicles,
                        selectedVehicleId = state.selectedVehicle?.id,
                        onSelect = onSelectVehicle,
                        onEdit = { editingVehicle = it },
                        onDelete = { id -> requestDelete { onDeleteVehicle(id) } },
                        modifier = contentModifier,
                    )
                    ProMoreDestination.CALCULATOR -> TripCalculatorScreen(state, contentModifier)
                    ProMoreDestination.MAP -> NearbyStationsMapScreen(onFindNearbyStations, oilPriceInfo, contentModifier)
                    ProMoreDestination.SETTINGS -> SettingsScreen(
                        onDismiss = { moreDestination = null },
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
                        vehicles = state.vehicles,
                    )
                    null -> Unit
                }
            } else {
                when (ProTab.entries[tab]) {
                    ProTab.DASHBOARD -> ProDashboardScreen(
                        state = state,
                        oilPriceInfo = oilPriceInfo,
                        modifier = contentModifier,
                        onImageClick = { images, index -> viewingImages = images; viewingImageIndex = index },
                    )
                    ProTab.TIMELINE -> TimelineScreen(
                        state = state,
                        modifier = contentModifier,
                        onImageClick = { images, index -> viewingImages = images; viewingImageIndex = index },
                        onFuelRecordClick = { editingFuel = it },
                    )
                    ProTab.STATS -> StatsScreen(state, contentModifier)
                    ProTab.TRIPS -> TripList(
                        trips = state.trips,
                        summary = state.tripSummary,
                        onDelete = { id -> requestDelete { onDeleteTrip(id) } },
                        onEdit = { editingTrip = it },
                        onStartRecording = onStartTripRecording,
                        onSaveRecordedTrip = { distanceKm ->
                            prefillTripDistanceKm = distanceKm
                            showAddTrip = true
                            com.songsit.fuellogpro.trip.TripRecordingState.reset()
                            com.songsit.fuellogpro.trip.TripRecordingPreferences(context).clear()
                        },
                        modifier = contentModifier,
                    )
                    ProTab.MORE -> ProMoreScreen(onSelect = { moreDestination = it }, modifier = contentModifier)
                }
            }
        }

        if (showAddFuel || editingFuel != null) {
            AddFuelScreen(
                saving = state.saving,
                latestOdometer = state.summary.latestOdometerKm,
                editing = editingFuel,
                vehicleFuelType = state.selectedVehicle?.fuelType ?: "",
                vehicleTankCapacity = state.selectedVehicle?.tankCapacity,
                onFindNearbyStations = onFindNearbyStations,
                onFetchWeather = onFetchWeather,
                onPickPhoto = onPickPhoto,
                onPickCameraPhoto = onPickCameraPhoto,
                initialPhotoPaths = pendingPhotoPathsForForm,
                onScanExistingPhoto = onScanExistingPhoto,
                onDismiss = { showAddFuel = false; editingFuel = null; pendingPhotoPathsForForm = emptyList() },
                onSave = onAddFuel,
                onUpdate = onUpdateFuel,
            )
        }
        if (showAddExpense || editingExpense != null) {
            AddExpenseScreen(
                saving = state.saving,
                latestOdometer = state.summary.latestOdometerKm,
                editing = editingExpense,
                selectedVehicleLabel = state.selectedVehicle?.name,
                selectedVehicleOdometer = state.summary.latestOdometerKm,
                onPickPhoto = onPickPhoto,
                onPickCameraPhoto = onPickCameraPhoto,
                onPickPdf = onPickPdf,
                initialPhotoPaths = pendingPhotoPathsForForm,
                onScanExistingPhoto = onScanExistingPhoto,
                onDismiss = { showAddExpense = false; editingExpense = null; pendingPhotoPathsForForm = emptyList() },
                onSave = onAddExpense,
                onUpdate = onUpdateExpense,
            )
        }
        if (sharedPhotoPaths.isNotEmpty()) {
            // Fuel/expense entries save against whichever vehicle is currently selected (there's
            // no per-entry vehicle picker in AddFuelScreen/AddExpenseScreen) — with more than one
            // vehicle, asking type-only would silently file the receipt under whatever vehicle
            // happened to be active, which is exactly the "wrong car" mistake this dialog exists
            // to prevent. Defaults to the vehicle already selected; switching here calls the same
            // onSelectVehicle the header's vehicle switcher uses.
            var chooserVehicleId by remember(sharedPhotoPaths) { mutableStateOf(state.selectedVehicle?.id) }
            AlertDialog(
                onDismissRequest = onSharedPhotoConsumed,
                title = { Text(stringResource(R.string.shared_photo_chooser_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.shared_photo_chooser_message))
                        if (state.vehicles.size > 1) {
                            Spacer(Modifier.height(14.dp))
                            Text(
                                stringResource(R.string.shared_photo_choose_vehicle),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            state.vehicles.forEach { vehicle ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { chooserVehicleId = vehicle.id },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(selected = chooserVehicleId == vehicle.id, onClick = { chooserVehicleId = vehicle.id })
                                    Text(vehicle.name)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        chooserVehicleId?.let { if (it != state.selectedVehicle?.id) onSelectVehicle(it) }
                        pendingPhotoPathsForForm = sharedPhotoPaths
                        showAddFuel = true
                        onSharedPhotoConsumed()
                    }) { Text(stringResource(R.string.fab_add_fuel)) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        chooserVehicleId?.let { if (it != state.selectedVehicle?.id) onSelectVehicle(it) }
                        pendingPhotoPathsForForm = sharedPhotoPaths
                        showAddExpense = true
                        onSharedPhotoConsumed()
                    }) { Text(stringResource(R.string.fab_add_expense)) }
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
        if (showAddVehicle || editingVehicle != null) {
            VehicleEditScreen(
                saving = state.saving,
                editing = editingVehicle,
                onPickPhoto = onPickPhoto,
                onPickCameraPhoto = onPickCameraPhoto,
                onDismiss = { showAddVehicle = false; editingVehicle = null },
                onSave = onAddVehicle,
                onUpdate = onUpdateVehicle,
            )
        }
        if (viewingImages.isNotEmpty()) {
            FullScreenImageViewer(
                imagePaths = viewingImages,
                initialIndex = viewingImageIndex,
                onDismiss = { viewingImages = emptyList() },
            )
        }
        pendingDeleteAction?.let { action ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { pendingDeleteAction = null },
                title = { Text(stringResource(R.string.confirm_delete_title)) },
                text = { Text(stringResource(R.string.confirm_delete_message)) },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { action(); pendingDeleteAction = null }) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { pendingDeleteAction = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun proTabTitle(tab: Int, moreDestination: ProMoreDestination?): String = when {
    moreDestination == ProMoreDestination.EXPENSES -> stringResource(R.string.more_hub_expenses)
    moreDestination == ProMoreDestination.MAINTENANCE -> stringResource(R.string.more_hub_maintenance)
    moreDestination == ProMoreDestination.VEHICLES -> stringResource(R.string.more_hub_vehicles)
    moreDestination == ProMoreDestination.CALCULATOR -> stringResource(R.string.nav_calculator)
    moreDestination == ProMoreDestination.MAP -> stringResource(R.string.more_hub_map)
    moreDestination == ProMoreDestination.SETTINGS -> stringResource(R.string.more_hub_settings)
    else -> when (ProTab.entries[tab]) {
        ProTab.DASHBOARD -> stringResource(R.string.nav_dashboard)
        ProTab.TIMELINE -> stringResource(R.string.nav_timeline)
        ProTab.STATS -> stringResource(R.string.title_stats)
        ProTab.TRIPS -> stringResource(R.string.title_trips)
        ProTab.MORE -> stringResource(R.string.nav_more)
    }
}
