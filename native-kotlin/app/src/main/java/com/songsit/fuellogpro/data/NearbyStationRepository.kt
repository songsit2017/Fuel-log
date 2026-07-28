package com.songsit.fuellogpro.data

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
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

private const val TAG = "NearbyStationRepo"
// local.defaults.properties ships this literal string when no real key is configured — the
// secrets-gradle-plugin bakes it into BuildConfig/manifest as-is rather than leaving it blank,
// so a plain isBlank() check doesn't catch it and the app was sending the literal string
// "MISSING" to Google as the API key, which surfaces as REQUEST_DENIED "API key is invalid".
private const val PLACEHOLDER_KEY = "MISSING"

data class NearbyStation(
    val name: String,
    val distanceMeters: Double,
    val lat: Double,
    val lon: Double,
)

/**
 * Queries the Google Places API's Nearby Search endpoint for amenity=gas_station places within
 * ~7km of the device. The key is resolved the same way the native Maps SDK resolves it for
 * NearbyStationsMapScreen's GoogleMap composable — from the com.google.android.geo.API_KEY
 * meta-data in AndroidManifest.xml — falling back to BuildConfig.MAPS_API_KEY if that's missing.
 * Places doesn't return a distance itself, so each result's distance from the device is computed
 * locally via haversine, same as the previous Overpass-based lookup.
 */
class NearbyStationRepository {

    private fun resolveApiKey(context: Context): String {
        val manifestKey = runCatching {
            context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                .metaData?.getString("com.google.android.geo.API_KEY")
        }.getOrNull()
        return manifestKey?.takeIf { it.isNotBlank() && it != PLACEHOLDER_KEY }
            ?: BuildConfig.MAPS_API_KEY.takeIf { it != PLACEHOLDER_KEY }
            ?: ""
    }

    private fun maskKey(key: String) = if (key.length > 8) key.take(8) + "..." else "***"

    suspend fun fetchNearbyStations(context: Context, lat: Double, lon: Double, radiusMeters: Int = 7000): List<NearbyStation> =
        withContext(Dispatchers.IO) {
            val apiKey = resolveApiKey(context)
            // Key is resolved from the manifest meta-data (com.google.android.geo.API_KEY,
            // injected via manifestPlaceholders in build.gradle.kts → local.properties) or falls
            // back to BuildConfig.MAPS_API_KEY. If the key is genuinely blank we surface a clean
            // error; otherwise we let the Places API validate it and report any auth issue itself.
            if (apiKey.isBlank()) {
                error("กรุณาตั้งค่า MAPS_API_KEY ใน local.properties")
            }
            val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=${URLEncoder.encode("$lat,$lon", "UTF-8")}" +
                "&radius=$radiusMeters" +
                "&type=gas_station" +
                "&key=$apiKey"
            val connection = (URL(url).openConnection() as HttpURLConnection)
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            try {
                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                if (responseCode !in 200..299) {
                    Log.e(TAG, "Places API HTTP $responseCode key=${maskKey(apiKey)} body=$body")
                    error("เรียก Google Places API ไม่สำเร็จ (HTTP $responseCode)")
                }
                parsePlacesResponse(body, lat, lon, apiKey)
            } finally {
                connection.disconnect()
            }
        }

    private fun parsePlacesResponse(body: String, lat: Double, lon: Double, apiKey: String): List<NearbyStation> {
        val root = JSONObject(body)
        val status = root.optString("status")
        if (status == "ZERO_RESULTS") return emptyList()
        if (status != "OK") {
            // Places' legacy Nearby Search always answers HTTP 200, even for auth failures —
            // the actual cause (bad key, billing disabled, API not enabled, ...) only shows up
            // in this JSON body, hence logging it here rather than at the HTTP-status check.
            val errorMessage = root.optString("error_message").takeIf { it.isNotBlank() }
            Log.e(TAG, "Places API status=$status message=${errorMessage ?: "-"} key=${maskKey(apiKey)}")
            if (status == "REQUEST_DENIED") {
                error("ไม่สามารถเชื่อมต่อ Google Maps ได้ (API Key ไม่ถูกต้อง) กรุณาตรวจสอบการตั้งค่า")
            } else {
                error("Google Places API: $status${errorMessage?.let { " ($it)" } ?: ""}")
            }
        }
        val results = root.optJSONArray("results") ?: return emptyList()
        val stations = mutableListOf<NearbyStation>()
        for (i in 0 until results.length()) {
            val place = results.getJSONObject(i)
            val name = place.optString("name").takeIf { it.isNotBlank() } ?: continue
            val location = place.optJSONObject("geometry")?.optJSONObject("location") ?: continue
            val placeLat = location.optDouble("lat", Double.NaN)
            val placeLon = location.optDouble("lng", Double.NaN)
            if (placeLat.isNaN() || placeLon.isNaN()) continue
            stations += NearbyStation(
                name = name,
                distanceMeters = haversineMeters(lat, lon, placeLat, placeLon),
                lat = placeLat,
                lon = placeLon,
            )
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
