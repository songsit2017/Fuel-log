package com.songsit.fuellogpro.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

data class DriveBackupResult(
    val photosUploaded: Int,
    val photosSkipped: Int,
)

// How many photo uploads run concurrently. Uploads are dominated by per-request round-trip
// latency (TLS + Drive API processing), not just raw bytes, so overlapping several requests
// meaningfully speeds up wall-clock backup time on connections with real bandwidth to spare —
// this was tuned down from a fully-sequential one-file-at-a-time loop after a user reported a
// 100+ photo backup crawling at a few percent per minute despite a fast connection.
private const val UPLOAD_CONCURRENCY = 4

/**
 * Backs up a JSON export and every original-resolution photo to the signed-in user's own
 * Google Drive, in a folder layout mirroring Fuelio's own Drive backup convention — an
 * "Android" parent folder at Drive root (the same place Fuelio's own backup folder lives, per
 * the user's screenshot), containing this app's "FuelLog Pro" folder with "backup" and
 * "Pictures" subfolders.
 *
 * Calls the Drive REST API v3 directly over HttpURLConnection, matching the lightweight style
 * of the other *Repository classes in this package (NearbyStationRepository,
 * OilPriceRepository, WeatherRepository) rather than pulling in the full
 * google-api-services-drive client SDK. The access token is obtained separately in
 * MainActivity via the Identity Services Authorization API (drive.file scope — this app can
 * only see/manage files it created itself, not the user's whole Drive).
 *
 * Re-running a backup is incremental for photos: [listFilenames] is called once per run and
 * any local file whose name already exists in the Pictures folder is skipped, so repeat
 * backups don't re-upload everything every time.
 */
class GoogleDriveBackupRepository {

    suspend fun backup(
        accessToken: String,
        backupJson: String,
        photoFiles: List<File>,
        onProgress: (percent: Int) -> Unit = {},
    ): DriveBackupResult = withContext(Dispatchers.IO) {
        val androidFolderId = findOrCreateFolder(accessToken, "Android", parentId = "root")
        val appFolderId = findOrCreateFolder(accessToken, "FuelLog Pro", parentId = androidFolderId)
        val backupFolderId = findOrCreateFolder(accessToken, "backup", parentId = appFolderId)
        val picturesFolderId = findOrCreateFolder(accessToken, "Pictures", parentId = appFolderId)

        val timestamp = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(Date())
        uploadFile(
            accessToken = accessToken,
            parentId = backupFolderId,
            filename = "FuelLog-Native-$timestamp.json",
            mimeType = "application/json",
            bytes = backupJson.toByteArray(Charsets.UTF_8),
        )

        val existingNames = listFilenames(accessToken, picturesFolderId)
        val toUpload = photoFiles.filter { it.name !in existingNames }
        val skipped = photoFiles.size - toUpload.size
        val totalWork = photoFiles.size.coerceAtLeast(1)
        val doneCount = AtomicInteger(skipped)
        onProgress(((doneCount.get() * 100) / totalWork).coerceIn(0, 99))

        val semaphore = Semaphore(UPLOAD_CONCURRENCY)
        coroutineScope {
            toUpload.map { file ->
                async {
                    semaphore.withPermit {
                        uploadFile(
                            accessToken = accessToken,
                            parentId = picturesFolderId,
                            filename = file.name,
                            mimeType = "image/jpeg",
                            bytes = file.readBytes(),
                        )
                        val done = doneCount.incrementAndGet()
                        onProgress(((done * 100) / totalWork).coerceIn(0, 99))
                    }
                }
            }.awaitAll()
        }
        onProgress(100)
        DriveBackupResult(photosUploaded = toUpload.size, photosSkipped = skipped)
    }

    // Downloads the most recently created JSON backup from the "backup" subfolder, or null if
    // the folder chain or any backup file doesn't exist yet (nothing to restore). Callers feed
    // the result straight into LocalBackupRepository.importJson(), which upserts by each
    // record's own id — matching local rows get updated in place, new ids get added, and
    // nothing already on the device is deleted, so this never blindly overwrites everything.
    suspend fun downloadLatestBackup(accessToken: String): String? = withContext(Dispatchers.IO) {
        val androidFolderId = findFolder(accessToken, "Android", parentId = "root") ?: return@withContext null
        val appFolderId = findFolder(accessToken, "FuelLog Pro", parentId = androidFolderId) ?: return@withContext null
        val backupFolderId = findFolder(accessToken, "backup", parentId = appFolderId) ?: return@withContext null
        val query = "'$backupFolderId' in parents and trashed=false"
        val url = "https://www.googleapis.com/drive/v3/files" +
            "?q=${URLEncoder.encode(query, "UTF-8")}" +
            "&orderBy=${URLEncoder.encode("createdTime desc", "UTF-8")}" +
            "&pageSize=1" +
            "&fields=${URLEncoder.encode("files(id,name)", "UTF-8")}"
        val files = getJson(accessToken, url).optJSONArray("files")
        if (files == null || files.length() == 0) return@withContext null
        val fileId = files.getJSONObject(0).getString("id")
        downloadFileContent(accessToken, fileId)
    }

    private fun findFolder(accessToken: String, name: String, parentId: String): String? {
        val query = "mimeType='application/vnd.google-apps.folder' and name='${escapeQueryValue(name)}' " +
            "and '$parentId' in parents and trashed=false"
        val url = "https://www.googleapis.com/drive/v3/files" +
            "?q=${URLEncoder.encode(query, "UTF-8")}" +
            "&fields=${URLEncoder.encode("files(id,name)", "UTF-8")}"
        val existing = getJson(accessToken, url).optJSONArray("files")
        return if (existing != null && existing.length() > 0) existing.getJSONObject(0).getString("id") else null
    }

    private fun findOrCreateFolder(accessToken: String, name: String, parentId: String): String {
        findFolder(accessToken, name, parentId)?.let { return it }
        val body = JSONObject().apply {
            put("name", name)
            put("mimeType", "application/vnd.google-apps.folder")
            put("parents", JSONArray().put(parentId))
        }
        return postJson(accessToken, "https://www.googleapis.com/drive/v3/files", body).getString("id")
    }

    private fun downloadFileContent(accessToken: String, fileId: String): String {
        val connection = (URL("https://www.googleapis.com/drive/v3/files/$fileId?alt=media").openConnection() as HttpURLConnection)
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.connectTimeout = 10_000
        connection.readTimeout = 30_000
        try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (responseCode !in 200..299) error("ดาวน์โหลดไฟล์สำรองไม่สำเร็จ (HTTP $responseCode)")
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun listFilenames(accessToken: String, folderId: String): Set<String> {
        val names = mutableSetOf<String>()
        var pageToken: String? = null
        do {
            val query = "'$folderId' in parents and trashed=false"
            var url = "https://www.googleapis.com/drive/v3/files" +
                "?q=${URLEncoder.encode(query, "UTF-8")}" +
                "&fields=${URLEncoder.encode("nextPageToken,files(name)", "UTF-8")}&pageSize=1000"
            pageToken?.let { url += "&pageToken=${URLEncoder.encode(it, "UTF-8")}" }
            val result = getJson(accessToken, url)
            result.optJSONArray("files")?.let { files ->
                for (i in 0 until files.length()) names += files.getJSONObject(i).getString("name")
            }
            pageToken = result.optString("nextPageToken").takeIf { it.isNotBlank() }
        } while (pageToken != null)
        return names
    }

    // Raw multipart upload (metadata JSON part + binary media part) per Drive API v3's
    // documented multipart upload format — deliberately NOT base64-encoded (that's unnecessary
    // for this format and would bloat every photo upload by ~33%), so the body is written
    // directly to the connection's OutputStream in three raw byte chunks rather than built up
    // as one Kotlin String (which can't safely hold arbitrary binary photo bytes).
    private fun uploadFile(accessToken: String, parentId: String, filename: String, mimeType: String, bytes: ByteArray) {
        val boundary = "fuellog-drive-${System.currentTimeMillis()}"
        val metadata = JSONObject().apply {
            put("name", filename)
            put("parents", JSONArray().put(parentId))
        }
        val preamble = "--$boundary\r\n" +
            "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
            metadata.toString() +
            "\r\n--$boundary\r\n" +
            "Content-Type: $mimeType\r\n\r\n"
        val closing = "\r\n--$boundary--"
        val connection = (URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart").openConnection() as HttpURLConnection)
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setChunkedStreamingMode(0)
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        try {
            connection.outputStream.use { output ->
                output.write(preamble.toByteArray(Charsets.UTF_8))
                output.write(bytes)
                output.write(closing.toByteArray(Charsets.UTF_8))
            }
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                error("อัปโหลดไฟล์ $filename ไม่สำเร็จ (HTTP $responseCode) $errorBody")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun getJson(accessToken: String, url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection)
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "{}"
            if (responseCode !in 200..299) error("เรียก Google Drive API ไม่สำเร็จ (HTTP $responseCode) $text")
            return JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun postJson(accessToken: String, url: String, body: JSONObject): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection)
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "{}"
            if (responseCode !in 200..299) error("เรียก Google Drive API ไม่สำเร็จ (HTTP $responseCode) $text")
            return JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun escapeQueryValue(value: String) = value.replace("\\", "\\\\").replace("'", "\\'")
}
