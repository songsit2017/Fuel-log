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
    )

    fun save(settings: DisplaySettings) {
        preferences.edit()
            .putString(CURRENCY, settings.currency)
            .putInt(DECIMALS, settings.decimals)
            .putString(DISTANCE_UNIT, settings.distanceUnit)
            .putString(VOLUME_UNIT, settings.volumeUnit)
            .putString(THEME_MODE, settings.themeMode)
            .apply()
    }

    private companion object {
        const val CURRENCY = "currency"
        const val DECIMALS = "decimals"
        const val DISTANCE_UNIT = "distance-unit"
        const val VOLUME_UNIT = "volume-unit"
        const val THEME_MODE = "theme-mode"
    }
}
