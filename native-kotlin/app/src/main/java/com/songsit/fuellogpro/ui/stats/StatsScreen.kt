package com.songsit.fuellogpro.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songsit.fuellogpro.domain.DueLevel
import com.songsit.fuellogpro.domain.calculateMaintenanceStatus
import com.songsit.fuellogpro.domain.model.Expense
import com.songsit.fuellogpro.domain.model.FuelEntry
import com.songsit.fuellogpro.ui.IconBadge
import com.songsit.fuellogpro.ui.NativeAppState
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private val statsCurrency = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
private val statsNumber = NumberFormat.getNumberInstance(Locale("th", "TH")).apply { maximumFractionDigits = 2 }
private val statsMonthShort = listOf(
    "ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.",
    "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค.",
)

// Drivvo-style stats screen: (1) a Fuelio-like icon hero + monthly-spend card, (2) a
// maintenance due/overdue alert banner, (3) the 3 most recent fuel/expense transactions.
@Composable
fun StatsScreen(state: NativeAppState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ExpenseBreakdownCard(state) }
        item { MonthlySpendCard(state) }
        item { MaintenanceAlertBanner(state) }
        item { RecentActivityCard(state) }
    }
}

private val donutPalette = listOf(
    Color(0xFF8E4585), Color(0xFFFFA726), Color(0xFF5C6BC0), Color(0xFF26A69A),
    Color(0xFFEF5350), Color(0xFF66BB6A), Color(0xFFAB47BC), Color(0xFF8D6E63),
)

private data class ExpenseSlice(val label: String, val amount: Double, val color: Color)

// Replaces the old decorative pump/droplet/card icon cluster (which just overlapped three
// icons with no real data) with an actual donut chart of where money goes: fuel spend plus
// each expense category, proportioned by amount.
@Composable
private fun ExpenseBreakdownCard(state: NativeAppState) {
    val slices = remember(state.entries, state.expenses) {
        val byCategory = linkedMapOf<String, Double>()
        if (state.summary.totalSpent > 0) byCategory["ค่าน้ำมัน"] = state.summary.totalSpent
        state.expenses.filterNot(Expense::income).forEach { expense ->
            val key = expense.category.ifBlank { "อื่นๆ" }
            byCategory[key] = (byCategory[key] ?: 0.0) + expense.amount
        }
        byCategory.entries
            .sortedByDescending { it.value }
            .mapIndexed { index, entry -> ExpenseSlice(entry.key, entry.value, donutPalette[index % donutPalette.size]) }
    }
    val total = slices.sumOf { it.amount }
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconBadge(Icons.Filled.PieChart, size = 26.dp)
                Text("สัดส่วนค่าใช้จ่าย", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (total <= 0) {
                Text("ยังไม่มีข้อมูลค่าใช้จ่าย", style = MaterialTheme.typography.bodySmall)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Canvas(modifier = Modifier.size(110.dp)) {
                        val strokeWidth = size.minDimension * 0.28f
                        var startAngle = -90f
                        slices.forEach { slice ->
                            val sweep = (slice.amount / total * 360.0).toFloat()
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                            )
                            startAngle += sweep
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        slices.take(6).forEach { slice ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(slice.color))
                                Text(
                                    "${slice.label} • ${"%.0f".format(Locale.US, slice.amount / total * 100)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun monthTotal(entries: List<FuelEntry>, expenses: List<Expense>, year: Int, month: Int): Double {
    fun matches(date: String) = runCatching { LocalDate.parse(date) }.getOrNull()?.let { it.year == year && it.monthValue == month } ?: false
    val fuelTotal = entries.filter { matches(it.date) }.sumOf { it.amount }
    val expenseTotal = expenses.filterNot(Expense::income).filter { matches(it.date) }.sumOf { it.amount }
    return fuelTotal + expenseTotal
}

@Composable
private fun MonthlySpendCard(state: NativeAppState) {
    val today = remember { LocalDate.now() }
    val months = remember(state.entries, state.expenses) {
        (5 downTo 0).map { offset ->
            val date = today.minusMonths(offset.toLong())
            Triple(date.year, date.monthValue, monthTotal(state.entries, state.expenses, date.year, date.monthValue))
        }
    }
    val maxValue = months.maxOf { it.third }.takeIf { it > 0 } ?: 1.0
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBadge(Icons.Filled.Payments, size = 30.dp)
                Column {
                    Text("ค่าใช้จ่ายรายเดือน", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${statsCurrency.format(months.last().third)} เดือนนี้",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                months.forEach { (year, month, total) ->
                    val isCurrent = year == today.year && month == today.monthValue
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height((72 * (total / maxValue).coerceIn(0.04, 1.0)).dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
                        )
                        Text(
                            statsMonthShort[month - 1],
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceAlertBanner(state: NativeAppState) {
    val currentOdometer = state.summary.latestOdometerKm
    val dueTasks = remember(state.maintenanceTasks, currentOdometer) {
        state.maintenanceTasks
            .map { it to calculateMaintenanceStatus(it, currentOdometer) }
            .filter { (_, status) -> status.level != DueLevel.OK }
            .sortedBy { (_, status) -> status.level.ordinal }
    }
    if (dueTasks.isEmpty()) return
    val overdue = dueTasks.first().second.level == DueLevel.OVERDUE
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (overdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Filled.Build,
                    contentDescription = null,
                    tint = if (overdue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    if (overdue) "ถึงกำหนดบำรุงรักษาแล้ว" else "ใกล้ถึงกำหนดบำรุงรักษา",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (overdue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                )
            }
            dueTasks.take(3).forEach { (task, status) ->
                Text(
                    "${task.name} • ${status.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class ActivityRow(
    val date: String,
    val sortKey: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val income: Boolean,
    val icon: ImageVector,
)

@Composable
private fun RecentActivityCard(state: NativeAppState) {
    val rows = remember(state.entries, state.expenses) {
        val fuelRows = state.entries.map { entry ->
            ActivityRow(
                date = entry.date,
                sortKey = "${entry.date} ${entry.time}",
                title = entry.station.ifBlank { "เติมน้ำมัน" },
                subtitle = "${statsNumber.format(entry.odometerKm)} กม.",
                amount = entry.amount,
                income = false,
                icon = Icons.Filled.LocalGasStation,
            )
        }
        val expenseRows = state.expenses.map { expense ->
            ActivityRow(
                date = expense.date,
                sortKey = "${expense.date} 00:00",
                title = expense.category.ifBlank { "ค่าใช้จ่าย" },
                subtitle = expense.description,
                amount = expense.amount,
                income = expense.income,
                icon = Icons.Filled.Build,
            )
        }
        (fuelRows + expenseRows).sortedByDescending { it.sortKey }.take(3)
    }
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconBadge(Icons.Filled.Payments, size = 26.dp)
                Text("รายการล่าสุด", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (rows.isEmpty()) {
                Text("ยังไม่มีรายการ", style = MaterialTheme.typography.bodySmall)
            } else {
                rows.forEachIndexed { index, row ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(row.icon, size = 32.dp)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(row.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            val detail = listOf(row.date, row.subtitle).filter(String::isNotBlank).joinToString(" • ")
                            Text(detail, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(
                            "${if (row.income) "+" else "-"}${statsCurrency.format(row.amount)}",
                            fontWeight = FontWeight.Bold,
                            color = if (row.income) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (index != rows.lastIndex) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
