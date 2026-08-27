// ============================================================================
// AI AGENT WARNING: CROSS-APP PAIRING SECURITY BOUNDARY
// This client calls the authenticated Firebase pairing adapter. Read
// /ARCHITECTURE.md before changing region, code format, auth, or vehicle IDs.
// Never move server credentials or Supabase service-role access into the APK.
// ============================================================================
package com.songsit.fuellogpro.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await

/** Redeems a short-lived code created by PU Pocket without exposing either backend secret. */
class PupuPocketLinkRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("asia-southeast1"),
) {
    suspend fun redeem(code: String, vehicleIds: List<String>) {
        check(auth.currentUser != null) { "กรุณาเข้าสู่ระบบ Google ก่อนเชื่อมต่อ" }
        functions.getHttpsCallable("redeemPupuLink")
            .withTimeout(540, TimeUnit.SECONDS)
            .call(mapOf("code" to code, "vehicleIds" to vehicleIds))
            .await()
    }
}
