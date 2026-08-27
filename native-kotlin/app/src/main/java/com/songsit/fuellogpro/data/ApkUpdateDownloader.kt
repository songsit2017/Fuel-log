package com.songsit.fuellogpro.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads an update APK into the app's own cache and hands it straight to the system package
 * installer, instead of handing the .apk URL to the browser — which turned "tap update" into
 * "leave the app, watch Chrome download a file, dig it out of the notification shade, then tap
 * it to install." REQUEST_INSTALL_PACKAGES (AndroidManifest.xml) and the existing FileProvider
 * "shared" cache path (file_paths.xml) already existed for the receipt-photo-share flow; this
 * reuses both instead of adding new plumbing.
 */
class ApkUpdateDownloader(private val context: Context) {

    suspend fun downloadAndInstall(downloadUrl: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val apkFile = File(sharedDir, "fuellog-update.apk")
            val connection = (URL(downloadUrl).openConnection() as HttpURLConnection)
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.connect()
            connection.inputStream.use { input -> apkFile.outputStream().use { output -> input.copyTo(output) } }
            connection.disconnect()

            val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.isSuccess
    }
}
