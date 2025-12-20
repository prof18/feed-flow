package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun HomeAppBar(
    currentFeedFilter: FeedFilter,
    unReadCount: Long,
    showDrawerMenu: Boolean,
    isDrawerOpen: Boolean,
    showDropdownMenu: Boolean,
    onDrawerMenuClick: () -> Unit,
    onMarkAllReadClicked: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    onClearOldArticlesClicked: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onForceRefreshClick: () -> Unit,
    onSearchClick: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        navigationIcon = if (showDrawerMenu) {
            {
                DrawerIcon(
                    onDrawerMenuClick = onDrawerMenuClick,
                    isDrawerOpen = isDrawerOpen,
                )
            }
        } else {
            { }
        },
        title = {
            Row {
                Text(
                    modifier = Modifier
                        .weight(1f, fill = false),
                    text = currentFeedFilter.getTitle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (currentFeedFilter !is FeedFilter.Read &&
                    currentFeedFilter !is FeedFilter.Bookmarks
                ) {
                    Spacer(modifier = Modifier.width(4.dp))

                    Text(text = "($unReadCount)")
                }
            }
        },
        actions = {
            IconButton(
                onClick = onSearchClick,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = LocalFeedFlowStrings.current.searchButtonContentDescription,
                )
            }

            if (showDropdownMenu) {
                IconButton(
                    onClick = {
                        showMenu = !showMenu
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = LocalFeedFlowStrings.current.moreOptionsButtonContentDescription,
                    )
                }

                HomeAppBarDropdownMenu(
                    showMenu = showMenu,
                    feedFilter = currentFeedFilter,
                    closeMenu = {
                        showMenu = false
                    },
                    onMarkAllReadClicked = onMarkAllReadClicked,
                    onClearOldArticlesClicked = onClearOldArticlesClicked,
                    onSettingsButtonClicked = {
                        showMenu = false
                        onSettingsButtonClicked()
                    },
                    onForceRefreshClick = onForceRefreshClick,
                    onEditFeedClick = { feedSource ->
                        showMenu = false
                        onEditFeedClick(feedSource)
                    },
                )
            }
        },
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleClick() },
                    onTap = { onClick() },
                )
            },
    )
}

@Composable
private fun FeedFilter.getTitle(): String =
    when (this) {
        is FeedFilter.Category -> this.feedCategory.title
        is FeedFilter.Source -> this.feedSource.title
        FeedFilter.Timeline -> LocalFeedFlowStrings.current.appName
        FeedFilter.Read -> LocalFeedFlowStrings.current.drawerTitleRead
        FeedFilter.Bookmarks -> LocalFeedFlowStrings.current.drawerTitleBookmarks
        FeedFilter.Uncategorized -> LocalFeedFlowStrings.current.noCategory
    }

@Composable
private fun DrawerIcon(onDrawerMenuClick: () -> Unit, isDrawerOpen: Boolean) {
    IconButton(
        onClick = {
            onDrawerMenuClick()
        },
    ) {
        Icon(
            imageVector = if (isDrawerOpen) {
                Icons.AutoMirrored.Filled.MenuOpen
            } else {
                Icons.Default.Menu
            },
            contentDescription = LocalFeedFlowStrings.current.drawerMenuButtonContentDescription,
        )
    }
}
