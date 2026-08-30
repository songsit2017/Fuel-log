package com.songsit.fuellogpro.data.firebase

import com.songsit.fuellogpro.data.local.FuelEntryEntity
import com.songsit.fuellogpro.data.local.ExpenseEntity
import org.junit.Assert.*
import org.junit.Test

class FuelPaymentContractTest {
    @Test fun encoderRetainsExplicitMethodAndSourceIdentity() {
        val entry = FuelEntryEntity("source-id", "car", "2026-08-26", "20:10",
            130350.0, 76.6, 38.51, 2950.0, true, "PTT", 1L,
            paymentMethod = "บัตรเครดิต FirstChoice")
        val encoded = fuelCloudMap(entry)
        assertEquals(entry.paymentMethod, encoded["paymentMethod"])
        assertEquals("source-id", encoded["id"])
        assertEquals(2950.0, encoded["total"])
    }

    @Test fun missingOrMalformedLegacyValueDoesNotBecomeCash() {
        assertEquals("", readFuelPaymentMethod(null))
        assertEquals("", readFuelPaymentMethod(42))
        assertEquals("เงินสด", readFuelPaymentMethod("เงินสด"))
    }

    @Test fun expenseEncoderRetainsPaymentAndVehicleExpenseFields() {
        val expense = ExpenseEntity(
            id = "expense-id",
            vehicleId = "car",
            date = "2026-08-29",
            category = "ทางด่วน",
            description = "ค่าทางด่วน",
            amount = 125.0,
            odometerKm = 130350.0,
            income = false,
            recurring = false,
            reminderDate = null,
            createdAt = 1L,
            time = "16:21",
            paymentMethod = "บัตรเครดิต KTC",
        )

        val encoded = expenseCloudMap(expense)

        assertEquals("บัตรเครดิต KTC", encoded["paymentMethod"])
        assertEquals("ทางด่วน", encoded["category"])
        assertEquals(false, encoded["income"])
        assertEquals(125.0, encoded["amount"])
    }

    @Test fun enrichOnlyAnAbsentFieldOnOtherwiseIdenticalRecords() {
        val local = mapOf("id" to "one", "total" to 100.0, "paymentMethod" to "เงินสด")
        val remote = local + ("paymentMethod" to "")
        assertTrue(canEnrichPayment(local, remote, false))
        assertFalse(canEnrichPayment(local, remote, true)) // explicitly cleared by user
        assertFalse(canEnrichPayment(local, remote + ("total" to 200.0), false))
        assertFalse(canEnrichPayment(remote, remote, false))
    }
}
