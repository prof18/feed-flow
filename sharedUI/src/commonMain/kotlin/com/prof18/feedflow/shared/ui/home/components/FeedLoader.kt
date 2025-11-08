package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.prof18.feedflow.core.model.FeedUpdateStatus
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.ConditionalAnimatedVisibility
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun FeedLoader(
    loadingState: FeedUpdateStatus,
    modifier: Modifier = Modifier,
) {
    ConditionalAnimatedVisibility(
        visible = loadingState.isLoading(),
        enter = slideInVertically {
            // Enters from the top (negative initial offset)
            -it
        },
        exit = slideOutVertically {
            // Exits towards the top (negative target offset)
            -it
        },

    ) {
        val feedRefreshCounter = if (loadingState.refreshedFeedCount > 0 && loadingState.totalFeedCount > 0) {
            "${loadingState.refreshedFeedCount}/${loadingState.totalFeedCount}"
        } else {
            "..."
        }

        Text(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.regular),
            text = LocalFeedFlowStrings.current.loadingFeedMessage(feedRefreshCounter),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
