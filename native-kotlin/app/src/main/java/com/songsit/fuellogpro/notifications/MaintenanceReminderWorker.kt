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
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.songsit.fuellogpro.MainActivity
import com.songsit.fuellogpro.R
import com.songsit.fuellogpro.data.local.FuelLogDatabase
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit

class MaintenanceReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val preferences = NotificationPreferences(applicationContext)
        val settings = preferences.load()
        val notificationsAllowed = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        val activeNotificationIds = mutableSetOf<Int>()
        val today = LocalDate.now()
        val database = FuelLogDatabase.get(applicationContext)
        val latestOdometerByVehicle = buildMap {
            database.fuelEntryDao().getAll().forEach { entry ->
                put(entry.vehicleId, maxOf(get(entry.vehicleId) ?: 0.0, entry.odometerKm))
            }
            database.expenseDao().getAll().forEach { expense ->
                expense.odometerKm?.let { odometer ->
                    put(expense.vehicleId, maxOf(get(expense.vehicleId) ?: 0.0, odometer))
                }
            }
        }

        database.maintenanceDao().getAll().forEach { task ->
            val messages = mutableListOf<String>()
            if (settings.dateReminders) {
                val dueDate = task.nextDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                val days = dueDate?.let { ChronoUnit.DAYS.between(today, it) }
                if (days != null && days <= task.warningDays) {
                    messages += if (days < 0) {
                        "เกินกำหนด ${-days} วัน"
                    } else {
                        "ถึงกำหนดใน $days วัน"
                    }
                }
            }
            if (settings.odometerReminders) {
                val currentOdometer = latestOdometerByVehicle[task.vehicleId]
                val dueOdometer = task.nextOdometerKm
                if (currentOdometer != null && dueOdometer != null) {
                    val remaining = dueOdometer - currentOdometer
                    if (remaining <= task.warningOdometerKm) {
                        messages += if (remaining < 0) {
                            "เกินกำหนด ${formatKm(-remaining)} กม."
                        } else {
                            "อีก ${formatKm(remaining)} กม."
                        }
                    }
                }
            }
            val notificationId = task.id.hashCode()
            if (messages.isEmpty() || !notificationsAllowed) {
                notificationManager.cancel(notificationId)
            } else {
                notifyTask(notificationId, task.name, messages.joinToString(" • "))
                activeNotificationIds += notificationId
            }
        }

        database.expenseDao().getItemsWithReminderDates().forEach { expense ->
            val notificationId = "expense-${expense.id}".hashCode()
            val reminderDate = expense.reminderDate
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val days = reminderDate?.let { ChronoUnit.DAYS.between(today, it) }
            if (settings.paymentReminders && days != null && days <= 7 && notificationsAllowed) {
                notifyTask(
                    id = notificationId,
                    title = expense.description.ifBlank { expense.category },
                    message = when {
                        days < 0 -> "เลยวันเตือนชำระ ${-days} วัน"
                        days == 0L -> "ถึงวันเตือนชำระวันนี้"
                        else -> "ถึงวันเตือนชำระใน $days วัน"
                    },
                )
                activeNotificationIds += notificationId
            } else {
                notificationManager.cancel(notificationId)
            }
        }
        (preferences.loadActiveNotificationIds() - activeNotificationIds)
            .forEach(notificationManager::cancel)
        preferences.saveActiveNotificationIds(activeNotificationIds)
        return Result.success()
    }

    private fun formatKm(value: Double): String = "%.0f".format(Locale.US, value)

    private fun notifyTask(id: Int, title: String, message: String) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_fuel)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(id, notification)
    }

    companion object {
        private const val CHANNEL_ID = "maintenance-reminders"
        private const val WORK_NAME = "maintenance-reminder-check"
        private const val REFRESH_WORK_NAME = "maintenance-reminder-refresh"

        fun schedule(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "กำหนดดูแลรถ",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "แจ้งเตือนตามวันที่ เลขไมล์ และกำหนดชำระ"
                },
            )
            val request = PeriodicWorkRequestBuilder<MaintenanceReminderWorker>(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun refresh(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                REFRESH_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<MaintenanceReminderWorker>().build(),
            )
        }
    }
}
