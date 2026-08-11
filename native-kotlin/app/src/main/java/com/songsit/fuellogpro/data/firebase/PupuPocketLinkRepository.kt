package com.songsit.fuellogpro.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/** Redeems a short-lived code created by PU Pocket without exposing either backend secret. */
class PupuPocketLinkRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("asia-southeast1"),
) {
    suspend fun redeem(code: String, vehicleIds: List<String>) {
        check(auth.currentUser != null) { "กรุณาเข้าสู่ระบบ Google ก่อนเชื่อมต่อ" }
        functions.getHttpsCallable("redeemPupuLink")
            .call(mapOf("code" to code, "vehicleIds" to vehicleIds))
            .await()
    }
}
