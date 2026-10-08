package com.prof18.feedflow.android.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.exposeTestTagsAsResourceIds

@Composable
internal fun AudioPlayerDock(
    state: AudioPlaybackState,
    onToggle: () -> Unit,
    onClose: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlaybackSpeedChange: (AudioPlaybackSpeed) -> Unit,
    onOpenEpisode: ((AudioEpisode) -> Unit)?,
    onOpenExternal: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val episode = state.episode ?: return
    val strings = LocalFeedFlowStrings.current
    var seekPosition by remember(episode.itemId) { mutableStateOf<Float?>(null) }
    Column(modifier = modifier.testTag("audio_player")) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Icon(Icons.Default.Headphones, null, tint = MaterialTheme.colorScheme.primary)
                episode.artworkUrl?.let { artwork ->
                    AsyncImage(
                        model = artwork,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f).then(
                    if (onOpenEpisode != null) {
                        Modifier.testTag("audio_dock_return_to_episode")
                            .clickable(onClickLabel = strings.audioReturnToEpisode) { onOpenEpisode(episode) }
                    } else {
                        Modifier
                    },
                ).padding(horizontal = 12.dp),
            ) {
                Text(
                    text = episode.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.testTag("audio_player_title"),
                )
                episode.feedName?.let { feed ->
                    Text(
                        text = feed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.isLoading) {
                    Text(strings.audioLoading, style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .testTag(if (state.isPlaying) "audio_pause" else "audio_play")
                    .semantics {
                        contentDescription = if (state.isPlaying) strings.audioPause else strings.audioPlay
                    },
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) strings.audioPause else strings.audioPlay,
                    )
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.testTag("audio_close_player")) {
                Icon(Icons.Default.Close, strings.audioClosePlayer)
            }
        }
        if (state.failed) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    strings.audioPlaybackFailed,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onOpenExternal(episode.url) }) { Text(strings.audioOpenExternal) }
            }
        }
        Slider(
            value = seekPosition ?: state.positionMs.toFloat(),
            onValueChange = { seekPosition = it },
            onValueChangeFinished = {
                seekPosition?.let { onSeek(it.toLong()) }
                seekPosition = null
            },
            valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(),
            enabled = state.canSeek,
            modifier = Modifier.padding(horizontal = 16.dp).testTag("audio_seek")
                .semantics { contentDescription = strings.audioSeek },
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                audioTime(state.positionMs),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.testTag("audio_position"),
            )
            PlaybackSpeedSelector(state.playbackSpeed, onPlaybackSpeedChange)
            Text(audioTime(state.durationMs), style = MaterialTheme.typography.labelSmall)
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
private fun PlaybackSpeedSelector(speed: AudioPlaybackSpeed, onSpeedChange: (AudioPlaybackSpeed) -> Unit) {
    val strings = LocalFeedFlowStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag("audio_speed").semantics { contentDescription = strings.audioPlaybackSpeed },
        ) {
            Text(speed.formattedLabel())
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.exposeTestTagsAsResourceIds(),
        ) {
            AudioPlaybackSpeed.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.formattedLabel()) },
                    onClick = {
                        onSpeedChange(option)
                        expanded = false
                    },
                    modifier = Modifier.testTag("audio_speed_${option.labelValue}"),
                )
            }
        }
    }
}

private fun audioTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / MILLIS_PER_SECOND
    val minutes = seconds / SECONDS_PER_MINUTE
    return "$minutes:${(seconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')}"
}

private const val MILLIS_PER_SECOND = 1000
private const val SECONDS_PER_MINUTE = 60

internal fun readerAudioDockContent(
    state: AudioPlaybackState,
    player: AndroidAudioPlayer,
    onOpenExternal: (String) -> Unit,
    onOpenEpisode: ((AudioEpisode) -> Unit)? = null,
): (@Composable () -> Unit)? {
    if (state.episode == null) return null
    return {
        AudioPlayerDock(
            state = state,
            onToggle = player::togglePlayback,
            onClose = player::close,
            onSeek = player::seek,
            onPlaybackSpeedChange = player::setPlaybackSpeed,
            onOpenEpisode = onOpenEpisode,
            onOpenExternal = onOpenExternal,
        )
    }
}
