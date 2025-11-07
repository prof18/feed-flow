package com.prof18.feedflow.shared.ui.feedsourcelist

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.PopupProperties
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun FeedSourceContextMenu(
    showFeedMenu: Boolean,
    feedSource: FeedSource,
    hideMenu: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onChangeFeedCategoryClick: ((FeedSource) -> Unit)? = null,
    onRenameFeedSourceClick: ((FeedSource) -> Unit)? = null,
    onOpenWebsite: ((String) -> Unit),
) {
    DropdownMenu(
        expanded = showFeedMenu,
        onDismissRequest = hideMenu,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
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

        if (onChangeFeedCategoryClick != null) {
            DropdownMenuItem(
                text = {
                    Text(LocalFeedFlowStrings.current.changeCategory)
                },
                onClick = {
                    onChangeFeedCategoryClick(feedSource)
                    hideMenu()
                },
            )
        }

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

        // Open website (if available and callback provided)
        val websiteUrl = feedSource.websiteUrl
        if (websiteUrl != null) {
            DropdownMenuItem(
                text = {
                    Text(LocalFeedFlowStrings.current.openWebsiteButton)
                },
                onClick = {
                    onOpenWebsite(websiteUrl)
                    hideMenu()
                },
            )
        }

        DropdownMenuItem(
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
