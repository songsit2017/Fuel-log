package com.songsit.fuellogpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.songsit.fuellogpro.settings.DisplaySettings
import com.songsit.fuellogpro.data.NearbyStation
import com.songsit.fuellogpro.data.OilPriceInfo
import com.songsit.fuellogpro.data.firebase.VehicleMember
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songsit.fuellogpro.domain.model.FuelEntry
import com.songsit.fuellogpro.domain.model.Expense
import com.songsit.fuellogpro.domain.model.Vehicle
import com.songsit.fuellogpro.domain.DueLevel
import com.songsit.fuellogpro.domain.calculateMaintenanceStatus
import com.songsit.fuellogpro.domain.model.MaintenanceTask
import com.songsit.fuellogpro.domain.model.Trip
import com.songsit.fuellogpro.domain.model.totalCost
import com.songsit.fuellogpro.domain.calculatePerEntryKmPerLiter
import com.songsit.fuellogpro.data.local.SyncConflictEntity
import com.songsit.fuellogpro.data.local.PhotoUris
import com.songsit.fuellogpro.notifications.ReminderSettings
import com.songsit.fuellogpro.ui.stats.StatsScreen
import com.songsit.fuellogpro.ui.timeline.TimelineScreen
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap

private val fuelTypeOptions = listOf(
    "แก๊สโซฮอล์ 91", "แก๊สโซฮอล์ 95", "แก๊สโซฮอล์ E20", "แก๊สโซฮอล์ E85", "ดีเซล B7", "ดีเซล B20", "เบนซิน",
)

private val thaiCurrency = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
private val number = NumberFormat.getNumberInstance(Locale("th", "TH")).apply {
    maximumFractionDigits = 2
}

@Composable
private fun AutocompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fieldWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                onExpandedChange(true)
            },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { fieldWidthPx = it.size.width },
        )
        DropdownMenu(
            expanded = expanded && options.isNotEmpty(),
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.width(with(density) { fieldWidthPx.toDp() }),
        ) {
            options.take(20).forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
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
    onAddFuel: (String, String, Double, Double, Double, Boolean, String, String?, () -> Unit) -> Unit,
    onUpdateFuel: (String, String, String, Double, Double, Double, Boolean, String, String?, () -> Unit) -> Unit,
    onDeleteFuel: (String) -> Unit,
    onAddExpense: (String, String, String, Double, Double?, Boolean, Boolean, String?, String?, () -> Unit) -> Unit,
    onUpdateExpense: (String, String, String, String, Double, Double?, Boolean, Boolean, String?, String?, () -> Unit) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onAddMaintenance: (String, String, String?, Double?, Int, Double, Int?, Double?, () -> Unit) -> Unit,
    onUpdateMaintenance: (String, String, String, String?, Double?, Int, Double, Int?, Double?, () -> Unit) -> Unit,
    onCompleteMaintenance: (String) -> Unit,
    onDeleteMaintenance: (String) -> Unit,
    onAddTrip: (String, String, Double, Double, Double, Double, Double, Double, () -> Unit) -> Unit,
    onUpdateTrip: (String, String, String, Double, Double, Double, Double, Double, Double, () -> Unit) -> Unit,
    onDeleteTrip: (String) -> Unit,
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
    onSelectVehicle: (String) -> Unit,
    onAddVehicle: (String, String, String, () -> Unit) -> Unit,
    onDeleteVehicle: (String) -> Unit,
    onClearError: () -> Unit,
    onFindNearbyStations: ((onResult: (List<NearbyStation>) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onPickPhoto: ((onPicked: (uris: List<String>, extractedAmount: Double?) -> Unit) -> Unit)? = null,
    onPickCameraPhoto: ((onPicked: (uris: List<String>, extractedAmount: Double?) -> Unit) -> Unit)? = null,
    oilPriceInfo: OilPriceInfo? = null,
    vehicleMembers: List<VehicleMember> = emptyList(),
    onCreateInvite: ((email: String, role: String, onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onJoinByCode: ((code: String, onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    displaySettings: DisplaySettings = DisplaySettings(),
    onDisplaySettingsChange: (DisplaySettings) -> Unit = {},
) {
    var tab by remember { mutableIntStateOf(0) }
    var showAddFuel by remember { mutableStateOf(false) }
    var showAddVehicle by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }
    var showAddMaintenance by remember { mutableStateOf(false) }
    var showAddTrip by remember { mutableStateOf(false) }
    var editingFuel by remember { mutableStateOf<FuelEntry?>(null) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var editingMaintenance by remember { mutableStateOf<MaintenanceTask?>(null) }
    var editingTrip by remember { mutableStateOf<Trip?>(null) }
    var showVehicleMenu by remember { mutableStateOf(false) }
    var recordsMode by remember { mutableIntStateOf(0) }
    val titles = listOf("ภาพรวม", "การเติมน้ำมัน", "บันทึก", "บำรุงรักษา", "รถของฉัน", "สถิติ", "ไทม์ไลน์")

    CompositionLocalProvider(LocalDisplaySettings provides displaySettings) {
    FuelLogTheme(themeMode = displaySettings.themeMode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(titles[tab], fontWeight = FontWeight.SemiBold)
                            if (state.vehicles.size > 1) {
                                Box {
                                    Row(
                                        modifier = Modifier.clickable { showVehicleMenu = true },
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            state.selectedVehicle?.name ?: "เลือกรถ",
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
                                    Text(it.name, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    val tabIcons = listOf(
                        Icons.Filled.Home,
                        Icons.Filled.LocalGasStation,
                        Icons.Filled.ReceiptLong,
                        Icons.Filled.Build,
                        Icons.Filled.DirectionsCar,
                        Icons.Filled.BarChart,
                        Icons.Filled.History,
                    )
                    listOf("หน้าหลัก", "น้ำมัน", "บันทึก", "ดูแลรถ", "รถ", "สถิติ", "ไทม์ไลน์").forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Icon(tabIcons[index], contentDescription = label) },
                            label = { Text(label) },
                        )
                    }
                }
            },
            floatingActionButton = {
                when (tab) {
                    0, 1 -> FloatingActionButton(
                        onClick = { if (state.vehicles.isEmpty()) showAddVehicle = true else showAddFuel = true },
                    ) { Text("+") }
                    2 -> FloatingActionButton(
                        onClick = {
                            if (state.vehicles.isEmpty()) showAddVehicle = true
                            else if (recordsMode == 0) showAddExpense = true
                            else showAddTrip = true
                        },
                    ) { Text("+") }
                    3 -> FloatingActionButton(
                        onClick = { if (state.vehicles.isEmpty()) showAddVehicle = true else showAddMaintenance = true },
                    ) { Text("+") }
                    4 -> FloatingActionButton(onClick = { showAddVehicle = true }) { Text("+") }
                }
            },
        ) { padding ->
            when (tab) {
                0 -> Dashboard(state, onExportCsv, oilPriceInfo, Modifier.padding(padding))
                1 -> FuelList(state.entries, onDeleteFuel, { editingFuel = it }, Modifier.padding(padding))
                2 -> RecordsPage(
                    mode = recordsMode,
                    onModeChange = { recordsMode = it },
                    state = state,
                    onDeleteExpense = onDeleteExpense,
                    onEditExpense = { editingExpense = it },
                    onDeleteTrip = onDeleteTrip,
                    onEditTrip = { editingTrip = it },
                    modifier = Modifier.padding(padding),
                )
                3 -> MaintenanceList(
                    tasks = state.maintenanceTasks,
                    currentOdometerKm = state.summary.latestOdometerKm,
                    onComplete = onCompleteMaintenance,
                    onDelete = onDeleteMaintenance,
                    onEdit = { editingMaintenance = it },
                    modifier = Modifier.padding(padding),
                )
                4 -> VehicleList(
                    vehicles = state.vehicles,
                    selectedVehicleId = state.selectedVehicle?.id,
                    onSelect = onSelectVehicle,
                    onDelete = onDeleteVehicle,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    cloudState = cloudState,
                    onGoogleSignIn = onGoogleSignIn,
                    onCloudSync = onCloudSync,
                    onSignOut = onSignOut,
                    syncConflicts = syncConflicts,
                    onResolveConflict = onResolveConflict,
                    reminderSettings = reminderSettings,
                    onReminderSettingsChange = onReminderSettingsChange,
                    vehicleMembers = vehicleMembers,
                    onCreateInvite = onCreateInvite,
                    onJoinByCode = onJoinByCode,
                    displaySettings = displaySettings,
                    onDisplaySettingsChange = onDisplaySettingsChange,
                    modifier = Modifier.padding(padding),
                )
                5 -> StatsScreen(state, Modifier.padding(padding))
                else -> TimelineScreen(state, Modifier.padding(padding))
            }
        }
        if (showAddFuel || editingFuel != null) {
            AddFuelDialog(
                saving = state.saving,
                latestOdometer = state.summary.latestOdometerKm,
                editing = editingFuel,
                stationSuggestions = state.stationSuggestions,
                onFindNearbyStations = onFindNearbyStations,
                onPickPhoto = onPickPhoto,
                onPickCameraPhoto = onPickCameraPhoto,
                onDismiss = { showAddFuel = false; editingFuel = null },
                onSave = onAddFuel,
                onUpdate = onUpdateFuel,
            )
        }
        if (showAddVehicle) {
            AddVehicleDialog(
                saving = state.saving,
                onDismiss = { showAddVehicle = false },
                onSave = onAddVehicle,
            )
        }
        if (showAddExpense || editingExpense != null) {
            AddExpenseDialog(
                saving = state.saving,
                latestOdometer = state.summary.latestOdometerKm,
                editing = editingExpense,
                categorySuggestions = state.expenseCategorySuggestions,
                onPickPhoto = onPickPhoto,
                onPickCameraPhoto = onPickCameraPhoto,
                onDismiss = { showAddExpense = false; editingExpense = null },
                onSave = onAddExpense,
                onUpdate = onUpdateExpense,
            )
        }
        if (showAddMaintenance || editingMaintenance != null) {
            AddMaintenanceDialog(
                saving = state.saving,
                latestOdometer = state.summary.latestOdometerKm,
                editing = editingMaintenance,
                categorySuggestions = state.maintenanceCategorySuggestions,
                onDismiss = { showAddMaintenance = false; editingMaintenance = null },
                onSave = onAddMaintenance,
                onUpdate = onUpdateMaintenance,
            )
        }
        if (showAddTrip || editingTrip != null) {
            AddTripDialog(
                saving = state.saving,
                editing = editingTrip,
                onDismiss = { showAddTrip = false; editingTrip = null },
                onSave = onAddTrip,
                onUpdate = onUpdateTrip,
            )
        }
        state.errorMessage?.let { message ->
            AlertDialog(
                onDismissRequest = onClearError,
                confirmButton = { TextButton(onClick = onClearError) { Text("ตกลง") } },
                title = { Text("ตรวจสอบข้อมูล") },
                text = { Text(message) },
            )
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.selectedVehicle == null) {
            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp)) {
                        Text("เริ่มต้นด้วยรถของคุณ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("แตะ + เพื่อเพิ่มรถ แล้วจึงบันทึกการเติมน้ำมัน")
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("อัตราสิ้นเปลืองเฉลี่ย", color = MaterialTheme.colorScheme.onPrimary)
                    Text(
                        state.summary.averageKmPerLiter?.let { formatEconomyKmPerLiter(it, displaySettings) } ?: "—",
                        color = MaterialTheme.colorScheme.onPrimary,
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
                MetricCard("ค่าน้ำมัน", formatCurrencyAmount(state.summary.totalSpent, displaySettings), Modifier.weight(1f), Icons.Filled.LocalGasStation)
                MetricCard("ปริมาณน้ำมัน", formatVolumeLiters(state.summary.totalLiters, displaySettings), Modifier.weight(1f), Icons.Filled.WaterDrop)
            }
        }
        item {
            MetricCard(
                "เลขไมล์ล่าสุด",
                state.summary.latestOdometerKm?.let { formatDistanceKm(it, displaySettings) } ?: "ยังไม่มีข้อมูล",
                Modifier.fillMaxWidth(),
                Icons.Filled.Speed,
            )
        }
        item {
            val operatingCost = state.summary.totalSpent + state.totalExpenses + state.tripSummary.totalCost
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SectionHeader(Icons.Filled.BarChart, "รายงานสรุป")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            "ค่าใช้จ่ายรวมทั้งหมด",
                            formatCurrencyAmount(operatingCost, displaySettings),
                            Modifier.weight(1f),
                            Icons.Filled.Payments,
                        )
                        MetricCard(
                            "สุทธิหลังรายรับ",
                            formatCurrencyAmount(operatingCost - state.totalIncome, displaySettings),
                            Modifier.weight(1f),
                            Icons.Filled.AccountBalanceWallet,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            "ระยะทางทริป",
                            formatDistanceKm(state.tripSummary.totalDistanceKm, displaySettings),
                            Modifier.weight(1f),
                            Icons.Filled.Route,
                        )
                        MetricCard(
                            "จำนวนรายการ",
                            number.format(
                                state.entries.size + state.expenses.size +
                                    state.maintenanceTasks.size + state.trips.size,
                            ),
                            Modifier.weight(1f),
                            Icons.Filled.ListAlt,
                        )
                    }
                    Button(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) {
                        Text("ส่งออกรายงาน CSV")
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader(Icons.Filled.LocalGasStation, "รายการล่าสุด")
        }
        if (state.entries.isEmpty()) {
            item { EmptyFuelState() }
        } else {
            items(state.entries.take(3), key = { it.id }) { FuelRow(it, kmPerLiter = kmPerLiterByEntry[it.id]) }
        }
    }
}

@Composable
private fun OilPriceCard(info: OilPriceInfo) {
    var tab by remember { mutableIntStateOf(0) }
    val selectedTab = tab.coerceIn(0, info.brands.lastIndex)
    val selected = info.brands[selectedTab]
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth()) {
            TabRow(selectedTabIndex = selectedTab) {
                info.brands.forEachIndexed { index, brand ->
                    Tab(selected = index == selectedTab, onClick = { tab = index }, text = { Text(brand.brand) })
                }
            }
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ราคาน้ำมันวันนี้", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OilPriceRow("แก๊สโซฮอล์ 95", selected.gasohol95)
                OilPriceRow("แก๊สโซฮอล์ 91", selected.gasohol91)
                OilPriceRow("ดีเซล B7", selected.dieselB7)
                if (info.dateLabel.isNotBlank()) {
                    Text(
                        info.dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun OilPriceRow(label: String, price: Double?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            price?.let { thaiCurrency.format(it) } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
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

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconBadge(icon, size = 26.dp)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Card(modifier, shape = RoundedCornerShape(18.dp)) {
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

private val fuelListThaiMonthNames = listOf(
    "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
    "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม",
)

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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (entries.isEmpty()) item { EmptyFuelState() }
        groups.forEach { (yearMonth, groupEntries) ->
            item {
                val label = yearMonth?.let { (year, month) -> "${fuelListThaiMonthNames[month - 1]} $year" } ?: "ไม่ทราบวันที่"
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("สุทธิ", style = MaterialTheme.typography.labelLarge)
                    Text(thaiCurrency.format(netExpense), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "รายจ่าย ${thaiCurrency.format(totalExpense)} • รายรับ ${thaiCurrency.format(totalIncome)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (expenses.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ยังไม่มีค่าใช้จ่าย", fontWeight = FontWeight.SemiBold)
                        Text("แตะ + เพื่อเพิ่มรายการแรก", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        items(expenses, key = { it.id }) { expense ->
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = if (onEdit != null) Modifier.clickable { onEdit(expense) } else Modifier,
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(expense.category, fontWeight = FontWeight.Bold)
                        Text(
                            listOf(expense.date, expense.description).filter(String::isNotBlank).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${if (expense.income) "+" else "−"}${thaiCurrency.format(expense.amount)}",
                            fontWeight = FontWeight.Bold,
                            color = if (expense.income) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                        )
                        if (expense.recurring) Text("รายการประจำ", style = MaterialTheme.typography.labelSmall)
                        expense.reminderDate?.let { Text("เตือน $it", style = MaterialTheme.typography.labelSmall) }
                    }
                    TextButton(onClick = { onDelete(expense.id) }) { Text("ลบ") }
                }
            }
        }
    }
}

@Composable
private fun RecordsPage(
    mode: Int,
    onModeChange: (Int) -> Unit,
    state: NativeAppState,
    onDeleteExpense: (String) -> Unit,
    onEditExpense: ((Expense) -> Unit)? = null,
    onDeleteTrip: (String) -> Unit,
    onEditTrip: ((Trip) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (mode == 0) Button(onClick = { onModeChange(0) }) { Text("ค่าใช้จ่าย") }
            else TextButton(onClick = { onModeChange(0) }) { Text("ค่าใช้จ่าย") }
            if (mode == 1) Button(onClick = { onModeChange(1) }) { Text("ทริป") }
            else TextButton(onClick = { onModeChange(1) }) { Text("ทริป") }
        }
        if (mode == 0) {
            ExpenseList(
                expenses = state.expenses,
                totalExpense = state.totalExpenses,
                totalIncome = state.totalIncome,
                netExpense = state.netExpense,
                onDelete = onDeleteExpense,
                onEdit = onEditExpense,
                modifier = Modifier.weight(1f),
            )
        } else {
            TripList(
                trips = state.trips,
                summary = state.tripSummary,
                onDelete = onDeleteTrip,
                onEdit = onEditTrip,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TripList(
    trips: List<Trip>,
    summary: com.songsit.fuellogpro.domain.TripSummary,
    onDelete: (String) -> Unit,
    onEdit: ((Trip) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("${summary.tripCount} ทริป • ${number.format(summary.totalDistanceKm)} กม.")
                    Text(
                        thaiCurrency.format(summary.totalCost),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    summary.costPerKm?.let {
                        Text("${number.format(it)} บาท/กม.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (trips.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ยังไม่มีทริป", fontWeight = FontWeight.Bold)
                        Text("แตะ + เพื่อบันทึกการเดินทาง")
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
                        Text("${trip.date} • ${number.format(trip.distanceKm)} กม.", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(thaiCurrency.format(trip.totalCost), fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onDelete(trip.id) }) { Text("ลบ") }
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (sortedTasks.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ยังไม่มีรายการดูแลรถ", fontWeight = FontWeight.Bold)
                        Text("แตะ + เพื่อเพิ่มภาษี ประกัน หรืองานบำรุงรักษา")
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
                        Text(status.label, fontWeight = FontWeight.SemiBold)
                    }
                    val due = listOfNotNull(
                        task.nextDate?.let { "วันที่ $it" },
                        task.nextOdometerKm?.let { "ที่ ${number.format(it)} กม." },
                    ).joinToString(" • ")
                    if (due.isNotBlank()) Text(due, style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onDelete(task.id) }) { Text("ลบ") }
                        Button(onClick = { onComplete(task.id) }) { Text("เสร็จแล้ว") }
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
                IconBadge(Icons.Filled.LocalGasStation)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.station.ifBlank { "เติมน้ำมัน" }, fontWeight = FontWeight.SemiBold)
                        if (!entry.photoUri.isNullOrBlank()) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.Image,
                                contentDescription = "มีรูปแนบ",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text("${entry.date} • ${number.format(entry.odometerKm)} กม.", style = MaterialTheme.typography.bodySmall)
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
                    Text("${number.format(entry.liters)} ลิตร • ${number.format(entry.pricePerLiter)}/ลิตร")
                    kmPerLiter?.let { Text("${number.format(it)} กม./ลิตร", style = MaterialTheme.typography.labelSmall) }
                }
                if (entry.fullTank) Text("เต็มถัง", color = MaterialTheme.colorScheme.tertiary)
                onDelete?.let { TextButton(onClick = { it(entry.id) }) { Text("ลบ") } }
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
            Text("การตั้งค่าการแสดงผล", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Text("สกุลเงิน", style = MaterialTheme.typography.labelLarge)
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

            Text("จำนวนทศนิยม", style = MaterialTheme.typography.labelLarge)
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
            Text("ระยะทาง", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("km" to "กิโลเมตร (กม.)", "mi" to "ไมล์ (mi)").forEach { (value, label) ->
                    if (settings.distanceUnit == value) {
                        Button(onClick = { onChange(settings.copy(distanceUnit = value)) }) { Text(label) }
                    } else {
                        TextButton(onClick = { onChange(settings.copy(distanceUnit = value)) }) { Text(label) }
                    }
                }
            }

            Text("ปริมาตรน้ำมัน", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("liters" to "ลิตร (L)", "gal" to "แกลลอน (US gal)").forEach { (value, label) ->
                    if (settings.volumeUnit == value) {
                        Button(onClick = { onChange(settings.copy(volumeUnit = value)) }) { Text(label) }
                    } else {
                        TextButton(onClick = { onChange(settings.copy(volumeUnit = value)) }) { Text(label) }
                    }
                }
            }
            Text(
                "ข้อมูลยังเก็บเป็นกิโลเมตร/ลิตรเสมอ แค่แสดงผลตามหน่วยที่เลือก เปลี่ยนได้ตลอดเวลาไม่กระทบข้อมูลเดิม",
                style = MaterialTheme.typography.bodySmall,
            )

            HorizontalDivider()
            Text("ธีม", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("light" to "สว่าง", "dark" to "มืด", "system" to "ตามระบบ").forEach { (value, label) ->
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
private fun VehicleList(
    vehicles: List<Vehicle>,
    selectedVehicleId: String?,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    cloudState: CloudUiState,
    onGoogleSignIn: () -> Unit,
    onCloudSync: () -> Unit,
    onSignOut: () -> Unit,
    syncConflicts: List<SyncConflictEntity>,
    onResolveConflict: (String, Boolean) -> Unit,
    reminderSettings: ReminderSettings,
    onReminderSettingsChange: (ReminderSettings) -> Unit,
    vehicleMembers: List<VehicleMember> = emptyList(),
    onCreateInvite: ((email: String, role: String, onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onJoinByCode: ((code: String, onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    displaySettings: DisplaySettings = DisplaySettings(),
    onDisplaySettingsChange: (DisplaySettings) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            DisplaySettingsCard(
                settings = displaySettings,
                onChange = onDisplaySettingsChange,
            )
        }
        if (cloudState.uid != null && selectedVehicleId != null && (onCreateInvite != null || onJoinByCode != null)) {
            item {
                VehicleSharingCard(
                    members = vehicleMembers,
                    onCreateInvite = onCreateInvite,
                    onJoinByCode = onJoinByCode,
                )
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("การแจ้งเตือน", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    NotificationSettingRow(
                        label = "ตามวันที่",
                        checked = reminderSettings.dateReminders,
                        onCheckedChange = {
                            onReminderSettingsChange(reminderSettings.copy(dateReminders = it))
                        },
                    )
                    NotificationSettingRow(
                        label = "ตามเลขไมล์",
                        checked = reminderSettings.odometerReminders,
                        onCheckedChange = {
                            onReminderSettingsChange(reminderSettings.copy(odometerReminders = it))
                        },
                    )
                    NotificationSettingRow(
                        label = "กำหนดชำระ",
                        checked = reminderSettings.paymentReminders,
                        onCheckedChange = {
                            onReminderSettingsChange(reminderSettings.copy(paymentReminders = it))
                        },
                    )
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cloud sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (cloudState.uid == null) {
                        Text("เข้าสู่ระบบเพื่อรวมข้อมูลกับ Firebase โดยไม่เขียนทับรายการที่ขัดแย้งกัน")
                        Button(onClick = onGoogleSignIn, enabled = !cloudState.syncing) {
                            Text(if (cloudState.syncing) "กำลังเข้าสู่ระบบ…" else "เข้าสู่ระบบด้วย Google")
                        }
                    } else {
                        Text(cloudState.email ?: "เชื่อมต่อบัญชี Google แล้ว")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onCloudSync, enabled = !cloudState.syncing) {
                                Text(if (cloudState.syncing) "กำลังซิงก์…" else "ซิงก์ตอนนี้")
                            }
                            TextButton(onClick = onSignOut, enabled = !cloudState.syncing) { Text("ออกจากระบบ") }
                        }
                    }
                    cloudState.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (syncConflicts.isNotEmpty()) {
                        Text(
                            "พบ ${syncConflicts.size} รายการที่ต่างกัน ระบบยังไม่เขียนทับทั้งสองฝั่ง",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        syncConflicts.take(5).forEach { conflict ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text(
                                        "${conflict.collectionName} • ${conflict.recordId.take(8)}",
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = { onResolveConflict(conflict.key, true) },
                                            enabled = !cloudState.syncing,
                                        ) { Text("ใช้ข้อมูลในเครื่อง") }
                                        TextButton(
                                            onClick = { onResolveConflict(conflict.key, false) },
                                            enabled = !cloudState.syncing,
                                        ) { Text("ใช้ข้อมูล Cloud") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ข้อมูลของฉัน", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "สำรองเป็น JSON หรือนำเข้ากลับแบบรวมข้อมูลโดยไม่ลบรายการเดิม",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onExportBackup) { Text("สำรองข้อมูล") }
                        TextButton(onClick = onImportBackup) { Text("นำเข้าข้อมูล") }
                    }
                }
            }
        }
        if (vehicles.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ยังไม่มีรถ", fontWeight = FontWeight.Bold)
                        Text("แตะ + เพื่อเพิ่มรถคันแรก")
                    }
                }
            }
        }
        items(vehicles, key = { it.id }) { vehicle ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(vehicle.id) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (vehicle.id == selectedVehicleId) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(vehicle.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val detail = listOf(vehicle.registration, vehicle.fuelType).filter(String::isNotBlank).joinToString(" • ")
                        if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall)
                    }
                    if (vehicle.id == selectedVehicleId) Text("กำลังใช้", color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { onDelete(vehicle.id) }) { Text("ลบ") }
                }
            }
        }
    }
}

@Composable
private fun VehicleSharingCard(
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

    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("แชร์รถกับสมาชิกครอบครัว", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (members.isNotEmpty()) {
                Text("สมาชิกปัจจุบัน", style = MaterialTheme.typography.labelLarge)
                members.forEach { member ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(member.displayName.ifBlank { member.email }, fontWeight = FontWeight.SemiBold)
                            if (member.email.isNotBlank()) Text(member.email, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(member.role, style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                Text("ยังไม่มีสมาชิกร่วมดูแลรถคันนี้", style = MaterialTheme.typography.bodySmall)
            }
            if (onCreateInvite != null) {
                HorizontalDivider()
                Text("สร้างคำเชิญ", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = inviteEmail,
                    onValueChange = { inviteEmail = it },
                    label = { Text("อีเมล Google ของสมาชิก") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("editor" to "แก้ไขได้", "viewer" to "ดูอย่างเดียว").forEach { (value, label) ->
                        if (inviteRole == value) Button(onClick = { inviteRole = value }) { Text(label) }
                        else TextButton(onClick = { inviteRole = value }) { Text(label) }
                    }
                }
                Button(
                    enabled = !inviteBusy && inviteEmail.isNotBlank(),
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
                ) { Text(if (inviteBusy) "กำลังสร้าง…" else "สร้างรหัสเชิญ") }
                inviteResult?.let { Text("รหัสเชิญ: $it (7 วัน)", fontWeight = FontWeight.Bold) }
                inviteError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            if (onJoinByCode != null) {
                HorizontalDivider()
                Text("เข้าร่วมด้วยรหัสเชิญ", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = joinCode,
                    onValueChange = { joinCode = it },
                    label = { Text("รหัสเชิญ 8 ตัว") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = !joinBusy && joinCode.isNotBlank(),
                    onClick = {
                        joinBusy = true
                        joinError = null
                        joinResult = null
                        onJoinByCode(
                            joinCode,
                            { vehicleName -> joinBusy = false; joinResult = "เข้าร่วม $vehicleName สำเร็จ"; joinCode = "" },
                            { message -> joinBusy = false; joinError = message },
                        )
                    },
                ) { Text(if (joinBusy) "กำลังตรวจสอบ…" else "เข้าร่วม") }
                joinResult?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                joinError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
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
            Text("ยังไม่มีรายการเติมน้ำมัน", fontWeight = FontWeight.SemiBold)
            Text("แตะ + เพื่อบันทึกรายการแรก", style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun AddVehicleDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, () -> Unit) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var registration by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf("เบนซิน") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("เพิ่มรถ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("ชื่อรถ") }, singleLine = true)
                OutlinedTextField(registration, { registration = it }, label = { Text("ทะเบียน (ไม่บังคับ)") }, singleLine = true)
                OutlinedTextField(fuelType, { fuelType = it }, label = { Text("ชนิดเชื้อเพลิง") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(enabled = !saving, onClick = { onSave(name, registration, fuelType, onDismiss) }) {
                Text(if (saving) "กำลังบันทึก…" else "บันทึก")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } },
    )
}

@Composable
private fun AddFuelDialog(
    saving: Boolean,
    latestOdometer: Double?,
    editing: FuelEntry? = null,
    stationSuggestions: List<String> = emptyList(),
    onFindNearbyStations: ((onResult: (List<NearbyStation>) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onPickPhoto: ((onPicked: (uris: List<String>, extractedAmount: Double?) -> Unit) -> Unit)? = null,
    onPickCameraPhoto: ((onPicked: (uris: List<String>, extractedAmount: Double?) -> Unit) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double, Double, Boolean, String, String?, () -> Unit) -> Unit,
    onUpdate: ((String, String, String, Double, Double, Double, Boolean, String, String?, () -> Unit) -> Unit)? = null,
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
    var fuelType by remember { mutableStateOf("") }
    var fuelTypeMenuExpanded by remember { mutableStateOf(false) }
    val onLitersChange: (String) -> Unit = { value ->
        liters = value
        val l = value.toDoubleOrNull()
        val p = price.toDoubleOrNull()
        if (l != null && p != null) total = "%.2f".format(Locale.US, l * p)
    }
    val onPriceChange: (String) -> Unit = { value ->
        price = value
        val l = liters.toDoubleOrNull()
        val p = value.toDoubleOrNull()
        if (l != null && p != null) total = "%.2f".format(Locale.US, l * p)
    }
    val onTotalChange: (String) -> Unit = { value ->
        total = value
        val l = liters.toDoubleOrNull()
        val t = value.toDoubleOrNull()
        if (l != null && l > 0 && t != null) price = "%.2f".format(Locale.US, t / l)
    }
    var station by remember { mutableStateOf(editing?.station ?: "") }
    var fullTank by remember { mutableStateOf(editing?.fullTank ?: true) }
    var stationMenuExpanded by remember { mutableStateOf(false) }
    var nearbyStations by remember { mutableStateOf<List<NearbyStation>>(emptyList()) }
    var nearbySearching by remember { mutableStateOf(false) }
    var nearbyError by remember { mutableStateOf<String?>(null) }
    var photoUris by remember { mutableStateOf(editing?.photoUrls ?: emptyList()) }
    val stationOptions = (nearbyStations.map { it.name } + stationSuggestions).distinct()
        .filter { station.isBlank() || it.contains(station, ignoreCase = true) }
    val runNearbySearch = {
        nearbySearching = true
        nearbyError = null
        onFindNearbyStations?.invoke(
            { results ->
                nearbySearching = false
                nearbyStations = results
                stationMenuExpanded = true
                if (results.isEmpty()) nearbyError = "ไม่พบปั๊มใกล้ฉัน"
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) "แก้ไขการเติมน้ำมัน" else "เพิ่มการเติมน้ำมัน") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    AutocompleteTextField(
                        value = station,
                        onValueChange = { station = it },
                        label = "สถานีบริการ",
                        options = stationOptions,
                        expanded = stationMenuExpanded,
                        onExpandedChange = { stationMenuExpanded = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (onFindNearbyStations != null) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                enabled = !nearbySearching,
                                onClick = { runNearbySearch() },
                            ) { Text(if (nearbySearching) "กำลังค้นหา…" else "หาปั๊มใกล้ฉัน") }
                            nearbyError?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                item {
                    FormRow(Icons.Filled.Speed) {
                        OutlinedTextField(
                            odometer,
                            { odometer = it },
                            label = { Text("มาตรวัดระยะทางรวม (km)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    FormRow(Icons.Filled.LocalGasStation) {
                        OutlinedTextField(
                            liters,
                            onLitersChange,
                            label = { Text("เชื้อเพลิง (L)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        AutocompleteTextField(
                            value = fuelType,
                            onValueChange = { fuelType = it },
                            label = "ชนิดเชื้อเพลิง",
                            options = fuelTypeOptions,
                            expanded = fuelTypeMenuExpanded,
                            onExpandedChange = { fuelTypeMenuExpanded = it },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    FormRow(Icons.Filled.AttachMoney) {
                        OutlinedTextField(
                            price,
                            onPriceChange,
                            label = { Text("ราคา/L") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            total,
                            onTotalChange,
                            label = { Text("ราคารวม") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    FormRow(Icons.Filled.CalendarToday) {
                        OutlinedTextField(
                            date,
                            { date = it },
                            label = { Text("วันที่") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            time,
                            { time = it },
                            label = { Text("เวลา") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (onPickPhoto != null) {
                    item {
                        PhotoAttachmentRow(
                            photoUris = photoUris,
                            onPickGallery = {
                                onPickPhoto { picked, _ ->
                                    photoUris = (photoUris + picked).distinct().take(MAX_PHOTOS)
                                }
                            },
                            onPickCamera = onPickCameraPhoto?.let { pick ->
                                {
                                    pick { picked, _ ->
                                        photoUris = (photoUris + picked).distinct().take(MAX_PHOTOS)
                                    }
                                }
                            },
                            onRemove = { uri -> photoUris = photoUris - uri },
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("เติมเต็มถัง")
                        Switch(checked = fullTank, onCheckedChange = { fullTank = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    val odometerValue = odometer.toDoubleOrNull() ?: 0.0
                    val litersValue = liters.toDoubleOrNull() ?: 0.0
                    val priceValue = price.toDoubleOrNull() ?: 0.0
                    val photoUri = PhotoUris.join(photoUris)
                    if (editing != null && onUpdate != null) {
                        onUpdate(editing.id, date, time, odometerValue, litersValue, priceValue, fullTank, station, photoUri, onDismiss)
                    } else {
                        onSave(date, time, odometerValue, litersValue, priceValue, fullTank, station, photoUri, onDismiss)
                    }
                },
            ) { Text(if (saving) "กำลังบันทึก…" else "บันทึก") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } },
    )
}

// Item C (multi-photo): shows up to MAX_PHOTOS thumbnails of attached receipts/photos (each
// decoded from its local file path), each individually removable, plus a pick button that's
// hidden once the cap is reached. Ported from V8's per-record photo attachment UI (app.js
// loadExistingLogPhotos()/photo-pick buttons), extended from the original single-photo version.
private const val MAX_PHOTOS = 3

@Composable
private fun PhotoAttachmentRow(
    photoUris: List<String>,
    onPickGallery: () -> Unit,
    onRemove: (String) -> Unit,
    onPickCamera: (() -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        photoUris.forEach { uri ->
            val bitmap = remember(uri) {
                runCatching { BitmapFactory.decodeFile(uri) }.getOrNull()
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "รูปที่แนบ",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }
                TextButton(onClick = { onRemove(uri) }) { Text("ลบ") }
            }
        }
        if (photoUris.size < MAX_PHOTOS) {
            var showSourceMenu by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { if (onPickCamera != null) showSourceMenu = true else onPickGallery() }) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (photoUris.isEmpty()) "แนบรูป/ใบเสร็จ" else "เพิ่มรูป")
                }
                if (onPickCamera != null) {
                    DropdownMenu(
                        expanded = showSourceMenu,
                        onDismissRequest = { showSourceMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("ถ่ายรูป") },
                            onClick = { showSourceMenu = false; onPickCamera() },
                        )
                        DropdownMenuItem(
                            text = { Text("เลือกจากแกลอรี่") },
                            onClick = { showSourceMenu = false; onPickGallery() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddExpenseDialog(
    saving: Boolean,
    latestOdometer: Double?,
    editing: Expense? = null,
    categorySuggestions: List<String> = emptyList(),
    onPickPhoto: ((onPicked: (uris: List<String>, extractedAmount: Double?) -> Unit) -> Unit)? = null,
    onPickCameraPhoto: ((onPicked: (uris: List<String>, extractedAmount: Double?) -> Unit) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, Double?, Boolean, Boolean, String?, String?, () -> Unit) -> Unit,
    onUpdate: ((String, String, String, String, Double, Double?, Boolean, Boolean, String?, String?, () -> Unit) -> Unit)? = null,
) {
    var date by remember { mutableStateOf(editing?.date ?: LocalDate.now().toString()) }
    var photoUris by remember { mutableStateOf(editing?.photoUrls ?: emptyList()) }
    var category by remember { mutableStateOf(editing?.category ?: "บำรุงรักษา") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var amount by remember { mutableStateOf(editing?.amount?.let { "%.2f".format(Locale.US, it) } ?: "") }
    var odometer by remember {
        mutableStateOf(editing?.odometerKm?.let { "%.0f".format(Locale.US, it) } ?: latestOdometer?.let { "%.0f".format(Locale.US, it) } ?: "")
    }
    var income by remember { mutableStateOf(editing?.income ?: false) }
    var recurring by remember { mutableStateOf(editing?.recurring ?: false) }
    var reminderDate by remember { mutableStateOf(editing?.reminderDate ?: "") }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val categoryOptions = categorySuggestions.filter { category.isBlank() || it.contains(category, ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) "แก้ไขค่าใช้จ่าย" else "เพิ่มค่าใช้จ่าย") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AutocompleteTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = "หมวดหมู่",
                    options = categoryOptions,
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(description, { description = it }, label = { Text("รายละเอียด") }, singleLine = true)
                OutlinedTextField(amount, { amount = it }, label = { Text("จำนวนเงิน") }, singleLine = true)
                OutlinedTextField(date, { date = it }, label = { Text("วันที่ YYYY-MM-DD") }, singleLine = true)
                OutlinedTextField(odometer, { odometer = it }, label = { Text("เลขไมล์ (ไม่บังคับ)") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(income, { income = it })
                    Text("เป็นรายรับ")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(recurring, { recurring = it })
                    Text("รายการประจำ")
                }
                OutlinedTextField(
                    reminderDate,
                    { reminderDate = it },
                    label = { Text("วันเตือนชำระ (ไม่บังคับ)") },
                    singleLine = true,
                )
                if (onPickPhoto != null) {
                    val handlePicked = { picked: List<String>, extractedAmount: Double? ->
                        photoUris = (photoUris + picked).distinct().take(MAX_PHOTOS)
                        // Item 3: mirrors V8's OCR auto-fill (autoOcrEnabled) — only
                        // pre-fills the amount field if the user hasn't typed one yet.
                        if (amount.isBlank() && extractedAmount != null) {
                            amount = "%.2f".format(Locale.US, extractedAmount)
                        }
                    }
                    PhotoAttachmentRow(
                        photoUris = photoUris,
                        onPickGallery = { onPickPhoto(handlePicked) },
                        onPickCamera = onPickCameraPhoto?.let { pick -> { pick(handlePicked) } },
                        onRemove = { uri -> photoUris = photoUris - uri },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    val odometerValue = odometer.toDoubleOrNull()
                    val reminderValue = reminderDate.takeIf(String::isNotBlank)
                    val photoUri = PhotoUris.join(photoUris)
                    if (editing != null && onUpdate != null) {
                        onUpdate(editing.id, date, category, description, amountValue, odometerValue, income, recurring, reminderValue, photoUri, onDismiss)
                    } else {
                        onSave(date, category, description, amountValue, odometerValue, income, recurring, reminderValue, photoUri, onDismiss)
                    }
                },
            ) { Text(if (saving) "กำลังบันทึก…" else "บันทึก") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } },
    )
}

@Composable
private fun AddMaintenanceDialog(
    saving: Boolean,
    latestOdometer: Double?,
    editing: MaintenanceTask? = null,
    categorySuggestions: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (String, String, String?, Double?, Int, Double, Int?, Double?, () -> Unit) -> Unit,
    onUpdate: ((String, String, String, String?, Double?, Int, Double, Int?, Double?, () -> Unit) -> Unit)? = null,
) {
    var name by remember { mutableStateOf(editing?.name ?: "เปลี่ยนน้ำมันเครื่อง") }
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
    val categoryOptions = categorySuggestions.filter { category.isBlank() || it.contains(category, ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) "แก้ไขรายการดูแลรถ" else "เพิ่มรายการดูแลรถ") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("รายการ") }, singleLine = true) }
                item {
                    AutocompleteTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = "ประเภท",
                        options = categoryOptions,
                        expanded = categoryMenuExpanded,
                        onExpandedChange = { categoryMenuExpanded = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item { OutlinedTextField(nextDate, { nextDate = it }, label = { Text("กำหนดวันที่ (ไม่บังคับ)") }, singleLine = true) }
                item { OutlinedTextField(nextOdometer, { nextOdometer = it }, label = { Text("กำหนดเลขไมล์ (ไม่บังคับ)") }, singleLine = true) }
                item { OutlinedTextField(warningDays, { warningDays = it }, label = { Text("เตือนล่วงหน้า (วัน)") }, singleLine = true) }
                item { OutlinedTextField(warningOdometer, { warningOdometer = it }, label = { Text("เตือนก่อนถึงระยะ (กม.)") }, singleLine = true) }
                item { OutlinedTextField(repeatMonths, { repeatMonths = it }, label = { Text("ทำซ้ำทุก (เดือน)") }, singleLine = true) }
                item { OutlinedTextField(repeatOdometer, { repeatOdometer = it }, label = { Text("ทำซ้ำทุก (กม.)") }, singleLine = true) }
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
            ) { Text(if (saving) "กำลังบันทึก…" else "บันทึก") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } },
    )
}

@Composable
private fun AddTripDialog(
    saving: Boolean,
    editing: Trip? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double, Double, Double, Double, Double, () -> Unit) -> Unit,
    onUpdate: ((String, String, String, Double, Double, Double, Double, Double, Double, () -> Unit) -> Unit)? = null,
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var date by remember { mutableStateOf(editing?.date ?: LocalDate.now().toString()) }
    var distance by remember { mutableStateOf(editing?.distanceKm?.let { "%.0f".format(Locale.US, it) } ?: "") }
    var fuel by remember { mutableStateOf(editing?.fuelCost?.let { "%.2f".format(Locale.US, it) } ?: "") }
    var toll by remember { mutableStateOf(editing?.tollCost?.let { "%.2f".format(Locale.US, it) } ?: "") }
    var parking by remember { mutableStateOf(editing?.parkingCost?.let { "%.2f".format(Locale.US, it) } ?: "") }
    var food by remember { mutableStateOf(editing?.foodCost?.let { "%.2f".format(Locale.US, it) } ?: "") }
    var other by remember { mutableStateOf(editing?.otherCost?.let { "%.2f".format(Locale.US, it) } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) "แก้ไขทริป" else "เพิ่มทริป") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("ชื่องาน/ปลายทาง") }, singleLine = true) }
                item { OutlinedTextField(date, { date = it }, label = { Text("วันที่ YYYY-MM-DD") }, singleLine = true) }
                item { OutlinedTextField(distance, { distance = it }, label = { Text("ระยะทาง (กม.)") }, singleLine = true) }
                item { OutlinedTextField(fuel, { fuel = it }, label = { Text("ค่าน้ำมัน") }, singleLine = true) }
                item { OutlinedTextField(toll, { toll = it }, label = { Text("ค่าทางด่วน") }, singleLine = true) }
                item { OutlinedTextField(parking, { parking = it }, label = { Text("ค่าที่จอด") }, singleLine = true) }
                item { OutlinedTextField(food, { food = it }, label = { Text("อาหาร/ที่พัก") }, singleLine = true) }
                item { OutlinedTextField(other, { other = it }, label = { Text("อื่น ๆ") }, singleLine = true) }
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
            ) { Text(if (saving) "กำลังบันทึก…" else "บันทึก") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } },
    )
}
