package com.example.ui.screens

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DownloadedTrack
import com.example.ui.ConversionState
import com.example.ui.UiState
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMutedSecondary
import com.example.ui.theme.TextWhitePrimary
import com.example.ui.theme.YtRedPrimary
import com.example.util.FileUtils
import com.example.util.SampleVideo
import com.example.util.YouTubeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertScreen(
    uiState: UiState,
    onUrlChanged: (String) -> Unit,
    onClearUrl: () -> Unit,
    onSampleSelected: (SampleVideo) -> Unit,
    onQualitySelected: (String) -> Unit,
    onStartConvert: () -> Unit,
    onCancelConvert: () -> Unit,
    onDismissSuccess: () -> Unit,
    onPlayTrack: (DownloadedTrack) -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val qualities = listOf("320 kbps", "256 kbps", "192 kbps", "128 kbps")
    val convState = uiState.conversionState

    fun pasteFromClipboard() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                val clipData = clipboard.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).coerceToText(context).toString().trim()
                    if (text.isNotEmpty()) {
                        onUrlChanged(text)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Quick Instructions / Hero Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF3B070B),
                                DarkSurfaceVariant
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(YtRedPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "YouTube to MP3 Converter",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhitePrimary
                            )
                        )
                        Text(
                            text = "Paste any link to convert and save to local storage",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMutedSecondary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // URL Input Box
        OutlinedTextField(
            value = uiState.urlInput,
            onValueChange = onUrlChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("url_input_field"),
            placeholder = {
                Text(
                    text = "https://www.youtube.com/watch?v=...",
                    color = TextMutedSecondary,
                    fontSize = 14.sp
                )
            },
            label = { Text("YouTube URL or Video Link") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = YtRedPrimary
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.urlInput.isNotEmpty()) {
                        IconButton(
                            onClick = onClearUrl,
                            modifier = Modifier.testTag("clear_url_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear URL",
                                tint = TextMutedSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = { pasteFromClipboard() },
                        modifier = Modifier.testTag("paste_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste from clipboard",
                            tint = YtRedPrimary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = YtRedPrimary,
                unfocusedBorderColor = DarkSurfaceBorder,
                focusedContainerColor = DarkSurfaceVariant,
                unfocusedContainerColor = DarkSurfaceVariant,
                focusedTextColor = TextWhitePrimary,
                unfocusedTextColor = TextWhitePrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Try Samples Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Samples:",
                style = MaterialTheme.typography.labelSmall.copy(color = TextMutedSecondary)
            )
            YouTubeUtils.POPULAR_SAMPLES.forEach { sample ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceHighlight)
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
                        .clickable { onSampleSelected(sample) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("sample_chip_${sample.videoId}")
                ) {
                    Text(
                        text = sample.title.take(18) + "...",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextWhitePrimary,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Analyzing Loading Indicator
        AnimatedVisibility(
            visible = uiState.isAnalyzing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = YtRedPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Analyzing YouTube video stream...",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedSecondary)
                )
            }
        }

        // Video Preview Card
        AnimatedVisibility(
            visible = uiState.previewInfo != null,
            enter = slideInVertically() + fadeIn(),
            exit = fadeOut()
        ) {
            val preview = uiState.previewInfo
            if (preview != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_preview_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkSurfaceBorder, YtRedPrimary.copy(alpha = 0.4f))))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Thumbnail with aspect ratio
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                        ) {
                            AsyncImage(
                                model = preview.thumbnailUrl,
                                contentDescription = "Video Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Duration / MP3 badge overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.8f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "AUDIO / MP3",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Title & Author
                        Text(
                            text = preview.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhitePrimary
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Channel: ${preview.author}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AccentEmerald,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Audio Bitrate Quality Selection
                        Text(
                            text = "Target Audio Bitrate:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextMutedSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            qualities.forEach { q ->
                                val isSelected = uiState.selectedQuality == q
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onQualitySelected(q) },
                                    label = { Text(q) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = YtRedPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = DarkSurfaceHighlight,
                                        labelColor = TextWhitePrimary
                                    ),
                                    modifier = Modifier.testTag("quality_chip_$q")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Local destination note
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = TextMutedSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Saves to: Device Storage / Music",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMutedSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Conversion Active State (Progress Bar)
        if (convState is ConversionState.Converting) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("converting_progress_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkSurfaceBorder, AccentEmerald)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = convState.statusMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhitePrimary
                            )
                        )
                        Text(
                            text = "${(convState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = YtRedPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { convState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = YtRedPrimary,
                        trackColor = DarkSurfaceHighlight
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onCancelConvert,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cancel_conversion_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMutedSecondary)
                    ) {
                        Text("Cancel Conversion")
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Error message
        if (convState is ConversionState.Error) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("error_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF331114))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = YtRedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = convState.message,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextWhitePrimary),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Primary Convert & Save Button
        val isConverting = convState is ConversionState.Converting
        Button(
            onClick = onStartConvert,
            enabled = !isConverting && (uiState.previewInfo != null || uiState.urlInput.isNotBlank()),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("convert_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = YtRedPrimary,
                disabledContainerColor = DarkSurfaceHighlight,
                contentColor = Color.White,
                disabledContentColor = TextMutedSecondary
            )
        ) {
            if (isConverting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Converting to MP3...")
            } else {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Convert & Quick Save as MP3",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Success Dialog on Completion
    if (convState is ConversionState.Success) {
        val track = convState.track
        AlertDialog(
            onDismissRequest = onDismissSuccess,
            shape = RoundedCornerShape(20.dp),
            containerColor = DarkSurfaceVariant,
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(AccentEmerald.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(34.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Saved to Local Device!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextWhitePrimary
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhitePrimary
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${track.artist} • ${FileUtils.formatFileSize(track.fileSizeBytes)} • ${track.quality}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMutedSecondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "File: ${track.fileName}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AccentEmerald,
                            fontSize = 11.sp
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissSuccess()
                        onPlayTrack(track)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YtRedPrimary),
                    modifier = Modifier.testTag("success_play_now_btn")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play Now")
                }
            },
            dismissButton = {
                Row {
                    IconButton(
                        onClick = { FileUtils.shareTrack(context, track) },
                        modifier = Modifier.testTag("success_share_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = TextWhitePrimary)
                    }
                    TextButton(
                        onClick = onDismissSuccess,
                        modifier = Modifier.testTag("success_done_btn")
                    ) {
                        Text("Done", color = TextMutedSecondary)
                    }
                }
            }
        )
    }
}
