package com.example.data.network

import com.example.util.YouTubeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class YouTubeVideoInfo(
    val videoId: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String,
    val sourceUrl: String
)

class YouTubeMetadataService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun fetchVideoInfo(rawInput: String): Result<YouTubeVideoInfo> = withContext(Dispatchers.IO) {
        val videoId = YouTubeUtils.extractVideoId(rawInput)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid YouTube URL. Please enter a valid YouTube video link."))

        val standardUrl = YouTubeUtils.toStandardUrl(videoId)
        val defaultThumbnail = YouTubeUtils.getThumbnailUrl(videoId)

        try {
            val oembedUrl = "https://www.youtube.com/oembed?url=$standardUrl&format=json"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string().orEmpty()
                val json = JSONObject(bodyString)
                val title = json.optString("title", "YouTube Audio - $videoId")
                val author = json.optString("author_name", "YouTube Creator")
                val thumb = json.optString("thumbnail_url", defaultThumbnail)

                return@withContext Result.success(
                    YouTubeVideoInfo(
                        videoId = videoId,
                        title = title,
                        author = author,
                        thumbnailUrl = if (thumb.isNotEmpty()) thumb else defaultThumbnail,
                        sourceUrl = standardUrl
                    )
                )
            } else {
                // If oEmbed returns 404 or other code, provide fallback with videoId
                return@withContext Result.success(
                    YouTubeVideoInfo(
                        videoId = videoId,
                        title = "YouTube Audio Track ($videoId)",
                        author = "YouTube Audio",
                        thumbnailUrl = defaultThumbnail,
                        sourceUrl = standardUrl
                    )
                )
            }
        } catch (e: Exception) {
            // Provide fallback if network oEmbed fails but ID was valid
            return@withContext Result.success(
                YouTubeVideoInfo(
                    videoId = videoId,
                    title = "YouTube Audio ($videoId)",
                    author = "Audio Rip",
                    thumbnailUrl = defaultThumbnail,
                    sourceUrl = standardUrl
                )
            )
        }
    }
}
