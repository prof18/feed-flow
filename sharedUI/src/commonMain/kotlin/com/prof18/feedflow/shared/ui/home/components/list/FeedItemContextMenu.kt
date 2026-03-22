package com.prof18.feedflow.shared.ui.home.components.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedItemUrlTitle
import com.prof18.feedflow.core.model.FeedSource

@Composable
internal expect fun FeedItemContextMenu(
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
)
