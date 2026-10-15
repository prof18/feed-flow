package com.prof18.feedflow.desktop.home.menubar

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBarScope
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.desktop.main.DesktopMenuCommands
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.MenuBarSettingsState

@Composable
internal fun MenuBarScope.ViewMenu(
    isMacOS: Boolean,
    settingsState: MenuBarSettingsState,
    callbacks: ViewMenuCallbacks,
    commands: DesktopMenuCommands,
) {
    Menu(LocalFeedFlowStrings.current.menuView, mnemonic = 'V') {
        Item(
            text = LocalFeedFlowStrings.current.searchButtonContentDescription,
            onClick = { commands.search?.invoke() },
            enabled = commands.search != null,
            shortcut = shortcut(isMacOS, Key.F),
        )
        Item(
            text = LocalFeedFlowStrings.current.menuToggleSidebar,
            onClick = { commands.toggleSidebar?.invoke() },
            enabled = commands.toggleSidebar != null,
            shortcut = if (isMacOS) {
                KeyShortcut(Key.S, meta = true, ctrl = true)
            } else {
                KeyShortcut(Key.B, ctrl = true)
            },
        )
        Separator()
        CheckboxItem(
            text = LocalFeedFlowStrings.current.settingsToggleShowReadArticles,
            checked = settingsState.isShowReadItemsEnabled,
            onCheckedChange = callbacks.onShowReadItemsToggled,
        )

        Menu(LocalFeedFlowStrings.current.settingsFeedOrderTitle) {
            RadioButtonItem(
                text = LocalFeedFlowStrings.current.settingsFeedOrderNewestFirst,
                selected = settingsState.feedOrder == FeedOrder.NEWEST_FIRST,
                onClick = { callbacks.onFeedOrderSelected(FeedOrder.NEWEST_FIRST) },
            )
            RadioButtonItem(
                text = LocalFeedFlowStrings.current.settingsFeedOrderOldestFirst,
                selected = settingsState.feedOrder == FeedOrder.OLDEST_FIRST,
                onClick = { callbacks.onFeedOrderSelected(FeedOrder.OLDEST_FIRST) },
            )
        }
    }
}

internal data class ViewMenuCallbacks(
    val onShowReadItemsToggled: (Boolean) -> Unit,
    val onFeedOrderSelected: (FeedOrder) -> Unit,
)
