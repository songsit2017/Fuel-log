package com.songsit.fuellogpro.settings

import android.content.Context

data class BackupSettings(
    val driveAutoSyncEnabled: Boolean = false,
)

class BackupPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "native-backup-settings",
        Context.MODE_PRIVATE,
    )

    fun load(): BackupSettings = BackupSettings(
        driveAutoSyncEnabled = preferences.getBoolean(DRIVE_AUTO_SYNC, false),
    )

    fun save(settings: BackupSettings) {
        preferences.edit()
            .putBoolean(DRIVE_AUTO_SYNC, settings.driveAutoSyncEnabled)
            .apply()
    }

    private companion object {
        const val DRIVE_AUTO_SYNC = "drive-auto-sync"
    }
}
