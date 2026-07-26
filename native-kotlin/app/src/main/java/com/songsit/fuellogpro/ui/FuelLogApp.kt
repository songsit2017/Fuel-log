package com.songsit.fuellogpro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

private val thaiCurrency = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
private val number = NumberFormat.getNumberInstance(Locale("th", "TH")).apply {
    maximumFractionDigits = 2
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelLogApp(
    state: NativeAppState,
    onAddFuel: (String, String, Double, Double, Double, Boolean, String, () -> Unit) -> Unit,
    onDeleteFuel: (String) -> Unit,
    onAddExpense: (String, String, String, Double, Double?, Boolean, Boolean, String?, () -> Unit) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onAddMaintenance: (String, String, String?, Double?, Int, Double, Int?, Double?, () -> Unit) -> Unit,
    onCompleteMaintenance: (String) -> Unit,
    onDeleteMaintenance: (String) -> Unit,
    onAddTrip: (String, String, Double, Double, Double, Double, Double, Double, () -> Unit) -> Unit,
    onDeleteTrip: (String) -> Unit,
    onSelectVehicle: (String) -> Unit,
    onAddVehicle: (String, String, String, () -> Unit) -> Unit,
    onDeleteVehicle: (String) -> Unit,
    onClearError: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var showAddFuel by remember { mutableStateOf(false) }
    var showAddVehicle by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }
    var showAddMaintenance by remember { mutableStateOf(false) }
    var showAddTrip by remember { mutableStateOf(false) }
    var recordsMode by remember { mutableIntStateOf(0) }
    val titles = listOf("ภาพรวม", "การเติมน้ำมัน", "บันทึก", "บำรุงรักษา", "รถของฉัน")

    FuelLogTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(titles[tab], fontWeight = FontWeight.SemiBold)
                            state.selectedVehicle?.let {
                                Text(it.name, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    listOf("หน้าหลัก", "น้ำมัน", "บันทึก", "ดูแลรถ", "รถ").forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Text(listOf("⌂", "⛽", "฿", "✓", "●")[index]) },
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
                0 -> Dashboard(state, Modifier.padding(padding))
                1 -> FuelList(state.entries, onDeleteFuel, Modifier.padding(padding))
                2 -> RecordsPage(
                    mode = recordsMode,
                    onModeChange = { recordsMode = it },
                    state = state,
                    onDeleteExpense = onDeleteExpense,
                    onDeleteTrip = onDeleteTrip,
                    modifier = Modifier.padding(padding),
                )
                3 -> MaintenanceList(
                    tasks = state.maintenanceTasks,
                    currentOdometerKm = state.summary.latestOdometerKm,
                    onComplete = onCompleteMaintenance,
                    onDelete = onDeleteMaintenance,
                    modifier = Modifier.padding(padding),
                )
                else -> VehicleList(
                    vehicles = state.vehicles,
                    selectedVehicleId = state.selectedVehicle?.id,
                    onSelect = onSelectVehicle,
                    onDelete = onDeleteVehicle,
                    modifier = Modifier.padding(padding),
                )
            }
        }
        if (showAddFuel) {
            AddFuelDialog(
                saving = state.saving,
                latestOdometer = state.summary.latestOdometerKm,
                onDismiss = { showAddFuel = false },
                onSave = onAddFuel,
            )
        }
        if (showAddVehicle) {
            AddVehicleDialog(
                saving = state.saving,
                onDismiss = { showAddVehicle = false },
                onSave = onAddVehicle,
            )
        }
        if (showAddExpense) {
            AddExpenseDialog(
                saving = state.saving,
                latestOdometer = state.summary.latestOdometerKm,
                onDismiss = { showAddExpense = false },
                onSave = onAddExpense,
            )
        }
        if (showAddMaintenance) {
            AddMaintenanceDialog(
                saving = state.saving,
                latestOdometer = state.summary.latestOdometerKm,
                onDismiss = { showAddMaintenance = false },
                onSave = onAddMaintenance,
            )
        }
        if (showAddTrip) {
            AddTripDialog(
                saving = state.saving,
                onDismiss = { showAddTrip = false },
                onSave = onAddTrip,
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

@Composable
private fun Dashboard(state: NativeAppState, modifier: Modifier = Modifier) {
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
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("อัตราสิ้นเปลืองเฉลี่ย", color = MaterialTheme.colorScheme.onPrimary)
                    Text(
                        state.summary.averageKmPerLiter?.let { "${number.format(it)} กม./ลิตร" } ?: "—",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("ค่าใช้จ่าย", thaiCurrency.format(state.summary.totalSpent), Modifier.weight(1f))
                MetricCard("น้ำมัน", "${number.format(state.summary.totalLiters)} ลิตร", Modifier.weight(1f))
            }
        }
        item {
            MetricCard(
                "เลขไมล์ล่าสุด",
                state.summary.latestOdometerKm?.let { "${number.format(it)} กม." } ?: "ยังไม่มีข้อมูล",
                Modifier.fillMaxWidth(),
            )
        }
        item { Text("รายการล่าสุด", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        if (state.entries.isEmpty()) item { EmptyFuelState() }
        else items(state.entries.take(3), key = { it.id }) { FuelRow(it) }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FuelList(entries: List<FuelEntry>, onDelete: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (entries.isEmpty()) item { EmptyFuelState() }
        items(entries, key = { it.id }) { FuelRow(it, onDelete) }
    }
}

@Composable
private fun ExpenseList(
    expenses: List<Expense>,
    totalExpense: Double,
    totalIncome: Double,
    netExpense: Double,
    onDelete: (String) -> Unit,
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
            Card(shape = RoundedCornerShape(18.dp)) {
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
                            color = if (expense.income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
    onDeleteTrip: (String) -> Unit,
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
                modifier = Modifier.weight(1f),
            )
        } else {
            TripList(
                trips = state.trips,
                summary = state.tripSummary,
                onDelete = onDeleteTrip,
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
            Card(shape = RoundedCornerShape(18.dp)) {
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
private fun FuelRow(entry: FuelEntry, onDelete: ((String) -> Unit)? = null) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(entry.station.ifBlank { "เติมน้ำมัน" }, fontWeight = FontWeight.SemiBold)
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
                Text("${number.format(entry.liters)} ลิตร • ${number.format(entry.pricePerLiter)}/ลิตร", modifier = Modifier.weight(1f))
                if (entry.fullTank) Text("เต็มถัง", color = MaterialTheme.colorScheme.secondary)
                onDelete?.let { TextButton(onClick = { it(entry.id) }) { Text("ลบ") } }
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double, Double, Boolean, String, () -> Unit) -> Unit,
) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var time by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0).toString()) }
    var odometer by remember { mutableStateOf(latestOdometer?.let { "%.0f".format(Locale.US, it) } ?: "") }
    var liters by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var station by remember { mutableStateOf("") }
    var fullTank by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("เพิ่มการเติมน้ำมัน") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(station, { station = it }, label = { Text("สถานีบริการ") }, singleLine = true) }
                item { OutlinedTextField(date, { date = it }, label = { Text("วันที่ YYYY-MM-DD") }, singleLine = true) }
                item { OutlinedTextField(time, { time = it }, label = { Text("เวลา HH:MM") }, singleLine = true) }
                item { OutlinedTextField(odometer, { odometer = it }, label = { Text("เลขไมล์ กม.") }, singleLine = true) }
                item { OutlinedTextField(liters, { liters = it }, label = { Text("จำนวนลิตร") }, singleLine = true) }
                item { OutlinedTextField(price, { price = it }, label = { Text("ราคาต่อลิตร") }, singleLine = true) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(fullTank, { fullTank = it })
                        Text("เติมเต็มถัง")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    onSave(
                        date,
                        time,
                        odometer.toDoubleOrNull() ?: 0.0,
                        liters.toDoubleOrNull() ?: 0.0,
                        price.toDoubleOrNull() ?: 0.0,
                        fullTank,
                        station,
                        onDismiss,
                    )
                },
            ) { Text(if (saving) "กำลังบันทึก…" else "บันทึก") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } },
    )
}

@Composable
private fun AddExpenseDialog(
    saving: Boolean,
    latestOdometer: Double?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, Double?, Boolean, Boolean, String?, () -> Unit) -> Unit,
) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var category by remember { mutableStateOf("บำรุงรักษา") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf(latestOdometer?.let { "%.0f".format(Locale.US, it) } ?: "") }
    var income by remember { mutableStateOf(false) }
    var recurring by remember { mutableStateOf(false) }
    var reminderDate by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("เพิ่มค่าใช้จ่าย") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(category, { category = it }, label = { Text("หมวดหมู่") }, singleLine = true)
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
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    onSave(
                        date,
                        category,
                        description,
                        amount.toDoubleOrNull() ?: 0.0,
                        odometer.toDoubleOrNull(),
                        income,
                        recurring,
                        reminderDate.takeIf(String::isNotBlank),
                        onDismiss,
                    )
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
    onDismiss: () -> Unit,
    onSave: (String, String, String?, Double?, Int, Double, Int?, Double?, () -> Unit) -> Unit,
) {
    var name by remember { mutableStateOf("เปลี่ยนน้ำมันเครื่อง") }
    var category by remember { mutableStateOf("บำรุงรักษา") }
    var nextDate by remember { mutableStateOf("") }
    var nextOdometer by remember {
        mutableStateOf(latestOdometer?.let { "%.0f".format(Locale.US, it + 10_000) } ?: "")
    }
    var warningDays by remember { mutableStateOf("30") }
    var warningOdometer by remember { mutableStateOf("1000") }
    var repeatMonths by remember { mutableStateOf("12") }
    var repeatOdometer by remember { mutableStateOf("10000") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("เพิ่มรายการดูแลรถ") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("รายการ") }, singleLine = true) }
                item { OutlinedTextField(category, { category = it }, label = { Text("ประเภท") }, singleLine = true) }
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
                    onSave(
                        name,
                        category,
                        nextDate.takeIf(String::isNotBlank),
                        nextOdometer.toDoubleOrNull(),
                        warningDays.toIntOrNull() ?: 30,
                        warningOdometer.toDoubleOrNull() ?: 1_000.0,
                        repeatMonths.toIntOrNull(),
                        repeatOdometer.toDoubleOrNull(),
                        onDismiss,
                    )
                },
            ) { Text(if (saving) "กำลังบันทึก…" else "บันทึก") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } },
    )
}

@Composable
private fun AddTripDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double, Double, Double, Double, Double, () -> Unit) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var distance by remember { mutableStateOf("") }
    var fuel by remember { mutableStateOf("") }
    var toll by remember { mutableStateOf("") }
    var parking by remember { mutableStateOf("") }
    var food by remember { mutableStateOf("") }
    var other by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("เพิ่มทริป") },
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
                    onSave(
                        name,
                        date,
                        distance.toDoubleOrNull() ?: 0.0,
                        fuel.toDoubleOrNull() ?: 0.0,
                        toll.toDoubleOrNull() ?: 0.0,
                        parking.toDoubleOrNull() ?: 0.0,
                        food.toDoubleOrNull() ?: 0.0,
                        other.toDoubleOrNull() ?: 0.0,
                        onDismiss,
                    )
                },
            ) { Text(if (saving) "กำลังบันทึก…" else "บันทึก") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } },
    )
}
