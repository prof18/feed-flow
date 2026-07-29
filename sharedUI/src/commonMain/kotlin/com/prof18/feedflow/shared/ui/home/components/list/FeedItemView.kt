package com.prof18.feedflow.shared.ui.home.components.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedItemUrlTitle
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.utils.ContentDirection
import com.prof18.feedflow.shared.ui.feedsourcelist.singleAndLongClickModifier
import com.prof18.feedflow.shared.ui.preview.feedItemsForPreview
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.PreviewHelper

private const val DefaultHeroImageAspectRatio = 16f / 9f

@Composable
internal fun FeedItemView(
    feedItem: FeedItem,
    feedFontSize: FeedFontSizes,
    shareMenuLabel: String,
    shareCommentsMenuLabel: String,
    feedLayout: FeedLayout,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    onOpenFeedSettings: (FeedSource) -> Unit,
    onOpenFeedWebsite: (String) -> Unit,
    onShareClick: (FeedItemUrlTitle) -> Unit,
    onMarkAllAboveAsRead: (String) -> Unit,
    modifier: Modifier = Modifier,
    disableClick: Boolean = false,
    isGridCell: Boolean = false,
    heroImageAspectRatio: Float = DefaultHeroImageAspectRatio,
    currentFeedFilter: FeedFilter = FeedFilter.Timeline,
    feedItemDisplaySettings: FeedItemDisplaySettings = FeedItemDisplaySettings(),
    onMarkAllBelowAsRead: (String) -> Unit,
) {
    var showItemMenu by remember {
        mutableStateOf(
            false,
        )
    }
    var menuPositionInWindow by remember { mutableStateOf<Offset?>(null) }

    val clickableItemModifier = if (disableClick) {
        Modifier
    } else {
        Modifier.singleAndLongClickModifier(
            onClick = {
                onFeedItemClick(
                    FeedItemUrlInfo(
                        id = feedItem.id,
                        url = feedItem.url,
                        title = feedItem.title,
                        isBookmarked = feedItem.isBookmarked,
                        articleOpenMode = feedItem.feedSource.articleOpenMode,
                        commentsUrl = feedItem.commentsUrl,
                        imageUrl = feedItem.imageUrl,
                        feedSourceTitle = feedItem.feedSource.title,
                    ),
                )
            },
            onLongClick = {
                menuPositionInWindow = null
                showItemMenu = true
            },
            onLongClickPositioned = { position ->
                menuPositionInWindow = position
                showItemMenu = true
            },
        )
    }

    val contentLayoutDirection = contentLayoutDirection(feedItem)

    val normalizedFeedLayout = if (feedLayout == FeedLayout.GRID) FeedLayout.BIG_IMAGE else feedLayout
    if (normalizedFeedLayout == FeedLayout.BIG_IMAGE) {
        Column(modifier = modifier) {
            Column(
                modifier = clickableItemModifier
                    .testTag(FeedItemE2eIds.row(feedItem.id)),
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides contentLayoutDirection) {
                    FeedItemImageCardContent(
                        feedItem = feedItem,
                        feedFontSize = feedFontSize,
                        isGridCell = isGridCell,
                        heroImageAspectRatio = heroImageAspectRatio,
                        currentFeedFilter = currentFeedFilter,
                        feedItemDisplaySettings = feedItemDisplaySettings,
                    )
                }

                FeedItemContextMenu(
                    showMenu = showItemMenu,
                    closeMenu = {
                        showItemMenu = false
                        menuPositionInWindow = null
                    },
                    menuPositionInWindow = menuPositionInWindow,
                    feedItem = feedItem,
                    shareMenuLabel = shareMenuLabel,
                    shareCommentsMenuLabel = shareCommentsMenuLabel,
                    onBookmarkClick = onBookmarkClick,
                    onReadStatusClick = onReadStatusClick,
                    onCommentClick = onCommentClick,
                    onShareClick = onShareClick,
                    onOpenFeedSettings = onOpenFeedSettings,
                    onOpenFeedWebsite = onOpenFeedWebsite,
                    onMarkAllAboveAsRead = onMarkAllAboveAsRead,
                    onMarkAllBelowAsRead = onMarkAllBelowAsRead,
                )
            }
        }
        return
    }

    Column(modifier = modifier) {
        Column(
            modifier = clickableItemModifier
                .testTag(FeedItemE2eIds.row(feedItem.id))
                .padding(horizontal = Spacing.regular)
                .padding(
                    top = Spacing.small,
                    bottom = Spacing.regular,
                ),
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides contentLayoutDirection) {
                FeedSourceAndUnreadDotRow(
                    feedItem = feedItem,
                    feedFontSize = feedFontSize,
                    currentFeedFilter = currentFeedFilter,
                    isHideUnreadDotEnabled = feedItemDisplaySettings.isHideUnreadDotEnabled,
                    isHideFeedSourceEnabled = feedItemDisplaySettings.isHideFeedSourceEnabled,
                )

                TitleSubtitleAndImageRow(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .fillMaxWidth(),
                    feedItem = feedItem,
                    feedFontSize = feedFontSize,
                    currentFeedFilter = currentFeedFilter,
                    descriptionLineLimit = feedItemDisplaySettings.descriptionLineLimit,
                )

                feedItem.dateString?.let { dateString ->
                    Text(
                        // Fills the width so the date follows the item direction: the enclosing
                        // Column resolves its alignment outside this provider, in locale direction.
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.small),
                        text = dateString,
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
                }
            }

            FeedItemContextMenu(
                showMenu = showItemMenu,
                closeMenu = {
                    showItemMenu = false
                    menuPositionInWindow = null
                },
                menuPositionInWindow = menuPositionInWindow,
                feedItem = feedItem,
                shareMenuLabel = shareMenuLabel,
                shareCommentsMenuLabel = shareCommentsMenuLabel,
                onBookmarkClick = onBookmarkClick,
                onReadStatusClick = onReadStatusClick,
                onCommentClick = onCommentClick,
                onShareClick = onShareClick,
                onOpenFeedSettings = onOpenFeedSettings,
                onOpenFeedWebsite = onOpenFeedWebsite,
                onMarkAllAboveAsRead = onMarkAllAboveAsRead,
                onMarkAllBelowAsRead = onMarkAllBelowAsRead,
            )
        }

        if (normalizedFeedLayout == FeedLayout.LIST) {
            HorizontalDivider(
                thickness = 0.2.dp,
                color = Color.Gray,
            )
        }
    }
}

/**
 * Mirrors a single item when its own content reads in the opposite direction of the app locale,
 * so a Persian article is laid out right-to-left even while the UI is in English. The direction
 * is resolved once while mapping; items whose text has no direction follow the app locale.
 */
@Composable
private fun contentLayoutDirection(feedItem: FeedItem): LayoutDirection =
    when (feedItem.contentDirection) {
        ContentDirection.LEFT_TO_RIGHT -> LayoutDirection.Ltr
        ContentDirection.RIGHT_TO_LEFT -> LayoutDirection.Rtl
        null -> LocalLayoutDirection.current
    }

@Preview
@Composable
internal fun FeedItemListViewPreview() {
    PreviewHelper {
        FeedItemView(
            feedItem = feedItemsForPreview.first(),
            feedFontSize = FeedFontSizes(),
            feedLayout = FeedLayout.LIST,
            shareMenuLabel = "Share",
            shareCommentsMenuLabel = "Share Comments",
            onFeedItemClick = {},
            onBookmarkClick = { _, _ -> },
            onReadStatusClick = { _, _ -> },
            onCommentClick = {},
            onShareClick = {},
            onOpenFeedSettings = {},
            onOpenFeedWebsite = {},
            disableClick = true,
            onMarkAllAboveAsRead = {},
            onMarkAllBelowAsRead = {},
        )
    }
}
