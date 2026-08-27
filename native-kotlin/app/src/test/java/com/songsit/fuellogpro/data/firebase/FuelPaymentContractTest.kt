package com.songsit.fuellogpro.data.firebase

import com.songsit.fuellogpro.data.local.FuelEntryEntity
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

    @Test fun enrichOnlyAnAbsentFieldOnOtherwiseIdenticalRecords() {
        val local = mapOf("id" to "one", "total" to 100.0, "paymentMethod" to "เงินสด")
        val remote = local + ("paymentMethod" to "")
        assertTrue(canEnrichPayment(local, remote, false))
        assertFalse(canEnrichPayment(local, remote, true)) // explicitly cleared by user
        assertFalse(canEnrichPayment(local, remote + ("total" to 200.0), false))
        assertFalse(canEnrichPayment(remote, remote, false))
    }
}
