package com.songsit.fuellogpro.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// "Modern Minimal Bento" identity: a unified dual-accent system (electric blue + vibrant orange)
// on neutral slate surfaces, replacing the earlier per-category rainbow — blue (primary) marks
// key metrics/highlight numbers/selected states, orange (secondary) marks actions (FAB, "full
// tank" badges, progress bars). Cards stay MaterialTheme.colorScheme.surface with a subtle
// outline hairline, not a tinted background. All text/icon pairs below clear at least 3:1
// (large/bold numerals, icon-on-fill) or 4.5:1 (body text) WCAG contrast against their background.
private val ProLight = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF075985),
    secondary = Color(0xFFEA580C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFEDD5),
    onSecondaryContainer = Color(0xFF9A3412),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF075985),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    surfaceContainerLowest = Color(0xFFF8FAFC),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFF1F5F9),
    outline = Color(0x0F0F172A),
    outlineVariant = Color(0x0F0F172A),
)
private val ProDark = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF04283D),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFF7DD3FC),
    secondary = Color(0xFFFB923C),
    onSecondary = Color(0xFF2B1400),
    secondaryContainer = Color(0xFF7C2D12),
    onSecondaryContainer = Color(0xFFFDBA74),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF04283D),
    tertiaryContainer = Color(0xFF0C4A6E),
    onTertiaryContainer = Color(0xFF7DD3FC),
    error = Color(0xFFF1746B),
    onError = Color(0xFF220806),
    errorContainer = Color(0xFF3A1A18),
    onErrorContainer = Color(0xFFF1746B),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF181B20),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF20242B),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF0F1115),
    surfaceContainerLow = Color(0xFF14171C),
    surfaceContainer = Color(0xFF14171C),
    surfaceContainerHigh = Color(0xFF181B20),
    surfaceContainerHighest = Color(0xFF20242B),
    outline = Color(0x14FFFFFF),
    outlineVariant = Color(0x14FFFFFF),
)

// Semantic "good/positive" green, kept separate from colorScheme.secondary (which is now the
// brand orange accent, not a semantic color) — used for improving-trend badges and income amounts.
val ProGood = Color(0xFF16A34A)
val ProGoodDark = Color(0xFF4ADE80)

// Exact translucent badge fills from the design spec ("Full Tank" style pills) — not expressible
// as a plain colorScheme role, so kept as named constants.
val FullTankBadgeLight = Color(0x1FEA580C)
val FullTankBadgeDark = Color(0x26FB923C)

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
