package com.songsit.fuellogpro.ui

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.songsit.fuellogpro.R

// Downloadable Fonts (Google Play Services) — no .ttf assets bundled in the app, the selected
// typeface is fetched at runtime. certificates come from res/values/font_certs.xml (the
// standard com_google_android_gms_fonts_certs hashes Google publishes for this provider).
private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private fun googleFontFamily(name: String) = FontFamily(Font(GoogleFont(name), fontProvider))

data class FontOption(val key: String, val label: String, val fontFamily: FontFamily)

// "system" and "roboto" both intentionally map to FontFamily.Default (no download) rather than a
// Google Fonts lookup — that's the device's actual system typeface, so no network fetch is
// needed. "system" is the explicit, clearly-labeled "follow system" choice; "roboto" is kept
// as-is (same underlying font) purely so existing saved settings referencing that key still
// resolve to what they always did.
val fontOptions = listOf(
    FontOption("system", "System default", FontFamily.Default),
    FontOption("ubuntu", "Ubuntu", googleFontFamily("Ubuntu")),
    FontOption("roboto", "Roboto (old default)", FontFamily.Default),
    FontOption("noto_sans", "Noto Sans", googleFontFamily("Noto Sans")),
    FontOption("lato", "Lato", googleFontFamily("Lato")),
    FontOption("open_sans", "Open Sans", googleFontFamily("Open Sans")),
    FontOption("source_sans_pro", "Source Sans Pro", googleFontFamily("Source Sans Pro")),
    // Paired with the "Pro" theme palette (FuelLogTheme.kt) to match the FuelLog Pro Redesign
    // concept, which specifies Kanit — kept as an independent font-picker entry rather than
    // forced by the palette, same as every other font/palette combination in this app.
    FontOption("kanit", "Kanit", googleFontFamily("Kanit")),
)

fun resolveFontFamily(key: String): FontFamily =
    fontOptions.firstOrNull { it.key == key }?.fontFamily ?: fontOptions[0].fontFamily
