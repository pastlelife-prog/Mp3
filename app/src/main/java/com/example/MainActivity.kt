package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AppDatabase
import com.example.data.repository.TrackRepository
import com.example.player.AudioPlayerController
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.ConvertScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMutedSecondary
import com.example.ui.theme.TextWhitePrimary
import com.example.ui.theme.YtRedPrimary
import com.example.util.YouTubeUtils

class MainActivity : ComponentActivity() {

    private lateinit var audioPlayerController: AudioPlayerController

    private val viewModel: MainViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TrackRepository(database.trackDao())
        audioPlayerController = AudioPlayerController(applicationContext)
        MainViewModelFactory(application, repository, audioPlayerController)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingShareIntent(intent)

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null) return
        if (Intent.ACTION_SEND == intent.action && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            if (sharedText.isNotBlank()) {
                val videoId = YouTubeUtils.extractVideoId(sharedText)
                if (videoId != null) {
                    viewModel.onUrlChanged(YouTubeUtils.toStandardUrl(videoId))
                } else {
                    viewModel.onUrlChanged(sharedText)
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadedTracks by viewModel.downloadedTracks.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        containerColor = DarkBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(YtRedPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "YouTube to MP3",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhitePrimary,
                                fontSize = 18.sp
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextWhitePrimary
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
            ) {
                // Persistent audio player when a track is loaded
                MiniPlayer(
                    playbackState = playbackState,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onDismiss = { viewModel.togglePlayPause() }
                )

                // Navigation Bar
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextWhitePrimary,
                    tonalElevation = 4.dp
                ) {
                    NavigationBarItem(
                        selected = uiState.activeTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        icon = {
                            Icon(
                                imageVector = if (uiState.activeTab == 0) Icons.Default.Download else Icons.Outlined.Download,
                                contentDescription = "Convert"
                            )
                        },
                        label = { Text("Convert") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = YtRedPrimary,
                            selectedTextColor = YtRedPrimary,
                            unselectedIconColor = TextMutedSecondary,
                            unselectedTextColor = TextMutedSecondary,
                            indicatorColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_convert")
                    )

                    NavigationBarItem(
                        selected = uiState.activeTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (downloadedTracks.isNotEmpty()) {
                                        Badge(
                                            containerColor = YtRedPrimary,
                                            contentColor = Color.White
                                        ) {
                                            Text("${downloadedTracks.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (uiState.activeTab == 1) Icons.Default.LibraryMusic else Icons.Outlined.LibraryMusic,
                                    contentDescription = "Saved MP3s"
                                )
                            }
                        },
                        label = { Text("Saved MP3s") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = YtRedPrimary,
                            selectedTextColor = YtRedPrimary,
                            unselectedIconColor = TextMutedSecondary,
                            unselectedTextColor = TextMutedSecondary,
                            indicatorColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_library")
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = uiState.activeTab,
            label = "ScreenCrossfade",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { tab ->
            when (tab) {
                0 -> ConvertScreen(
                    uiState = uiState,
                    onUrlChanged = { viewModel.onUrlChanged(it) },
                    onClearUrl = { viewModel.clearUrl() },
                    onSampleSelected = { viewModel.loadSample(it) },
                    onQualitySelected = { viewModel.selectQuality(it) },
                    onStartConvert = { viewModel.startConversion() },
                    onCancelConvert = { viewModel.cancelConversion() },
                    onDismissSuccess = { viewModel.dismissSuccess() },
                    onPlayTrack = {
                        viewModel.playTrack(it)
                        viewModel.selectTab(1)
                    },
                    onNavigateToLibrary = { viewModel.selectTab(1) }
                )
                1 -> LibraryScreen(
                    tracks = downloadedTracks,
                    playbackState = playbackState,
                    onPlayTrack = { viewModel.playTrack(it) },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onDeleteTrack = { viewModel.deleteTrack(it) },
                    onNavigateToConvert = { viewModel.selectTab(0) }
                )
            }
        }
    }
}
