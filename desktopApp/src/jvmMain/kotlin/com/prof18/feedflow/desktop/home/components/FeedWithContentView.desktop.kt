package com.prof18.feedflow.desktop.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.shared.presentation.preview.inProgressFeedUpdateStatus
import com.prof18.feedflow.shared.ui.home.components.FeedList
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
internal fun FeedWithContentView(
    paddingValues: PaddingValues,
    feedState: ImmutableList<FeedItem>,
    feedFontSizes: FeedFontSizes,
    loadingState: FeedUpdateStatus,
    listState: LazyListState,
    currentFeedFilter: FeedFilter,
    updateReadStatus: (Int) -> Unit,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    requestMoreItems: () -> Unit,
    markAllAsRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        FeedLoader(loadingState = loadingState)

        Box(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues)
                .padding(end = 4.dp),
        ) {
            FeedList(
                modifier = Modifier,
                feedItems = feedState,
                listState = listState,
                feedFontSize = feedFontSizes,
                shareCommentsMenuLabel = LocalFeedFlowStrings.current.menuCopyLinkComments,
                shareMenuLabel = LocalFeedFlowStrings.current.menuCopyLink,
                currentFeedFilter = currentFeedFilter,
                requestMoreItems = requestMoreItems,
                onFeedItemClick = onFeedItemClick,
                onBookmarkClick = onBookmarkClick,
                onReadStatusClick = onReadStatusClick,
                onCommentClick = onCommentClick,
                updateReadStatus = { index ->
                    updateReadStatus(index)
                },
                markAllAsRead = markAllAsRead,
                onShareClick = { urlTitle ->
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(StringSelection(urlTitle.url), null)
                }
            )

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(
                    scrollState = listState,
                ),
            )
        }
    }
}

@Composable
private fun ColumnScope.FeedLoader(loadingState: FeedUpdateStatus) {
    AnimatedVisibility(loadingState.isLoading()) {
        val feedRefreshCounter = if (loadingState.refreshedFeedCount > 0 && loadingState.totalFeedCount > 0) {
            "${loadingState.refreshedFeedCount}/${loadingState.totalFeedCount}"
        } else {
            "..."
        }

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.regular),
            text = LocalFeedFlowStrings.current.loadingFeedMessage(feedRefreshCounter),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview
@Composable
private fun FeedWithContentViewPreview() {
    FeedFlowTheme {
        FeedWithContentView(
            feedState = feedItemsForPreview,
            loadingState = inProgressFeedUpdateStatus,
            listState = LazyListState(),
            feedFontSizes = FeedFontSizes(),
            currentFeedFilter = FeedFilter.Timeline,
            updateReadStatus = { },
            onFeedItemClick = { },
            onBookmarkClick = { _, _ -> },
            onReadStatusClick = { _, _ -> },
            onCommentClick = { },
            requestMoreItems = { },
            paddingValues = PaddingValues(),
            markAllAsRead = { },
        )
    }
}
