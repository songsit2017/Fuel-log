package com.songsit.fuellogpro.data

import android.util.Base64
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

data class ReceiptScanResult(
    val date: String? = null,
    val liters: Double? = null,
    val pricePerLiter: Double? = null,
    val total: Double? = null,
    val station: String? = null,
    val title: String? = null,
    val amount: Double? = null,
)

/**
 * Calls the `scanReceipt` Cloud Function (functions/index.js) — the same Claude/Anthropic-backed
 * receipt OCR V8's web app used (scanReceiptWithClaude(), app.js:508). The Anthropic API key is
 * a server-side Secret the function reads via defineSecret('ANTHROPIC_API_KEY'); the app only
 * ever sends a base64 image to an authenticated callable and gets structured JSON back — no key
 * ever reaches the client. Requires the user to be signed in (the function itself rejects
 * unauthenticated calls); callers should fall back to on-device OcrRepository otherwise.
 */
class ClaudeOcrRepository {
    private val functions = FirebaseFunctions.getInstance("asia-southeast1")

    suspend fun scanReceipt(filePath: String, type: String): ReceiptScanResult? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = File(filePath).readBytes()
            val imageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val data = mapOf(
                "imageBase64" to imageBase64,
                "mediaType" to "image/jpeg",
                "type" to type,
            )
            val result = functions.getHttpsCallable("scanReceipt").call(data).await()
            @Suppress("UNCHECKED_CAST")
            val payload = result.data as? Map<String, Any?> ?: return@withContext null
            ReceiptScanResult(
                date = payload["date"] as? String,
                liters = (payload["liters"] as? Number)?.toDouble(),
                pricePerLiter = (payload["pricePerLiter"] as? Number)?.toDouble(),
                total = (payload["total"] as? Number)?.toDouble(),
                station = payload["station"] as? String,
                title = payload["title"] as? String,
                amount = (payload["amount"] as? Number)?.toDouble(),
            )
        }.getOrNull()
    }
}
