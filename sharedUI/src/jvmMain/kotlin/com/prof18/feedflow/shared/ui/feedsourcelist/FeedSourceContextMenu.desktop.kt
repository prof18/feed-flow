package com.prof18.feedflow.shared.ui.feedsourcelist

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.ui.components.menu.DesktopPopupMenu
import com.prof18.feedflow.shared.ui.components.menu.DesktopPopupMenuEntry
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
internal actual fun FeedSourceContextMenu(
    showFeedMenu: Boolean,
    menuPositionInWindow: Offset?,
    feedSource: FeedSource,
    hideMenu: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onChangeFeedCategoryClick: ((FeedSource) -> Unit)?,
    onRenameFeedSourceClick: ((FeedSource) -> Unit)?,
    onOpenWebsite: (String) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val menuEntries = buildFeedSourceDesktopMenuEntries(
        strings = strings,
        feedSource = feedSource,
        hideMenu = hideMenu,
        onEditFeedClick = onEditFeedClick,
        onDeleteFeedSourceClick = onDeleteFeedSourceClick,
        onPinFeedClick = onPinFeedClick,
        onChangeFeedCategoryClick = onChangeFeedCategoryClick,
        onRenameFeedSourceClick = onRenameFeedSourceClick,
        onOpenWebsite = onOpenWebsite,
    )

    DesktopPopupMenu(
        showMenu = showFeedMenu,
        menuPositionInWindow = menuPositionInWindow,
        menuEntries = menuEntries,
        closeMenu = hideMenu,
    )
}

private fun buildFeedSourceDesktopMenuEntries(
    strings: com.prof18.feedflow.i18n.FeedFlowStrings,
    feedSource: FeedSource,
    hideMenu: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onChangeFeedCategoryClick: ((FeedSource) -> Unit)?,
    onRenameFeedSourceClick: ((FeedSource) -> Unit)?,
    onOpenWebsite: (String) -> Unit,
): ImmutableList<DesktopPopupMenuEntry> = buildList {
    add(
        DesktopPopupMenuEntry.Action(
            text = strings.deleteFeed,
            onClick = {
                onDeleteFeedSourceClick(feedSource)
                hideMenu()
            },
        ),
    )

    val websiteUrl = feedSource.websiteUrlFallback()
    if (websiteUrl != null) {
        add(
            DesktopPopupMenuEntry.Action(
                text = strings.openWebsiteButton,
                onClick = {
                    onOpenWebsite(websiteUrl)
                    hideMenu()
                },
            ),
        )
    }

    if (onRenameFeedSourceClick != null) {
        add(
            DesktopPopupMenuEntry.Action(
                text = strings.renameFeedSourceNameButton,
                onClick = {
                    onRenameFeedSourceClick(feedSource)
                    hideMenu()
                },
            ),
        )
    }

    add(
        DesktopPopupMenuEntry.Action(
            text = strings.editFeedSourceNameButton,
            onClick = {
                onEditFeedClick(feedSource)
                hideMenu()
            },
        ),
    )

    if (onChangeFeedCategoryClick != null) {
        add(
            DesktopPopupMenuEntry.Action(
                text = strings.changeCategory,
                onClick = {
                    onChangeFeedCategoryClick(feedSource)
                    hideMenu()
                },
            ),
        )
    }

    add(
        DesktopPopupMenuEntry.Action(
            text = if (feedSource.isPinned) {
                strings.menuRemoveFromPinned
            } else {
                strings.menuAddToPinned
            },
            onClick = {
                onPinFeedClick(feedSource)
                hideMenu()
            },
        ),
    )
}.toImmutableList()
