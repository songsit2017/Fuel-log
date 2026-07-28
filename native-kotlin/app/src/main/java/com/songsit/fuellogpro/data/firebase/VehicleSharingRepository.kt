package com.songsit.fuellogpro.data.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

data class VehicleMember(
    val uid: String,
    val email: String,
    val displayName: String,
    val role: String,
    val photoUrl: String? = null,
)

data class InviteResult(
    val code: String,
    val role: String,
    val expiresAt: Long,
)

/**
 * Ports V8's family-sharing UI (app.js: invite() at ~1508, join() at 1509-1575,
 * loadMembers() at 1507) which app.js's own comments never touched for role
 * changes/removal — V8 only supports: owner generates an invite code for an email+role,
 * and any signed-in user with a matching invite redeems it to become a member. This is
 * a UI-layer port only; firestore.rules (repo root, unmodified) already defines and
 * enforces the exact /invites/{code} and /vehicles/{vid}.members shape this file writes.
 *
 * Deliberately kept separate from FirestoreSyncRepository.kt (which is being worked on
 * in a concurrent session) — this file does not import or reference it.
 */
class VehicleSharingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun loadMembers(vehicleId: String): List<VehicleMember> {
        val snapshot = firestore.collection("vehicles").document(vehicleId).get().await()
        val membersMap = snapshot.get("members") as? Map<*, *> ?: return emptyList()
        return membersMap.entries.mapNotNull { (uidKey, value) ->
            val uid = uidKey as? String ?: return@mapNotNull null
            val fields = value as? Map<*, *> ?: return@mapNotNull null
            VehicleMember(
                uid = uid,
                email = fields["email"] as? String ?: "",
                displayName = fields["displayName"] as? String ?: "",
                role = fields["role"] as? String ?: "viewer",
                photoUrl = fields["photoUrl"] as? String,
            )
        }
    }

    /**
     * Owner-only (mirrors V8's "เฉพาะ Owner สร้างคำเชิญได้" / firestore.rules'
     * `allow create: if ... request.resource.data.ownerUid == request.auth.uid`).
     * Generates an 8-character invite code valid for 7 days, same as V8.
     */
    suspend fun createInvite(
        vehicleId: String,
        vehicleName: String,
        ownerUid: String,
        targetEmail: String,
        role: String,
    ): InviteResult {
        require(role == "editor" || role == "viewer") { "สิทธิ์ต้องเป็น editor หรือ viewer" }
        val code = randomCode()
        val expiresAt = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
        val invite = mapOf(
            "vehicleId" to vehicleId,
            "vehicleName" to vehicleName,
            "emailLower" to targetEmail.trim().lowercase(),
            "role" to role,
            "ownerUid" to ownerUid,
            "expiresAt" to expiresAt,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        firestore.collection("invites").document(code).set(invite).await()
        return InviteResult(code, role, expiresAt)
    }

    /**
     * Redeems an invite code for the signed-in user, mirroring V8's join(): validates
     * expiry + email match, then adds the user to the vehicle's members map and
     * memberUids array, and stamps lastJoinCode — the exact field firestore.rules checks
     * to authorize this update from a non-owner.
     */
    suspend fun joinByCode(
        code: String,
        currentUid: String,
        currentEmail: String,
        currentDisplayName: String,
        currentPhotoUrl: String? = null,
    ): String {
        val normalizedCode = code.trim().uppercase()
        require(normalizedCode.length == 8) { "กรอกรหัสเชิญให้ครบ 8 ตัว" }
        val inviteSnapshot = firestore.collection("invites").document(normalizedCode).get().await()
        if (!inviteSnapshot.exists()) error("ไม่พบรหัสเชิญ")
        val expiresAt = inviteSnapshot.getLong("expiresAt") ?: 0L
        if (expiresAt < System.currentTimeMillis()) error("รหัสเชิญหมดอายุแล้ว")
        val emailLower = currentEmail.trim().lowercase()
        val inviteEmail = inviteSnapshot.getString("emailLower")
        if (!inviteEmail.isNullOrBlank() && inviteEmail != emailLower) error("รหัสนี้สร้างไว้สำหรับบัญชี Google อื่น")
        val role = inviteSnapshot.getString("role")
        if (role != "editor" && role != "viewer") error("สิทธิ์ในรหัสเชิญไม่ถูกต้อง")
        val vehicleId = inviteSnapshot.getString("vehicleId") ?: error("รหัสเชิญไม่มีข้อมูลรถ")
        val vehicleName = inviteSnapshot.getString("vehicleName") ?: "รถที่แชร์"

        val vehicleRef = firestore.collection("vehicles").document(vehicleId)
        vehicleRef.update(
            mapOf(
                "members.$currentUid" to mapOf(
                    "role" to role,
                    "email" to currentEmail,
                    "emailLower" to emailLower,
                    "displayName" to currentDisplayName,
                    "photoUrl" to currentPhotoUrl,
                ),
                "memberUids" to FieldValue.arrayUnion(currentUid),
                "lastJoinCode" to normalizedCode,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        return vehicleName
    }

    private fun randomCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..8).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
    }
}
