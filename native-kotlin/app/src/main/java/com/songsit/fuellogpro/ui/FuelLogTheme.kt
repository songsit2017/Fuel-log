package com.songsit.fuellogpro.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette: matches the app launcher icon (fuellog_icon.xml / colors.xml
// ic_launcher_background) — lavender background with a deep-plum ink accent — applied
// app-wide so every screen's cards/badges/nav share one identity instead of the default
// M3 gray. Deliberately not Fuelio's navy-blue branding (that decision predates this pass);
// Fuelio is used as a reference for layout/organization, not for hue.
private val FuelOrange = Color(0xFFFFA726)
private val DeepPlum = Color(0xFF241B33)
private val PlumAccent = Color(0xFF8E4585)
private val Lavender = Color(0xFFF3E9F7)

private val LightColors = lightColorScheme(
    primary = DeepPlum,
    onPrimary = Lavender,
    primaryContainer = Color(0xFFE4D3EC),
    onPrimaryContainer = DeepPlum,
    secondary = PlumAccent,
    onSecondary = Color.White,
    tertiary = FuelOrange,
    background = Color(0xFFFBF7FC),
    surface = Color(0xFFFBF7FC),
    surfaceContainer = Lavender,
    surfaceContainerHigh = Color(0xFFEADCF0),
    surfaceContainerLow = Color(0xFFF8F1FA),
    onSurface = DeepPlum,
    onSurfaceVariant = Color(0xFF4A3B57),
)
// True AMOLED black — background/surface pinned to #000000 rather than the previous
// dark-plum-tinted gray, matching the Netflix/YouTube-style deep-black dark theme (saves power
// on OLED panels since black pixels are fully off). surfaceContainer* stay a hair off black
// (#0A0A0A / #121212 / #1C1C1C) so Cards, the bottom nav, and dialogs stay visually distinct
// from the background instead of blending into it.
private val DarkColors = darkColorScheme(
    primary = Color(0xFFD8BEE0),
    onPrimary = DeepPlum,
    primaryContainer = Color(0xFF3A2E47),
    onPrimaryContainer = Color(0xFFEDE3F2),
    secondary = Color(0xFFCB8FC0),
    onSecondary = DeepPlum,
    tertiary = FuelOrange,
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    onBackground = Color(0xFFEDE3F2),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerLow = Color(0xFF0A0A0A),
    onSurface = Color(0xFFEDE3F2),
    onSurfaceVariant = Color(0xFFC9BBD1),
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
