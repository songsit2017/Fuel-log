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
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.songsit.fuellogpro.MainActivity
import com.songsit.fuellogpro.R
import com.songsit.fuellogpro.data.local.FuelLogDatabase
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class MaintenanceReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }
        val today = LocalDate.now()
        FuelLogDatabase.get(applicationContext).maintenanceDao().getTasksWithDates()
            .forEach { task ->
                val dueDate = task.nextDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@forEach
                val days = ChronoUnit.DAYS.between(today, dueDate)
                if (days <= task.warningDays) {
                    notifyTask(
                        id = task.id.hashCode(),
                        title = task.name,
                        message = if (days < 0) {
                            "เกินกำหนด ${-days} วัน"
                        } else {
                            "ถึงกำหนดใน $days วัน"
                        },
                    )
                }
            }
        FuelLogDatabase.get(applicationContext).expenseDao().getItemsWithReminderDates()
            .forEach { expense ->
                val reminderDate = expense.reminderDate
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@forEach
                val days = ChronoUnit.DAYS.between(today, reminderDate)
                if (days <= 7) {
                    notifyTask(
                        id = "expense-${expense.id}".hashCode(),
                        title = expense.description.ifBlank { expense.category },
                        message = when {
                            days < 0 -> "เลยวันเตือนชำระ ${-days} วัน"
                            days == 0L -> "ถึงวันเตือนชำระวันนี้"
                            else -> "ถึงวันเตือนชำระใน $days วัน"
                        },
                    )
                }
            }
        return Result.success()
    }

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
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(id, notification)
    }

    companion object {
        private const val CHANNEL_ID = "maintenance-reminders"
        private const val WORK_NAME = "maintenance-reminder-check"

        fun schedule(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "กำหนดดูแลรถ",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "แจ้งเตือนภาษี ประกัน และงานบำรุงรักษาที่ใกล้ครบกำหนด"
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
    }
}
