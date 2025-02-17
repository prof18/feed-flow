package com.prof18.feedflow.shared.ui.feedsourcelist

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.PopupProperties
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.tagForTesting

@Composable
internal fun FeedSourceContextMenu(
    showFeedMenu: Boolean,
    feedSource: FeedSource,
    hideMenu: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onRenameFeedSourceClick: ((FeedSource) -> Unit)? = null,
    onPinFeedClick: (FeedSource) -> Unit,
) {
    DropdownMenu(
        expanded = showFeedMenu,
        onDismissRequest = hideMenu,
        properties = PopupProperties(),
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    if (feedSource.isPinned) {
                        LocalFeedFlowStrings.current.menuRemoveFromPinned
                    } else {
                        LocalFeedFlowStrings.current.menuAddToPinned
                    },
                )
            },
            onClick = {
                onPinFeedClick(feedSource)
                hideMenu()
            },
        )

        DropdownMenuItem(
            text = {
                Text(LocalFeedFlowStrings.current.editFeedSourceNameButton)
            },
            onClick = {
                onEditFeedClick(feedSource)
                hideMenu()
            },
        )

        if (onRenameFeedSourceClick != null) {
            DropdownMenuItem(
                text = {
                    Text(LocalFeedFlowStrings.current.renameFeedSourceNameButton)
                },
                onClick = {
                    onRenameFeedSourceClick(feedSource)
                    hideMenu()
                },
            )
        }

        DropdownMenuItem(
            modifier = Modifier
                .tagForTesting(TestingTag.FEED_SOURCE_DELETE_BUTTON),
            text = {
                Text(LocalFeedFlowStrings.current.deleteFeed)
            },
            onClick = {
                onDeleteFeedSourceClick(feedSource)
                hideMenu()
            },
        )
    }
}
