package com.songsit.fuellogpro.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class NearbyStation(
    val name: String,
    val distanceMeters: Double,
)

/**
 * Ports V8's fetchNearbyStations() (app.js) which queried the public Overpass API
 * for amenity=fuel nodes/ways/relations within ~7km of the device, ranked by haversine
 * distance. This mirrors that query/parsing logic using plain HttpURLConnection + org.json
 * since the project has no Retrofit/OkHttp dependency.
 */
class NearbyStationRepository {

    suspend fun fetchNearbyStations(lat: Double, lon: Double, radiusMeters: Int = 7000): List<NearbyStation> =
        withContext(Dispatchers.IO) {
            val query = "[out:json];(node[amenity=fuel](around:$radiusMeters,$lat,$lon);" +
                "way[amenity=fuel](around:$radiusMeters,$lat,$lon);" +
                "relation[amenity=fuel](around:$radiusMeters,$lat,$lon););out center tags;"
            val connection = (URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection)
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            runCatching {
                connection.outputStream.use { it.write(query.toByteArray(Charsets.UTF_8)) }
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) return@withContext emptyList()
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                parseOverpassResponse(body, lat, lon)
            }.getOrElse { emptyList() }.also { connection.disconnect() }
        }

    private fun parseOverpassResponse(body: String, lat: Double, lon: Double): List<NearbyStation> {
        val root = JSONObject(body)
        val elements = root.optJSONArray("elements") ?: return emptyList()
        val results = mutableListOf<NearbyStation>()
        for (i in 0 until elements.length()) {
            val element = elements.getJSONObject(i)
            val elementLat = if (element.has("lat")) element.optDouble("lat") else {
                element.optJSONObject("center")?.optDouble("lat") ?: Double.NaN
            }
            val elementLon = if (element.has("lon")) element.optDouble("lon") else {
                element.optJSONObject("center")?.optDouble("lon") ?: Double.NaN
            }
            if (elementLat.isNaN() || elementLon.isNaN()) continue
            val tags = element.optJSONObject("tags")
            val name = tags?.optString("name")?.takeIf { it.isNotBlank() }
                ?: tags?.optString("brand")?.takeIf { it.isNotBlank() }
                ?: tags?.optString("operator")?.takeIf { it.isNotBlank() }
                ?: "ปั๊มน้ำมัน"
            results += NearbyStation(name = name, distanceMeters = haversineMeters(lat, lon, elementLat, elementLon))
        }
        return results.sortedBy { it.distanceMeters }.take(30)
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusMeters = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusMeters * c
    }
}
