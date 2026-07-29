package com.songsit.fuellogpro.ui.timeline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.songsit.fuellogpro.domain.model.FuelEntry
import com.songsit.fuellogpro.ui.LocalDisplaySettings
import com.songsit.fuellogpro.ui.formatCurrencyAmount
import com.songsit.fuellogpro.ui.formatDistanceKm
import com.songsit.fuellogpro.ui.formatVolumeLiters
import java.text.NumberFormat
import java.util.Locale

// Read-only detail view for a single FuelEntry, matching Fuelio's UX pattern of
// card tap → detail view (not edit form). The Edit icon in the TopAppBar lets the
// user opt-in to editing from here.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefuelDetailScreen(
    entry: FuelEntry,
    onDismiss: () -> Unit,
    onEdit: (FuelEntry) -> Unit,
    onImageClick: (String) -> Unit = {},
) {
    val settings = LocalDisplaySettings.current
    val numFmt = remember { NumberFormat.getNumberInstance(Locale("th", "TH")).apply { maximumFractionDigits = 2 } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("รายละเอียดการเติมน้ำมัน") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ย้อนกลับ")
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(entry) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "แก้ไข")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Hero card: total cost ─────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("ค่าใช้จ่ายรวม", style = MaterialTheme.typography.labelLarge)
                    Text(
                        formatCurrencyAmount(entry.amount, settings),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                        )
                        Text(
                            "${entry.date}  ${entry.time}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                        )
                    }
                }
            }

            // ── Details card ──────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    if (entry.station.isNotBlank()) {
                        DetailRow(Icons.Filled.LocalGasStation, "สถานีบริการ", entry.station)
                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    }
                    DetailRow(
                        Icons.Filled.Speed,
                        "เลขไมล์รวม",
                        formatDistanceKm(entry.odometerKm, settings),
                    )
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    DetailRow(
                        Icons.Filled.WaterDrop,
                        "ปริมาณเชื้อเพลิง",
                        formatVolumeLiters(entry.liters, settings),
                    )
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    DetailRow(
                        Icons.Filled.AttachMoney,
                        "ราคา/ลิตร",
                        formatCurrencyAmount(entry.pricePerLiter, settings),
                    )
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    DetailRow(
                        Icons.Filled.LocalGasStation,
                        "เติมเต็มถัง",
                        if (entry.fullTank) "ใช่" else "ไม่",
                    )
                    if (entry.discountEnabled && entry.discountAmount > 0) {
                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                        val discountLabel = if (entry.discountPerLiter)
                            "${numFmt.format(entry.discountAmount)} บาท/ลิตร"
                        else
                            formatCurrencyAmount(entry.discountAmount, settings)
                        DetailRow(Icons.Filled.Percent, "ส่วนลด", discountLabel)
                    }
                }
            }

            // ── Weather card (optional) ───────────────────────────────────────
            if (!entry.weatherDescription.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Filled.WbSunny,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp),
                        )
                        Column {
                            Text(
                                "สภาพอากาศ",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            Text(
                                buildString {
                                    append(entry.weatherDescription)
                                    entry.weatherTemperatureC?.let { append("  ${numFmt.format(it)} °C") }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            // ── Photo gallery (all photos, 3-column grid) ─────────────────────
            if (entry.photoUrls.isNotEmpty()) {
                Text(
                    "รูปภาพที่แนบ (${entry.photoUrls.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    entry.photoUrls.chunked(3).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { uri ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clickable { onImageClick(uri) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                ) {
                                    AsyncImage(
                                        model = if (uri.startsWith("/")) java.io.File(uri) else uri,
                                        contentDescription = "รูปที่แนบ",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }
                            // Fill empty slots to keep 3-column alignment
                            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
