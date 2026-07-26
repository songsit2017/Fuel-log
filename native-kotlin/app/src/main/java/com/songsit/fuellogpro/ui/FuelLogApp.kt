package com.songsit.fuellogpro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
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
    onAddFuel: (
        date: String,
        time: String,
        odometerKm: Double,
        liters: Double,
        pricePerLiter: Double,
        fullTank: Boolean,
        station: String,
        onSaved: () -> Unit,
    ) -> Unit,
    onDeleteFuel: (String) -> Unit,
    onClearError: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    val titles = listOf("ภาพรวม", "เติมน้ำมัน", "รถ", "เพิ่มเติม")

    FuelLogTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(titles[tab], fontWeight = FontWeight.SemiBold)
                            Text(state.vehicleName, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    listOf("หน้าแรก", "น้ำมัน", "รถ", "อื่นๆ").forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Text(listOf("⌂", "⛽", "▣", "•••")[index]) },
                            label = { Text(label) },
                        )
                    }
                }
            },
            floatingActionButton = {
                if (tab == 0 || tab == 1) {
                    FloatingActionButton(onClick = { showAdd = true }) { Text("+") }
                }
            },
        ) { padding ->
            when (tab) {
                0 -> Dashboard(state, Modifier.padding(padding))
                1 -> FuelList(state.entries, onDeleteFuel, Modifier.padding(padding))
                2 -> SimplePage("รถของฉัน", "การจัดการรถและ Cloud sync จะตามมาใน Native milestone ถัดไป", Modifier.padding(padding))
                else -> SimplePage("Native Preview", "กำลังย้ายฟีเจอร์จากแอปตัวเต็มทีละส่วนโดยไม่ทำให้ข้อมูลเดิมเสีย", Modifier.padding(padding))
            }
        }
        if (showAdd) {
            AddFuelDialog(
                saving = state.saving,
                onDismiss = { showAdd = false },
                onSave = onAddFuel,
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
                MetricCard("จ่ายทั้งหมด", thaiCurrency.format(state.summary.totalSpent), Modifier.weight(1f))
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
        item {
            Text("รายการล่าสุด", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (state.entries.isEmpty()) {
            item { EmptyState() }
        } else {
            items(state.entries.take(3), key = { it.id }) { FuelRow(it) }
        }
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
        if (entries.isEmpty()) item { EmptyState() }
        items(entries, key = { it.id }) { entry ->
            FuelRow(entry, onDelete)
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
private fun EmptyState() {
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
private fun AddFuelDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double, Double, Boolean, String, () -> Unit) -> Unit,
) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var time by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0).toString()) }
    var odometer by remember { mutableStateOf("") }
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
