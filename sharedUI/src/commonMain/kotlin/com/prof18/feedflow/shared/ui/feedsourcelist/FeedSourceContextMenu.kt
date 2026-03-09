package com.prof18.feedflow.shared.ui.feedsourcelist

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import com.prof18.feedflow.core.model.FeedSource

@Composable
internal expect fun FeedSourceContextMenu(
    showFeedMenu: Boolean,
    menuPositionInWindow: Offset?,
    feedSource: FeedSource,
    hideMenu: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onChangeFeedCategoryClick: ((FeedSource) -> Unit)? = null,
    onRenameFeedSourceClick: ((FeedSource) -> Unit)? = null,
    onOpenWebsite: (String) -> Unit,
)
