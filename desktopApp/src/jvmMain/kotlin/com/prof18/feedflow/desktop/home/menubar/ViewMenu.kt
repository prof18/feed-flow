package com.prof18.feedflow.desktop.home.menubar

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.MenuBarScope
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.presentation.model.MenuBarSettingsState
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun MenuBarScope.ViewMenu(
    settingsState: MenuBarSettingsState,
    callbacks: ViewMenuCallbacks,
) {
    Menu(LocalFeedFlowStrings.current.menuView, mnemonic = 'V') {
        Menu(LocalFeedFlowStrings.current.settingsTheme) {
            RadioButtonItem(
                text = LocalFeedFlowStrings.current.settingsThemeSystem,
                selected = settingsState.themeMode == ThemeMode.SYSTEM,
                onClick = { callbacks.onThemeModeSelected(ThemeMode.SYSTEM) },
            )
            RadioButtonItem(
                text = LocalFeedFlowStrings.current.settingsThemeLight,
                selected = settingsState.themeMode == ThemeMode.LIGHT,
                onClick = { callbacks.onThemeModeSelected(ThemeMode.LIGHT) },
            )
            RadioButtonItem(
                text = LocalFeedFlowStrings.current.settingsThemeDark,
                selected = settingsState.themeMode == ThemeMode.DARK,
                onClick = { callbacks.onThemeModeSelected(ThemeMode.DARK) },
            )
        }

        Item(
            text = LocalFeedFlowStrings.current.feedListAppearance,
            onClick = callbacks.onFeedListAppearanceClick,
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
    val onThemeModeSelected: (ThemeMode) -> Unit,
    val onFeedListAppearanceClick: () -> Unit,
    val onShowReadItemsToggled: (Boolean) -> Unit,
    val onFeedOrderSelected: (FeedOrder) -> Unit,
)
