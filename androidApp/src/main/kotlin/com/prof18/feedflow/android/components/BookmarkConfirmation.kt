package com.prof18.feedflow.android.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.shared.ui.components.ConfirmationDialog
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun rememberConfirmedBookmarkAction(
    scopeKey: Any?,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
): (FeedItemId, Boolean) -> Unit {
    var pendingRemoval by remember(scopeKey) { mutableStateOf<FeedItemId?>(null) }
    val latestOnBookmarkClick by rememberUpdatedState(onBookmarkClick)

    pendingRemoval?.let { feedItemId ->
        ConfirmationDialog(
            title = LocalFeedFlowStrings.current.menuRemoveFromBookmark,
            message = LocalFeedFlowStrings.current.removeBookmarkDialogMessage,
            onConfirm = { latestOnBookmarkClick(feedItemId, false) },
            onDismiss = { pendingRemoval = null },
        )
    }

    return remember(scopeKey) {
        {
                feedItemId, isBookmarked ->
            if (isBookmarked) {
                latestOnBookmarkClick(feedItemId, true)
            } else {
                pendingRemoval = feedItemId
            }
        }
    }
}
