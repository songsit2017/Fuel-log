package com.songsit.fuellogpro.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// "Pro" — warm amber/teal palette matching the FuelLog Pro Redesign concept (Claude Design
// project ea1e0f11-0f7f-400f-8f99-9a0eeb7e3b62). Hex values are sRGB conversions of that
// mockup's exact oklch() tokens (amber/teal/red, plus warm-neutral bg/surface/text), so this
// scheme reproduces the concept's colors rather than approximating them by eye. Dark mode uses
// a near-black (AMOLED-friendly) background/surface.
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
 * The Pro palette (see ProLight/ProDark above) is the app's only color scheme — there is no
 * palette picker anymore, so [themePalette] no longer affects anything; it's kept as a
 * parameter only because DisplaySettings still persists a themePalette field.
 * [fontFamily] is one of the keys in [fontOptions].
 */
@Composable
fun FuelLogTheme(
    themeMode: String = "system",
    themePalette: String = "pro",
    fontFamily: String = "ubuntu",
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) ProDark else ProLight,
        typography = typographyFor(resolveFontFamily(fontFamily)),
        content = content,
    )
}
