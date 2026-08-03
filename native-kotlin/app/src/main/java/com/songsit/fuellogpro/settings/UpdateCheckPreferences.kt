package com.songsit.fuellogpro.settings

import android.content.Context

/** Local state for the auto-update checker: remembers which version the user chose to skip. */
class UpdateCheckPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "native-update-check-settings",
        Context.MODE_PRIVATE,
    )

    fun skippedVersionCode(): Int = preferences.getInt(SKIPPED_VERSION_CODE, 0)

    fun skipVersion(versionCode: Int) {
        preferences.edit().putInt(SKIPPED_VERSION_CODE, versionCode).apply()
    }

    private companion object {
        const val SKIPPED_VERSION_CODE = "skipped-version-code"
    }
}
