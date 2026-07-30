package com.songsit.fuellogpro.settings

import android.content.Context

/** Local display and data-entry preferences. Stored values never alter existing records. */
data class DisplaySettings(
    val currency: String = "THB",
    val decimals: Int = 2,
    val distanceUnit: String = "km",
    val volumeUnit: String = "liters",
    val themeMode: String = "system",
    val fontFamily: String = "ubuntu",
    val themePalette: String = "default",
    val defaultFullTank: Boolean = true,
    val confirmBeforeDelete: Boolean = true,
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
        defaultFullTank = preferences.getBoolean(DEFAULT_FULL_TANK, true),
        confirmBeforeDelete = preferences.getBoolean(CONFIRM_BEFORE_DELETE, true),
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
            .putBoolean(DEFAULT_FULL_TANK, settings.defaultFullTank)
            .putBoolean(CONFIRM_BEFORE_DELETE, settings.confirmBeforeDelete)
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
        const val DEFAULT_FULL_TANK = "default-full-tank"
        const val CONFIRM_BEFORE_DELETE = "confirm-before-delete"
    }
}
