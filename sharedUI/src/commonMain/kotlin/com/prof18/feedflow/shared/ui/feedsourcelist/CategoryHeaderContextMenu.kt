package com.prof18.feedflow.shared.ui.feedsourcelist

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.PopupProperties
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.ui.components.ConfirmationDialog
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun CategoryHeaderContextMenu(
    showMenu: Boolean,
    feedSources: ImmutableList<FeedSource>,
    hideMenu: () -> Unit,
    onDeleteAllFeedsClick: (List<FeedSource>) -> Unit,
) {
    var showConfirmation by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = hideMenu,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        DropdownMenuItem(
            text = {
                Text(LocalFeedFlowStrings.current.deleteAllFeedsInCategory)
            },
            onClick = {
                hideMenu()
                showConfirmation = true
            },
        )
    }

    if (showConfirmation) {
        ConfirmationDialog(
            title = LocalFeedFlowStrings.current.deleteAllFeedsConfirmationTitle,
            message = LocalFeedFlowStrings.current.deleteAllFeedsConfirmationMessage,
            onConfirm = {
                onDeleteAllFeedsClick(feedSources)
            },
            onDismiss = {
                showConfirmation = false
            },
            isDestructive = true,
        )
    }
}
