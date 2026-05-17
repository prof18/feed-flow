package com.prof18.feedflow.android.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun HomeAppBarDropdownMenu(
    showMenu: Boolean,
    feedFilter: FeedFilter,
    closeMenu: () -> Unit,
    onMarkAllReadClicked: () -> Unit,
    onClearOldArticlesClicked: () -> Unit,
    onForceRefreshClick: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    isSyncUploadRequired: Boolean,
    onBackupClick: () -> Unit,
    onViewOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMarkAllReadDialog by remember { mutableStateOf(false) }
    var showClearOldArticlesDialog by remember { mutableStateOf(false) }

    if (showMarkAllReadDialog) {
        AlertDialog(
            onDismissRequest = { showMarkAllReadDialog = false },
            title = { Text(LocalFeedFlowStrings.current.markAllReadButton) },
            text = { Text(LocalFeedFlowStrings.current.markAllReadDialogMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onMarkAllReadClicked()
                        showMarkAllReadDialog = false
                        closeMenu()
                    },
                ) {
                    Text(LocalFeedFlowStrings.current.confirmButton)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showMarkAllReadDialog = false },
                ) {
                    Text(LocalFeedFlowStrings.current.cancelButton)
                }
            },
        )
    }

    if (showClearOldArticlesDialog) {
        AlertDialog(
            onDismissRequest = { showClearOldArticlesDialog = false },
            title = { Text(LocalFeedFlowStrings.current.clearOldArticlesButton) },
            text = { Text(LocalFeedFlowStrings.current.clearOldArticlesDialogMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearOldArticlesClicked()
                        showClearOldArticlesDialog = false
                        closeMenu()
                    },
                ) {
                    Text(LocalFeedFlowStrings.current.confirmButton)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearOldArticlesDialog = false },
                ) {
                    Text(LocalFeedFlowStrings.current.cancelButton)
                }
            },
        )
    }

    DropdownMenu(
        modifier = modifier,
        expanded = showMenu,
        onDismissRequest = closeMenu,
        offset = DpOffset(x = 0.dp, y = -(96.dp)),
        shape = MaterialTheme.shapes.large,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        if (isSyncUploadRequired) {
            DropdownMenuItem(
                onClick = {
                    onBackupClick()
                    closeMenu()
                },
                text = {
                    Text(LocalFeedFlowStrings.current.triggerFeedSync)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                    )
                },
            )
        }

        DropdownMenuItem(
            onClick = {
                showMarkAllReadDialog = true
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

        DropdownMenuItem(
            onClick = {
                closeMenu()
                onViewOptionsClick()
            },
            text = {
                Text(LocalFeedFlowStrings.current.sortAndFilterButton)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Sort,
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

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = Spacing.xsmall),
            thickness = 0.2.dp,
            color = Color.Gray,
        )

        DropdownMenuItem(
            onClick = {
                showClearOldArticlesDialog = true
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
    }
}
