package com.prof18.feedflow.home.components

import FeedFlowTheme
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.prof18.feedflow.BuildConfig
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.ui.preview.FeedFlowPreview
import dev.icerock.moko.resources.compose.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
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

                Spacer(modifier = Modifier.width(4.dp))

                Text(text = "($unReadCount)")
            }
        },
        actions = {
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                )
            }

            SettingsDropdownMenu(
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
        modifier = Modifier.pointerInput(Unit) {
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
        FeedFilter.Timeline -> stringResource(resource = MR.strings.app_name)
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
                Icons.Default.MenuOpen
            } else {
                Icons.Default.Menu
            },
            contentDescription = null,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun SettingsDropdownMenu(
    showMenu: Boolean,
    closeMenu: () -> Unit,
    onMarkAllReadClicked: () -> Unit,
    onClearOldArticlesClicked: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    onForceRefreshClick: () -> Unit,
    onDeleteDatabase: () -> Unit,
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = closeMenu,
    ) {
        DropdownMenuItem(
            onClick = {
                onMarkAllReadClicked()
                closeMenu()
            },
            text = {
                Text(stringResource(resource = MR.strings.mark_all_read_button))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = null,
                )
            },
        )

        DropdownMenuItem(
            onClick = {
                onClearOldArticlesClicked()
                closeMenu()
            },
            text = {
                Text(stringResource(resource = MR.strings.clear_old_articles_button))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                )
            },
        )

        DropdownMenuItem(
            onClick = {
                onForceRefreshClick()
                closeMenu()
            },
            text = {
                Text(stringResource(resource = MR.strings.force_feed_refresh))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                )
            },
        )

        DropdownMenuItem(
            onClick = {
                onSettingsButtonClicked()
            },
            text = {
                Text(stringResource(resource = MR.strings.settings_button))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                )
            },
        )

        if (BuildConfig.DEBUG) {
            DropdownMenuItem(
                onClick = {
                    onDeleteDatabase()
                },
                text = {
                    Text("Delete database")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

@FeedFlowPreview
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
        )
    }
}

@FeedFlowPreview
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
        )
    }
}
