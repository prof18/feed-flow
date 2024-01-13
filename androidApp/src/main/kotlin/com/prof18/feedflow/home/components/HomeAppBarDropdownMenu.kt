package com.prof18.feedflow.home.components

import FeedFlowTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.prof18.feedflow.BuildConfig
import com.prof18.feedflow.MR
import com.prof18.feedflow.ui.preview.FeedFlowPhonePreview
import dev.icerock.moko.resources.compose.stringResource

@Suppress("LongMethod")
@Composable
internal fun HomeAppBarDropdownMenu(
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

@FeedFlowPhonePreview
@Composable
fun SettingsDropdownMenuPreview() {
    FeedFlowTheme {
        HomeAppBarDropdownMenu(
            showMenu = true,
            closeMenu = {},
            onMarkAllReadClicked = {},
            onClearOldArticlesClicked = {},
            onSettingsButtonClicked = {},
            onForceRefreshClick = {},
            onDeleteDatabase = {},
        )
    }
}
