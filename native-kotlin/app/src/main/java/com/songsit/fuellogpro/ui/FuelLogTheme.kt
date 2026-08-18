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
    primaryContainer = Color(0xFFF0E5F4),
    onPrimaryContainer = DeepPlum,
    secondary = PlumAccent,
    onSecondary = Color.White,
    tertiary = FuelOrange,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCFAFD),
    surfaceContainer = Color(0xFFF7F5F8),
    surfaceContainerHigh = Color(0xFFF1EEF3),
    surfaceContainerHighest = Color(0xFFEAE6ED),
    surfaceVariant = Color(0xFFF1EEF3),
    onSurface = DeepPlum,
    onSurfaceVariant = Color(0xFF4A3B57),
    outline = Color(0xFF79727E),
    outlineVariant = Color(0xFFD0CAD3),
)
// True AMOLED black — background/surface pinned to #000000 rather than a dark-plum-tinted gray,
// matching the Netflix/YouTube-style deep-black dark theme (saves power on OLED panels since
// black pixels are fully off). surfaceContainer* stay a hair off black (#0A0A0A / #121212 /
// #1C1C1C) so Cards, the bottom nav, and dialogs stay visually distinct from the background.
private val DefaultDark = darkColorScheme(
    primary = Color(0xFFD8BEE0),
    onPrimary = DeepPlum,
    primaryContainer = Color(0xFF21102B),
    onPrimaryContainer = Color(0xFFF0E4F5),
    secondary = Color(0xFFCB8FC0),
    onSecondary = DeepPlum,
    tertiary = FuelOrange,
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    onBackground = Color(0xFFEDE3F2),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF050506),
    surfaceContainer = Color(0xFF0A0A0B),
    surfaceContainerHigh = Color(0xFF101012),
    surfaceContainerHighest = Color(0xFF151517),
    surfaceVariant = Color(0xFF1A191E),
    onSurface = Color(0xFFEDE3F2),
    onSurfaceVariant = Color(0xFFC9BBD1),
    outline = Color(0xFF77717B),
    outlineVariant = Color(0xFF302D33),
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

// "Pro" — warm amber/teal palette matching the FuelLog Pro Redesign concept (Claude Design
// project ea1e0f11-0f7f-400f-8f99-9a0eeb7e3b62). Hex values are sRGB conversions of that
// mockup's exact oklch() tokens (amber/teal/red, plus warm-neutral bg/surface/text), so this
// scheme reproduces the concept's colors rather than approximating them by eye. Dark mode keeps
// this app's established AMOLED-black convention (see DefaultDark above) for consistency.
// onXContainer intentionally equals the saturated X color (not a darker/neutral tone) because the
// mockup's status badges render e.g. amber text directly on amberDim background.
private val ProLight = lightColorScheme(
    primary = Color(0xFFE15F0A),
    onPrimary = Color(0xFFFCFCFC),
    primaryContainer = Color(0xFFFBEADF),
    onPrimaryContainer = Color(0xFFE15F0A),
    secondary = Color(0xFF00847E),
    onSecondary = Color(0xFFFCFCFC),
    secondaryContainer = Color(0xFFE0EEED),
    onSecondaryContainer = Color(0xFF00847E),
    tertiary = Color(0xFF00847E),
    onTertiary = Color(0xFFFCFCFC),
    tertiaryContainer = Color(0xFFE0EEED),
    onTertiaryContainer = Color(0xFF00847E),
    error = Color(0xFFC8393A),
    onError = Color(0xFFFCFCFC),
    errorContainer = Color(0xFFF8E5E5),
    onErrorContainer = Color(0xFFC8393A),
    background = Color(0xFFF7F5F1),
    onBackground = Color(0xFF281C17),
    surface = Color(0xFFFEFDFC),
    onSurface = Color(0xFF281C17),
    surfaceVariant = Color(0xFFEFEAE4),
    onSurfaceVariant = Color(0xFF6D6059),
    surfaceContainerLowest = Color(0xFFFEFDFC),
    surfaceContainerLow = Color(0xFFFEFDFC),
    surfaceContainer = Color(0xFFFEFDFC),
    surfaceContainerHigh = Color(0xFFEFEAE4),
    surfaceContainerHighest = Color(0xFFE8E2DA),
    outline = Color(0xFF6D6059),
    outlineVariant = Color(0xFFE6E4E0),
)
// The mockup's literal dark-mode oklch tokens (amber H95 C0.06, teal C0.065, red C0.08) are
// intentionally low-chroma "night comfort" tones — accurate to the mockup, but on a real AMOLED
// panel next to a near-black background they read as flat/murky rather than refined. Bumped
// lightness+chroma here (still oklch, same hue families) for a dark palette that stays true to
// the concept's warm-amber/teal identity but is actually legible: primary/secondary/error each
// clear 6.4:1+ contrast against `background` (was ~5-7:1 with the literal mockup values).
private val ProDark = darkColorScheme(
    primary = Color(0xFFF28E42),
    onPrimary = Color(0xFF140B06),
    primaryContainer = Color(0xFF3B2717),
    onPrimaryContainer = Color(0xFFF28E42),
    // First pass (#1DBCB5) was too light/high-chroma and read as bright mint instead of the
    // mockup's deep teal — pulled lightness down and hue slightly bluer for a richer teal that
    // still clears 5:1+ contrast on background.
    secondary = Color(0xFF1F9197),
    onSecondary = Color(0xFF010C0B),
    secondaryContainer = Color(0xFF152524),
    onSecondaryContainer = Color(0xFF1F9197),
    tertiary = Color(0xFF1F9197),
    onTertiary = Color(0xFF010C0B),
    tertiaryContainer = Color(0xFF152524),
    onTertiaryContainer = Color(0xFF1F9197),
    error = Color(0xFFEA6A64),
    onError = Color(0xFF1A0503),
    errorContainer = Color(0xFF3A201D),
    onErrorContainer = Color(0xFFEA6A64),
    background = Color(0xFF080706),
    onBackground = Color(0xFFBBB6B3),
    surface = Color(0xFF13100E),
    onSurface = Color(0xFFBBB6B3),
    surfaceVariant = Color(0xFF1E1917),
    // Lightened from the mockup's literal textMuted (#6D6863, 3.65:1 on background — borderline
    // for body text) to #8A8580 (5.5:1) so secondary labels stay legible on a true-black screen.
    onSurfaceVariant = Color(0xFF8A8580),
    surfaceContainerLowest = Color(0xFF080706),
    surfaceContainerLow = Color(0xFF0E0C0A),
    surfaceContainer = Color(0xFF0E0C0A),
    surfaceContainerHigh = Color(0xFF13100E),
    surfaceContainerHighest = Color(0xFF1E1917),
    outline = Color(0xFF8A8580),
    outlineVariant = Color(0xFF141312),
)

private data class ThemePalette(val key: String, val label: String, val light: ColorScheme, val dark: ColorScheme)

private val staticPalettes = listOf(
    ThemePalette("default", "Default", DefaultLight, DefaultDark),
    ThemePalette("blue", "Light/Blue", BlueLight, BlueDark),
    ThemePalette("green", "Light/Green", GreenLight, GreenDark),
    ThemePalette("orange", "Light/Orange", OrangeLight, OrangeDark),
    ThemePalette("dracula", "Dark/Dracula", DraculaLight, DraculaDark),
    ThemePalette("night", "Dark/Night", NightLight, NightDark),
    ThemePalette("pro", "Pro", ProLight, ProDark),
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
