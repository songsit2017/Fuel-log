package com.songsit.fuellogpro.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.songsit.fuellogpro.data.firebase.PupuPocketLinkRepository
import com.songsit.fuellogpro.domain.model.Vehicle
import kotlinx.coroutines.launch

@Composable
fun PupuPocketLinkDialog(
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val repository = remember { PupuPocketLinkRepository() }
    var code by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(vehicles.map(Vehicle::id).toSet()) }
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text("เชื่อมต่อ PU Pocket") },
        text = {
            Column {
                Text("สร้างรหัสจาก PU Pocket > ตั้งค่า > เชื่อมต่อ Fuel Log แล้วใส่รหัส 10 หลักด้านล่าง")
                androidx.compose.material3.OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().filter { char -> char.isLetterOrDigit() }.take(10) },
                    label = { Text("รหัสเชื่อมต่อ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text("รถที่จะนำเข้ารายการ", style = MaterialTheme.typography.labelLarge)
                LazyColumn(Modifier.height(150.dp)) {
                    items(vehicles, key = Vehicle::id) { vehicle ->
                        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(
                                checked = vehicle.id in selectedIds,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + vehicle.id else selectedIds - vehicle.id
                                },
                            )
                            Text(vehicle.name.ifBlank { vehicle.registration.ifBlank { "รถของฉัน" } })
                        }
                    }
                }
                message?.let { Text(it, color = if (it.startsWith("เชื่อมต่อแล้ว")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = code.length == 10 && selectedIds.isNotEmpty() && !working,
                onClick = {
                    working = true
                    message = null
                    scope.launch {
                        runCatching { repository.redeem(code, selectedIds.toList()) }
                            .onSuccess { message = "เชื่อมต่อแล้ว กำลังนำเข้ารายการเดิม" }
                            .onFailure { message = it.message ?: "เชื่อมต่อไม่สำเร็จ" }
                        working = false
                    }
                },
            ) { Text(if (working) "กำลังเชื่อมต่อ..." else "เชื่อมต่อ") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !working) { Text("ยกเลิก") } },
    )
}
