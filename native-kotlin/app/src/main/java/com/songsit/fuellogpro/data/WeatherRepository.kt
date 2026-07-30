package com.songsit.fuellogpro.data

import android.content.Context
import com.songsit.fuellogpro.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WeatherInfo(
    val description: String,
    val temperatureC: Double?,
    val latitude: Double,
    val longitude: Double,
)

/**
 * Calls Open-Meteo's free forecast endpoint (no API key) with the device's current GPS position
 * and reads back the current weather code + temperature, so a fill-up can be tagged with
 * conditions at fill time.
 */
class WeatherRepository(private val context: Context) {

    suspend fun fetchCurrent(lat: Double, lon: Double): WeatherInfo = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${URLEncoder.encode("%.5f".format(lat), "UTF-8")}" +
            "&longitude=${URLEncoder.encode("%.5f".format(lon), "UTF-8")}" +
            "&current=temperature_2m,weather_code" +
            "&timezone=auto"
        val connection = (URL(url).openConnection() as HttpURLConnection)
        connection.requestMethod = "GET"
        connection.connectTimeout = 8_000
        connection.readTimeout = 10_000
        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                error(context.getString(R.string.error_weather_fetch_http, responseCode))
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val current = JSONObject(body).optJSONObject("current") ?: JSONObject()
            val weatherCode = if (current.has("weather_code")) current.optInt("weather_code") else null
            val temperature = if (current.has("temperature_2m")) current.optDouble("temperature_2m") else null
            WeatherInfo(
                description = weatherCode?.let { WEATHER_CODE_RES[it] }?.let { context.getString(it) }
                    ?: context.getString(R.string.weather_unknown),
                temperatureC = temperature?.takeIf { !it.isNaN() },
                latitude = lat,
                longitude = lon,
            )
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private val WEATHER_CODE_RES = mapOf(
            0 to R.string.weather_code_0, 1 to R.string.weather_code_1, 2 to R.string.weather_code_2, 3 to R.string.weather_code_3,
            45 to R.string.weather_code_45, 48 to R.string.weather_code_48, 51 to R.string.weather_code_51, 53 to R.string.weather_code_53,
            55 to R.string.weather_code_55, 56 to R.string.weather_code_56, 57 to R.string.weather_code_57,
            61 to R.string.weather_code_61, 63 to R.string.weather_code_63, 65 to R.string.weather_code_65, 66 to R.string.weather_code_66,
            67 to R.string.weather_code_67, 71 to R.string.weather_code_71, 73 to R.string.weather_code_73, 75 to R.string.weather_code_75,
            77 to R.string.weather_code_77, 80 to R.string.weather_code_80, 81 to R.string.weather_code_81, 82 to R.string.weather_code_82,
            85 to R.string.weather_code_85, 86 to R.string.weather_code_86, 95 to R.string.weather_code_95,
            96 to R.string.weather_code_96, 99 to R.string.weather_code_99,
        )
    }
}
