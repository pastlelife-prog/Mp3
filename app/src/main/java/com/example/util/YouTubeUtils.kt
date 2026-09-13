package com.example.util

import java.util.regex.Pattern

data class SampleVideo(
    val title: String,
    val artist: String,
    val url: String,
    val videoId: String
)

object YouTubeUtils {

    private val YOUTUBE_URL_PATTERNS = listOf(
        // Standard watch: https://www.youtube.com/watch?v=XXXXXXXXXXX
        Pattern.compile("(?:https?://)?(?:www\\.|m\\.|music\\.)?youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})"),
        // Shortened: https://youtu.be/XXXXXXXXXXX
        Pattern.compile("(?:https?://)?youtu\\.be/([a-zA-Z0-9_-]{11})"),
        // Shorts: https://www.youtube.com/shorts/XXXXXXXXXXX
        Pattern.compile("(?:https?://)?(?:www\\.)?youtube\\.com/shorts/([a-zA-Z0-9_-]{11})"),
        // Embed: https://www.youtube.com/embed/XXXXXXXXXXX
        Pattern.compile("(?:https?://)?(?:www\\.)?youtube\\.com/embed/([a-zA-Z0-9_-]{11})"),
        // Direct 11 char ID
        Pattern.compile("^([a-zA-Z0-9_-]{11})$")
    )

    fun extractVideoId(text: String): String? {
        val trimmed = text.trim()
        for (pattern in YOUTUBE_URL_PATTERNS) {
            val matcher = pattern.matcher(trimmed)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        // Check for general URL inside messy shared text
        val urlRegex = Pattern.compile("https?://[^\\s]+")
        val urlMatcher = urlRegex.matcher(trimmed)
        while (urlMatcher.find()) {
            val candidate = urlMatcher.group()
            for (pattern in YOUTUBE_URL_PATTERNS) {
                val candidateMatcher = pattern.matcher(candidate)
                if (candidateMatcher.find()) {
                    return candidateMatcher.group(1)
                }
            }
        }
        return null
    }

    fun toStandardUrl(videoId: String): String {
        return "https://www.youtube.com/watch?v=$videoId"
    }

    fun getThumbnailUrl(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
    }

    val POPULAR_SAMPLES = listOf(
        SampleVideo(
            title = "Lofi Hip Hop - Chill Beats to Relax",
            artist = "Lofi Girl",
            url = "https://www.youtube.com/watch?v=jfKfPfyJRdk",
            videoId = "jfKfPfyJRdk"
        ),
        SampleVideo(
            title = "Synthwave Nostalgia - Retro Chill",
            artist = "Chillwave Records",
            url = "https://www.youtube.com/watch?v=4xDzrJKXOOY",
            videoId = "4xDzrJKXOOY"
        ),
        SampleVideo(
            title = "Acoustic Sunset Melody (Instrumental)",
            artist = "Acoustic Vibe",
            url = "https://www.youtube.com/watch?v=DWcJFNfaw9c",
            videoId = "DWcJFNfaw9c"
        )
    )
}
