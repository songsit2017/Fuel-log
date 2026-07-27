package com.songsit.fuellogpro.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class BrandOilPrices(
    val brand: String,
    val gasohol95: Double?,
    val gasohol91: Double?,
    val dieselB7: Double?,
)

data class OilPriceInfo(
    val brands: List<BrandOilPrices>,
    val dateLabel: String,
)

/**
 * Ports V8's fetchBangchakLocal()/loadTodayPrices() (app.js:732-746), which read a
 * `oil-prices.json` file kept at the repo root and refreshed by a GitHub Action scraping
 * https://oil-price.bangchak.co.th/ApiOilPrice2/th. The native app has no bundled copy of
 * that file, so this repository fetches the same JSON from the project's GitHub raw content
 * URL (the file is auto-updated on the `main` branch). Alongside Bangchak's own `data[0].OilList`
 * (a JSON string listing every grade with a `PriceToday` field), the same file's `comparison`
 * object (populated by scripts/update_oil_price.py's fetch_ptt()/fetch_shell()) carries PTT's
 * and Shell's official grade prices, so all three brands come from one fetch.
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
        val dataArray = root.optJSONArray("data") ?: return null
        if (dataArray.length() == 0) return null
        val first = dataArray.getJSONObject(0)
        val oilListRaw = first.optString("OilList").takeIf { it.isNotBlank() } ?: return null
        val list: JSONArray = JSONArray(oilListRaw)
        fun findPrice(pred: (String) -> Boolean): Double? {
            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                val name = item.optString("OilName")
                if (pred(name)) {
                    val price = item.optDouble("PriceToday", Double.NaN)
                    if (!price.isNaN() && price > 0) return price
                }
            }
            return null
        }
        val bangchak = BrandOilPrices(
            brand = "บางจาก",
            gasohol95 = findPrice { it.contains("95") && it.contains("แก๊สโซฮอล์") },
            gasohol91 = findPrice { it.contains("91") && it.contains("แก๊สโซฮอล์") },
            dieselB7 = findPrice { it.contains("ไฮดีเซล") && !it.contains("พรีเมียม") },
        )
        val comparison = root.optJSONObject("comparison")
        fun comparisonBrand(key: String, label: String): BrandOilPrices? {
            val grades = comparison?.optJSONObject(key)?.optJSONObject("grades") ?: return null
            fun grade(name: String): Double? =
                grades.optDouble(name, Double.NaN).takeIf { !it.isNaN() && it > 0 }
            val prices = BrandOilPrices(
                brand = label,
                gasohol95 = grade("gasohol_95"),
                gasohol91 = grade("gasohol_91"),
                dieselB7 = grade("diesel_b7"),
            )
            return prices.takeIf { it.gasohol95 != null || it.gasohol91 != null || it.dieselB7 != null }
        }
        val brands = listOfNotNull(
            comparisonBrand("ptt", "ปตท."),
            bangchak,
            comparisonBrand("shell", "Shell"),
        )
        if (brands.isEmpty()) return null
        return OilPriceInfo(
            brands = brands,
            dateLabel = first.optString("OilRemark2", first.optString("OilPriceDate", "")),
        )
    }

    companion object {
        // Raw GitHub content URL for this repo's auto-updated oil-prices.json (see item 4
        // in the port request). If the repo ever goes private or is renamed, this call will
        // simply fail and the UI falls back to "unavailable" text — no crash.
        private const val SOURCE_URL =
            "https://raw.githubusercontent.com/songsit2017/Fuel-log/main/oil-prices.json"
    }
}
