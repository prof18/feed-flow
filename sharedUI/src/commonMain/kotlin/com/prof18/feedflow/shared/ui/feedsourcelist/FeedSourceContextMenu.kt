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
        // 1. Delete (least frequent + destructive - keep far from accidental taps)
        DropdownMenuItem(
            text = {
                Text(LocalFeedFlowStrings.current.deleteFeed)
            },
            onClick = {
                onDeleteFeedSourceClick(feedSource)
                hideMenu()
            },
        )

        // 2. Open website (rare - only for checking if feed/website is still alive)
        val websiteUrl = feedSource.websiteUrlFallback()
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

        // 3. Rename feed (if available)
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

        // 4. Edit feed (medium frequency - occasional settings adjustment)
        DropdownMenuItem(
            text = {
                Text(LocalFeedFlowStrings.current.editFeedSourceNameButton)
            },
            onClick = {
                onEditFeedClick(feedSource)
                hideMenu()
            },
        )

        // 5. Change category (frequent - organizing feeds)
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

        // 6. Pin (most frequent - at bottom for easy thumb reach)
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
    }
}
