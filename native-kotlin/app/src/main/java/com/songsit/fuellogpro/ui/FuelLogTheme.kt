package com.songsit.fuellogpro.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FuelOrange = Color(0xFFFFA726)
private val LightColors = lightColorScheme(
    primary = Color(0xFF171717),
    onPrimary = Color.White,
    secondary = FuelOrange,
    surface = Color(0xFFFFFBFF),
    surfaceContainer = Color(0xFFF4F1F4),
)
private val DarkColors = darkColorScheme(
    primary = FuelOrange,
    secondary = FuelOrange,
    surface = Color(0xFF121316),
    surfaceContainer = Color(0xFF1C1D21),
)

/**
 * [themeMode] mirrors the web app's theme setting: "light", "dark", or "system" (follow the
 * device setting). Any other/unknown value falls back to "system".
 */
@Composable
fun FuelLogTheme(themeMode: String = "system", content: @Composable () -> Unit) {
    val useDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content,
    )
}
