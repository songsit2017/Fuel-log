package com.songsit.fuellogpro.ui

import androidx.compose.runtime.compositionLocalOf
import com.songsit.fuellogpro.settings.DisplaySettings
import java.text.NumberFormat
import java.util.Locale

/** Currency code -> (display symbol/label, format locale). Mirrors modules/settings.js CURRENCIES. */
data class CurrencyOption(val code: String, val label: String, val locale: Locale)

val CURRENCY_OPTIONS: List<CurrencyOption> = listOf(
    CurrencyOption("THB", "บาทไทย (THB)", Locale("th", "TH")),
    CurrencyOption("USD", "ดอลลาร์สหรัฐ (USD)", Locale("en", "US")),
    CurrencyOption("EUR", "ยูโร (EUR)", Locale.GERMANY),
    CurrencyOption("GBP", "ปอนด์อังกฤษ (GBP)", Locale.UK),
    CurrencyOption("JPY", "เยนญี่ปุ่น (JPY)", Locale.JAPAN),
)

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
fun formatDistanceKm(km: Double, settings: DisplaySettings): String {
    val number = NumberFormat.getNumberInstance(Locale("th", "TH")).apply { maximumFractionDigits = 2 }
    return if (settings.isMiles) {
        "${number.format(km * KM_PER_MILE)} mi"
    } else {
        "${number.format(km)} กม."
    }
}

/** [liters] is always the value stored on disk; converts for display only when the user prefers gallons. */
fun formatVolumeLiters(liters: Double, settings: DisplaySettings): String {
    val number = NumberFormat.getNumberInstance(Locale("th", "TH")).apply { maximumFractionDigits = 2 }
    return if (settings.isGallons) {
        "${number.format(liters * LITERS_PER_GALLON)} gal"
    } else {
        "${number.format(liters)} ลิตร"
    }
}

/** Fuel economy: stored as km/L; shown as mi/gal when both display units are imperial-ish, otherwise km/L. */
fun formatEconomyKmPerLiter(kmPerLiter: Double, settings: DisplaySettings): String {
    val number = NumberFormat.getNumberInstance(Locale("th", "TH")).apply { maximumFractionDigits = 2 }
    return if (settings.isMiles && settings.isGallons) {
        val milesPerGallon = kmPerLiter * (KM_PER_MILE / LITERS_PER_GALLON)
        "${number.format(milesPerGallon)} mi/gal"
    } else {
        "${number.format(kmPerLiter)} กม./ลิตร"
    }
}
