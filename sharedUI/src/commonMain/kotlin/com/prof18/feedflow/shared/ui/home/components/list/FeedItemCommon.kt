package com.prof18.feedflow.shared.ui.home.components.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.core.model.DescriptionLineLimit
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.shared.ui.components.FeedSourceLogoImage
import com.prof18.feedflow.shared.ui.home.components.FeedItemHeroImage
import com.prof18.feedflow.shared.ui.home.components.FeedItemImage
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

private val ImageCardUnreadDotSize = 9.dp
private const val ReadTextAlpha = 0.6f
private const val ReadImageAlpha = 0.76f

@Composable
internal fun FeedSourceAndUnreadDotRow(
    feedItem: FeedItem,
    feedFontSize: FeedFontSizes,
    currentFeedFilter: FeedFilter = FeedFilter.Timeline,
    isHideUnreadDotEnabled: Boolean = false,
    isHideFeedSourceEnabled: Boolean = false,
) {
    val showUnreadDot = !feedItem.isRead && !isHideUnreadDotEnabled
    val showFeedSource = !isHideFeedSourceEnabled
    val showBookmark = feedItem.isBookmarked

    if (!showUnreadDot && !showFeedSource && !showBookmark) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showUnreadDot) {
            UnreadDot(
                modifier = Modifier
                    .padding(
                        bottom = Spacing.small,
                        end = Spacing.small,
                    ),
            )
        }

        if (showFeedSource) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = Spacing.small),
                text = feedItem.feedSource.title,
                fontSize = feedFontSize.feedMetaFontSize.sp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (feedItem.isRead &&
                        currentFeedFilter !is FeedFilter.Read && currentFeedFilter !is FeedFilter.Bookmarks
                    ) {
                        0.6f
                    } else {
                        1f
                    },
                ),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        if (showBookmark) {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .padding(bottom = Spacing.small),
                tint = MaterialTheme.colorScheme.primary,
                imageVector = Icons.Filled.Bookmark,
                contentDescription = LocalFeedFlowStrings.current.articleBookmarkedContentDescription,
            )
        }
    }
}

@Composable
internal fun TitleSubtitleAndImageRow(
    feedItem: FeedItem,
    feedFontSize: FeedFontSizes,
    modifier: Modifier = Modifier,
    currentFeedFilter: FeedFilter = FeedFilter.Timeline,
    descriptionLineLimit: DescriptionLineLimit = DescriptionLineLimit.THREE,
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
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (feedItem.isRead &&
                            currentFeedFilter !is FeedFilter.Read && currentFeedFilter !is FeedFilter.Bookmarks
                        ) {
                            0.6f
                        } else {
                            1f
                        },
                    ),
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
                    maxLines = descriptionLineLimit.lines,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = feedFontSize.feedDescFontSize.sp,
                    lineHeight = (feedFontSize.feedDescFontSize + 6).sp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (feedItem.isRead &&
                            currentFeedFilter !is FeedFilter.Read && currentFeedFilter !is FeedFilter.Bookmarks
                        ) {
                            0.6f
                        } else {
                            1f
                        },
                    ),
                )
            }
        }

        feedItem.imageUrl?.let { url ->
            FeedItemImage(
                modifier = Modifier
                    .testTag(FeedItemE2eIds.image(feedItem.id))
                    .padding(start = Spacing.regular),
                url = url,
                width = 96.dp,
                imageAlpha = feedItem.imageAlpha(currentFeedFilter),
            )
        }
    }
}

@Composable
internal fun FeedItemImageCardContent(
    feedItem: FeedItem,
    feedFontSize: FeedFontSizes,
    isGridCell: Boolean,
    heroImageAspectRatio: Float,
    currentFeedFilter: FeedFilter = FeedFilter.Timeline,
    feedItemDisplaySettings: FeedItemDisplaySettings = FeedItemDisplaySettings(),
) {
    val hasSourceRow = feedItem.hasCardSourceRow(
        isHideUnreadDotEnabled = feedItemDisplaySettings.isHideUnreadDotEnabled,
        isHideFeedSourceEnabled = feedItemDisplaySettings.isHideFeedSourceEnabled,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        feedItem.imageUrl?.let { url ->
            FeedItemHeroImage(
                modifier = Modifier.testTag(FeedItemE2eIds.image(feedItem.id)),
                url = url,
                aspectRatio = heroImageAspectRatio,
                imageAlpha = feedItem.imageAlpha(currentFeedFilter),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Spacing.regular,
                    vertical = Spacing.regular,
                ),
        ) {
            FeedItemCardSourceRow(
                feedItem = feedItem,
                feedFontSize = feedFontSize,
                currentFeedFilter = currentFeedFilter,
                isHideUnreadDotEnabled = feedItemDisplaySettings.isHideUnreadDotEnabled,
                isHideFeedSourceEnabled = feedItemDisplaySettings.isHideFeedSourceEnabled,
            )

            feedItem.title?.let { title ->
                Text(
                    modifier = Modifier.padding(top = if (hasSourceRow) Spacing.small else 0.dp),
                    text = title,
                    maxLines = if (isGridCell) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = feedFontSize.feedTitleFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    lineHeight = (feedFontSize.feedTitleFontSize + 4).sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = feedItem.readAlpha(currentFeedFilter),
                    ),
                )
            }

            feedItem.subtitle?.let { subtitle ->
                Text(
                    modifier = Modifier.padding(top = Spacing.small),
                    text = subtitle,
                    maxLines = feedItemDisplaySettings.descriptionLineLimit.lines,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = feedFontSize.feedDescFontSize.sp,
                    lineHeight = (feedFontSize.feedDescFontSize + 6).sp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = feedItem.readAlpha(currentFeedFilter),
                    ),
                )
            }

            feedItem.dateString?.let { dateString ->
                Text(
                    modifier = Modifier.padding(top = Spacing.small),
                    text = dateString,
                    fontSize = feedFontSize.feedMetaFontSize.sp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = feedItem.readAlpha(currentFeedFilter),
                    ),
                )
            }
        }
    }
}

internal fun FeedItem.hasCardSourceRow(
    isHideUnreadDotEnabled: Boolean,
    isHideFeedSourceEnabled: Boolean,
): Boolean = (!isRead && !isHideUnreadDotEnabled) || !isHideFeedSourceEnabled || isBookmarked

@Composable
internal fun FeedItemCardSourceRow(
    feedItem: FeedItem,
    feedFontSize: FeedFontSizes,
    currentFeedFilter: FeedFilter,
    isHideUnreadDotEnabled: Boolean,
    isHideFeedSourceEnabled: Boolean,
) {
    val showUnreadDot = !feedItem.isRead && !isHideUnreadDotEnabled
    val showFeedSource = !isHideFeedSourceEnabled
    val showBookmark = feedItem.isBookmarked

    if (!showUnreadDot && !showFeedSource && !showBookmark) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showFeedSource) {
            FeedSourceLogo(
                feedItem = feedItem,
            )
            Text(
                modifier = Modifier
                    .padding(start = Spacing.small)
                    .weight(1f),
                text = feedItem.feedSource.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = feedFontSize.feedMetaFontSize.sp,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = feedItem.readAlpha(currentFeedFilter),
                ),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        if (showBookmark) {
            Icon(
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
                imageVector = Icons.Filled.Bookmark,
                contentDescription = LocalFeedFlowStrings.current.articleBookmarkedContentDescription,
            )
        }

        if (showUnreadDot) {
            UnreadDot(
                modifier = Modifier
                    .padding(start = Spacing.small),
                size = ImageCardUnreadDotSize,
            )
        }
    }
}

@Composable
private fun FeedSourceLogo(
    feedItem: FeedItem,
) {
    feedItem.feedSource.logoUrl?.let { logoUrl ->
        FeedSourceLogoImage(
            imageUrl = logoUrl,
            size = 24.dp,
            cornerRadius = 7.dp,
        )
    } ?: Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(size = 7.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.Default.Category,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

private fun FeedItem.readAlpha(currentFeedFilter: FeedFilter): Float =
    if (isRead && currentFeedFilter !is FeedFilter.Read && currentFeedFilter !is FeedFilter.Bookmarks) {
        ReadTextAlpha
    } else {
        1f
    }

private fun FeedItem.imageAlpha(currentFeedFilter: FeedFilter): Float =
    if (isRead && currentFeedFilter !is FeedFilter.Read && currentFeedFilter !is FeedFilter.Bookmarks) {
        ReadImageAlpha
    } else {
        1f
    }

@Composable
private fun UnreadDot(
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
    )
}
