package com.songsit.fuellogpro.data

import com.songsit.fuellogpro.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class NearbyStation(
    val name: String,
    val distanceMeters: Double,
)

/**
 * Queries the Google Places API's Nearby Search endpoint for amenity=gas_station places within
 * ~7km of the device, using the MAPS_API_KEY wired up via the Secrets Gradle Plugin
 * (BuildConfig.MAPS_API_KEY). Places doesn't return a distance itself, so each result's distance
 * from the device is computed locally via haversine, same as the previous Overpass-based lookup.
 */
class NearbyStationRepository {

    suspend fun fetchNearbyStations(lat: Double, lon: Double, radiusMeters: Int = 7000): List<NearbyStation> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.MAPS_API_KEY.isBlank()) return@withContext emptyList()
            val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=${URLEncoder.encode("$lat,$lon", "UTF-8")}" +
                "&radius=$radiusMeters" +
                "&type=gas_station" +
                "&key=${BuildConfig.MAPS_API_KEY}"
            val connection = (URL(url).openConnection() as HttpURLConnection)
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            runCatching {
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) return@withContext emptyList()
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                parsePlacesResponse(body, lat, lon)
            }.getOrElse { emptyList() }.also { connection.disconnect() }
        }

    private fun parsePlacesResponse(body: String, lat: Double, lon: Double): List<NearbyStation> {
        val root = JSONObject(body)
        if (root.optString("status") != "OK") return emptyList()
        val results = root.optJSONArray("results") ?: return emptyList()
        val stations = mutableListOf<NearbyStation>()
        for (i in 0 until results.length()) {
            val place = results.getJSONObject(i)
            val name = place.optString("name").takeIf { it.isNotBlank() } ?: continue
            val location = place.optJSONObject("geometry")?.optJSONObject("location") ?: continue
            val placeLat = location.optDouble("lat", Double.NaN)
            val placeLon = location.optDouble("lng", Double.NaN)
            if (placeLat.isNaN() || placeLon.isNaN()) continue
            stations += NearbyStation(name = name, distanceMeters = haversineMeters(lat, lon, placeLat, placeLon))
        }
        return stations.sortedBy { it.distanceMeters }.take(30)
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
