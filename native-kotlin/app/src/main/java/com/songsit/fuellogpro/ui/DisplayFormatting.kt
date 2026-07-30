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

// Fuel type and category values are persisted/matched in Thai (see the comments at
// fuelTypeOptions/maintenanceCategoryOptions/expenseCategories in FuelLogApp.kt) — these two
// functions only translate the DISPLAYED label; the stored/compared string never changes, so
// old records keep matching correctly regardless of which language wrote or reads them.
@Composable
fun fuelTypeDisplayLabel(raw: String): String = when (raw) {
    "เบนซิน" -> stringResource(com.songsit.fuellogpro.R.string.fuel_type_gasoline)
    "ดีเซล" -> stringResource(com.songsit.fuellogpro.R.string.fuel_type_diesel)
    "แก๊สโซฮอล์ 95" -> stringResource(com.songsit.fuellogpro.R.string.fuel_grade_gasohol_95)
    "แก๊สโซฮอล์ 91" -> stringResource(com.songsit.fuellogpro.R.string.fuel_grade_gasohol_91)
    "ดีเซล B7" -> stringResource(com.songsit.fuellogpro.R.string.fuel_grade_diesel_b7)
    "ดีเซล B20" -> stringResource(com.songsit.fuellogpro.R.string.fuel_grade_diesel_b20)
    else -> raw // E20, E85, LPG, NGV, EV are already language-neutral
}

@Composable
fun categoryDisplayLabel(raw: String): String = when (raw) {
    "เช็คระยะ" -> stringResource(com.songsit.fuellogpro.R.string.category_scheduled_service)
    "เปลี่ยนถ่ายน้ำมันเครื่อง" -> stringResource(com.songsit.fuellogpro.R.string.category_oil_change)
    "ประกันภัย/พ.ร.บ." -> stringResource(com.songsit.fuellogpro.R.string.category_insurance_ctpl)
    "ภาษี" -> stringResource(com.songsit.fuellogpro.R.string.category_vehicle_tax)
    "บำรุงรักษา" -> stringResource(com.songsit.fuellogpro.R.string.category_maintenance)
    "อื่นๆ" -> stringResource(com.songsit.fuellogpro.R.string.expense_category_other)
    "บริการ" -> stringResource(com.songsit.fuellogpro.R.string.category_service)
    "ทะเบียน" -> stringResource(com.songsit.fuellogpro.R.string.category_registration)
    "ที่จอดรถ" -> stringResource(com.songsit.fuellogpro.R.string.category_parking)
    "ล้างรถ" -> stringResource(com.songsit.fuellogpro.R.string.category_car_wash)
    "ทางด่วน" -> stringResource(com.songsit.fuellogpro.R.string.category_toll)
    "ตั๋ว/ค่าปรับ" -> stringResource(com.songsit.fuellogpro.R.string.category_tickets_fines)
    "ปรับแต่ง" -> stringResource(com.songsit.fuellogpro.R.string.category_modifications)
    "การประกันภัย" -> stringResource(com.songsit.fuellogpro.R.string.category_insurance)
    else -> raw // blank/custom user-typed category text passes through unchanged
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
