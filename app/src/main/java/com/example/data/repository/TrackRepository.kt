package com.example.data.repository

import com.example.data.database.TrackDao
import com.example.data.model.DownloadedTrack
import kotlinx.coroutines.flow.Flow
import java.io.File

class TrackRepository(private val trackDao: TrackDao) {

    val allTracks: Flow<List<DownloadedTrack>> = trackDao.getAllTracks()

    suspend fun getTrackById(id: Long): DownloadedTrack? = trackDao.getTrackById(id)

    suspend fun getTrackByVideoId(videoId: String): DownloadedTrack? = trackDao.getTrackByVideoId(videoId)

    suspend fun insertTrack(track: DownloadedTrack): Long = trackDao.insertTrack(track)

    suspend fun deleteTrack(track: DownloadedTrack) {
        // Also delete the physical file if it exists
        try {
            val file = File(track.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        trackDao.deleteTrack(track)
    }

    suspend fun deleteTrackById(id: Long) {
        trackDao.deleteTrackById(id)
    }
}
