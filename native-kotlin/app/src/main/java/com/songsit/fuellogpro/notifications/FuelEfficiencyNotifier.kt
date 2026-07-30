package com.songsit.fuellogpro.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.songsit.fuellogpro.MainActivity
import com.songsit.fuellogpro.R
import com.songsit.fuellogpro.domain.FuelEfficiencyAlert
import java.util.Locale

// Fired right after a fill-up is saved (see NativeAppViewModel.addFuel), not on a WorkManager
// schedule like MaintenanceReminderWorker — there's nothing to check periodically, only right
// after a new per-entry km/L figure becomes available.
object FuelEfficiencyNotifier {
    private const val CHANNEL_ID = "fuel-efficiency-alerts"
    private const val NOTIFICATION_ID = 9101

    fun notify(context: Context, alert: FuelEfficiencyAlert, maintenanceOverdue: Boolean) {
        val notificationsAllowed = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!notificationsAllowed) return

        ensureChannel(context)

        val dropPercent = "%.0f".format(Locale.US, alert.dropPercent)
        val latestKmPerLiter = "%.1f".format(Locale.US, alert.latestKmPerLiter)
        val message = if (maintenanceOverdue) {
            context.getString(R.string.fuel_efficiency_alert_body_maintenance, dropPercent, latestKmPerLiter)
        } else {
            context.getString(R.string.fuel_efficiency_alert_body, dropPercent, latestKmPerLiter)
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_fuel)
            .setContentTitle(context.getString(R.string.fuel_efficiency_alert_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_fuel_efficiency_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.notification_channel_fuel_efficiency_desc)
            },
        )
    }
}
