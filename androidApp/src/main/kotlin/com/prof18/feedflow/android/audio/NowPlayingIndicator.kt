package com.prof18.feedflow.android.audio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun NowPlayingIndicator(
    state: AudioPlaybackState,
    onOpenEpisode: (AudioEpisode) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val episode = state.episode ?: return
    val strings = LocalFeedFlowStrings.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.testTag("audio_now_playing"),
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                        .testTag("audio_return_to_episode")
                        .clickable(onClickLabel = strings.audioReturnToEpisode) { onOpenEpisode(episode) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    if (episode.artworkUrl != null) {
                        AsyncImage(
                            model = episode.artworkUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(size = 38.dp).clip(MaterialTheme.shapes.small),
                        )
                    } else {
                        Icon(Icons.Default.Headphones, null, modifier = Modifier.size(size = 38.dp))
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(
                            if (state.isPlaying) strings.audioNowPlaying else strings.audioPaused,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        Text(
                            episode.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.padding(end = 6.dp).testTag(
                        if (state.isPlaying) "audio_mini_pause" else "audio_mini_play",
                    ),
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (state.isPlaying) strings.audioPause else strings.audioPlay,
                    )
                }
            }
        }
    }
}
