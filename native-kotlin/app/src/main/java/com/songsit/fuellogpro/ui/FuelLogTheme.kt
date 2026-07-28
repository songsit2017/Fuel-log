package com.songsit.fuellogpro.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

// Brand palette: matches the app launcher icon (fuellog_icon.xml / colors.xml
// ic_launcher_background) — lavender background with a deep-plum ink accent — applied
// app-wide so every screen's cards/badges/nav share one identity instead of the default
// M3 gray. Deliberately not Fuelio's navy-blue branding (that decision predates this pass);
// Fuelio is used as a reference for layout/organization, not for hue.
private val FuelOrange = Color(0xFFFFA726)
private val DeepPlum = Color(0xFF241B33)
private val PlumAccent = Color(0xFF8E4585)
private val Lavender = Color(0xFFF3E9F7)

private val DefaultLight = lightColorScheme(
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
// True AMOLED black — background/surface pinned to #000000 rather than a dark-plum-tinted gray,
// matching the Netflix/YouTube-style deep-black dark theme (saves power on OLED panels since
// black pixels are fully off). surfaceContainer* stay a hair off black (#0A0A0A / #121212 /
// #1C1C1C) so Cards, the bottom nav, and dialogs stay visually distinct from the background.
private val DefaultDark = darkColorScheme(
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

private val BlueLight = lightColorScheme(primary = Color(0xFF1565C0), onPrimary = Color.White, secondary = Color(0xFF0277BD), tertiary = FuelOrange)
private val BlueDark = darkColorScheme(primary = Color(0xFF90CAF9), onPrimary = Color(0xFF0D2A4A), secondary = Color(0xFF81D4FA), tertiary = FuelOrange)

private val GreenLight = lightColorScheme(primary = Color(0xFF2E7D32), onPrimary = Color.White, secondary = Color(0xFF00695C), tertiary = FuelOrange)
private val GreenDark = darkColorScheme(primary = Color(0xFFA5D6A7), onPrimary = Color(0xFF0F3312), secondary = Color(0xFF80CBC4), tertiary = FuelOrange)

private val OrangeLight = lightColorScheme(primary = Color(0xFFE65100), onPrimary = Color.White, secondary = Color(0xFFEF6C00), tertiary = Color(0xFF6D4C41))
private val OrangeDark = darkColorScheme(primary = Color(0xFFFFCC80), onPrimary = Color(0xFF4A2600), secondary = Color(0xFFFFB74D), tertiary = Color(0xFFBCAAA4))

// Dracula (draculatheme.com palette): background/currentLine/foreground/purple/pink/green.
// Kept dark-leaning even in its "light" slot (a soft lavender-on-cream stand-in) since Dracula
// is a dark theme by design — selecting it under themeMode="light" still needs a usable scheme.
private val DraculaBackground = Color(0xFF282A36)
private val DraculaCurrentLine = Color(0xFF44475A)
private val DraculaForeground = Color(0xFFF8F8F2)
private val DraculaPurple = Color(0xFFBD93F9)
private val DraculaPink = Color(0xFFFF79C6)
private val DraculaLight = lightColorScheme(primary = Color(0xFF7C4DBD), onPrimary = Color.White, secondary = Color(0xFFC2408F), background = Color(0xFFF6F3FB), surface = Color(0xFFF6F3FB))
private val DraculaDark = darkColorScheme(
    primary = DraculaPurple,
    onPrimary = DraculaBackground,
    secondary = DraculaPink,
    onSecondary = DraculaBackground,
    background = DraculaBackground,
    surface = DraculaBackground,
    surfaceContainer = DraculaCurrentLine,
    onSurface = DraculaForeground,
    onBackground = DraculaForeground,
)

// Deep-navy "Night" — distinct from Default's AMOLED-black+plum dark scheme.
private val NightLight = lightColorScheme(primary = Color(0xFF283593), onPrimary = Color.White, secondary = Color(0xFF3949AB))
private val NightDark = darkColorScheme(
    primary = Color(0xFF8C9EFF),
    onPrimary = Color(0xFF0A1030),
    secondary = Color(0xFF7986CB),
    background = Color(0xFF05070F),
    surface = Color(0xFF05070F),
    surfaceContainer = Color(0xFF10152A),
)

private data class ThemePalette(val key: String, val label: String, val light: ColorScheme, val dark: ColorScheme)

private val staticPalettes = listOf(
    ThemePalette("default", "Default", DefaultLight, DefaultDark),
    ThemePalette("blue", "Light/Blue", BlueLight, BlueDark),
    ThemePalette("green", "Light/Green", GreenLight, GreenDark),
    ThemePalette("orange", "Light/Orange", OrangeLight, OrangeDark),
    ThemePalette("dracula", "Dark/Dracula", DraculaLight, DraculaDark),
    ThemePalette("night", "Dark/Night", NightLight, NightDark),
)

// Every selectable theme-palette key, in display order — "dynamic" (Material You, wallpaper
// colors) is handled separately in FuelLogTheme since it needs a Context, so it isn't in
// staticPalettes but must still appear in the Settings picker.
val themePaletteKeys = staticPalettes.map { it.key } + "dynamic"

fun themePaletteLabel(key: String): String =
    if (key == "dynamic") "Dynamic Colors" else staticPalettes.firstOrNull { it.key == key }?.label ?: "Default"

private fun typographyFor(fontFamily: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = fontFamily),
    )
}

/**
 * [themeMode] mirrors the web app's theme setting: "light", "dark", or "system" (follow the
 * device setting). Any other/unknown value falls back to "system".
 * [themePalette] picks the color scheme (see [themePaletteKeys]); "dynamic" uses Android 12+
 * wallpaper-derived Material You colors and falls back to the default brand palette below that.
 * [fontFamily] is one of the keys in [fontOptions].
 */
@Composable
fun FuelLogTheme(
    themeMode: String = "system",
    themePalette: String = "default",
    fontFamily: String = "ubuntu",
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colorScheme = if (themePalette == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val palette = staticPalettes.firstOrNull { it.key == themePalette } ?: staticPalettes[0]
        if (useDark) palette.dark else palette.light
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typographyFor(resolveFontFamily(fontFamily)),
        content = content,
    )
}
