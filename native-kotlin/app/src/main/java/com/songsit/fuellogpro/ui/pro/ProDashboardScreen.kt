package com.songsit.fuellogpro.ui.pro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songsit.fuellogpro.R
import com.songsit.fuellogpro.data.OilPriceInfo
import com.songsit.fuellogpro.domain.calculatePerEntryKmPerLiter
import com.songsit.fuellogpro.domain.model.FuelEntry
import com.songsit.fuellogpro.ui.EmptyFuelState
import com.songsit.fuellogpro.ui.FuelRow
import com.songsit.fuellogpro.ui.LocalDisplaySettings
import com.songsit.fuellogpro.ui.MetricCard
import com.songsit.fuellogpro.ui.NativeAppState
import com.songsit.fuellogpro.ui.OilPriceCard
import com.songsit.fuellogpro.ui.formatCurrencyAmount
import com.songsit.fuellogpro.ui.formatDistanceKm
import com.songsit.fuellogpro.ui.formatEconomyKmPerLiter
import com.songsit.fuellogpro.ui.formatVolumeLiters
import java.time.LocalDate
import java.time.YearMonth

/**
 * Dashboard for the "Pro" theme (FuelLog Pro Redesign concept) — the one screen with no real
 * equivalent to reuse, since the default Dashboard() in FuelLogApp.kt doesn't have a hero gauge.
 * Every number below reads the same [NativeAppState]/[OilPriceInfo] the default Dashboard uses;
 * nothing here is mock data. Secondary cards (oil prices, recent entries) reuse the existing
 * OilPriceCard/MetricCard/FuelRow/EmptyFuelState composables so they inherit the Pro palette
 * automatically instead of being re-styled by hand.
 */
@Composable
fun ProDashboardScreen(
    state: NativeAppState,
    oilPriceInfo: OilPriceInfo?,
    modifier: Modifier = Modifier,
    onImageClick: ((List<String>, Int) -> Unit)? = null,
) {
    val displaySettings = LocalDisplaySettings.current
    val kmPerLiterByEntry = remember(state.entries) { calculatePerEntryKmPerLiter(state.entries) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (state.selectedVehicle == null) {
            item { EmptyFuelState() }
            return@LazyColumn
        }
        item {
            EfficiencyGaugeCard(
                valueLabel = state.summary.averageKmPerLiter?.let { formatEconomyKmPerLiter(it, displaySettings) },
                distanceThisMonthLabel = distanceThisMonth(state.entries)?.let { formatDistanceKm(it, displaySettings) },
                trendPercent = efficiencyTrendPercent(state.entries, kmPerLiterByEntry),
            )
        }
        if (oilPriceInfo != null && oilPriceInfo.brands.isNotEmpty()) {
            item { OilPriceCard(oilPriceInfo) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    stringResource(R.string.dashboard_fuel_cost),
                    formatCurrencyAmount(state.summary.totalSpent, displaySettings),
                    Modifier.weight(1f),
                    Icons.Filled.LocalGasStation,
                )
                MetricCard(
                    stringResource(R.string.dashboard_fuel_volume),
                    formatVolumeLiters(state.summary.totalLiters, displaySettings),
                    Modifier.weight(1f),
                )
            }
        }
        item {
            val operatingCost = state.summary.totalSpent + state.totalExpenses + state.tripSummary.totalCost
            val noDataLabel = stringResource(R.string.dashboard_no_data)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 3.dp,
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.dashboard_report_summary),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    SummaryRow(
                        stringResource(R.string.dashboard_total_cost),
                        formatCurrencyAmount(operatingCost, displaySettings),
                        stringResource(R.string.dashboard_net_cost),
                        formatCurrencyAmount(operatingCost - state.totalIncome, displaySettings),
                    )
                    HorizontalDivider()
                    SummaryRow(
                        stringResource(R.string.dashboard_cumulative_distance),
                        formatDistanceKm(
                            (state.entries.maxOfOrNull { it.odometerKm } ?: 0.0) - (state.entries.minOfOrNull { it.odometerKm } ?: 0.0),
                            displaySettings,
                        ),
                        stringResource(R.string.dashboard_latest_odometer),
                        state.summary.latestOdometerKm?.let { formatDistanceKm(it, displaySettings) } ?: noDataLabel,
                    )
                }
            }
        }
        item {
            Text(
                stringResource(R.string.dashboard_recent_entries),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        if (state.entries.isEmpty()) {
            item { EmptyFuelState() }
        } else {
            items(state.entries.take(3), key = { it.id }) {
                FuelRow(it, kmPerLiter = kmPerLiterByEntry[it.id], onImageClick = onImageClick)
            }
        }
    }
}

@Composable
private fun SummaryRow(label1: String, value1: String, label2: String, value2: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value1, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(label2, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value2, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// This month's distance = odometer spread among this calendar month's fill-ups. Needs at least
// 2 entries in the month to mean anything (a single fill-up has no "distance since" within the
// month); returns null rather than a misleading 0 km otherwise.
private fun distanceThisMonth(entries: List<FuelEntry>): Double? {
    val now = YearMonth.now()
    val monthEntries = entries.filter { runCatching { YearMonth.from(LocalDate.parse(it.date)) }.getOrNull() == now }
    if (monthEntries.size < 2) return null
    val spread = monthEntries.maxOf { it.odometerKm } - monthEntries.minOf { it.odometerKm }
    return spread.takeIf { it > 0 }
}

// Real month-over-month efficiency trend (not the mockup's illustrative fixed "+3%") — the
// average of each fill-up's own km/L (calculatePerEntryKmPerLiter) within this month vs last
// month. Returns null (badge omitted) unless both months have at least one usable data point,
// rather than showing a fabricated trend.
private fun efficiencyTrendPercent(
    entries: List<FuelEntry>,
    kmPerLiterByEntry: Map<String, Double>,
): Double? {
    fun averageFor(month: YearMonth): Double? {
        val values = entries
            .filter { runCatching { YearMonth.from(LocalDate.parse(it.date)) }.getOrNull() == month }
            .mapNotNull { kmPerLiterByEntry[it.id] }
        return values.takeIf { it.isNotEmpty() }?.average()
    }
    val now = YearMonth.now()
    val current = averageFor(now) ?: return null
    val previous = averageFor(now.minusMonths(1))?.takeIf { it > 0 } ?: return null
    return (current - previous) / previous * 100
}

// Hero card — the one visual centerpiece from the mockup with no existing equivalent. Layout
// mirrors the mockup's compact horizontal card (small ring + stats beside it, not a big empty
// circle) rather than the earlier draft, and Surface (not Card) gives a real soft drop shadow
// without M3's default tonal-tint color contamination (see FuelLogTheme.kt's ProLight/ProDark
// comment on onXContainer). The ring's filled sweep (250° of 360°) is decorative, matching the
// mockup exactly, not a literal percentage — there's no natural 0-100% scale for km/L.
@Composable
private fun EfficiencyGaugeCard(valueLabel: String?, distanceThisMonthLabel: String?, trendPercent: Double?) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center) {
                val trackColor = MaterialTheme.colorScheme.outlineVariant
                val sweepColor = MaterialTheme.colorScheme.primary
                Canvas(Modifier.size(88.dp)) {
                    val stroke = Stroke(width = 9.dp.toPx())
                    val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                    val topLeft = Offset(stroke.width / 2, stroke.width / 2)
                    drawArc(trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke, topLeft = topLeft, size = arcSize)
                    drawArc(sweepColor, startAngle = -90f, sweepAngle = 250f, useCenter = false, style = stroke, topLeft = topLeft, size = arcSize)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(valueLabel ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.unit_km_per_liter_short),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.dashboard_avg_efficiency),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                trendPercent?.let { percent ->
                    val improving = percent >= 0
                    val trendColor = if (improving) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    Surface(shape = RoundedCornerShape(20.dp), color = trendColor.copy(alpha = 0.14f)) {
                        Text(
                            stringResource(
                                if (improving) R.string.dashboard_trend_up else R.string.dashboard_trend_down,
                                kotlin.math.abs(percent).let { "%.0f".format(it) },
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = trendColor,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                }
                distanceThisMonthLabel?.let {
                    Text(
                        stringResource(R.string.dashboard_distance_this_month, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
