package com.songsit.fuellogpro.trip

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.songsit.fuellogpro.MainActivity
import com.songsit.fuellogpro.R
import java.util.Locale

// GPS live trip recording, matching Fuelio's "บันทึกการเดินทาง" behavior (see the notification
// screenshot that prompted this feature): a foreground service accumulates distance from
// consecutive location fixes while a persistent notification shows live km/elapsed time with
// "หยุด"/"ดำเนินการต่อ"/"เสร็จสิ้น" actions. Runs as a foreground service (not a background
// location subscription) specifically so ACCESS_BACKGROUND_LOCATION is never needed — the
// existing ACCESS_FINE_LOCATION/ACCESS_COARSE_LOCATION permissions (already granted for nearby
// stations / weather) are sufficient.
class TripRecordingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private var distanceMeters = 0.0
    private var segmentStartElapsedMs = 0L
    private var accumulatedElapsedMs = 0L
    private var recording = false
    private var paused = false

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            publishState()
            updateNotification()
            if (recording && !paused) handler.postDelayed(this, 1_000L)
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (!recording || paused) return
            val location = result.lastLocation ?: return
            lastLocation?.let { distanceMeters += it.distanceTo(location) }
            lastLocation = location
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_FINISH -> finishRecording()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        if (::fusedLocationClient.isInitialized) runCatching { fusedLocationClient.removeLocationUpdates(locationCallback) }
        super.onDestroy()
    }

    private fun startRecording() {
        if (recording) return
        recording = true
        paused = false
        distanceMeters = 0.0
        lastLocation = null
        accumulatedElapsedMs = 0L
        segmentStartElapsedMs = SystemClock.elapsedRealtime()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        requestLocationUpdates()
        handler.post(tickRunnable)
    }

    private fun pauseRecording() {
        if (!recording || paused) return
        paused = true
        accumulatedElapsedMs += SystemClock.elapsedRealtime() - segmentStartElapsedMs
        runCatching { fusedLocationClient.removeLocationUpdates(locationCallback) }
        lastLocation = null
        publishState()
        updateNotification()
    }

    private fun resumeRecording() {
        if (!recording || !paused) return
        paused = false
        segmentStartElapsedMs = SystemClock.elapsedRealtime()
        requestLocationUpdates()
        handler.post(tickRunnable)
        publishState()
        updateNotification()
    }

    private fun finishRecording() {
        if (!recording) return
        if (!paused) accumulatedElapsedMs += SystemClock.elapsedRealtime() - segmentStartElapsedMs
        recording = false
        paused = false
        handler.removeCallbacks(tickRunnable)
        runCatching { fusedLocationClient.removeLocationUpdates(locationCallback) }
        publishState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateDistanceMeters(5f)
            .build()
        runCatching { fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper) }
    }

    private fun elapsedMs(): Long =
        accumulatedElapsedMs + if (recording && !paused) SystemClock.elapsedRealtime() - segmentStartElapsedMs else 0L

    private fun publishState() {
        TripRecordingState.update {
            TripRecordingStatus(
                active = recording,
                paused = paused,
                distanceKm = distanceMeters / 1000.0,
                elapsedSeconds = elapsedMs() / 1000,
            )
        }
    }

    private fun formatElapsed(totalSeconds: Long): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    private fun servicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, TripRecordingService::class.java).setAction(action)
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun buildNotification(): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val distanceText = "%.2fkm".format(Locale.US, distanceMeters / 1000.0)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_fuel)
            .setContentTitle(if (paused) getString(R.string.trip_recording_paused) else getString(R.string.trip_recording_active))
            .setContentText("$distanceText · ${formatElapsed(elapsedMs() / 1000)}")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (paused) {
            builder.addAction(0, getString(R.string.trip_resume), servicePendingIntent(ACTION_RESUME))
        } else {
            builder.addAction(0, getString(R.string.trip_pause), servicePendingIntent(ACTION_PAUSE))
        }
        builder.addAction(0, getString(R.string.trip_finish), servicePendingIntent(ACTION_FINISH))
        return builder.build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_trip_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.notification_channel_trip_desc)
            },
        )
    }

    companion object {
        private const val CHANNEL_ID = "trip-recording"
        private const val NOTIFICATION_ID = 9001
        const val ACTION_START = "com.songsit.fuellogpro.trip.START"
        const val ACTION_PAUSE = "com.songsit.fuellogpro.trip.PAUSE"
        const val ACTION_RESUME = "com.songsit.fuellogpro.trip.RESUME"
        const val ACTION_FINISH = "com.songsit.fuellogpro.trip.FINISH"
    }
}
