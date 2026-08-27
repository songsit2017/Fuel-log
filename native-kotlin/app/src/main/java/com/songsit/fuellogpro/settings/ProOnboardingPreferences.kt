package com.songsit.fuellogpro.settings

import android.content.Context

/** Local state for the Pro theme's one-time onboarding screen — remembers whether it's been dismissed. */
class ProOnboardingPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "native-pro-onboarding-settings",
        Context.MODE_PRIVATE,
    )

    fun hasSeenOnboarding(): Boolean = preferences.getBoolean(SEEN, false)

    fun markOnboardingSeen() {
        preferences.edit().putBoolean(SEEN, true).apply()
    }

    private companion object {
        const val SEEN = "seen"
    }
}
