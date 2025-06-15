package com.prof18.feedflow.shared.ui.home.components.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedItemUrlTitle
import com.prof18.feedflow.shared.ui.feedsourcelist.feedSourceMenuClickModifier
import com.prof18.feedflow.shared.ui.preview.feedItemsForPreview
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.PreviewHelper
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun FeedItemCard(
    feedItem: FeedItem,
    feedFontSize: FeedFontSizes,
    shareMenuLabel: String,
    shareCommentsMenuLabel: String,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    onShareClick: (FeedItemUrlTitle) -> Unit,
    modifier: Modifier = Modifier,
    disableClick: Boolean = false,
) {
    var showItemMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.padding(Spacing.small),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (disableClick) {
                        Modifier
                    } else {
                        Modifier.feedSourceMenuClickModifier(
                            onClick = {
                                onFeedItemClick(
                                    FeedItemUrlInfo(
                                        id = feedItem.id,
                                        url = feedItem.url,
                                        title = feedItem.title,
                                        isBookmarked = feedItem.isBookmarked,
                                        linkOpeningPreference = feedItem.feedSource.linkOpeningPreference,
                                    ),
                                )
                            },
                            onLongClick = {
                                showItemMenu = true
                            },
                        )
                    },
                )
                .padding(horizontal = Spacing.regular)
                .padding(vertical = Spacing.regular),
        ) {
            FeedSourceAndUnreadDotRow(
                feedItem = feedItem,
                feedFontSize = feedFontSize,
            )

            TitleSubtitleAndImageRow(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth(),
                feedItem = feedItem,
                feedFontSize = feedFontSize,
            )

            feedItem.dateString?.let { dateString ->
                Text(
                    modifier = Modifier
                        .padding(top = Spacing.small),
                    text = dateString,
                    fontSize = feedFontSize.feedMetaFontSize.sp,
                    style = MaterialTheme.typography.bodySmall,
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
            )
        }
    }
}

@Preview
@Composable
private fun FeedItemCardPreview() {
    PreviewHelper {
        FeedItemCard(
            feedItem = feedItemsForPreview.first(),
            feedFontSize = FeedFontSizes(),
            shareMenuLabel = "Share",
            shareCommentsMenuLabel = "Share Comments",
            onFeedItemClick = {},
            onBookmarkClick = { _, _ -> },
            onReadStatusClick = { _, _ -> },
            onCommentClick = {},
            onShareClick = {},
            disableClick = true,
        )
    }
}
