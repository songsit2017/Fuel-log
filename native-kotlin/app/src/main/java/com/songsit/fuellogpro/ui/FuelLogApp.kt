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
    onAddExpense: (String, String, String, Double, Double?, () -> Unit) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onSelectVehicle: (String) -> Unit,
    onAddVehicle: (String, String, String, () -> Unit) -> Unit,
    onDeleteVehicle: (String) -> Unit,
    onClearError: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var showAddFuel by remember { mutableStateOf(false) }
    var showAddVehicle by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }
    val titles = listOf("ภาพรวม", "การเติมน้ำมัน", "ค่าใช้จ่าย", "รถของฉัน")

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
                    listOf("หน้าหลัก", "น้ำมัน", "ค่าใช้จ่าย", "รถ").forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Text(listOf("⌂", "⛽", "฿", "●")[index]) },
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
                        onClick = { if (state.vehicles.isEmpty()) showAddVehicle = true else showAddExpense = true },
                    ) { Text("+") }
                    3 -> FloatingActionButton(onClick = { showAddVehicle = true }) { Text("+") }
                }
            },
        ) { padding ->
            when (tab) {
                0 -> Dashboard(state, Modifier.padding(padding))
                1 -> FuelList(state.entries, onDeleteFuel, Modifier.padding(padding))
                2 -> ExpenseList(state.expenses, state.totalExpenses, onDeleteExpense, Modifier.padding(padding))
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
    total: Double,
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
                    Text("ค่าใช้จ่ายทั้งหมด", style = MaterialTheme.typography.labelLarge)
                    Text(thaiCurrency.format(total), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
                    Text(thaiCurrency.format(expense.amount), fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onDelete(expense.id) }) { Text("ลบ") }
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
    onSave: (String, String, String, Double, Double?, () -> Unit) -> Unit,
) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var category by remember { mutableStateOf("บำรุงรักษา") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf(latestOdometer?.let { "%.0f".format(Locale.US, it) } ?: "") }
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
                        onDismiss,
                    )
                },
            ) { Text(if (saving) "กำลังบันทึก…" else "บันทึก") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } },
    )
}
