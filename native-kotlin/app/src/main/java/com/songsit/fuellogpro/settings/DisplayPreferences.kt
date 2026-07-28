package com.songsit.fuellogpro.settings

import android.content.Context

/**
 * Ported from the web app's display-settings panel (see modules/settings.js in the repo root):
 * currency, decimal places, distance/volume unit, and theme mode. Values are stored locally
 * only — they affect formatting/appearance, never the underlying stored numbers (always km/L).
 */
data class DisplaySettings(
    val currency: String = "THB",
    val decimals: Int = 2,
    val distanceUnit: String = "km",
    val volumeUnit: String = "liters",
    val themeMode: String = "system",
    val fontFamily: String = "ubuntu",
    val themePalette: String = "default",
) {
    val isMiles: Boolean get() = distanceUnit == "mi"
    val isGallons: Boolean get() = volumeUnit == "gal"
}

class DisplayPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "native-display-settings",
        Context.MODE_PRIVATE,
    )

    fun load(): DisplaySettings = DisplaySettings(
        currency = preferences.getString(CURRENCY, null) ?: "THB",
        decimals = preferences.getInt(DECIMALS, 2).coerceIn(0, 3),
        distanceUnit = preferences.getString(DISTANCE_UNIT, null) ?: "km",
        volumeUnit = preferences.getString(VOLUME_UNIT, null) ?: "liters",
        themeMode = preferences.getString(THEME_MODE, null) ?: "system",
        fontFamily = preferences.getString(FONT_FAMILY, null) ?: "ubuntu",
        themePalette = preferences.getString(THEME_PALETTE, null) ?: "default",
    )

    fun save(settings: DisplaySettings) {
        preferences.edit()
            .putString(CURRENCY, settings.currency)
            .putInt(DECIMALS, settings.decimals)
            .putString(DISTANCE_UNIT, settings.distanceUnit)
            .putString(VOLUME_UNIT, settings.volumeUnit)
            .putString(THEME_MODE, settings.themeMode)
            .putString(FONT_FAMILY, settings.fontFamily)
            .putString(THEME_PALETTE, settings.themePalette)
            .apply()
    }

    private companion object {
        const val CURRENCY = "currency"
        const val DECIMALS = "decimals"
        const val DISTANCE_UNIT = "distance-unit"
        const val VOLUME_UNIT = "volume-unit"
        const val THEME_MODE = "theme-mode"
        const val FONT_FAMILY = "font-family"
        const val THEME_PALETTE = "theme-palette"
    }
}
