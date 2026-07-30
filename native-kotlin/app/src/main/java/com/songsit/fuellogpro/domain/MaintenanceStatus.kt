package com.songsit.fuellogpro.domain

import com.songsit.fuellogpro.domain.model.MaintenanceTask
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class DueLevel { OVERDUE, DUE_SOON, OK }
enum class MaintenanceUnit { DAYS, DISTANCE, NONE }

// label text is resolved by the UI layer (stringResource), not here — this is a plain domain
// function with no Android Context/Compose access, so it can only hand back the raw
// unit+magnitude and let the composable pick the right localized template.
data class MaintenanceStatus(
    val level: DueLevel,
    val unit: MaintenanceUnit,
    val magnitudeDays: Long = 0,
    val magnitudeKm: String = "",
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
                        days < 0 -> MaintenanceStatus(DueLevel.OVERDUE, MaintenanceUnit.DAYS, magnitudeDays = -days)
                        days <= task.warningDays -> MaintenanceStatus(DueLevel.DUE_SOON, MaintenanceUnit.DAYS, magnitudeDays = days)
                        else -> MaintenanceStatus(DueLevel.OK, MaintenanceUnit.DAYS, magnitudeDays = days)
                    },
                )
            }
        }
        if (task.nextOdometerKm != null && currentOdometerKm != null) {
            val distance = task.nextOdometerKm - currentOdometerKm
            add(
                when {
                    distance < 0 -> MaintenanceStatus(DueLevel.OVERDUE, MaintenanceUnit.DISTANCE, magnitudeKm = formatDistance(-distance))
                    distance <= task.warningOdometerKm -> MaintenanceStatus(DueLevel.DUE_SOON, MaintenanceUnit.DISTANCE, magnitudeKm = formatDistance(distance))
                    else -> MaintenanceStatus(DueLevel.OK, MaintenanceUnit.DISTANCE, magnitudeKm = formatDistance(distance))
                },
            )
        }
    }
    return candidates.minByOrNull { it.level.ordinal }
        ?: MaintenanceStatus(DueLevel.OK, MaintenanceUnit.NONE)
}

private fun formatDistance(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(value)
