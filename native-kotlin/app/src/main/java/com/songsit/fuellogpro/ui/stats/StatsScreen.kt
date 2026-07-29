package com.songsit.fuellogpro.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songsit.fuellogpro.domain.model.FuelEntry
import com.songsit.fuellogpro.ui.NativeAppState
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max

private val statsCurrency = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
private val statsNumber = NumberFormat.getNumberInstance(Locale("th", "TH")).apply { maximumFractionDigits = 2 }

data class FuelioStats(
    val totalRefills: Int,
    val refillsThisYear: Int,
    val refillsThisMonth: Int,
    val refillsPrevYear: Int,
    val refillsPrevMonth: Int,

    val totalLiters: Double,
    val litersThisYear: Double,
    val litersThisMonth: Double,
    val litersPrevYear: Double,
    val litersPrevMonth: Double,
    val minLiters: Double,
    val maxLiters: Double,

    val avgKml: Double,
    val bestKml: Double,
    val worstKml: Double,

    val totalCost: Double,
    val costThisYear: Double,
    val costThisMonth: Double,
    val costPrevYear: Double,
    val costPrevMonth: Double,

    val minBill: Double,
    val maxBill: Double,
    val bestPricePerLiter: Double,
    val worstPricePerLiter: Double,

    val avgCostPerKm: Double,
    val bestCostPerKm: Double,
    val worstCostPerKm: Double,
    val avgCostPerDay: Double,
    val avgCostPerMonth: Double,

    val totalDistance: Double,
    val lastOdo: Double,
    val distanceThisYear: Double,
    val distanceThisMonth: Double,
    val distancePrevYear: Double,
    val distancePrevMonth: Double,
    val avgDistancePerDay: Double,
    val avgDistancePerMonth: Double
)

private fun computeFuelioStats(entries: List<FuelEntry>): FuelioStats {
    if (entries.isEmpty()) {
        return FuelioStats(
            0, 0, 0, 0, 0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        )
    }

    val today = LocalDate.now()
    val thisYear = today.year
    val thisMonth = YearMonth.now()
    val prevYear = thisYear - 1
    val prevMonth = thisMonth.minusMonths(1)

    val dates = entries.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
    val minDate = dates.minOrNull() ?: today
    val maxDate = dates.maxOrNull() ?: today
    val daysBetween = max(1L, ChronoUnit.DAYS.between(minDate, maxDate))
    val monthsBetween = max(1L, ChronoUnit.MONTHS.between(minDate.withDayOfMonth(1), maxDate.withDayOfMonth(1)) + 1)

    var refillsThisYear = 0
    var refillsThisMonth = 0
    var refillsPrevYear = 0
    var refillsPrevMonth = 0

    var litersThisYear = 0.0
    var litersThisMonth = 0.0
    var litersPrevYear = 0.0
    var litersPrevMonth = 0.0

    var costThisYear = 0.0
    var costThisMonth = 0.0
    var costPrevYear = 0.0
    var costPrevMonth = 0.0

    var distanceThisYear = 0.0
    var distanceThisMonth = 0.0
    var distancePrevYear = 0.0
    var distancePrevMonth = 0.0

    val sortedEntries = entries.sortedBy { "${it.date} ${it.time}" }
    val sortedOdo = sortedEntries.filter { it.odometerKm > 0 }
    val lastOdo = sortedOdo.lastOrNull()?.odometerKm ?: 0.0
    val firstOdo = sortedOdo.firstOrNull()?.odometerKm ?: 0.0
    val totalDistance = if (lastOdo > firstOdo) lastOdo - firstOdo else 0.0

    var totalLiters = 0.0
    var minLiters = Double.MAX_VALUE
    var maxLiters = 0.0
    
    var totalCost = 0.0
    var minBill = Double.MAX_VALUE
    var maxBill = 0.0
    
    var minPrice = Double.MAX_VALUE
    var maxPrice = 0.0

    val kmls = mutableListOf<Double>()
    val cpks = mutableListOf<Double>()
    var validDistanceSum = 0.0
    var validLitersSum = 0.0

    var lastFullOdo = -1.0
    var accumulatedLiters = 0.0
    var accumulatedCost = 0.0

    for (entry in sortedOdo) {
        if (entry.missedPreviousFillUp) {
            lastFullOdo = -1.0
            accumulatedLiters = 0.0
            accumulatedCost = 0.0
        }
        
        if (lastFullOdo >= 0) {
            val dist = entry.odometerKm - lastFullOdo
            accumulatedLiters += entry.liters
            accumulatedCost += entry.amount
            
            if (entry.fullTank) {
                if (dist > 0 && accumulatedLiters > 0) {
                    val kml = dist / accumulatedLiters
                    if (kml in 1.0..100.0) kmls.add(kml)
                    
                    val cpk = accumulatedCost / dist
                    if (cpk > 0.0) cpks.add(cpk)
                    
                    validDistanceSum += dist
                    validLitersSum += accumulatedLiters
                }
                lastFullOdo = entry.odometerKm
                accumulatedLiters = 0.0
                accumulatedCost = 0.0
            }
        } else {
            if (entry.fullTank) {
                lastFullOdo = entry.odometerKm
                accumulatedLiters = 0.0
                accumulatedCost = 0.0
            }
        }
    }

    var prevOdometer = -1.0

    sortedEntries.forEach { entry ->
        val date = runCatching { LocalDate.parse(entry.date) }.getOrNull()
        if (date != null) {
            val ym = YearMonth.from(date)
            
            if (date.year == thisYear) { refillsThisYear++; litersThisYear += entry.liters; costThisYear += entry.amount }
            if (date.year == prevYear) { refillsPrevYear++; litersPrevYear += entry.liters; costPrevYear += entry.amount }
            if (ym == thisMonth) { refillsThisMonth++; litersThisMonth += entry.liters; costThisMonth += entry.amount }
            if (ym == prevMonth) { refillsPrevMonth++; litersPrevMonth += entry.liters; costPrevMonth += entry.amount }
        }
        
        if (entry.odometerKm > 0) {
            if (prevOdometer >= 0) {
                val dist = entry.odometerKm - prevOdometer
                if (dist > 0 && date != null) {
                    val ym = YearMonth.from(date)
                    if (date.year == thisYear) distanceThisYear += dist
                    if (date.year == prevYear) distancePrevYear += dist
                    if (ym == thisMonth) distanceThisMonth += dist
                    if (ym == prevMonth) distancePrevMonth += dist
                }
            }
            prevOdometer = entry.odometerKm
        }
        
        totalLiters += entry.liters
        totalCost += entry.amount
        
        if (entry.liters > 0) {
            minLiters = minOf(minLiters, entry.liters)
            maxLiters = maxOf(maxLiters, entry.liters)
        }
        if (entry.amount > 0) {
            minBill = minOf(minBill, entry.amount)
            maxBill = maxOf(maxBill, entry.amount)
        }
        if (entry.pricePerLiter > 0) {
            minPrice = minOf(minPrice, entry.pricePerLiter)
            maxPrice = maxOf(maxPrice, entry.pricePerLiter)
        }
    }

    if (minLiters == Double.MAX_VALUE) minLiters = 0.0
    if (minBill == Double.MAX_VALUE) minBill = 0.0
    if (minPrice == Double.MAX_VALUE) minPrice = 0.0

    val avgKml = if (validLitersSum > 0) validDistanceSum / validLitersSum else 0.0
    val bestKml = if (kmls.isNotEmpty()) kmls.maxOrNull() ?: 0.0 else 0.0
    val worstKml = if (kmls.isNotEmpty()) kmls.minOrNull() ?: 0.0 else 0.0

    val avgCpk = if (totalDistance > 0) totalCost / totalDistance else 0.0
    val bestCpk = if (cpks.isNotEmpty()) cpks.minOrNull() ?: 0.0 else 0.0
    val worstCpk = if (cpks.isNotEmpty()) cpks.maxOrNull() ?: 0.0 else 0.0

    return FuelioStats(
        totalRefills = sortedEntries.size,
        refillsThisYear = refillsThisYear,
        refillsThisMonth = refillsThisMonth,
        refillsPrevYear = refillsPrevYear,
        refillsPrevMonth = refillsPrevMonth,
        totalLiters = totalLiters,
        litersThisYear = litersThisYear,
        litersThisMonth = litersThisMonth,
        litersPrevYear = litersPrevYear,
        litersPrevMonth = litersPrevMonth,
        minLiters = minLiters,
        maxLiters = maxLiters,
        avgKml = avgKml,
        bestKml = bestKml,
        worstKml = worstKml,
        totalCost = totalCost,
        costThisYear = costThisYear,
        costThisMonth = costThisMonth,
        costPrevYear = costPrevYear,
        costPrevMonth = costPrevMonth,
        minBill = minBill,
        maxBill = maxBill,
        bestPricePerLiter = minPrice,
        worstPricePerLiter = maxPrice,
        avgCostPerKm = avgCpk,
        bestCostPerKm = bestCpk,
        worstCostPerKm = worstCpk,
        avgCostPerDay = totalCost / daysBetween,
        avgCostPerMonth = totalCost / monthsBetween,
        totalDistance = totalDistance,
        lastOdo = lastOdo,
        distanceThisYear = distanceThisYear,
        distanceThisMonth = distanceThisMonth,
        distancePrevYear = distancePrevYear,
        distancePrevMonth = distancePrevMonth,
        avgDistancePerDay = totalDistance / daysBetween,
        avgDistancePerMonth = totalDistance / monthsBetween
    )
}

@Composable
fun StatsScreen(state: NativeAppState, modifier: Modifier = Modifier) {
    val stats = remember(state.entries) { computeFuelioStats(state.entries) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("เติม-เพิ่ม", "ค่าใช้จ่าย", "ระยะทาง")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Refills Tab
                    item { HeroCard("Refills") }
                    item {
                        MainValueCard(
                            title = "เติม-เพิ่ม",
                            value = "${stats.totalRefills}",
                            thisYear = "${stats.refillsThisYear} ปีนี้",
                            prevYear = "${stats.refillsPrevYear} ปีก่อนหน้านี้",
                            thisMonth = "${stats.refillsThisMonth} เดือนนี้",
                            prevMonth = "${stats.refillsPrevMonth} เดือนก่อนหน้า",
                            icon = Icons.Filled.LocalGasStation
                        )
                    }
                    item {
                        MainValueCard(
                            title = "เชื้อเพลิง",
                            value = "${statsNumber.format(stats.totalLiters)} L",
                            thisYear = "${statsNumber.format(stats.litersThisYear)} L\nปีนี้",
                            prevYear = "${statsNumber.format(stats.litersPrevYear)} L\nปีก่อนหน้านี้",
                            thisMonth = "${statsNumber.format(stats.litersThisMonth)} L\nเดือนนี้",
                            prevMonth = "${statsNumber.format(stats.litersPrevMonth)} L\nเดือนก่อนหน้า",
                            icon = Icons.Filled.WaterDrop,
                            bottomLeftLabel = "${statsNumber.format(stats.minLiters)} L\nเติมต่ำสุด",
                            bottomLeftIcon = Icons.Filled.ArrowDownward,
                            bottomRightLabel = "${statsNumber.format(stats.maxLiters)} L\nเติมสูงสุด",
                            bottomRightIcon = Icons.Filled.ArrowUpward
                        )
                    }
                    item {
                        MainValueCard(
                            title = "ปริมาณการใช้เชื้อเพลิงเฉลี่ย",
                            value = "${statsNumber.format(stats.avgKml)} km/l",
                            bottomLeftLabel = "${statsNumber.format(stats.bestKml)} km/l\nปริมาณการใช้เชื้อเพลิงที่ดีที่สุด",
                            bottomLeftIcon = Icons.Filled.ThumbUp,
                            bottomLeftIconTint = Color(0xFF4CAF50),
                            bottomRightLabel = "${statsNumber.format(stats.worstKml)} km/l\nปริมาณการใช้เชื้อเพลิงที่เลวร้ายที่สุด",
                            bottomRightIcon = Icons.Filled.ThumbDown,
                            bottomRightIconTint = Color(0xFFF44336)
                        )
                    }
                }
                1 -> {
                    // Costs Tab
                    item { HeroCard("Costs") }
                    item {
                        MainValueCard(
                            title = "ค่าใช้จ่าย",
                            value = statsCurrency.format(stats.totalCost),
                            thisYear = "${statsCurrency.format(stats.costThisYear)}\nปีนี้",
                            prevYear = "${statsCurrency.format(stats.costPrevYear)}\nปีก่อนหน้านี้",
                            thisMonth = "${statsCurrency.format(stats.costThisMonth)}\nเดือนนี้",
                            prevMonth = "${statsCurrency.format(stats.costPrevMonth)}\nเดือนก่อนหน้า",
                            icon = Icons.Filled.TrendingUp,
                            iconTint = Color(0xFF4CAF50)
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                MinMaxCard(
                                    title = "บิล",
                                    minVal = statsCurrency.format(stats.minBill),
                                    minLabel = "รายจ่ายต่ำสุด",
                                    maxVal = statsCurrency.format(stats.maxBill),
                                    maxLabel = "รายจ่ายสูงสุด",
                                    iconMin = Icons.Filled.MonetizationOn,
                                    iconMinTint = Color(0xFF4CAF50),
                                    iconMax = Icons.Filled.MonetizationOn,
                                    iconMaxTint = Color(0xFFF44336)
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MinMaxCard(
                                    title = "ราคาเชื้อเพลิง",
                                    minVal = statsCurrency.format(stats.bestPricePerLiter),
                                    minLabel = "ราคาที่ดีที่สุด",
                                    maxVal = statsCurrency.format(stats.worstPricePerLiter),
                                    maxLabel = "ราคาที่เลวร้ายที่สุด",
                                    iconMin = Icons.Filled.LocalGasStation,
                                    iconMinTint = Color(0xFF4CAF50),
                                    iconMax = Icons.Filled.LocalGasStation,
                                    iconMaxTint = Color(0xFFF44336)
                                )
                            }
                        }
                    }
                    item {
                        MainValueCard(
                            title = "รายจ่ายเฉลี่ยต่อกิโลเมตร",
                            value = "${statsNumber.format(stats.avgCostPerKm)}/km",
                            bottomLeftLabel = "${statsCurrency.format(stats.bestCostPerKm)}/km\nรายจ่ายที่ดีที่สุดต่อกิโลเมตร",
                            bottomLeftIcon = Icons.Filled.MonetizationOn,
                            bottomLeftIconTint = Color(0xFF4CAF50),
                            bottomRightLabel = "${statsCurrency.format(stats.worstCostPerKm)}/km\nรายจ่ายที่เลวร้ายที่สุดต่อกิโลเมตร",
                            bottomRightIcon = Icons.Filled.MonetizationOn,
                            bottomRightIconTint = Color(0xFFF44336)
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard("ค่าใช้จ่ายเฉลี่ยต่อวัน", statsCurrency.format(stats.avgCostPerDay), Icons.Filled.AttachMoney)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard("ค่าใช้จ่ายเฉลี่ยต่อเดือน", statsCurrency.format(stats.avgCostPerMonth), Icons.Filled.AttachMoney)
                            }
                        }
                    }
                }
                2 -> {
                    // Distance Tab
                    item { HeroCard("Distance") }
                    item {
                        MainValueCard(
                            title = "ระยะทางที่ขับด้วย FuelLog",
                            value = "${statsNumber.format(stats.totalDistance)} km",
                            icon = Icons.Filled.DirectionsCar,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        MainValueCard(
                            title = "ค่าการนับ odo สุดท้าย",
                            value = "${statsNumber.format(stats.lastOdo)} km",
                            thisYear = "${statsNumber.format(stats.distanceThisYear)} km\nปีนี้",
                            prevYear = "${statsNumber.format(stats.distancePrevYear)} km\nปีก่อนหน้านี้",
                            thisMonth = "${statsNumber.format(stats.distanceThisMonth)} km\nเดือนนี้",
                            prevMonth = "${statsNumber.format(stats.distancePrevMonth)} km\nเดือนก่อนหน้า",
                            icon = Icons.Filled.Speed
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard("ระยะทางเฉลี่ยต่อวัน", "${statsNumber.format(stats.avgDistancePerDay)} km", Icons.Filled.DirectionsCar)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard("ระยะทางเฉลี่ยต่อเดือน", "${statsNumber.format(stats.avgDistancePerMonth)} km", Icons.Filled.DirectionsCar)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroCard(type: String) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            when (type) {
                "Refills" -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Filled.LocalGasStation, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF4285F4))
                        Icon(Icons.Filled.WaterDrop, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color(0xFFEA4335))
                    }
                }
                "Costs" -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Filled.LocalGasStation, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF4285F4))
                        Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFFEA4335))
                    }
                }
                "Distance" -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF4285F4))
                        Icon(Icons.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFFEA4335))
                    }
                }
            }
        }
    }
}

@Composable
fun MainValueCard(
    title: String,
    value: String,
    thisYear: String? = null,
    prevYear: String? = null,
    thisMonth: String? = null,
    prevMonth: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    bottomLeftLabel: String? = null,
    bottomLeftIcon: ImageVector? = null,
    bottomLeftIconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    bottomRightLabel: String? = null,
    bottomRightIcon: ImageVector? = null,
    bottomRightIconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            if (icon != null && thisYear == null && bottomLeftLabel == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(icon, contentDescription = null, tint = iconTint)
                    Column {
                        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

                if (thisYear != null && prevYear != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (icon != null) Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                                Text(thisYear, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(prevYear, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = if (icon != null) 26.dp else 0.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (icon != null) Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                                Text(thisMonth ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(prevMonth ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = if (icon != null) 26.dp else 0.dp))
                        }
                    }
                }

                if (bottomLeftLabel != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(top = 16.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (bottomLeftIcon != null) Icon(bottomLeftIcon, contentDescription = null, tint = bottomLeftIconTint, modifier = Modifier.size(20.dp))
                                Text(bottomLeftLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (bottomRightLabel != null) {
                            Column(Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (bottomRightIcon != null) Icon(bottomRightIcon, contentDescription = null, tint = bottomRightIconTint, modifier = Modifier.size(20.dp))
                                    Text(bottomRightLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MinMaxCard(
    title: String,
    minVal: String,
    minLabel: String,
    maxVal: String,
    maxLabel: String,
    iconMin: ImageVector,
    iconMinTint: Color,
    iconMax: ImageVector,
    iconMaxTint: Color,
) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(iconMin, contentDescription = null, tint = iconMinTint, modifier = Modifier.size(24.dp))
                Column {
                    Text(minVal, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(minLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(iconMax, contentDescription = null, tint = iconMaxTint, modifier = Modifier.size(24.dp))
                Column {
                    Text(maxVal, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(maxLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun TitleValueCard(title: String, value: String, icon: ImageVector) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}
