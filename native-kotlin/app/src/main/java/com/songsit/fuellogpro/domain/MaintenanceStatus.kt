package com.songsit.fuellogpro.domain

import com.songsit.fuellogpro.domain.model.MaintenanceTask
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class DueLevel { OVERDUE, DUE_SOON, OK }

data class MaintenanceStatus(
    val level: DueLevel,
    val label: String,
)

fun calculateMaintenanceStatus(
    task: MaintenanceTask,
    currentOdometerKm: Double?,
    today: LocalDate = LocalDate.now(),
): MaintenanceStatus {
    val candidates = buildList {
        task.nextDate?.let { rawDate ->
            runCatching { LocalDate.parse(rawDate) }.getOrNull()?.let { date ->
                val days = ChronoUnit.DAYS.between(today, date)
                add(
                    when {
                        days < 0 -> MaintenanceStatus(DueLevel.OVERDUE, "เกินกำหนด ${-days} วัน")
                        days <= task.warningDays -> MaintenanceStatus(DueLevel.DUE_SOON, "อีก $days วัน")
                        else -> MaintenanceStatus(DueLevel.OK, "อีก $days วัน")
                    },
                )
            }
        }
        if (task.nextOdometerKm != null && currentOdometerKm != null) {
            val distance = task.nextOdometerKm - currentOdometerKm
            add(
                when {
                    distance < 0 -> MaintenanceStatus(DueLevel.OVERDUE, "เกิน ${formatDistance(-distance)} กม.")
                    distance <= task.warningOdometerKm -> MaintenanceStatus(DueLevel.DUE_SOON, "อีก ${formatDistance(distance)} กม.")
                    else -> MaintenanceStatus(DueLevel.OK, "อีก ${formatDistance(distance)} กม.")
                },
            )
        }
    }
    return candidates.minByOrNull { it.level.ordinal }
        ?: MaintenanceStatus(DueLevel.OK, "ยังไม่กำหนด")
}

private fun formatDistance(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(value)
