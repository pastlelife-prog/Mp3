package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.converter.AudioDownloader
import com.example.data.database.AppDatabase
import com.example.data.model.DownloadedTrack
import com.example.data.network.YouTubeMetadataService
import com.example.data.network.YouTubeVideoInfo
import com.example.data.repository.TrackRepository
import com.example.player.AudioPlayerController
import com.example.player.PlaybackState
import com.example.util.SampleVideo
import com.example.util.YouTubeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ConversionState {
    data object Idle : ConversionState
    data class Converting(val progress: Float, val statusMessage: String) : ConversionState
    data class Success(val track: DownloadedTrack) : ConversionState
    data class Error(val message: String) : ConversionState
}

data class UiState(
    val urlInput: String = "",
    val isAnalyzing: Boolean = false,
    val previewInfo: YouTubeVideoInfo? = null,
    val conversionState: ConversionState = ConversionState.Idle,
    val selectedQuality: String = "320 kbps",
    val activeTab: Int = 0 // 0: Convert, 1: Library
)

class MainViewModel(
    application: Application,
    private val repository: TrackRepository,
    private val audioPlayerController: AudioPlayerController
) : AndroidViewModel(application) {

    private val metadataService = YouTubeMetadataService()
    private val audioDownloader = AudioDownloader(application.applicationContext)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val downloadedTracks: StateFlow<List<DownloadedTrack>> = repository.allTracks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val playbackState: StateFlow<PlaybackState> = audioPlayerController.playbackState

    private var analyzeJob: Job? = null
    private var convertJob: Job? = null

    fun onUrlChanged(newUrl: String) {
        _uiState.value = _uiState.value.copy(
            urlInput = newUrl,
            conversionState = ConversionState.Idle
        )
        val extractedId = YouTubeUtils.extractVideoId(newUrl)
        if (extractedId != null && (_uiState.value.previewInfo == null || _uiState.value.previewInfo?.videoId != extractedId)) {
            analyzeUrl(newUrl)
        } else if (newUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(previewInfo = null)
        }
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(activeTab = index)
    }

    fun selectQuality(quality: String) {
        _uiState.value = _uiState.value.copy(selectedQuality = quality)
    }

    fun clearUrl() {
        analyzeJob?.cancel()
        _uiState.value = _uiState.value.copy(
            urlInput = "",
            previewInfo = null,
            conversionState = ConversionState.Idle
        )
    }

    fun loadSample(sample: SampleVideo) {
        onUrlChanged(sample.url)
    }

    fun analyzeUrl(url: String = _uiState.value.urlInput) {
        if (url.isBlank()) return

        analyzeJob?.cancel()
        analyzeJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            val result = metadataService.fetchVideoInfo(url)
            result.onSuccess { info ->
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    previewInfo = info
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    conversionState = ConversionState.Error(err.localizedMessage ?: "Failed to read YouTube video info")
                )
            }
        }
    }

    fun startConversion() {
        val currentInfo = _uiState.value.previewInfo
        val rawInput = _uiState.value.urlInput

        if (currentInfo == null && rawInput.isBlank()) {
            _uiState.value = _uiState.value.copy(
                conversionState = ConversionState.Error("Please enter or paste a YouTube video link first.")
            )
            return
        }

        convertJob?.cancel()
        convertJob = viewModelScope.launch {
            val videoInfo = currentInfo ?: run {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = true,
                    conversionState = ConversionState.Converting(0.05f, "Fetching video details...")
                )
                val fetchResult = metadataService.fetchVideoInfo(rawInput)
                val info = fetchResult.getOrNull()
                _uiState.value = _uiState.value.copy(isAnalyzing = false, previewInfo = info)
                info
            }

            if (videoInfo == null) {
                _uiState.value = _uiState.value.copy(
                    conversionState = ConversionState.Error("Could not recognize YouTube URL. Please check the link.")
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                conversionState = ConversionState.Converting(0.1f, "Starting MP3 conversion...")
            )

            val downloadResult = audioDownloader.downloadAndConvert(
                videoInfo = videoInfo,
                quality = _uiState.value.selectedQuality
            ) { progress, statusMessage ->
                _uiState.value = _uiState.value.copy(
                    conversionState = ConversionState.Converting(progress, statusMessage)
                )
            }

            downloadResult.onSuccess { track ->
                repository.insertTrack(track)
                _uiState.value = _uiState.value.copy(
                    conversionState = ConversionState.Success(track)
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    conversionState = ConversionState.Error(error.localizedMessage ?: "Conversion failed. Please try again.")
                )
            }
        }
    }

    fun cancelConversion() {
        convertJob?.cancel()
        _uiState.value = _uiState.value.copy(conversionState = ConversionState.Idle)
    }

    fun dismissSuccess() {
        _uiState.value = _uiState.value.copy(conversionState = ConversionState.Idle)
    }

    // Audio Playback controls
    fun playTrack(track: DownloadedTrack) {
        audioPlayerController.playTrack(track)
    }

    fun togglePlayPause() {
        audioPlayerController.togglePlayPause()
    }

    fun seekTo(positionMs: Int) {
        audioPlayerController.seekTo(positionMs)
    }

    fun deleteTrack(track: DownloadedTrack) {
        viewModelScope.launch {
            if (playbackState.value.currentTrack?.id == track.id) {
                audioPlayerController.stop()
            }
            repository.deleteTrack(track)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerController.release()
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: TrackRepository,
    private val audioPlayerController: AudioPlayerController
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(application, repository, audioPlayerController) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
