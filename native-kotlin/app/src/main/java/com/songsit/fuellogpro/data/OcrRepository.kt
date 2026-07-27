package com.songsit.fuellogpro.data

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern

/**
 * Item 3 (receipt scanning): V8 ran real OCR on attached receipt photos (Tesseract.js locally,
 * or Claude OCR via a Firebase Functions backend) to pre-fill the expense amount. This ports the
 * "extract a total amount from the photo" half of that feature using on-device ML Kit text
 * recognition — no cloud round-trip needed for a simple amount pre-fill.
 */
class OcrRepository {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Looks for the largest plausible currency-like number in the recognized text (e.g. a
    // fuel/repair receipt total such as "650.00" or "1,200"). This is a heuristic, same spirit
    // as V8's regex-based amount guess after OCR — not a guaranteed-correct parse.
    private val amountPattern = Pattern.compile("""\d{1,3}(?:[,.]\d{3})*(?:\.\d{1,2})?""")

    suspend fun extractAmount(filePath: String): Double? {
        val bitmap = runCatching { BitmapFactory.decodeFile(filePath) }.getOrNull() ?: return null
        val image = InputImage.fromBitmap(bitmap, 0)
        val text = runCatching { recognizer.process(image).await() }.getOrNull()?.text ?: return null
        val matcher = amountPattern.matcher(text)
        var best: Double? = null
        while (matcher.find()) {
            val raw = matcher.group().replace(",", "")
            val value = raw.toDoubleOrNull() ?: continue
            if (value in 1.0..1_000_000.0 && (best == null || value > best!!)) {
                best = value
            }
        }
        return best
    }
}
