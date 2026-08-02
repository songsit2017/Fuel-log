package com.songsit.fuellogpro.settings

import android.content.Context

/** Local state for the auto-update checker: throttles background checks and remembers skipped versions. */
class UpdateCheckPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "native-update-check-settings",
        Context.MODE_PRIVATE,
    )

    fun lastCheckedAtMillis(): Long = preferences.getLong(LAST_CHECKED_AT, 0L)

    fun skippedVersionCode(): Int = preferences.getInt(SKIPPED_VERSION_CODE, 0)

    fun recordCheck(atMillis: Long) {
        preferences.edit().putLong(LAST_CHECKED_AT, atMillis).apply()
    }

    fun skipVersion(versionCode: Int) {
        preferences.edit().putInt(SKIPPED_VERSION_CODE, versionCode).apply()
    }

    private companion object {
        const val LAST_CHECKED_AT = "last-checked-at"
        const val SKIPPED_VERSION_CODE = "skipped-version-code"
    }
}
