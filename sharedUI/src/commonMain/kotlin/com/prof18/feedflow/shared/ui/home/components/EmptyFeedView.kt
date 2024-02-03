package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.tagForTesting
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun EmptyFeedView(
    currentFeedFilter: FeedFilter,
    onReloadClick: () -> Unit,
    onBackToTimelineClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val emptyMessage = when (currentFeedFilter) {
            is FeedFilter.Read -> stringResource(resource = MR.strings.read_articles_empty_screen_message)
            is FeedFilter.Bookmarks -> stringResource(resource = MR.strings.bookmarked_articles_empty_screen_message)
            else -> stringResource(resource = MR.strings.empty_feed_message)
        }

        Text(
            modifier = Modifier
                .tagForTesting(TestingTag.NO_ITEMS_MESSAGE),
            text = emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
        )

        val buttonAction = if (currentFeedFilter is FeedFilter.Read || currentFeedFilter is FeedFilter.Bookmarks) {
            onBackToTimelineClick
        } else {
            onReloadClick
        }

        val buttonText = when (currentFeedFilter) {
            is FeedFilter.Read, is FeedFilter.Bookmarks -> {
                stringResource(resource = MR.strings.empty_screen_back_to_timeline)
            }
            else -> {
                stringResource(resource = MR.strings.refresh_feeds)
            }
        }

        Button(
            modifier = Modifier
                .padding(top = Spacing.regular)
                .tagForTesting(TestingTag.REFRESH_FEEDS_BUTTON),
            onClick = buttonAction,
        ) {
            Text(buttonText)
        }
    }
}
