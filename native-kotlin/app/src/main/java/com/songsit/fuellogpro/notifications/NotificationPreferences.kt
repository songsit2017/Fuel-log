package com.songsit.fuellogpro.notifications

import android.content.Context

data class ReminderSettings(
    val dateReminders: Boolean = true,
    val odometerReminders: Boolean = true,
    val paymentReminders: Boolean = true,
    val fuelEfficiencyAlerts: Boolean = true,
) {
    val anyEnabled: Boolean
        get() = dateReminders || odometerReminders || paymentReminders
}

class NotificationPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "native-notification-settings",
        Context.MODE_PRIVATE,
    )

    fun load(): ReminderSettings = ReminderSettings(
        dateReminders = preferences.getBoolean(DATE_REMINDERS, true),
        odometerReminders = preferences.getBoolean(ODOMETER_REMINDERS, true),
        paymentReminders = preferences.getBoolean(PAYMENT_REMINDERS, true),
        fuelEfficiencyAlerts = preferences.getBoolean(FUEL_EFFICIENCY_ALERTS, true),
    )

    fun save(settings: ReminderSettings) {
        preferences.edit()
            .putBoolean(DATE_REMINDERS, settings.dateReminders)
            .putBoolean(ODOMETER_REMINDERS, settings.odometerReminders)
            .putBoolean(PAYMENT_REMINDERS, settings.paymentReminders)
            .putBoolean(FUEL_EFFICIENCY_ALERTS, settings.fuelEfficiencyAlerts)
            .apply()
    }

    fun loadActiveNotificationIds(): Set<Int> =
        preferences.getStringSet(ACTIVE_NOTIFICATION_IDS, emptySet())
            .orEmpty()
            .mapNotNull(String::toIntOrNull)
            .toSet()

    fun saveActiveNotificationIds(ids: Set<Int>) {
        preferences.edit()
            .putStringSet(ACTIVE_NOTIFICATION_IDS, ids.map(Int::toString).toSet())
            .apply()
    }

    private companion object {
        const val DATE_REMINDERS = "date-reminders"
        const val ODOMETER_REMINDERS = "odometer-reminders"
        const val PAYMENT_REMINDERS = "payment-reminders"
        const val FUEL_EFFICIENCY_ALERTS = "fuel-efficiency-alerts"
        const val ACTIVE_NOTIFICATION_IDS = "active-notification-ids"
    }
}
