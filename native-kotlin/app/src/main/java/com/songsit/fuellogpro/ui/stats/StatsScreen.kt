package com.songsit.fuellogpro.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songsit.fuellogpro.ui.IconBadge
import com.songsit.fuellogpro.domain.distanceByMonth
import com.songsit.fuellogpro.domain.distanceByYear
import com.songsit.fuellogpro.domain.expenseTotalByMonth
import com.songsit.fuellogpro.domain.expenseTotalByYear
import com.songsit.fuellogpro.domain.fuelCountByMonth
import com.songsit.fuellogpro.domain.fuelCountByYear
import com.songsit.fuellogpro.domain.fuelLitersByMonth
import com.songsit.fuellogpro.domain.fuelLitersByYear
import com.songsit.fuellogpro.ui.NativeAppState
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private val statsCurrency = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
private val statsNumber = NumberFormat.getNumberInstance(Locale("th", "TH")).apply { maximumFractionDigits = 2 }

// Item D: "สถิติ" screen, three sub-tabs via the stable (non-experimental) material3 TabRow/Tab.
@Composable
fun StatsScreen(state: NativeAppState, modifier: Modifier = Modifier) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("เติม-เพิ่ม", "ค่าใช้จ่าย", "ระยะทาง")
    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            titles.forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
            }
        }
        when (tab) {
            0 -> FillUpStatsTab(state, Modifier.fillMaxSize())
            1 -> ExpenseStatsTab(state, Modifier.fillMaxSize())
            else -> DistanceStatsTab(state, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun StatCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconBadge(icon, size = 26.dp)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

private fun compareLine(current: Number, previous: Number, currentLabel: String, previousLabel: String): String =
    "${statsNumber.format(current)} $currentLabel / ${statsNumber.format(previous)} $previousLabel"

private fun previousMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 1) (year - 1) to 12 else year to (month - 1)

@Composable
private fun FillUpStatsTab(state: NativeAppState, modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now() }
    val thisYear = today.year
    val lastYear = thisYear - 1
    val thisMonth = today.monthValue
    val (prevYear, prevMonth) = previousMonth(thisYear, thisMonth)
    val entries = state.entries
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StatCard("จำนวนครั้งเติมน้ำมัน", Icons.Filled.LocalGasStation) {
                Text(compareLine(fuelCountByYear(entries, thisYear), fuelCountByYear(entries, lastYear), "ปีนี้", "ปีก่อนหน้านี้"))
                Text(compareLine(fuelCountByMonth(entries, thisYear, thisMonth), fuelCountByMonth(entries, prevYear, prevMonth), "เดือนนี้", "เดือนก่อนหน้านี้"))
            }
        }
        item {
            StatCard("ปริมาณน้ำมันรวม", Icons.Filled.WaterDrop) {
                Text(compareLine(fuelLitersByYear(entries, thisYear), fuelLitersByYear(entries, lastYear), "ลิตร ปีนี้", "ลิตร ปีก่อนหน้านี้"))
                Text(compareLine(fuelLitersByMonth(entries, thisYear, thisMonth), fuelLitersByMonth(entries, prevYear, prevMonth), "ลิตร เดือนนี้", "ลิตร เดือนก่อนหน้านี้"))
            }
        }
        item {
            StatCard("ปริมาณต่อครั้ง", Icons.Filled.LocalGasStation) {
                val minLiters = entries.minOfOrNull { it.liters }
                val maxLiters = entries.maxOfOrNull { it.liters }
                Text("เติมต่ำสุด: ${minLiters?.let { "${statsNumber.format(it)} ลิตร" } ?: "—"}")
                Text("เติมสูงสุด: ${maxLiters?.let { "${statsNumber.format(it)} ลิตร" } ?: "—"}")
            }
        }
        item {
            StatCard("อัตราสิ้นเปลืองเฉลี่ย", Icons.Filled.Speed) {
                // Reuses NativeAppState.summary (calculateFuelSummary() in NativeAppViewModel) —
                // not recomputed here.
                Text(
                    state.summary.averageKmPerLiter?.let { "${statsNumber.format(it)} กม./ลิตร" } ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ExpenseStatsTab(state: NativeAppState, modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now() }
    val thisYear = today.year
    val lastYear = thisYear - 1
    val thisMonth = today.monthValue
    val (prevYear, prevMonth) = previousMonth(thisYear, thisMonth)
    val expenses = state.expenses
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StatCard("ค่าใช้จ่ายรวม", Icons.Filled.Payments) {
                Text(
                    "${statsCurrency.format(expenseTotalByYear(expenses, thisYear))} ปีนี้ / " +
                        "${statsCurrency.format(expenseTotalByYear(expenses, lastYear))} ปีก่อนหน้านี้",
                )
                Text(
                    "${statsCurrency.format(expenseTotalByMonth(expenses, thisYear, thisMonth))} เดือนนี้ / " +
                        "${statsCurrency.format(expenseTotalByMonth(expenses, prevYear, prevMonth))} เดือนก่อนหน้านี้",
                )
            }
        }
    }
}

@Composable
private fun DistanceStatsTab(state: NativeAppState, modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now() }
    val thisYear = today.year
    val lastYear = thisYear - 1
    val thisMonth = today.monthValue
    val (prevYear, prevMonth) = previousMonth(thisYear, thisMonth)
    val entries = state.entries
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StatCard("ระยะทางที่ขับ", Icons.Filled.Route) {
                Text(compareLine(distanceByYear(entries, thisYear), distanceByYear(entries, lastYear), "กม. ปีนี้", "กม. ปีก่อนหน้านี้"))
                Text(compareLine(distanceByMonth(entries, thisYear, thisMonth), distanceByMonth(entries, prevYear, prevMonth), "กม. เดือนนี้", "กม. เดือนก่อนหน้านี้"))
            }
        }
    }
}
