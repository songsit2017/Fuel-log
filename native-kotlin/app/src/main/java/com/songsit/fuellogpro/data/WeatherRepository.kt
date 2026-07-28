package com.songsit.fuellogpro.data

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
 * Ports the legacy web app's modules/weather.js captureWeather(): calls Open-Meteo's free
 * forecast endpoint (no API key) with the device's current GPS position and reads back the
 * current weather code + temperature, so a fill-up can be tagged with conditions at fill time.
 */
class WeatherRepository {

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
                error("เรียกข้อมูลสภาพอากาศไม่สำเร็จ (HTTP $responseCode)")
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val current = JSONObject(body).optJSONObject("current") ?: JSONObject()
            val weatherCode = if (current.has("weather_code")) current.optInt("weather_code") else null
            val temperature = if (current.has("temperature_2m")) current.optDouble("temperature_2m") else null
            WeatherInfo(
                description = WEATHER_CODES[weatherCode] ?: "ไม่ทราบสภาพอากาศ",
                temperatureC = temperature?.takeIf { !it.isNaN() },
                latitude = lat,
                longitude = lon,
            )
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private val WEATHER_CODES = mapOf(
            0 to "ท้องฟ้าแจ่มใส", 1 to "แจ่มใสเป็นส่วนใหญ่", 2 to "มีเมฆบางส่วน", 3 to "ครึ้ม",
            45 to "มีหมอก", 48 to "มีหมอกน้ำค้างแข็ง", 51 to "ฝนปรอยเบา", 53 to "ฝนปรอย",
            55 to "ฝนปรอยหนัก", 56 to "ฝนปรอยเยือกแข็งเบา", 57 to "ฝนปรอยเยือกแข็งหนัก",
            61 to "ฝนเบา", 63 to "ฝนปานกลาง", 65 to "ฝนหนัก", 66 to "ฝนเยือกแข็งเบา",
            67 to "ฝนเยือกแข็งหนัก", 71 to "หิมะเบา", 73 to "หิมะปานกลาง", 75 to "หิมะหนัก",
            77 to "เกล็ดหิมะ", 80 to "ฝนซู่เบา", 81 to "ฝนซู่ปานกลาง", 82 to "ฝนซู่หนัก",
            85 to "หิมะซู่เบา", 86 to "หิมะซู่หนัก", 95 to "พายุฝนฟ้าคะนอง",
            96 to "พายุฝนฟ้าคะนองและลูกเห็บเล็ก", 99 to "พายุฝนฟ้าคะนองและลูกเห็บหนัก",
        )
    }
}
