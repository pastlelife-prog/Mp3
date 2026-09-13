package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DownloadedTrack
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM downloaded_tracks ORDER BY timestamp DESC")
    fun getAllTracks(): Flow<List<DownloadedTrack>>

    @Query("SELECT * FROM downloaded_tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: Long): DownloadedTrack?

    @Query("SELECT * FROM downloaded_tracks WHERE videoId = :videoId LIMIT 1")
    suspend fun getTrackByVideoId(videoId: String): DownloadedTrack?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: DownloadedTrack): Long

    @Delete
    suspend fun deleteTrack(track: DownloadedTrack)

    @Query("DELETE FROM downloaded_tracks WHERE id = :id")
    suspend fun deleteTrackById(id: Long)
}
