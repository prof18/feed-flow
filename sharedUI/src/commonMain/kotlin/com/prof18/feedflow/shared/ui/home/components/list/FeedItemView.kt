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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedItemUrlTitle
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.ui.feedsourcelist.singleAndLongClickModifier
import com.prof18.feedflow.shared.ui.preview.feedItemsForPreview
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.PreviewHelper

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
    currentFeedFilter: FeedFilter = FeedFilter.Timeline,
    onMarkAllBelowAsRead: (String) -> Unit,
) {
    var showItemMenu by remember {
        mutableStateOf(
            false,
        )
    }

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
                        linkOpeningPreference = feedItem.feedSource.linkOpeningPreference,
                        commentsUrl = feedItem.commentsUrl,
                    ),
                )
            },
            onLongClick = {
                showItemMenu = true
            },
        )
    }

    Column(modifier = modifier) {
        Column(
            modifier = clickableItemModifier
                .padding(horizontal = Spacing.regular)
                .padding(
                    top = Spacing.small,
                    bottom = Spacing.regular,
                ),
        ) {
            FeedSourceAndUnreadDotRow(
                feedItem = feedItem,
                feedFontSize = feedFontSize,
                currentFeedFilter = currentFeedFilter,
            )

            TitleSubtitleAndImageRow(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth(),
                feedItem = feedItem,
                feedFontSize = feedFontSize,
                currentFeedFilter = currentFeedFilter,
            )

            feedItem.dateString?.let { dateString ->
                Text(
                    modifier = Modifier
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

            FeedItemContextMenu(
                showMenu = showItemMenu,
                closeMenu = {
                    showItemMenu = false
                },
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

        if (feedLayout == FeedLayout.LIST) {
            HorizontalDivider(
                thickness = 0.2.dp,
                color = Color.Gray,
            )
        }
    }
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
