package com.songsit.fuellogpro.ui.timeline

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.songsit.fuellogpro.domain.model.Expense
import com.songsit.fuellogpro.domain.model.FuelEntry
import com.songsit.fuellogpro.ui.NativeAppState
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private val timelineCurrency = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
private val timelineNumber = NumberFormat.getNumberInstance(Locale("th", "TH")).apply { maximumFractionDigits = 2 }

private val thaiMonthNames = listOf(
    "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
    "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม",
)

private sealed class TimelineRow(val date: String, val sortKey: String) {
    class FuelRow(val entry: FuelEntry) : TimelineRow(entry.date, entry.date + " " + entry.time)
    class ExpenseRow(val expense: Expense) : TimelineRow(expense.date, expense.date + " 00:00")
}

@Composable
fun TimelineScreen(state: NativeAppState, modifier: Modifier = Modifier) {
    val rows = remember(state.entries, state.expenses) {
        val combined = state.entries.map { TimelineRow.FuelRow(it) as TimelineRow } +
            state.expenses.map { TimelineRow.ExpenseRow(it) as TimelineRow }
        combined.sortedByDescending { it.sortKey }
    }
    val groups = remember(rows) {
        rows.groupBy { row ->
            val parsed = runCatching { LocalDate.parse(row.date) }.getOrNull()
            parsed?.let { it.year to it.monthValue }
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (rows.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ยังไม่มีรายการ", fontWeight = FontWeight.SemiBold)
                        Text("การเติมน้ำมันและค่าใช้จ่ายจะปรากฏที่นี่", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        groups.forEach { (yearMonth, groupRows) ->
            item {
                val label = yearMonth?.let { (year, month) -> "${thaiMonthNames[month - 1]} $year" } ?: "ไม่ทราบวันที่"
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(groupRows, key = {
                when (it) {
                    is TimelineRow.FuelRow -> "fuel-${it.entry.id}"
                    is TimelineRow.ExpenseRow -> "expense-${it.expense.id}"
                }
            }) { row ->
                TimelineEntryRow(row)
            }
        }
    }
}

@Composable
private fun TimelineEntryRow(row: TimelineRow) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Simple vertical line + dot, built from plain Box/Column (no external library).
        Column(
            modifier = Modifier.width(24.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            )
        }
        Spacer(Modifier.width(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            when (row) {
                is TimelineRow.FuelRow -> FuelTimelineContent(row.entry)
                is TimelineRow.ExpenseRow -> ExpenseTimelineContent(row.expense)
            }
        }
    }
}

@Composable
private fun FuelTimelineContent(entry: FuelEntry) {
    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(entry.date, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(timelineCurrency.format(entry.amount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        }
        val detail = listOfNotNull(
            entry.station.takeIf(String::isNotBlank),
            "${timelineNumber.format(entry.odometerKm)} กม.",
        ).joinToString(" • ")
        Text(detail, style = MaterialTheme.typography.bodySmall)
        if (entry.photoUrls.isNotEmpty()) TimelineThumbnails(entry.photoUrls)
    }
}

@Composable
private fun ExpenseTimelineContent(expense: Expense) {
    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (expense.income) Icons.Filled.Savings else Icons.Filled.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(expense.date, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                "${if (expense.income) "+" else "−"}${timelineCurrency.format(expense.amount)}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        val detail = listOf(expense.category, expense.description).filter(String::isNotBlank).joinToString(" • ")
        if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall)
        if (expense.photoUrls.isNotEmpty()) TimelineThumbnails(expense.photoUrls)
    }
}

// Reuses the same decode-a-local-file-path pattern as PhotoAttachmentRow in FuelLogApp.kt,
// but read-only and capped at 3 thumbnails per Item C/E.
@Composable
private fun TimelineThumbnails(photoUris: List<String>) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        photoUris.take(3).forEach { uri ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.size(52.dp),
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "รูปที่แนบ",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}
