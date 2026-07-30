package com.songsit.fuellogpro.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.res.stringResource
import com.songsit.fuellogpro.settings.DisplaySettings
import java.text.NumberFormat
import java.util.Locale

/** Currency code -> format locale; the display label is resolved separately via [currencyDisplayLabel]. */
data class CurrencyOption(val code: String, val locale: Locale)

val CURRENCY_OPTIONS: List<CurrencyOption> = listOf(
    CurrencyOption("THB", Locale("th", "TH")),
    CurrencyOption("USD", Locale("en", "US")),
    CurrencyOption("EUR", Locale.GERMANY),
    CurrencyOption("GBP", Locale.UK),
    CurrencyOption("JPY", Locale.JAPAN),
)

@Composable
fun currencyDisplayLabel(code: String): String = when (code) {
    "THB" -> stringResource(com.songsit.fuellogpro.R.string.currency_thb)
    "USD" -> stringResource(com.songsit.fuellogpro.R.string.currency_usd)
    "EUR" -> stringResource(com.songsit.fuellogpro.R.string.currency_eur)
    "GBP" -> stringResource(com.songsit.fuellogpro.R.string.currency_gbp)
    "JPY" -> stringResource(com.songsit.fuellogpro.R.string.currency_jpy)
    else -> code
}

private const val KM_PER_MILE = 0.621371
private const val LITERS_PER_GALLON = 0.264172

/** Provides the active [DisplaySettings] to any composable in the tree without threading params everywhere. */
val LocalDisplaySettings = compositionLocalOf { DisplaySettings() }

fun formatCurrencyAmount(amount: Double, settings: DisplaySettings): String {
    val option = CURRENCY_OPTIONS.firstOrNull { it.code == settings.currency } ?: CURRENCY_OPTIONS[0]
    val format = NumberFormat.getCurrencyInstance(option.locale)
    format.currency = java.util.Currency.getInstance(option.code)
    format.minimumFractionDigits = settings.decimals
    format.maximumFractionDigits = settings.decimals
    return format.format(amount)
}

/** [km] is always the value stored on disk; converts for display only when the user prefers miles. */
@Composable
fun formatDistanceKm(km: Double, settings: DisplaySettings): String {
    val number = NumberFormat.getNumberInstance(Locale("th", "TH")).apply { maximumFractionDigits = 2 }
    return if (settings.isMiles) {
        "${number.format(km * KM_PER_MILE)} mi"
    } else {
        "${number.format(km)} ${stringResource(com.songsit.fuellogpro.R.string.unit_km_short)}"
    }
}

/** [liters] is always the value stored on disk; converts for display only when the user prefers gallons. */
@Composable
fun formatVolumeLiters(liters: Double, settings: DisplaySettings): String {
    val number = NumberFormat.getNumberInstance(Locale("th", "TH")).apply { maximumFractionDigits = 2 }
    return if (settings.isGallons) {
        "${number.format(liters * LITERS_PER_GALLON)} gal"
    } else {
        "${number.format(liters)} ${stringResource(com.songsit.fuellogpro.R.string.unit_liters_short)}"
    }
}

/** Fuel economy: stored as km/L; shown as mi/gal when both display units are imperial-ish, otherwise km/L. */
@Composable
fun formatEconomyKmPerLiter(kmPerLiter: Double, settings: DisplaySettings): String {
    val number = NumberFormat.getNumberInstance(Locale("th", "TH")).apply { maximumFractionDigits = 2 }
    return if (settings.isMiles && settings.isGallons) {
        val milesPerGallon = kmPerLiter * (KM_PER_MILE / LITERS_PER_GALLON)
        "${number.format(milesPerGallon)} mi/gal"
    } else {
        "${number.format(kmPerLiter)} ${stringResource(com.songsit.fuellogpro.R.string.unit_km_per_liter_short)}"
    }
}
