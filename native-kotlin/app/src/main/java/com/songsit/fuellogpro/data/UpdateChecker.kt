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

/** One published stable release, for the in-app changelog screen. */
data class ReleaseNote(
    val versionName: String,
    val tagName: String,
    val publishedAt: String,
    val body: String,
)

/**
 * Checks GitHub Releases for a build newer than [currentVersionCode].
 *
 * Both build-native-preview.yml (prerelease) and build-native-release.yml (stable) stamp
 * their release body with a `<!-- versionCode: N -->` marker holding the real
 * FUELLOG_VERSION_CODE (git rev-list count) baked into that build — the tag name can't be
 * used since preview tags carry github.run_number, an unrelated counter, and stable tags
 * (`v1.0.3`) don't carry any number at all. `/releases/latest` (which ignores prereleases)
 * can't be used either — this hits the releases list and takes the first entry, which
 * GitHub always returns most-recent-first regardless of prerelease status.
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

    /** All stable (non-prerelease) releases, newest first, for the in-app changelog screen. */
    suspend fun fetchStableReleases(): List<ReleaseNote> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(RELEASES_URL).openConnection() as HttpURLConnection)
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            connection.disconnect()
            val releases = JSONArray(body)
            (0 until releases.length())
                .map { releases.getJSONObject(it) }
                .filterNot { it.optBoolean("prerelease", false) }
                .map { release ->
                    val tagName = release.optString("tag_name", "")
                    ReleaseNote(
                        versionName = release.optString("name", tagName),
                        tagName = tagName,
                        publishedAt = release.optString("published_at", ""),
                        body = cleanReleaseBody(release.optString("body", "")),
                    )
                }
        }.getOrElse { emptyList() }
    }

    // Strips the machine-readable bits (versionCode marker, workflow-run link) that don't belong
    // in a user-facing changelog — everything else in the body is meant to be read as-is.
    private fun cleanReleaseBody(body: String): String = body.lineSequence()
        .filterNot { it.contains("versionCode:") || it.contains("ดู workflow run") }
        .joinToString("\n")
        .trim()

    private fun parse(body: String, currentVersionCode: Int): UpdateInfo? {
        val releases = JSONArray(body)
        if (releases.length() == 0) return null
        val latest = releases.getJSONObject(0)
        val tagName = latest.optString("tag_name", "")
        val releaseBody = latest.optString("body", "")
        val versionCode = VERSION_CODE_REGEX.find(releaseBody)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        if (versionCode <= currentVersionCode) return null

        val assets = latest.optJSONArray("assets")
        val apkUrl = (0 until (assets?.length() ?: 0))
            .map { assets!!.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk") }
            ?.optString("browser_download_url")
            ?: latest.optString("html_url")

        return UpdateInfo(
            versionCode = versionCode,
            versionName = latest.optString("name", tagName),
            downloadUrl = apkUrl,
            releaseNotesUrl = latest.optString("html_url"),
        )
    }

    private companion object {
        const val RELEASES_URL = "https://api.github.com/repos/songsit2017/Fuel-log/releases"
        val VERSION_CODE_REGEX = Regex("""versionCode:\s*(\d+)""")
    }
}
