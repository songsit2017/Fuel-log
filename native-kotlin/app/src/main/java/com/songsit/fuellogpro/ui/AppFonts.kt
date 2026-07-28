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

// "roboto" intentionally maps to FontFamily.Default (no download) rather than a Google Fonts
// lookup — it's this app's original typeface, and matching the system default exactly avoids a
// network fetch for the one option most likely to be picked by users who don't want to change
// anything.
val fontOptions = listOf(
    FontOption("ubuntu", "Ubuntu", googleFontFamily("Ubuntu")),
    FontOption("roboto", "Roboto (old default)", FontFamily.Default),
    FontOption("noto_sans", "Noto Sans", googleFontFamily("Noto Sans")),
    FontOption("lato", "Lato", googleFontFamily("Lato")),
    FontOption("open_sans", "Open Sans", googleFontFamily("Open Sans")),
    FontOption("source_sans_pro", "Source Sans Pro", googleFontFamily("Source Sans Pro")),
)

fun resolveFontFamily(key: String): FontFamily =
    fontOptions.firstOrNull { it.key == key }?.fontFamily ?: fontOptions[0].fontFamily
