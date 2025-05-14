package com.prof18.feedflow.android.home.components

import FeedFlowTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.prof18.feedflow.android.BuildConfig
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun HomeAppBarDropdownMenu(
    showMenu: Boolean,
    feedFilter: FeedFilter,
    closeMenu: () -> Unit,
    onMarkAllReadClicked: () -> Unit,
    onClearOldArticlesClicked: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    onForceRefreshClick: () -> Unit,
    onDeleteDatabase: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
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
                Text(LocalFeedFlowStrings.current.markAllReadButton)
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
                Text(LocalFeedFlowStrings.current.clearOldArticlesButton)
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
                Text(LocalFeedFlowStrings.current.forceFeedRefresh)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                )
            },
        )

        if (feedFilter is FeedFilter.Source) {
            DropdownMenuItem(
                onClick = {
                    onEditFeedClick(feedFilter.feedSource)
                },
                text = {
                    Text(LocalFeedFlowStrings.current.editFeed)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                    )
                },
            )
        }

        DropdownMenuItem(
            onClick = {
                onSettingsButtonClicked()
            },
            text = {
                Text(LocalFeedFlowStrings.current.settingsButton)
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

@PreviewPhone
@Composable
private fun SettingsDropdownMenuPreview() {
    FeedFlowTheme {
        HomeAppBarDropdownMenu(
            showMenu = true,
            feedFilter = FeedFilter.Timeline,
            closeMenu = {},
            onMarkAllReadClicked = {},
            onClearOldArticlesClicked = {},
            onSettingsButtonClicked = {},
            onForceRefreshClick = {},
            onDeleteDatabase = {},
            onEditFeedClick = {},
        )
    }
}
