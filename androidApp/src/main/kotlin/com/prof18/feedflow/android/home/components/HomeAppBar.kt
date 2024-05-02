package com.prof18.feedflow.android.home.components

import FeedFlowTheme
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
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
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.tagForTesting

@Composable
internal fun HomeAppBar(
    currentFeedFilter: FeedFilter,
    unReadCount: Long,
    showDrawerMenu: Boolean,
    isDrawerOpen: Boolean,
    onDrawerMenuClick: () -> Unit,
    onMarkAllReadClicked: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    onClearOldArticlesClicked: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onForceRefreshClick: () -> Unit,
    onDeleteDatabase: () -> Unit,
    onSearchClick: () -> Unit,
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
                    contentDescription = null,
                )
            }

            IconButton(
                modifier = Modifier
                    .tagForTesting(TestingTag.SETTING_BUTTON),
                onClick = {
                    showMenu = !showMenu
                },
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                )
            }

            HomeAppBarDropdownMenu(
                showMenu = showMenu,
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
                onDeleteDatabase = onDeleteDatabase,
            )
        },
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleClick() },
                    onTap = { onClick() },
                )
            }
            .tagForTesting(TestingTag.HOME_TOOLBAR),
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
    }

@Composable
private fun DrawerIcon(onDrawerMenuClick: () -> Unit, isDrawerOpen: Boolean) {
    IconButton(
        modifier = Modifier
            .tagForTesting(TestingTag.DRAWER_MENU_BUTTON),
        onClick = {
            onDrawerMenuClick()
        },
    ) {
        Icon(
            imageVector = if (isDrawerOpen) {
                Icons.Default.MenuOpen
            } else {
                Icons.Default.Menu
            },
            contentDescription = null,
        )
    }
}

@PreviewPhone
@Composable
private fun HomeAppBarPreview() {
    FeedFlowTheme {
        HomeAppBar(
            currentFeedFilter = FeedFilter.Source(
                feedSource = FeedSource(
                    id = 0,
                    url = "",
                    title = "A very very very very very very long title",
                    category = null,
                    lastSyncTimestamp = null,
                    logoUrl = null,
                ),
            ),
            showDrawerMenu = true,
            isDrawerOpen = false,
            onDrawerMenuClick = {},
            unReadCount = 42,
            onMarkAllReadClicked = { },
            onSettingsButtonClicked = { },
            onClearOldArticlesClicked = { },
            onClick = { },
            onDoubleClick = {},
            onForceRefreshClick = {},
            onDeleteDatabase = {},
            onSearchClick = {},
        )
    }
}

@PreviewPhone
@Composable
private fun HomeAppBarSmallPreview() {
    FeedFlowTheme {
        HomeAppBar(
            currentFeedFilter = FeedFilter.Timeline,
            showDrawerMenu = true,
            isDrawerOpen = false,
            onDrawerMenuClick = {},
            unReadCount = 42,
            onMarkAllReadClicked = { },
            onSettingsButtonClicked = { },
            onClearOldArticlesClicked = { },
            onClick = { },
            onDoubleClick = {},
            onForceRefreshClick = {},
            onDeleteDatabase = {},
            onSearchClick = {},
        )
    }
}
