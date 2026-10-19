package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

// Swipe actions only change with the swipe settings, and rebuilding the rows then is expected.
@Suppress("ContentSlotReused")
@Composable
internal fun ArticleSwipeRow(
    item: FeedItem,
    swipeActions: SwipeActions,
    enableSwipe: Boolean,
    onOpenInBrowser: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val background = JewelTheme.globalColors.panelBackground
    fun action(type: SwipeActionType): SwipeAction? {
        val label: String
        val execute: () -> Unit
        when (type) {
            SwipeActionType.NONE -> return null
            SwipeActionType.TOGGLE_BOOKMARK_STATUS -> {
                label = if (item.isBookmarked) strings.menuRemoveFromBookmark else strings.menuAddToBookmark
                execute = { onBookmarkClick(FeedItemId(item.id), !item.isBookmarked) }
            }
            SwipeActionType.TOGGLE_READ_STATUS -> {
                label = if (item.isRead) strings.menuMarkAsUnread else strings.menuMarkAsRead
                execute = { onReadStatusClick(FeedItemId(item.id), !item.isRead) }
            }
            SwipeActionType.OPEN_IN_BROWSER -> {
                if (item.url.isBlank()) return null
                label = strings.readerModeBrowserButtonContentDescription
                execute = {
                    onOpenInBrowser(
                        FeedItemUrlInfo(
                            id = item.id,
                            url = item.url,
                            title = item.title,
                            isBookmarked = item.isBookmarked,
                            articleOpenMode = ArticleOpenMode.PREFERRED_BROWSER,
                            commentsUrl = item.commentsUrl,
                            imageUrl = item.imageUrl,
                        ),
                    )
                    if (!item.isRead) onReadStatusClick(FeedItemId(item.id), true)
                }
            }
        }
        return SwipeAction(
            icon = { Text(label, modifier = Modifier.padding(16.dp)) },
            background = background,
            onSwipe = execute,
        )
    }
    val left = if (enableSwipe) action(swipeActions.leftSwipeAction) else null
    val right = if (enableSwipe) action(swipeActions.rightSwipeAction) else null
    if (left == null && right == null) {
        content()
    } else {
        SwipeableActionsBox(
            endActions = listOfNotNull(left),
            startActions = listOfNotNull(right),
        ) { content() }
    }
}
