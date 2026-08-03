package com.songsit.fuellogpro.trip

import android.content.Context

data class PersistedTripState(
    val active: Boolean = false,
    val paused: Boolean = false,
    val distanceMeters: Double = 0.0,
    val accumulatedElapsedMs: Long = 0L,
)

// Mirrors TripRecordingService's in-memory counters to disk so an in-progress trip can be
// recovered (resume/save/discard) if the OS kills the whole app process mid-trip — see
// TripRecordingState's doc comment for why the live state alone doesn't survive that.
class TripRecordingPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "native-trip-recording-state",
        Context.MODE_PRIVATE,
    )

    fun load(): PersistedTripState = PersistedTripState(
        active = preferences.getBoolean(ACTIVE, false),
        paused = preferences.getBoolean(PAUSED, false),
        distanceMeters = java.lang.Double.longBitsToDouble(preferences.getLong(DISTANCE_METERS_BITS, 0L)),
        accumulatedElapsedMs = preferences.getLong(ACCUMULATED_ELAPSED_MS, 0L),
    )

    fun save(state: PersistedTripState) {
        preferences.edit()
            .putBoolean(ACTIVE, state.active)
            .putBoolean(PAUSED, state.paused)
            .putLong(DISTANCE_METERS_BITS, java.lang.Double.doubleToRawLongBits(state.distanceMeters))
            .putLong(ACCUMULATED_ELAPSED_MS, state.accumulatedElapsedMs)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val ACTIVE = "active"
        const val PAUSED = "paused"
        const val DISTANCE_METERS_BITS = "distance-meters-bits"
        const val ACCUMULATED_ELAPSED_MS = "accumulated-elapsed-ms"
    }
}
