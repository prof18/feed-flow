package com.prof18.feedflow.desktop.home.menubar

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBarScope
import androidx.compose.ui.window.MenuScope
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isLinux
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings

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
            enabled = state.commands.refreshEnabled,
            shortcut = if (isMacOS) {
                KeyShortcut(Key.R, meta = true)
            } else {
                KeyShortcut(Key.F5)
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
            enabled = state.commands.bulkActionsEnabled,
            shortcut = if (isMacOS) {
                KeyShortcut(Key.A, meta = true, shift = true)
            } else {
                KeyShortcut(Key.A, ctrl = true, shift = true)
            },
        )

        Separator()

        Item(
            text = LocalFeedFlowStrings.current.importExportLabel,
            onClick = actions.onImportExportClick,
            shortcut = if (isMacOS) {
                KeyShortcut(Key.I, meta = true)
            } else {
                KeyShortcut(Key.I, ctrl = true)
            },
        )

        // On macOS Settings lives in the application menu (see installMacOsAppMenuHandlers).
        if (!isMacOS) {
            Item(
                text = LocalFeedFlowStrings.current.settingsTitle,
                onClick = actions.onSettingsClick,
                shortcut = KeyShortcut(Key.Comma, ctrl = true),
            )
        }

        if (!isMacOS) {
            Separator()

            Item(
                text = LocalFeedFlowStrings.current.exitMenu,
                onClick = actions.onExitClick,
                // Windows closes with Alt+F4; Ctrl+Q is the Linux convention.
                shortcut = if (getDesktopOS().isLinux()) KeyShortcut(Key.Q, ctrl = true) else null,
            )
        }

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
