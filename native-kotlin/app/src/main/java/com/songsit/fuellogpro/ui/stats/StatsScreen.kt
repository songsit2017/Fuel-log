package com.songsit.fuellogpro.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.PieValueFormatter
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.songsit.fuellogpro.domain.model.Expense
import com.songsit.fuellogpro.domain.model.FuelEntry
import com.songsit.fuellogpro.domain.model.MaintenanceTask
import com.songsit.fuellogpro.ui.NativeAppState
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
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

private val monthLabelFormatter = DateTimeFormatter.ofPattern("MM/yyyy")
private val dayLabelFormatter = DateTimeFormatter.ofPattern("dd/MM")

private fun monthlyCostSeries(entries: List<FuelEntry>): List<Pair<YearMonth, Double>> =
    entries
        .mapNotNull { entry -> runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { YearMonth.from(it) to entry.amount } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, amounts) -> amounts.sum() }
        .toSortedMap()
        .map { it.key to it.value }

// Fuelio's own reports donut caps at a handful of slices before folding the rest into a
// residual bucket — an unbounded number of distinct stations would otherwise need an
// unbounded number of categorical hues, which this app's per-user theme palette doesn't have.
private fun stationCostBreakdown(
    entries: List<FuelEntry>,
    unspecifiedStationLabel: String,
    otherLabel: String,
    maxSlices: Int = 4,
): List<Pair<String, Double>> {
    val byStation = entries
        .groupBy { it.station.ifBlank { unspecifiedStationLabel } }
        .mapValues { (_, list) -> list.sumOf(FuelEntry::amount) }
        .toList()
        .sortedByDescending { it.second }
    if (byStation.size <= maxSlices) return byStation
    val otherTotal = byStation.drop(maxSlices).sumOf { it.second }
    return byStation.take(maxSlices) + (otherLabel to otherTotal)
}

private fun odometerSeries(entries: List<FuelEntry>): List<Pair<LocalDate, Double>> =
    entries
        .mapNotNull { entry -> runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { it to entry.odometerKm } }
        .filter { it.second > 0 }
        .sortedBy { it.first }

// ── Date-range filter (applies to every tab) ────────────────────────────────────────────
enum class StatsDateRangeMode { ALL_TIME, THIS_YEAR, THIS_MONTH, LAST_30_DAYS, CUSTOM }

data class StatsDateRangeSelection(
    val mode: StatsDateRangeMode = StatsDateRangeMode.ALL_TIME,
    val customStart: String = "",
    val customEnd: String = "",
)

private fun effectiveDateRange(selection: StatsDateRangeSelection, today: LocalDate): Pair<LocalDate?, LocalDate?> = when (selection.mode) {
    StatsDateRangeMode.ALL_TIME -> null to null
    StatsDateRangeMode.THIS_YEAR -> LocalDate.of(today.year, 1, 1) to today
    StatsDateRangeMode.THIS_MONTH -> today.withDayOfMonth(1) to today
    StatsDateRangeMode.LAST_30_DAYS -> today.minusDays(29) to today
    StatsDateRangeMode.CUSTOM ->
        runCatching { LocalDate.parse(selection.customStart) }.getOrNull() to
            runCatching { LocalDate.parse(selection.customEnd) }.getOrNull()
}

// Records with an unparseable date are kept rather than silently dropped — a malformed date
// shouldn't make a real record invisible from every report.
private fun <T> List<T>.filterByDateRange(start: LocalDate?, end: LocalDate?, dateOf: (T) -> String): List<T> {
    if (start == null && end == null) return this
    return filter { item ->
        val date = runCatching { LocalDate.parse(dateOf(item)) }.getOrNull() ?: return@filter true
        (start == null || !date.isBefore(start)) && (end == null || !date.isAfter(end))
    }
}

// ── General / Income / Service tabs (Fuelio-parity backlog: the "รายรับ"/"บริการ" money flows
// live on Expense records, not FuelEntry — MaintenanceTask itself carries no cost, it's only a
// due-date/odometer reminder, so "Service" spend reads from Expense rows in these categories). ──
private val serviceExpenseCategories = setOf("บริการ", "บำรุงรักษา")

data class GeneralStats(
    val totalRecords: Int,
    val totalCost: Double,
    val totalIncome: Double,
    val netCost: Double,
    val totalDistance: Double,
    val avgCostPerDay: Double,
    val avgCostPerMonth: Double,
)

private fun computeGeneralStats(entries: List<FuelEntry>, expenses: List<Expense>): GeneralStats {
    val fuelCost = entries.sumOf { it.amount }
    val expenseCost = expenses.filterNot(Expense::income).sumOf { it.amount }
    val income = expenses.filter(Expense::income).sumOf { it.amount }
    val totalCost = fuelCost + expenseCost
    val dates = entries.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() } +
        expenses.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
    val daysBetween = if (dates.isEmpty()) 1L else max(1L, ChronoUnit.DAYS.between(dates.min(), dates.max()))
    val monthsBetween = if (dates.isEmpty()) {
        1L
    } else {
        max(1L, ChronoUnit.MONTHS.between(dates.min().withDayOfMonth(1), dates.max().withDayOfMonth(1)) + 1)
    }
    val sortedOdo = entries.filter { it.odometerKm > 0 }.sortedBy { "${it.date} ${it.time}" }
    val distance = (sortedOdo.lastOrNull()?.odometerKm ?: 0.0) - (sortedOdo.firstOrNull()?.odometerKm ?: 0.0)
    return GeneralStats(
        totalRecords = entries.size + expenses.size,
        totalCost = totalCost,
        totalIncome = income,
        netCost = totalCost - income,
        totalDistance = if (distance > 0) distance else 0.0,
        avgCostPerDay = totalCost / daysBetween,
        avgCostPerMonth = totalCost / monthsBetween,
    )
}

data class IncomeStats(
    val count: Int,
    val totalIncome: Double,
    val incomeThisYear: Double,
    val incomeThisMonth: Double,
    val incomePrevYear: Double,
    val incomePrevMonth: Double,
    val minIncome: Double,
    val maxIncome: Double,
    val avgIncomePerDay: Double,
    val avgIncomePerMonth: Double,
)

private fun computeIncomeStats(expenses: List<Expense>): IncomeStats {
    val incomeEntries = expenses.filter(Expense::income)
    if (incomeEntries.isEmpty()) return IncomeStats(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

    val today = LocalDate.now()
    val thisYear = today.year
    val thisMonth = YearMonth.now()
    val prevYear = thisYear - 1
    val prevMonth = thisMonth.minusMonths(1)
    var incomeThisYear = 0.0
    var incomeThisMonth = 0.0
    var incomePrevYear = 0.0
    var incomePrevMonth = 0.0
    var total = 0.0
    var minIncome = Double.MAX_VALUE
    var maxIncome = 0.0
    val dates = mutableListOf<LocalDate>()
    incomeEntries.forEach { expense ->
        total += expense.amount
        minIncome = minOf(minIncome, expense.amount)
        maxIncome = maxOf(maxIncome, expense.amount)
        val date = runCatching { LocalDate.parse(expense.date) }.getOrNull()
        if (date != null) {
            dates += date
            val ym = YearMonth.from(date)
            if (date.year == thisYear) incomeThisYear += expense.amount
            if (date.year == prevYear) incomePrevYear += expense.amount
            if (ym == thisMonth) incomeThisMonth += expense.amount
            if (ym == prevMonth) incomePrevMonth += expense.amount
        }
    }
    val daysBetween = if (dates.isEmpty()) 1L else max(1L, ChronoUnit.DAYS.between(dates.min(), dates.max()))
    val monthsBetween = if (dates.isEmpty()) {
        1L
    } else {
        max(1L, ChronoUnit.MONTHS.between(dates.min().withDayOfMonth(1), dates.max().withDayOfMonth(1)) + 1)
    }
    return IncomeStats(
        count = incomeEntries.size,
        totalIncome = total,
        incomeThisYear = incomeThisYear,
        incomeThisMonth = incomeThisMonth,
        incomePrevYear = incomePrevYear,
        incomePrevMonth = incomePrevMonth,
        minIncome = if (minIncome == Double.MAX_VALUE) 0.0 else minIncome,
        maxIncome = maxIncome,
        avgIncomePerDay = total / daysBetween,
        avgIncomePerMonth = total / monthsBetween,
    )
}

data class ServiceStats(
    val count: Int,
    val totalCost: Double,
    val costThisYear: Double,
    val costThisMonth: Double,
    val costPrevYear: Double,
    val costPrevMonth: Double,
    val minBill: Double,
    val maxBill: Double,
    val avgCostPerDay: Double,
    val avgCostPerMonth: Double,
)

private fun computeServiceStats(expenses: List<Expense>): ServiceStats {
    val serviceEntries = expenses.filter { !it.income && it.category in serviceExpenseCategories }
    if (serviceEntries.isEmpty()) return ServiceStats(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

    val today = LocalDate.now()
    val thisYear = today.year
    val thisMonth = YearMonth.now()
    val prevYear = thisYear - 1
    val prevMonth = thisMonth.minusMonths(1)
    var costThisYear = 0.0
    var costThisMonth = 0.0
    var costPrevYear = 0.0
    var costPrevMonth = 0.0
    var total = 0.0
    var minBill = Double.MAX_VALUE
    var maxBill = 0.0
    val dates = mutableListOf<LocalDate>()
    serviceEntries.forEach { expense ->
        total += expense.amount
        minBill = minOf(minBill, expense.amount)
        maxBill = maxOf(maxBill, expense.amount)
        val date = runCatching { LocalDate.parse(expense.date) }.getOrNull()
        if (date != null) {
            dates += date
            val ym = YearMonth.from(date)
            if (date.year == thisYear) costThisYear += expense.amount
            if (date.year == prevYear) costPrevYear += expense.amount
            if (ym == thisMonth) costThisMonth += expense.amount
            if (ym == prevMonth) costPrevMonth += expense.amount
        }
    }
    val daysBetween = if (dates.isEmpty()) 1L else max(1L, ChronoUnit.DAYS.between(dates.min(), dates.max()))
    val monthsBetween = if (dates.isEmpty()) {
        1L
    } else {
        max(1L, ChronoUnit.MONTHS.between(dates.min().withDayOfMonth(1), dates.max().withDayOfMonth(1)) + 1)
    }
    return ServiceStats(
        count = serviceEntries.size,
        totalCost = total,
        costThisYear = costThisYear,
        costThisMonth = costThisMonth,
        costPrevYear = costPrevYear,
        costPrevMonth = costPrevMonth,
        minBill = if (minBill == Double.MAX_VALUE) 0.0 else minBill,
        maxBill = maxBill,
        avgCostPerDay = total / daysBetween,
        avgCostPerMonth = total / monthsBetween,
    )
}

@Composable
private fun MonthlyCostChart(monthlyCosts: List<Pair<YearMonth, Double>>) {
    if (monthlyCosts.size < 2) return
    val modelProducer = remember { CartesianChartModelProducer() }
    val labels = remember(monthlyCosts) { monthlyCosts.map { it.first.format(monthLabelFormatter) } }
    LaunchedEffect(monthlyCosts) {
        modelProducer.runTransaction { columnModel { series(monthlyCosts.map { it.second }) } }
    }
    val bottomFormatter = remember(labels) {
        CartesianValueFormatter { _, value, _ -> labels.getOrElse(value.toInt()) { "" } }
    }
    val startFormatter = remember { CartesianValueFormatter.decimal(decimalCount = 0, thousandsSeparator = ",", prefix = "฿") }
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(com.songsit.fuellogpro.R.string.stats_monthly_cost_chart_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(valueFormatter = startFormatter),
                    bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomFormatter),
                ),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().height(220.dp),
            )
        }
    }
}

@Composable
private fun StationCostDonutChart(breakdown: List<Pair<String, Double>>, otherLabel: String) {
    if (breakdown.isEmpty()) return
    val modelProducer = remember { PieChartModelProducer() }
    LaunchedEffect(breakdown) {
        modelProducer.runTransaction { pieSeries { series(breakdown.map { it.second }) } }
    }
    val total = remember(breakdown) { breakdown.sumOf { it.second } }
    // Fixed categorical order (primary/tertiary/secondary/primaryContainer), never cycled, so a
    // station keeps its color across recompositions; the residual "Other" bucket always gets a
    // neutral tone rather than competing for a "real" hue.
    val sliceColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primaryContainer,
    )
    val otherColor = MaterialTheme.colorScheme.outlineVariant
    val colors = remember(breakdown, sliceColors, otherColor) {
        breakdown.mapIndexed { index, (label, _) ->
            if (label == otherLabel) otherColor else sliceColors.getOrElse(index) { otherColor }
        }
    }
    val pieChart = rememberPieChart(
        sliceProvider = PieChart.SliceProvider.series(colors.map { PieChart.Slice(fill = Fill(it)) }),
        spacing = 2.dp,
        innerSize = PieSize.Inner.fixed(56.dp),
        valueFormatter = PieValueFormatter { _, value, _ -> "${(value / total.toFloat() * 100).toInt()}%" },
    )
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(com.songsit.fuellogpro.R.string.stats_station_chart_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            PieChartHost(chart = pieChart, modelProducer = modelProducer, modifier = Modifier.fillMaxWidth().height(200.dp))
            Spacer(Modifier.height(12.dp))
            breakdown.forEachIndexed { index, (label, amount) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors.getOrElse(index) { otherColor }, CircleShape),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("$label - ${statsCurrency.format(amount)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun OdometerLineChart(odometerPoints: List<Pair<LocalDate, Double>>) {
    if (odometerPoints.size < 2) return
    val modelProducer = remember { CartesianChartModelProducer() }
    val labels = remember(odometerPoints) { odometerPoints.map { it.first.format(dayLabelFormatter) } }
    LaunchedEffect(odometerPoints) {
        modelProducer.runTransaction { lineModel { series(odometerPoints.map { it.second }) } }
    }
    val bottomFormatter = remember(labels) {
        CartesianValueFormatter { _, value, _ -> labels.getOrElse(value.toInt()) { "" } }
    }
    val startFormatter = remember { CartesianValueFormatter.decimal(decimalCount = 0, thousandsSeparator = ",", suffix = " km") }
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(com.songsit.fuellogpro.R.string.stats_distance_chart_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(valueFormatter = startFormatter),
                    bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomFormatter),
                ),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().height(220.dp),
            )
        }
    }
}

@Composable
private fun DateRangeFilterChip(
    selection: StatsDateRangeSelection,
    onSelectionChange: (StatsDateRangeSelection) -> Unit,
    recordCount: Int,
    rangeStart: LocalDate?,
    rangeEnd: LocalDate?,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    val chipLabel = if (selection.mode == StatsDateRangeMode.ALL_TIME) {
        stringResource(com.songsit.fuellogpro.R.string.date_range_chip_all_time, recordCount)
    } else {
        stringResource(
            com.songsit.fuellogpro.R.string.date_range_chip_dated,
            recordCount,
            rangeStart?.toString().orEmpty(),
            rangeEnd?.toString().orEmpty(),
        )
    }
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        AssistChip(
            onClick = { menuExpanded = true },
            label = { Text(chipLabel) },
            leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(com.songsit.fuellogpro.R.string.date_range_all_time)) },
                onClick = { onSelectionChange(StatsDateRangeSelection(StatsDateRangeMode.ALL_TIME)); menuExpanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(com.songsit.fuellogpro.R.string.date_range_this_year)) },
                onClick = { onSelectionChange(StatsDateRangeSelection(StatsDateRangeMode.THIS_YEAR)); menuExpanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(com.songsit.fuellogpro.R.string.date_range_this_month)) },
                onClick = { onSelectionChange(StatsDateRangeSelection(StatsDateRangeMode.THIS_MONTH)); menuExpanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(com.songsit.fuellogpro.R.string.date_range_last_30_days)) },
                onClick = { onSelectionChange(StatsDateRangeSelection(StatsDateRangeMode.LAST_30_DAYS)); menuExpanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(com.songsit.fuellogpro.R.string.date_range_custom)) },
                onClick = { menuExpanded = false; showCustomDialog = true },
            )
        }
    }
    if (showCustomDialog) {
        var startText by remember { mutableStateOf(selection.customStart.ifBlank { LocalDate.now().minusMonths(1).toString() }) }
        var endText by remember { mutableStateOf(selection.customEnd.ifBlank { LocalDate.now().toString() }) }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text(stringResource(com.songsit.fuellogpro.R.string.date_range_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        startText,
                        { startText = it },
                        label = { Text(stringResource(com.songsit.fuellogpro.R.string.date_range_start_date)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        endText,
                        { endText = it },
                        label = { Text(stringResource(com.songsit.fuellogpro.R.string.date_range_end_date)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSelectionChange(StatsDateRangeSelection(StatsDateRangeMode.CUSTOM, startText, endText))
                    showCustomDialog = false
                }) { Text(stringResource(com.songsit.fuellogpro.R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text(stringResource(com.songsit.fuellogpro.R.string.action_cancel)) }
            },
        )
    }
}

@Composable
fun StatsScreen(state: NativeAppState, modifier: Modifier = Modifier) {
    var dateRangeSelection by remember { mutableStateOf(StatsDateRangeSelection()) }
    val today = remember { LocalDate.now() }
    val (rangeStart, rangeEnd) = remember(dateRangeSelection, today) { effectiveDateRange(dateRangeSelection, today) }

    val filteredEntries = remember(state.entries, rangeStart, rangeEnd) {
        state.entries.filterByDateRange(rangeStart, rangeEnd, FuelEntry::date)
    }
    val filteredExpenses = remember(state.expenses, rangeStart, rangeEnd) {
        state.expenses.filterByDateRange(rangeStart, rangeEnd, Expense::date)
    }

    val stats = remember(filteredEntries) { computeFuelioStats(filteredEntries) }
    val generalStats = remember(filteredEntries, filteredExpenses) { computeGeneralStats(filteredEntries, filteredExpenses) }
    val incomeStats = remember(filteredExpenses) { computeIncomeStats(filteredExpenses) }
    val serviceStats = remember(filteredExpenses) { computeServiceStats(filteredExpenses) }
    val monthlyCosts = remember(filteredEntries) { monthlyCostSeries(filteredEntries) }
    val unspecifiedStationLabel = stringResource(com.songsit.fuellogpro.R.string.stats_unspecified_station)
    val otherLabel = stringResource(com.songsit.fuellogpro.R.string.stats_other)
    val stationBreakdown = remember(filteredEntries, unspecifiedStationLabel, otherLabel) {
        stationCostBreakdown(filteredEntries, unspecifiedStationLabel, otherLabel)
    }
    val odometerPoints = remember(filteredEntries) { odometerSeries(filteredEntries) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = stringArrayResource(com.songsit.fuellogpro.R.array.stats_tab_titles)
    val thisYearLabel = stringResource(com.songsit.fuellogpro.R.string.stats_this_year)
    val prevYearLabel = stringResource(com.songsit.fuellogpro.R.string.stats_prev_year)
    val thisMonthLabel = stringResource(com.songsit.fuellogpro.R.string.stats_this_month)
    val prevMonthLabel = stringResource(com.songsit.fuellogpro.R.string.stats_prev_month)

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
        DateRangeFilterChip(
            selection = dateRangeSelection,
            onSelectionChange = { dateRangeSelection = it },
            recordCount = filteredEntries.size + filteredExpenses.size,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
        )

        ProvideVicoTheme(rememberM3VicoTheme()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // General Tab
                    item { HeroCard("General") }
                    item {
                        MainValueCard(
                            title = stringResource(com.songsit.fuellogpro.R.string.dashboard_record_count),
                            value = "${generalStats.totalRecords}",
                            icon = Icons.Filled.ListAlt,
                        )
                    }
                    item {
                        MainValueCard(
                            title = stringResource(com.songsit.fuellogpro.R.string.dashboard_total_cost),
                            value = statsCurrency.format(generalStats.totalCost),
                            icon = Icons.Filled.Payments,
                            iconTint = Color(0xFFF44336),
                        )
                    }
                    item {
                        MainValueCard(
                            title = stringResource(com.songsit.fuellogpro.R.string.dashboard_net_cost),
                            value = statsCurrency.format(generalStats.netCost),
                            icon = Icons.Filled.AccountBalanceWallet,
                            iconTint = Color(0xFF4CAF50),
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard(
                                    stringResource(com.songsit.fuellogpro.R.string.dashboard_cumulative_distance),
                                    "${statsNumber.format(generalStats.totalDistance)} km",
                                    Icons.Filled.Route,
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard(
                                    stringResource(com.songsit.fuellogpro.R.string.stats_income),
                                    statsCurrency.format(generalStats.totalIncome),
                                    Icons.Filled.Savings,
                                )
                            }
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard(stringResource(com.songsit.fuellogpro.R.string.stats_avg_cost_per_day), statsCurrency.format(generalStats.avgCostPerDay), Icons.Filled.AttachMoney)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard(stringResource(com.songsit.fuellogpro.R.string.stats_avg_cost_per_month), statsCurrency.format(generalStats.avgCostPerMonth), Icons.Filled.AttachMoney)
                            }
                        }
                    }
                }
                1 -> {
                    // Refills Tab
                    item { HeroCard("Refills") }
                    item {
                        MainValueCard(
                            title = stringResource(com.songsit.fuellogpro.R.string.stats_refills),
                            value = "${stats.totalRefills}",
                            thisYear = "${stats.refillsThisYear} $thisYearLabel",
                            prevYear = "${stats.refillsPrevYear} $prevYearLabel",
                            thisMonth = "${stats.refillsThisMonth} $thisMonthLabel",
                            prevMonth = "${stats.refillsPrevMonth} $prevMonthLabel",
                            icon = Icons.Filled.LocalGasStation
                        )
                    }
                    item {
                        MainValueCard(
                            title = stringResource(com.songsit.fuellogpro.R.string.stats_fuel),
                            value = "${statsNumber.format(stats.totalLiters)} L",
                            thisYear = "${statsNumber.format(stats.litersThisYear)} L\n$thisYearLabel",
                            prevYear = "${statsNumber.format(stats.litersPrevYear)} L\n$prevYearLabel",
                            thisMonth = "${statsNumber.format(stats.litersThisMonth)} L\n$thisMonthLabel",
                            prevMonth = "${statsNumber.format(stats.litersPrevMonth)} L\n$prevMonthLabel",
                            icon = Icons.Filled.WaterDrop,
                            bottomLeftLabel = "${statsNumber.format(stats.minLiters)} L\n${stringResource(com.songsit.fuellogpro.R.string.stats_min_refill)}",
                            bottomLeftIcon = Icons.Filled.ArrowDownward,
                            bottomRightLabel = "${statsNumber.format(stats.maxLiters)} L\n${stringResource(com.songsit.fuellogpro.R.string.stats_max_refill)}",
                            bottomRightIcon = Icons.Filled.ArrowUpward
                        )
                    }
                    item {
                        MainValueCard(
                            title = stringResource(com.songsit.fuellogpro.R.string.stats_avg_fuel_efficiency),
                            value = "${statsNumber.format(stats.avgKml)} km/l",
                            bottomLeftLabel = "${statsNumber.format(stats.bestKml)} km/l\n${stringResource(com.songsit.fuellogpro.R.string.stats_best_fuel_efficiency)}",
                            bottomLeftIcon = Icons.Filled.ThumbUp,
                            bottomLeftIconTint = Color(0xFF4CAF50),
                            bottomRightLabel = "${statsNumber.format(stats.worstKml)} km/l\n${stringResource(com.songsit.fuellogpro.R.string.stats_worst_fuel_efficiency)}",
                            bottomRightIcon = Icons.Filled.ThumbDown,
                            bottomRightIconTint = Color(0xFFF44336)
                        )
                    }
                }
                2 -> {
                    // Costs Tab
                    item { HeroCard("Costs") }
                    item {
                        MainValueCard(
                            title = stringResource(com.songsit.fuellogpro.R.string.stats_costs),
                            value = statsCurrency.format(stats.totalCost),
                            thisYear = "${statsCurrency.format(stats.costThisYear)}\n$thisYearLabel",
                            prevYear = "${statsCurrency.format(stats.costPrevYear)}\n$prevYearLabel",
                            thisMonth = "${statsCurrency.format(stats.costThisMonth)}\n$thisMonthLabel",
                            prevMonth = "${statsCurrency.format(stats.costPrevMonth)}\n$prevMonthLabel",
                            icon = Icons.Filled.TrendingUp,
                            iconTint = Color(0xFF4CAF50)
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                MinMaxCard(
                                    title = stringResource(com.songsit.fuellogpro.R.string.stats_bill),
                                    minVal = statsCurrency.format(stats.minBill),
                                    minLabel = stringResource(com.songsit.fuellogpro.R.string.stats_min_expense),
                                    maxVal = statsCurrency.format(stats.maxBill),
                                    maxLabel = stringResource(com.songsit.fuellogpro.R.string.stats_max_expense),
                                    iconMin = Icons.Filled.MonetizationOn,
                                    iconMinTint = Color(0xFF4CAF50),
                                    iconMax = Icons.Filled.MonetizationOn,
                                    iconMaxTint = Color(0xFFF44336)
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MinMaxCard(
                                    title = stringResource(com.songsit.fuellogpro.R.string.stats_fuel_price),
                                    minVal = statsCurrency.format(stats.bestPricePerLiter),
                                    minLabel = stringResource(com.songsit.fuellogpro.R.string.stats_best_price),
                                    maxVal = statsCurrency.format(stats.worstPricePerLiter),
                                    maxLabel = stringResource(com.songsit.fuellogpro.R.string.stats_worst_price),
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
                            title = stringResource(com.songsit.fuellogpro.R.string.stats_avg_cost_per_km),
                            value = "${statsNumber.format(stats.avgCostPerKm)}/km",
                            bottomLeftLabel = "${statsCurrency.format(stats.bestCostPerKm)}/km\n${stringResource(com.songsit.fuellogpro.R.string.stats_best_cost_per_km)}",
                            bottomLeftIcon = Icons.Filled.MonetizationOn,
                            bottomLeftIconTint = Color(0xFF4CAF50),
                            bottomRightLabel = "${statsCurrency.format(stats.worstCostPerKm)}/km\n${stringResource(com.songsit.fuellogpro.R.string.stats_worst_cost_per_km)}",
                            bottomRightIcon = Icons.Filled.MonetizationOn,
                            bottomRightIconTint = Color(0xFFF44336)
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard(stringResource(com.songsit.fuellogpro.R.string.stats_avg_cost_per_day), statsCurrency.format(stats.avgCostPerDay), Icons.Filled.AttachMoney)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard(stringResource(com.songsit.fuellogpro.R.string.stats_avg_cost_per_month), statsCurrency.format(stats.avgCostPerMonth), Icons.Filled.AttachMoney)
                            }
                        }
                    }
                    item { MonthlyCostChart(monthlyCosts) }
                    item { StationCostDonutChart(stationBreakdown, otherLabel) }
                }
                3 -> {
                    // Income Tab
                    item { HeroCard("Income") }
                    item {
                        MainValueCard(
                            title = stringResource(com.songsit.fuellogpro.R.string.stats_income),
                            value = statsCurrency.format(incomeStats.totalIncome),
                            thisYear = "${statsCurrency.format(incomeStats.incomeThisYear)}\n$thisYearLabel",
                            prevYear = "${statsCurrency.format(incomeStats.incomePrevYear)}\n$prevYearLabel",
                            thisMonth = "${statsCurrency.format(incomeStats.incomeThisMonth)}\n$thisMonthLabel",
                            prevMonth = "${statsCurrency.format(incomeStats.incomePrevMonth)}\n$prevMonthLabel",
                            icon = Icons.Filled.Savings,
                            iconTint = Color(0xFF4CAF50),
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                MinMaxCard(
                                    title = stringResource(com.songsit.fuellogpro.R.string.stats_income),
                                    minVal = statsCurrency.format(incomeStats.minIncome),
                                    minLabel = stringResource(com.songsit.fuellogpro.R.string.stats_min_income),
                                    maxVal = statsCurrency.format(incomeStats.maxIncome),
                                    maxLabel = stringResource(com.songsit.fuellogpro.R.string.stats_max_income),
                                    iconMin = Icons.Filled.Savings,
                                    iconMinTint = Color(0xFF4CAF50),
                                    iconMax = Icons.Filled.Savings,
                                    iconMaxTint = Color(0xFF4CAF50),
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard(stringResource(com.songsit.fuellogpro.R.string.stats_income_count), "${incomeStats.count}", Icons.Filled.ListAlt)
                            }
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard(stringResource(com.songsit.fuellogpro.R.string.stats_avg_income_per_day), statsCurrency.format(incomeStats.avgIncomePerDay), Icons.Filled.Savings)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard(stringResource(com.songsit.fuellogpro.R.string.stats_avg_income_per_month), statsCurrency.format(incomeStats.avgIncomePerMonth), Icons.Filled.Savings)
                            }
                        }
                    }
                }
                4 -> {
                    // Distance Tab
                    item { HeroCard("Distance") }
                    item {
                        MainValueCard(
                            title = stringResource(com.songsit.fuellogpro.R.string.stats_distance_tracked),
                            value = "${statsNumber.format(stats.totalDistance)} km",
                            icon = Icons.Filled.DirectionsCar,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        MainValueCard(
                            title = stringResource(com.songsit.fuellogpro.R.string.stats_last_odometer),
                            value = "${statsNumber.format(stats.lastOdo)} km",
                            thisYear = "${statsNumber.format(stats.distanceThisYear)} km\n$thisYearLabel",
                            prevYear = "${statsNumber.format(stats.distancePrevYear)} km\n$prevYearLabel",
                            thisMonth = "${statsNumber.format(stats.distanceThisMonth)} km\n$thisMonthLabel",
                            prevMonth = "${statsNumber.format(stats.distancePrevMonth)} km\n$prevMonthLabel",
                            icon = Icons.Filled.Speed
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard(stringResource(com.songsit.fuellogpro.R.string.stats_avg_distance_per_day), "${statsNumber.format(stats.avgDistancePerDay)} km", Icons.Filled.DirectionsCar)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard(stringResource(com.songsit.fuellogpro.R.string.stats_avg_distance_per_month), "${statsNumber.format(stats.avgDistancePerMonth)} km", Icons.Filled.DirectionsCar)
                            }
                        }
                    }
                    item { OdometerLineChart(odometerPoints) }
                }
                5 -> {
                    // Service Tab
                    item { HeroCard("Service") }
                    item {
                        MainValueCard(
                            title = stringResource(com.songsit.fuellogpro.R.string.stats_service),
                            value = statsCurrency.format(serviceStats.totalCost),
                            thisYear = "${statsCurrency.format(serviceStats.costThisYear)}\n$thisYearLabel",
                            prevYear = "${statsCurrency.format(serviceStats.costPrevYear)}\n$prevYearLabel",
                            thisMonth = "${statsCurrency.format(serviceStats.costThisMonth)}\n$thisMonthLabel",
                            prevMonth = "${statsCurrency.format(serviceStats.costPrevMonth)}\n$prevMonthLabel",
                            icon = Icons.Filled.Build,
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                MinMaxCard(
                                    title = stringResource(com.songsit.fuellogpro.R.string.stats_bill),
                                    minVal = statsCurrency.format(serviceStats.minBill),
                                    minLabel = stringResource(com.songsit.fuellogpro.R.string.stats_min_service_bill),
                                    maxVal = statsCurrency.format(serviceStats.maxBill),
                                    maxLabel = stringResource(com.songsit.fuellogpro.R.string.stats_max_service_bill),
                                    iconMin = Icons.Filled.Build,
                                    iconMinTint = Color(0xFF4CAF50),
                                    iconMax = Icons.Filled.Build,
                                    iconMaxTint = Color(0xFFF44336),
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard(stringResource(com.songsit.fuellogpro.R.string.stats_service_count), "${serviceStats.count}", Icons.Filled.ListAlt)
                            }
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard(stringResource(com.songsit.fuellogpro.R.string.stats_avg_cost_per_day), statsCurrency.format(serviceStats.avgCostPerDay), Icons.Filled.AttachMoney)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TitleValueCard(stringResource(com.songsit.fuellogpro.R.string.stats_avg_cost_per_month), statsCurrency.format(serviceStats.avgCostPerMonth), Icons.Filled.AttachMoney)
                            }
                        }
                    }
                    item {
                        TitleValueCard(stringResource(com.songsit.fuellogpro.R.string.stats_upcoming_tasks), "${state.maintenanceTasks.size}", Icons.Filled.Build)
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
                "General" -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Filled.Dashboard, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF4285F4))
                        Icon(Icons.Filled.Insights, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFFEA4335))
                    }
                }
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
                "Income" -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Filled.Savings, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF4285F4))
                        Icon(Icons.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFFEA4335))
                    }
                }
                "Service" -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF4285F4))
                        Icon(Icons.Filled.CarRepair, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFFEA4335))
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
