package com.prof18.feedflow.desktop.home.menubar

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBarScope
import androidx.compose.ui.window.MenuScope
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun MenuBarScope.FileMenu(
    isMacOS: Boolean,
    state: MenuBarState,
    actions: MenuBarActions,
) {
    Menu(LocalFeedFlowStrings.current.fileMenu, mnemonic = 'F') {
        Item(
            text = LocalFeedFlowStrings.current.refreshFeeds,
            onClick = { actions.onRefreshClick() },
            shortcut = if (isMacOS) {
                KeyShortcut(Key.R, meta = true)
            } else {
                KeyShortcut(Key.F5)
            },
        )

        Item(
            text = LocalFeedFlowStrings.current.forceFeedRefresh,
            onClick = { actions.onForceRefreshClick() },
            shortcut = if (isMacOS) {
                KeyShortcut(Key.R, meta = true, shift = true)
            } else {
                KeyShortcut(Key.F5, shift = true)
            },
        )

        if (state.isSyncUploadRequired) {
            Separator()
            Item(
                text = LocalFeedFlowStrings.current.triggerFeedSync,
                onClick = { actions.onBackupClick() },
                shortcut = if (isMacOS) {
                    KeyShortcut(Key.S, meta = true)
                } else {
                    KeyShortcut(Key.S, ctrl = true)
                },
            )
        }

        Separator()

        Item(
            text = LocalFeedFlowStrings.current.markAllReadButton,
            onClick = { actions.onMarkAllReadClick() },
            shortcut = if (isMacOS) {
                KeyShortcut(Key.A, meta = true, shift = true)
            } else {
                KeyShortcut(Key.A, ctrl = true, shift = true)
            },
        )

        Item(
            text = LocalFeedFlowStrings.current.clearOldArticlesButton,
            onClick = { actions.onClearOldFeedClick() },
            shortcut = if (isMacOS) {
                KeyShortcut(Key.D, meta = true, shift = true)
            } else {
                KeyShortcut(Key.D, ctrl = true, shift = true)
            },
        )

        Separator()

        Item(
            text = LocalFeedFlowStrings.current.importExportOpml,
            onClick = actions.onImportExportClick,
            shortcut = if (isMacOS) {
                KeyShortcut(Key.I, meta = true)
            } else {
                KeyShortcut(Key.I, ctrl = true)
            },
        )

        DebugMenu(
            showDebugMenu = state.showDebugMenu,
            deleteFeeds = actions.deleteFeeds,
        )
    }
}

@Composable
private fun MenuScope.DebugMenu(
    showDebugMenu: Boolean,
    deleteFeeds: () -> Unit,
) {
    if (showDebugMenu) {
        Separator()

        Item(
            text = "Delete all feeds",
            onClick = {
                deleteFeeds()
            },
        )
    }
}
