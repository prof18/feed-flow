package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text

internal const val READER_AUDIO_BANNER_TAG = "reader_audio_banner"

// The reader header already shows the article title, so the banner only offers the episode.
@Composable
internal fun AudioEpisodeBanner(
    title: String?,
    onOpenAudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val episodeTitle = title?.takeIf { it.isNotBlank() } ?: strings.audioEpisodeUntitled
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(width = 1.dp, color = JewelTheme.globalColors.borders.normal, shape = shape)
            .clickable(role = Role.Button, onClick = onOpenAudio)
            .testTag(READER_AUDIO_BANNER_TAG)
            .semantics(mergeDescendants = true) {
                contentDescription = "${strings.menuOpenAudio}: $episodeTitle"
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = rememberVectorPainter(Icons.Outlined.Headphones),
            contentDescription = null,
            tint = JewelTheme.globalColors.text.normal,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(strings.audioEpisodeLabel, color = JewelTheme.globalColors.text.info)
            Text(episodeTitle, fontWeight = FontWeight.SemiBold)
            Text(strings.menuOpenAudio, fontWeight = FontWeight.SemiBold)
        }
    }
}
