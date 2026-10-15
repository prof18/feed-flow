package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.home.drawer.DesktopDrawer
import com.prof18.feedflow.desktop.main.DesktopDialogWindowDestination
import com.prof18.feedflow.desktop.main.LocalDesktopDialogWindowNavigator
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState

@Composable
internal fun FeedManagementDialog(
    displayState: HomeDisplayState,
    feedManagementActions: FeedManagementActions,
    onFeedSuggestionsClick: () -> Unit,
    onImportExportClick: () -> Unit,
) {
    val navigator = LocalDesktopDialogWindowNavigator.current
    val destination = DesktopDialogWindowDestination.FeedSources
    if (!navigator.isOpen(destination)) return
    val close = { navigator.close(destination) }
    DesktopDialogWindow(
        title = LocalFeedFlowStrings.current.drawerTitleFeedSources,
        size = DpSize(480.dp, 640.dp),
        onCloseRequest = close,
        onKeyEvent = {
            if (it.key == Key.Escape && it.type == KeyEventType.KeyDown) {
                close()
                true
            } else {
                false
            }
        },
    ) { modifier ->
        DesktopDrawer(
            displayState = displayState,
            initialSelectedFeedSourceIds = navigator.feedSourceSelection,
            feedManagementActions = feedManagementActions,
            onFeedFilterSelected = { filter ->
                feedManagementActions.onFeedFilterSelected(filter)
                close()
            },
            modifier = modifier,
            onFeedSuggestionsClick = {
                close()
                onFeedSuggestionsClick()
            },
            onImportExportClick = {
                close()
                onImportExportClick()
            },
        )
    }
}
