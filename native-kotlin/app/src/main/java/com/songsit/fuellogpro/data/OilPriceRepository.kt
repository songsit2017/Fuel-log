package com.songsit.fuellogpro.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class OilPriceInfo(
    val gasohol95: Double?,
    val gasohol91: Double?,
    val dieselB7: Double?,
    val dateLabel: String,
)

/**
 * Ports V8's fetchBangchakLocal()/loadTodayPrices() (app.js:732-746), which read a
 * `oil-prices.json` file kept at the repo root and refreshed by a GitHub Action scraping
 * https://oil-price.bangchak.co.th/ApiOilPrice2/th. The native app has no bundled copy of
 * that file, so this repository fetches the same JSON from the project's GitHub raw content
 * URL (the file is auto-updated on the `main` branch) and parses it the same way V8 did:
 * data[0].OilList is itself a JSON string listing every grade with a `PriceToday` field.
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
        return OilPriceInfo(
            gasohol95 = findPrice { it.contains("95") && it.contains("แก๊สโซฮอล์") },
            gasohol91 = findPrice { it.contains("91") && it.contains("แก๊สโซฮอล์") },
            dieselB7 = findPrice { it.contains("ไฮดีเซล") && !it.contains("พรีเมียม") },
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
