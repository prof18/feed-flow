package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun EmptyFeedView(
    currentFeedFilter: FeedFilter,
    isDrawerVisible: Boolean,
    onReloadClick: () -> Unit,
    onBackToTimelineClick: () -> Unit,
    onOpenDrawerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val emptyMessage = when (currentFeedFilter) {
            is FeedFilter.Read -> LocalFeedFlowStrings.current.readArticlesEmptyScreenMessage
            is FeedFilter.Bookmarks -> LocalFeedFlowStrings.current.bookmarkedArticlesEmptyScreenMessage
            else -> LocalFeedFlowStrings.current.emptyFeedMessage
        }

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.regular),
            text = emptyMessage,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )

        val buttonAction = if (currentFeedFilter is FeedFilter.Read || currentFeedFilter is FeedFilter.Bookmarks) {
            onBackToTimelineClick
        } else {
            onReloadClick
        }

        val buttonText = when (currentFeedFilter) {
            is FeedFilter.Read, is FeedFilter.Bookmarks -> {
                LocalFeedFlowStrings.current.emptyScreenBackToTimeline
            }
            else -> {
                LocalFeedFlowStrings.current.refreshFeeds
            }
        }

        Button(
            modifier = Modifier
                .padding(top = Spacing.regular),
            onClick = buttonAction,
        ) {
            Text(buttonText)
        }

        if (isDrawerVisible) {
            Button(
                modifier = Modifier
                    .padding(top = Spacing.regular),
                onClick = onOpenDrawerClick,
            ) {
                Text(LocalFeedFlowStrings.current.openAnotherFeed)
            }
        }
    }
}
