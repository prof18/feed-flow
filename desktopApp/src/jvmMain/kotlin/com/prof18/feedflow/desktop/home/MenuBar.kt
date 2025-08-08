package com.prof18.feedflow.desktop.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.desktop.accounts.AccountsScreen
import com.prof18.feedflow.desktop.editfeed.EditFeedScreen
import com.prof18.feedflow.desktop.feedsourcelist.FeedSourceListScreen
import com.prof18.feedflow.shared.presentation.model.SettingsState
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun FrameWindowScope.FeedFlowMenuBar(
    showDebugMenu: Boolean,
    feedFilter: FeedFilter,
    settingsState: SettingsState,
    onRefreshClick: () -> Unit,
    onMarkAllReadClick: () -> Unit,
    onImportExportClick: () -> Unit,
    onClearOldFeedClick: () -> Unit,
    onAboutClick: () -> Unit,
    onBugReportClick: () -> Unit,
    onForceRefreshClick: () -> Unit,
    deleteFeeds: () -> Unit,
    setMarkReadWhenScrolling: (Boolean) -> Unit,
    setShowReadItem: (Boolean) -> Unit,
    setReaderMode: (Boolean) -> Unit,
    onFeedFontScaleClick: () -> Unit,
    onAutoDeletePeriodSelected: (AutoDeletePeriod) -> Unit,
    setCrashReportingEnabled: (Boolean) -> Unit,
    onFeedOrderSelected: (FeedOrder) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
) {
    MenuBar {
        Menu(LocalFeedFlowStrings.current.fileMenu, mnemonic = 'F') {
            Item(
                text = LocalFeedFlowStrings.current.refreshFeeds,
                onClick = {
                    onRefreshClick()
                },
                shortcut = KeyShortcut(Key.R, meta = true),
            )

            Item(
                text = LocalFeedFlowStrings.current.forceFeedRefresh,
                onClick = {
                    onForceRefreshClick()
                },
                shortcut = KeyShortcut(Key.R, meta = true, shift = true),
            )

            Item(
                text = LocalFeedFlowStrings.current.markAllReadButton,
                onClick = {
                    onMarkAllReadClick()
                },
            )

            Item(
                text = LocalFeedFlowStrings.current.clearOldArticlesButton,
                onClick = {
                    onClearOldFeedClick()
                },
            )

            Menu(LocalFeedFlowStrings.current.settingsTheme) {
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsThemeSystem,
                    selected = settingsState.themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeModeSelected(ThemeMode.SYSTEM) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsThemeLight,
                    selected = settingsState.themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeSelected(ThemeMode.LIGHT) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsThemeDark,
                    selected = settingsState.themeMode == ThemeMode.DARK,
                    onClick = { onThemeModeSelected(ThemeMode.DARK) },
                )
            }

            DebugMenu(
                showDebugMenu = showDebugMenu,
                deleteFeeds = deleteFeeds,
            )
        }

        Menu(LocalFeedFlowStrings.current.settingsTitleFeed) {
            val navigator = LocalNavigator.currentOrThrow

            if (feedFilter is FeedFilter.Source) {
                Item(
                    text = LocalFeedFlowStrings.current.editFeed,
                    onClick = {
                        navigator.push(EditFeedScreen(feedFilter.feedSource))
                    },
                )
            }

            Item(
                text = LocalFeedFlowStrings.current.feedsTitle,
                onClick = {
                    navigator.push(FeedSourceListScreen())
                },
            )

            Item(
                text = LocalFeedFlowStrings.current.importExportOpml,
                onClick = onImportExportClick,
            )

            Item(
                text = LocalFeedFlowStrings.current.settingsAccounts,
                onClick = {
                    navigator.push(AccountsScreen())
                },
            )

            Item(
                text = LocalFeedFlowStrings.current.feedListAppearance,
                onClick = onFeedFontScaleClick,
            )
        }

        Menu(LocalFeedFlowStrings.current.settingsBehaviourTitle, mnemonic = 'B') {
            CheckboxItem(
                text = LocalFeedFlowStrings.current.settingsReaderMode,
                checked = settingsState.isReaderModeEnabled,
                onCheckedChange = setReaderMode,
            )

            CheckboxItem(
                text = LocalFeedFlowStrings.current.toggleMarkReadWhenScrolling,
                checked = settingsState.isMarkReadWhenScrollingEnabled,
                onCheckedChange = setMarkReadWhenScrolling,
            )

            CheckboxItem(
                text = LocalFeedFlowStrings.current.settingsToggleShowReadArticles,
                checked = settingsState.isShowReadItemsEnabled,
                onCheckedChange = setShowReadItem,
            )

            Menu(LocalFeedFlowStrings.current.settingsAutoDelete) {
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodDisabled,
                    selected = settingsState.autoDeletePeriod == AutoDeletePeriod.DISABLED,
                    onClick = { onAutoDeletePeriodSelected(AutoDeletePeriod.DISABLED) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneWeek,
                    selected = settingsState.autoDeletePeriod == AutoDeletePeriod.ONE_WEEK,
                    onClick = { onAutoDeletePeriodSelected(AutoDeletePeriod.ONE_WEEK) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodTwoWeeks,
                    selected = settingsState.autoDeletePeriod == AutoDeletePeriod.TWO_WEEKS,
                    onClick = { onAutoDeletePeriodSelected(AutoDeletePeriod.TWO_WEEKS) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneMonth,
                    selected = settingsState.autoDeletePeriod == AutoDeletePeriod.ONE_MONTH,
                    onClick = { onAutoDeletePeriodSelected(AutoDeletePeriod.ONE_MONTH) },
                )
            }

            Menu(LocalFeedFlowStrings.current.settingsFeedOrderTitle) {
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsFeedOrderNewestFirst,
                    selected = settingsState.feedOrder == FeedOrder.NEWEST_FIRST,
                    onClick = { onFeedOrderSelected(FeedOrder.NEWEST_FIRST) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsFeedOrderOldestFirst,
                    selected = settingsState.feedOrder == FeedOrder.OLDEST_FIRST,
                    onClick = { onFeedOrderSelected(FeedOrder.OLDEST_FIRST) },
                )
            }
        }

        Menu(LocalFeedFlowStrings.current.settingsHelpTitle, mnemonic = 'B') {
            Item(
                text = LocalFeedFlowStrings.current.reportIssueButton,
                onClick = onBugReportClick,
            )

            CheckboxItem(
                text = LocalFeedFlowStrings.current.settingsCrashReporting,
                checked = settingsState.isCrashReportingEnabled,
                onCheckedChange = setCrashReportingEnabled,
            )

            Item(
                text = LocalFeedFlowStrings.current.aboutButton,
                onClick = onAboutClick,
            )
        }
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
