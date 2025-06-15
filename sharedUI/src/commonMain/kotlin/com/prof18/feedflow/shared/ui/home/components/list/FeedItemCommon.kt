package com.prof18.feedflow.shared.ui.home.components.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.shared.ui.home.components.FeedItemImage
import com.prof18.feedflow.shared.ui.style.Spacing

@Composable
internal fun FeedSourceAndUnreadDotRow(
    feedItem: FeedItem,
    feedFontSize: FeedFontSizes,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!feedItem.isRead) {
            UnreadDot(
                modifier = Modifier
                    .padding(
                        bottom = Spacing.small,
                        end = Spacing.small,
                    ),
            )
        }

        Text(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = Spacing.small),
            text = feedItem.feedSource.title,
            fontSize = feedFontSize.feedMetaFontSize.sp,
            style = MaterialTheme.typography.bodySmall,
        )

        if (feedItem.isBookmarked) {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .padding(bottom = Spacing.small),
                tint = MaterialTheme.colorScheme.primary,
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
            )
        }
    }
}

@Composable
internal fun TitleSubtitleAndImageRow(
    feedItem: FeedItem,
    feedFontSize: FeedFontSizes,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f),
        ) {
            feedItem.title?.let { title ->
                Text(
                    text = title,
                    fontSize = feedFontSize.feedTitleFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    lineHeight = (feedFontSize.feedTitleFontSize + 4).sp,
                )
            }

            val paddingTop = when {
                feedItem.title != null -> Spacing.small
                else -> 0.dp
            }

            feedItem.subtitle?.let { subtitle ->
                Text(
                    modifier = Modifier
                        .padding(top = paddingTop),
                    text = subtitle,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = feedFontSize.feedDescFontSize.sp,
                    lineHeight = (feedFontSize.feedDescFontSize + 6).sp,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        feedItem.imageUrl?.let { url ->
            FeedItemImage(
                modifier = Modifier
                    .padding(start = Spacing.regular),
                url = url,
                width = 96.dp,
            )
        }
    }
}

@Composable
private fun UnreadDot(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
    )
}
