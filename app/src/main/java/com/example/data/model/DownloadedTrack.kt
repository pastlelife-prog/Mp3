package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_tracks")
data class DownloadedTrack(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val youtubeUrl: String,
    val videoId: String,
    val filePath: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val durationFormatted: String = "03:45",
    val thumbnailUrl: String = "",
    val quality: String = "320 kbps",
    val timestamp: Long = System.currentTimeMillis()
)
