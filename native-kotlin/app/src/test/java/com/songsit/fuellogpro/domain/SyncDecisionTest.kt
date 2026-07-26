package com.songsit.fuellogpro.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncDecisionTest {
    @Test fun localOnlyUploads() =
        assertEquals(SyncDecision.UPLOAD, decideSync(true, false))

    @Test fun cloudOnlyDownloads() =
        assertEquals(SyncDecision.DOWNLOAD, decideSync(false, true))

    @Test fun matchingCopiesRemainUnchanged() =
        assertEquals(SyncDecision.UNCHANGED, decideSync(true, true, contentEqual = true))

    @Test fun divergentCopiesNeverOverwriteAutomatically() =
        assertEquals(SyncDecision.CONFLICT, decideSync(true, true, contentEqual = false))
}
