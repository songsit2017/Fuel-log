package com.songsit.fuellogpro.trip

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TripRecordingStatus(
    val active: Boolean = false,
    val paused: Boolean = false,
    val distanceKm: Double = 0.0,
    val elapsedSeconds: Long = 0L,
)

// Single-process shared state between TripRecordingService (writer) and the Compose UI
// (reader, via collectAsState()) — the service is the only thing that ever runs while the
// Activity may not exist, so a StateFlow is simpler than a bound-service/Messenger setup here.
// `active == false && distanceKm > 0` means a recording just finished and is waiting for the
// user to either save it as a Trip (AddTripDialog, prefilled) or discard it (reset()).
object TripRecordingState {
    private val _status = MutableStateFlow(TripRecordingStatus())
    val status = _status.asStateFlow()

    fun update(transform: (TripRecordingStatus) -> TripRecordingStatus) {
        _status.value = transform(_status.value)
    }

    fun reset() {
        _status.value = TripRecordingStatus()
    }
}
