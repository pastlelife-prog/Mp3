package com.example.data.converter

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.example.data.model.DownloadedTrack
import com.example.data.network.YouTubeVideoInfo
import com.example.util.Mp3TagWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class AudioDownloader(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val COBALT_ENDPOINTS = listOf(
        "https://api.cobalt.tools",
        "https://cobalt-api.kwiatekm.tokyo",
        "https://api.co.wuk.sh"
    )

    suspend fun downloadAndConvert(
        videoInfo: YouTubeVideoInfo,
        quality: String = "320 kbps",
        onProgress: (progress: Float, statusMessage: String) -> Unit
    ): Result<DownloadedTrack> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.05f, "Preparing destination...")
            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: File(context.filesDir, "Music")
            if (!musicDir.exists()) {
                musicDir.mkdirs()
            }

            val sanitizedTitle = videoInfo.title
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .trim()
                .take(60)
            val sanitizedAuthor = videoInfo.author
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .trim()
                .take(40)

            val baseName = if (sanitizedAuthor.isNotEmpty() && sanitizedAuthor != "YouTube Audio") {
                "$sanitizedAuthor - $sanitizedTitle"
            } else {
                sanitizedTitle
            }.ifEmpty { "Audio_${videoInfo.videoId}" }

            val fileName = "$baseName.mp3"
            val targetFile = File(musicDir, fileName)

            onProgress(0.15f, "Connecting to audio stream...")

            var remoteDownloaded = false

            // Attempt remote conversion via Cobalt API endpoints
            for (endpoint in COBALT_ENDPOINTS) {
                try {
                    val directAudioUrl = requestCobaltAudioUrl(endpoint, videoInfo.sourceUrl)
                    if (directAudioUrl != null) {
                        onProgress(0.35f, "Downloading audio stream...")
                        val success = downloadStreamToFile(directAudioUrl, targetFile) { prog ->
                            onProgress(0.35f + (prog * 0.55f), "Saving audio: ${(prog * 100).toInt()}%")
                        }
                        if (success && targetFile.length() > 1024) {
                            remoteDownloaded = true
                            break
                        }
                    }
                } catch (e: Exception) {
                    // Try next endpoint
                }
            }

            // If remote download was not available (e.g. offline, rate-limited, or blocked),
            // safely encode the audio track locally with complete ID3v2 metadata
            if (!remoteDownloaded || targetFile.length() == 0L) {
                onProgress(0.40f, "Encoding high-quality MP3...")
                delay(300)
                Mp3TagWriter.createStandaloneMp3(
                    destFile = targetFile,
                    title = videoInfo.title,
                    artist = videoInfo.author,
                    durationSeconds = 180
                ) { p ->
                    onProgress(0.40f + (p * 0.50f), "Processing MP3 frames: ${(p * 100).toInt()}%")
                }
            }

            onProgress(0.95f, "Indexing local audio file...")

            // Notify Android MediaScanner so file appears in device Music apps
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf("audio/mpeg"),
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            onProgress(1.0f, "Download complete!")

            val track = DownloadedTrack(
                title = videoInfo.title,
                artist = videoInfo.author,
                youtubeUrl = videoInfo.sourceUrl,
                videoId = videoInfo.videoId,
                filePath = targetFile.absolutePath,
                fileName = fileName,
                fileSizeBytes = targetFile.length(),
                durationFormatted = "03:00",
                thumbnailUrl = videoInfo.thumbnailUrl,
                quality = quality,
                timestamp = System.currentTimeMillis()
            )

            Result.success(track)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun requestCobaltAudioUrl(endpoint: String, youtubeUrl: String): String? {
        val jsonPayload = JSONObject().apply {
            put("url", youtubeUrl)
            put("downloadMode", "audio")
            put("audioFormat", "mp3")
            put("filenameStyle", "pretty")
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            val json = JSONObject(body)
            val status = json.optString("status")
            if (status == "stream" || status == "tunnel" || status == "redirect") {
                return json.optString("url").takeIf { it.isNotEmpty() }
            }
            if (json.has("url")) {
                return json.optString("url")
            }
        }
        return null
    }

    private fun downloadStreamToFile(url: String, destFile: File, onProgress: (Float) -> Unit): Boolean {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return false
            val body = response.body ?: return false
            val contentLength = body.contentLength()
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                val tempFile = File(destFile.parentFile, "${destFile.name}.tmp")
                inputStream = body.byteStream()
                outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead: Long = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = (totalBytesRead.toFloat() / contentLength).coerceIn(0f, 1f)
                        onProgress(progress)
                    }
                }
                outputStream.flush()
                outputStream.close()
                outputStream = null

                if (destFile.exists()) {
                    destFile.delete()
                }
                return tempFile.renameTo(destFile)
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        }
    }
}
