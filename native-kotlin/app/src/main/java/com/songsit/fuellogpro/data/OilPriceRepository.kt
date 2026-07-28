package com.songsit.fuellogpro.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fuel price for a single brand — every grade field is nullable so the UI can
 * suppress rows that a brand genuinely doesn't sell (e.g. Shell may not carry E85).
 * A null value must NOT be rendered as "—" (dash) per task requirements; instead
 * the row is omitted entirely by the composable.
 */
data class BrandOilPrices(
    val brand: String,
    val gasohol95: Double?,
    val gasohol91: Double?,
    val e20: Double?,
    val e85: Double?,
    val dieselB7: Double?,
    val dieselB20: Double?,
    val premium95: Double?,
)

data class OilPriceInfo(
    val brands: List<BrandOilPrices>,
    val dateLabel: String,
)

/**
 * Fetches today's fuel prices for the 4 major Thai brands (PTT, Shell, PT, Caltex)
 * from the project's auto-updated `oil-prices.json` on GitHub.
 *
 * Data layout of oil-prices.json:
 *   • `data[0].OilList` — Bangchak grades (used only for dateLabel fallback now)
 *   • `comparison.ptt|shell|pt|caltex.grades` — each brand's grade map
 *
 * Bangchak is intentionally excluded from the displayed cards because the task
 * specifies exactly these 4 brands: ปตท. (PTT), เชลล์ (Shell), พีที (PT), คาลเท็กซ์ (Caltex).
 */
class OilPriceRepository {

    suspend fun fetchTodayPrices(): OilPriceInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(SOURCE_URL).openConnection() as HttpURLConnection)
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 10_000
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            connection.disconnect()
            parse(body)
        }.getOrNull()
    }

    private fun parse(body: String): OilPriceInfo? {
        val root = JSONObject(body)

        // Date label from Bangchak data block (still present for this purpose)
        val dataArray = root.optJSONArray("data")
        val dateLabel = if (dataArray != null && dataArray.length() > 0) {
            val first = dataArray.getJSONObject(0)
            first.optString("OilRemark2", first.optString("OilPriceDate", ""))
        } else ""

        val comparison = root.optJSONObject("comparison") ?: return null

        fun comparisonBrand(key: String, label: String): BrandOilPrices? {
            val grades = comparison.optJSONObject(key)?.optJSONObject("grades") ?: return null
            fun grade(name: String): Double? =
                grades.optDouble(name, Double.NaN).takeIf { !it.isNaN() && it > 0 }
            val prices = BrandOilPrices(
                brand = label,
                gasohol95 = grade("gasohol_95"),
                gasohol91 = grade("gasohol_91"),
                e20 = grade("e20"),
                e85 = grade("e85"),
                dieselB7 = grade("diesel_b7"),
                dieselB20 = grade("diesel_b20"),
                premium95 = grade("premium_95"),
            )
            // Only include brand if it has at least one price
            return prices.takeIf {
                it.gasohol95 != null || it.gasohol91 != null || it.e20 != null ||
                    it.e85 != null || it.dieselB7 != null || it.dieselB20 != null || it.premium95 != null
            }
        }

        // 4 main brands only, in display order
        val brands = listOfNotNull(
            comparisonBrand("ptt", "ปตท."),
            comparisonBrand("shell", "เชลล์"),
            comparisonBrand("pt", "พีที"),
            comparisonBrand("caltex", "คาลเท็กซ์"),
        )
        if (brands.isEmpty()) return null
        return OilPriceInfo(brands = brands, dateLabel = dateLabel)
    }

    companion object {
        // Raw GitHub content URL for this repo's auto-updated oil-prices.json.
        // If the repo ever goes private or is renamed, this call fails gracefully
        // and the UI silently hides the card — no crash.
        private const val SOURCE_URL =
            "https://raw.githubusercontent.com/songsit2017/Fuel-log/main/oil-prices.json"
    }
}
