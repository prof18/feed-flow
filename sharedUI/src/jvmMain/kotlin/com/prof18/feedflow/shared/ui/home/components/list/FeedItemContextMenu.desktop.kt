package com.prof18.feedflow.shared.ui.home.components.list

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedItemUrlTitle
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.shared.ui.components.menu.DesktopPopupMenu
import com.prof18.feedflow.shared.ui.components.menu.DesktopPopupMenuEntry
import com.prof18.feedflow.shared.ui.home.components.ShareCommentsIcon
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
internal actual fun FeedItemContextMenu(
    showMenu: Boolean,
    menuPositionInWindow: Offset?,
    feedItem: FeedItem,
    shareMenuLabel: String,
    shareCommentsMenuLabel: String,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    closeMenu: () -> Unit,
    onShareClick: (FeedItemUrlTitle) -> Unit,
    onOpenFeedSettings: (FeedSource) -> Unit,
    onOpenFeedWebsite: (String) -> Unit,
    onMarkAllAboveAsRead: (String) -> Unit,
    onMarkAllBelowAsRead: (String) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val menuEntries = buildFeedItemDesktopMenuEntries(
        strings = strings,
        feedItem = feedItem,
        shareMenuLabel = shareMenuLabel,
        shareCommentsMenuLabel = shareCommentsMenuLabel,
        onBookmarkClick = onBookmarkClick,
        onReadStatusClick = onReadStatusClick,
        onCommentClick = onCommentClick,
        closeMenu = closeMenu,
        onShareClick = onShareClick,
        onOpenFeedSettings = onOpenFeedSettings,
        onOpenFeedWebsite = onOpenFeedWebsite,
        onMarkAllAboveAsRead = onMarkAllAboveAsRead,
        onMarkAllBelowAsRead = onMarkAllBelowAsRead,
    )

    DesktopPopupMenu(
        showMenu = showMenu,
        menuPositionInWindow = menuPositionInWindow,
        menuEntries = menuEntries,
        closeMenu = closeMenu,
    )
}

private fun buildFeedItemDesktopMenuEntries(
    strings: com.prof18.feedflow.i18n.FeedFlowStrings,
    feedItem: FeedItem,
    shareMenuLabel: String,
    shareCommentsMenuLabel: String,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    closeMenu: () -> Unit,
    onShareClick: (FeedItemUrlTitle) -> Unit,
    onOpenFeedSettings: (FeedSource) -> Unit,
    onOpenFeedWebsite: (String) -> Unit,
    onMarkAllAboveAsRead: (String) -> Unit,
    onMarkAllBelowAsRead: (String) -> Unit,
): ImmutableList<DesktopPopupMenuEntry> = buildList {
    add(
        DesktopPopupMenuEntry.Action(
            text = strings.openFeedSettings,
            icon = Icons.Default.Settings,
            onClick = {
                onOpenFeedSettings(feedItem.feedSource)
                closeMenu()
            },
        ),
    )

    val websiteUrl = feedItem.feedSource.websiteUrlFallback()
    if (websiteUrl != null) {
        add(
            DesktopPopupMenuEntry.Action(
                text = strings.openFeedWebsiteButton,
                icon = Icons.Default.Public,
                onClick = {
                    onOpenFeedWebsite(websiteUrl)
                    closeMenu()
                },
            ),
        )
    }

    add(DesktopPopupMenuEntry.Divider)
    add(
        DesktopPopupMenuEntry.Action(
            text = strings.menuMarkAllAboveAsRead,
            icon = Icons.Default.KeyboardDoubleArrowUp,
            onClick = {
                onMarkAllAboveAsRead(feedItem.id)
                closeMenu()
            },
        ),
    )
    add(
        DesktopPopupMenuEntry.Action(
            text = strings.menuMarkAllBelowAsRead,
            icon = Icons.Default.KeyboardDoubleArrowDown,
            onClick = {
                onMarkAllBelowAsRead(feedItem.id)
                closeMenu()
            },
        ),
    )
    add(DesktopPopupMenuEntry.Divider)

    if (feedItem.commentsUrl != null) {
        add(
            DesktopPopupMenuEntry.Action(
                text = shareCommentsMenuLabel,
                icon = ShareCommentsIcon,
                onClick = {
                    onShareClick(
                        FeedItemUrlTitle(
                            title = feedItem.title,
                            url = requireNotNull(feedItem.commentsUrl),
                        ),
                    )
                    closeMenu()
                },
            ),
        )
        add(
            DesktopPopupMenuEntry.Action(
                text = strings.menuOpenComments,
                icon = Icons.Default.Forum,
                onClick = {
                    onCommentClick(
                        FeedItemUrlInfo(
                            id = feedItem.id,
                            url = requireNotNull(feedItem.commentsUrl),
                            title = feedItem.title,
                            openOnlyOnBrowser = true,
                            isBookmarked = feedItem.isBookmarked,
                            linkOpeningPreference = LinkOpeningPreference.PREFERRED_BROWSER,
                            commentsUrl = feedItem.commentsUrl,
                        ),
                    )
                    closeMenu()
                },
            ),
        )
        add(DesktopPopupMenuEntry.Divider)
    }

    add(
        DesktopPopupMenuEntry.Action(
            text = shareMenuLabel,
            icon = Icons.Default.Share,
            onClick = {
                onShareClick(
                    FeedItemUrlTitle(
                        title = feedItem.title,
                        url = feedItem.url,
                    ),
                )
                closeMenu()
            },
        ),
    )
    add(
        DesktopPopupMenuEntry.Action(
            text = if (feedItem.isBookmarked) {
                strings.menuRemoveFromBookmark
            } else {
                strings.menuAddToBookmark
            },
            icon = if (feedItem.isBookmarked) {
                Icons.Default.BookmarkRemove
            } else {
                Icons.Default.BookmarkAdd
            },
            onClick = {
                onBookmarkClick(FeedItemId(feedItem.id), !feedItem.isBookmarked)
                closeMenu()
            },
        ),
    )
    add(
        DesktopPopupMenuEntry.Action(
            text = if (feedItem.isRead) {
                strings.menuMarkAsUnread
            } else {
                strings.menuMarkAsRead
            },
            icon = if (feedItem.isRead) {
                Icons.Default.MarkEmailUnread
            } else {
                Icons.Default.MarkEmailRead
            },
            onClick = {
                onReadStatusClick(FeedItemId(feedItem.id), !feedItem.isRead)
                closeMenu()
            },
        ),
    )
}.toImmutableList()
