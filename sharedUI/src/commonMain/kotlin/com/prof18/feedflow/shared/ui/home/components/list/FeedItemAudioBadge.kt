package com.prof18.feedflow.shared.ui.home.components.list

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun FeedItemAudioBadge(
    feedItemId: String,
    modifier: Modifier = Modifier,
) {
    Icon(
        modifier = modifier
            .size(size = 16.dp)
            .testTag(FeedItemE2eIds.audio(feedItemId)),
        imageVector = Icons.Outlined.Headphones,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        contentDescription = LocalFeedFlowStrings.current.audioEpisodeBadgeContentDescription,
    )
}
