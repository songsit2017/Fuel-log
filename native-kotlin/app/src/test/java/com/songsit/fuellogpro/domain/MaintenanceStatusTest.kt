package com.songsit.fuellogpro.domain

import com.songsit.fuellogpro.domain.model.MaintenanceTask
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class MaintenanceStatusTest {
    private val today = LocalDate.of(2026, 7, 27)

    @Test
    fun overdueDateHasHighestPriority() {
        val status = calculateMaintenanceStatus(
            task(nextDate = "2026-07-20", nextOdometerKm = 20_000.0),
            currentOdometerKm = 10_000.0,
            today = today,
        )

        assertEquals(DueLevel.OVERDUE, status.level)
        assertEquals("เกินกำหนด 7 วัน", status.label)
    }

    @Test
    fun odometerInsideWarningWindowIsDueSoon() {
        val status = calculateMaintenanceStatus(
            task(nextDate = null, nextOdometerKm = 10_500.0),
            currentOdometerKm = 10_000.0,
            today = today,
        )

        assertEquals(DueLevel.DUE_SOON, status.level)
        assertEquals("อีก 500 กม.", status.label)
    }

    @Test
    fun missingScheduleIsNotReportedAsOverdue() {
        assertEquals(
            MaintenanceStatus(DueLevel.OK, "ยังไม่กำหนด"),
            calculateMaintenanceStatus(task(nextDate = null, nextOdometerKm = null), null, today),
        )
    }
}

private fun task(nextDate: String?, nextOdometerKm: Double?) = MaintenanceTask(
    id = "task",
    vehicleId = "car",
    name = "เปลี่ยนน้ำมันเครื่อง",
    category = "บำรุงรักษา",
    nextDate = nextDate,
    nextOdometerKm = nextOdometerKm,
    warningDays = 30,
    warningOdometerKm = 1_000.0,
    repeatMonths = 12,
    repeatOdometerKm = 10_000.0,
)
