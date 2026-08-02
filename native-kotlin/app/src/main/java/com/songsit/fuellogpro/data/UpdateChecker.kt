package com.songsit.fuellogpro.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** A newer release than the installed build, found on GitHub Releases. */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotesUrl: String,
)

/**
 * Checks GitHub Releases for a build newer than [currentVersionCode].
 *
 * Every release published by build-native-preview.yml is a prerelease tagged
 * `v9.0.0-native-preview.<run_number>`, where run_number IS the app's versionCode
 * (see FUELLOG_VERSION_CODE in the workflow). So `/releases/latest` (which ignores
 * prereleases) can't be used — this hits the releases list and takes the first
 * entry, which GitHub always returns most-recent-first.
 */
class UpdateChecker {

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(RELEASES_URL).openConnection() as HttpURLConnection)
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            connection.disconnect()
            parse(body, currentVersionCode)
        }.getOrNull()
    }

    private fun parse(body: String, currentVersionCode: Int): UpdateInfo? {
        val releases = JSONArray(body)
        if (releases.length() == 0) return null
        val latest = releases.getJSONObject(0)
        val tagName = latest.optString("tag_name", "")
        val runNumber = VERSION_CODE_REGEX.find(tagName)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        if (runNumber <= currentVersionCode) return null

        val assets = latest.optJSONArray("assets")
        val apkUrl = (0 until (assets?.length() ?: 0))
            .map { assets!!.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk") }
            ?.optString("browser_download_url")
            ?: latest.optString("html_url")

        return UpdateInfo(
            versionCode = runNumber,
            versionName = latest.optString("name", tagName),
            downloadUrl = apkUrl,
            releaseNotesUrl = latest.optString("html_url"),
        )
    }

    private companion object {
        const val RELEASES_URL = "https://api.github.com/repos/songsit2017/Fuel-log/releases"
        val VERSION_CODE_REGEX = Regex("""native-preview\.(\d+)""")
    }
}
