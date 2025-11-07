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
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isLinux
import com.prof18.feedflow.desktop.accounts.AccountsScreen
import com.prof18.feedflow.desktop.editfeed.EditFeedScreen
import com.prof18.feedflow.desktop.feedsourcelist.FeedSourceListScreen
import com.prof18.feedflow.desktop.settings.blocked.BlockedWordsScreen
import com.prof18.feedflow.shared.presentation.model.SettingsState
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import java.awt.Desktop
import java.net.URI

data class MenuBarActions(
    val onRefreshClick: () -> Unit,
    val onMarkAllReadClick: () -> Unit,
    val onImportExportClick: () -> Unit,
    val onClearOldFeedClick: () -> Unit,
    val onAboutClick: () -> Unit,
    val onBugReportClick: () -> Unit,
    val onForceRefreshClick: () -> Unit,
    val onFeedFontScaleClick: () -> Unit,
    val deleteFeeds: () -> Unit,
)

data class MenuBarSettings(
    val setMarkReadWhenScrolling: (Boolean) -> Unit,
    val setShowReadItem: (Boolean) -> Unit,
    val setReaderMode: (Boolean) -> Unit,
    val setSaveReaderModeContent: (Boolean) -> Unit,
    val setPrefetchArticleContent: (Boolean) -> Unit,
    val onAutoDeletePeriodSelected: (AutoDeletePeriod) -> Unit,
    val setCrashReportingEnabled: (Boolean) -> Unit,
    val onFeedOrderSelected: (FeedOrder) -> Unit,
    val onThemeModeSelected: (ThemeMode) -> Unit,
    val onClearDownloadedArticles: () -> Unit,
    val onClearImageCache: () -> Unit,
)

data class MenuBarState(
    val showDebugMenu: Boolean,
    val feedFilter: FeedFilter,
    val settingsState: SettingsState,
)

@Composable
fun FrameWindowScope.FeedFlowMenuBar(
    state: MenuBarState,
    actions: MenuBarActions,
    settings: MenuBarSettings,
) {
    MenuBar {
        Menu(LocalFeedFlowStrings.current.fileMenu, mnemonic = 'F') {
            Item(
                text = LocalFeedFlowStrings.current.refreshFeeds,
                onClick = {
                    actions.onRefreshClick()
                },
                shortcut = KeyShortcut(Key.R, meta = true),
            )

            Item(
                text = LocalFeedFlowStrings.current.forceFeedRefresh,
                onClick = {
                    actions.onForceRefreshClick()
                },
                shortcut = KeyShortcut(Key.R, meta = true, shift = true),
            )

            Item(
                text = LocalFeedFlowStrings.current.markAllReadButton,
                onClick = {
                    actions.onMarkAllReadClick()
                },
            )

            Item(
                text = LocalFeedFlowStrings.current.clearOldArticlesButton,
                onClick = {
                    actions.onClearOldFeedClick()
                },
            )

            Menu(LocalFeedFlowStrings.current.settingsTheme) {
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsThemeSystem,
                    selected = state.settingsState.themeMode == ThemeMode.SYSTEM,
                    onClick = { settings.onThemeModeSelected(ThemeMode.SYSTEM) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsThemeLight,
                    selected = state.settingsState.themeMode == ThemeMode.LIGHT,
                    onClick = { settings.onThemeModeSelected(ThemeMode.LIGHT) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsThemeDark,
                    selected = state.settingsState.themeMode == ThemeMode.DARK,
                    onClick = { settings.onThemeModeSelected(ThemeMode.DARK) },
                )
            }

            Separator()

            Item(
                text = LocalFeedFlowStrings.current.settingsClearDownloadedArticles,
                onClick = settings.onClearDownloadedArticles,
            )

            Item(
                text = LocalFeedFlowStrings.current.settingsClearImageCache,
                onClick = settings.onClearImageCache,
            )

            DebugMenu(
                showDebugMenu = state.showDebugMenu,
                deleteFeeds = actions.deleteFeeds,
            )
        }

        Menu(LocalFeedFlowStrings.current.settingsTitleFeed) {
            val navigator = LocalNavigator.currentOrThrow

            if (state.feedFilter is FeedFilter.Source) {
                Item(
                    text = LocalFeedFlowStrings.current.editFeed,
                    onClick = {
                        navigator.push(EditFeedScreen(state.feedFilter.feedSource))
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
                onClick = actions.onImportExportClick,
            )

            Item(
                text = LocalFeedFlowStrings.current.settingsAccounts,
                onClick = {
                    navigator.push(AccountsScreen())
                },
            )

            Item(
                text = LocalFeedFlowStrings.current.feedListAppearance,
                onClick = actions.onFeedFontScaleClick,
            )

            Item(
                text = LocalFeedFlowStrings.current.settingsBlockedWords,
                onClick = {
                    navigator.push(BlockedWordsScreen())
                },
            )
        }

        Menu(LocalFeedFlowStrings.current.settingsBehaviourTitle, mnemonic = 'B') {
            CheckboxItem(
                text = LocalFeedFlowStrings.current.settingsReaderMode,
                checked = state.settingsState.isReaderModeEnabled,
                onCheckedChange = settings.setReaderMode,
            )

            CheckboxItem(
                text = LocalFeedFlowStrings.current.settingsSaveReaderModeContent,
                checked = state.settingsState.isSaveReaderModeContentEnabled,
                onCheckedChange = settings.setSaveReaderModeContent,
            )

            CheckboxItem(
                text = LocalFeedFlowStrings.current.settingsPrefetchArticleContent,
                checked = state.settingsState.isPrefetchArticleContentEnabled,
                onCheckedChange = settings.setPrefetchArticleContent,
            )

            CheckboxItem(
                text = LocalFeedFlowStrings.current.toggleMarkReadWhenScrolling,
                checked = state.settingsState.isMarkReadWhenScrollingEnabled,
                onCheckedChange = settings.setMarkReadWhenScrolling,
            )

            CheckboxItem(
                text = LocalFeedFlowStrings.current.settingsToggleShowReadArticles,
                checked = state.settingsState.isShowReadItemsEnabled,
                onCheckedChange = settings.setShowReadItem,
            )

            Menu(LocalFeedFlowStrings.current.settingsAutoDelete) {
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodDisabled,
                    selected = state.settingsState.autoDeletePeriod == AutoDeletePeriod.DISABLED,
                    onClick = { settings.onAutoDeletePeriodSelected(AutoDeletePeriod.DISABLED) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneDay,
                    selected = state.settingsState.autoDeletePeriod == AutoDeletePeriod.ONE_DAY,
                    onClick = { settings.onAutoDeletePeriodSelected(AutoDeletePeriod.ONE_DAY) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneWeek,
                    selected = state.settingsState.autoDeletePeriod == AutoDeletePeriod.ONE_WEEK,
                    onClick = { settings.onAutoDeletePeriodSelected(AutoDeletePeriod.ONE_WEEK) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodTwoWeeks,
                    selected = state.settingsState.autoDeletePeriod == AutoDeletePeriod.TWO_WEEKS,
                    onClick = { settings.onAutoDeletePeriodSelected(AutoDeletePeriod.TWO_WEEKS) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneMonth,
                    selected = state.settingsState.autoDeletePeriod == AutoDeletePeriod.ONE_MONTH,
                    onClick = { settings.onAutoDeletePeriodSelected(AutoDeletePeriod.ONE_MONTH) },
                )
            }

            Menu(LocalFeedFlowStrings.current.settingsFeedOrderTitle) {
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsFeedOrderNewestFirst,
                    selected = state.settingsState.feedOrder == FeedOrder.NEWEST_FIRST,
                    onClick = { settings.onFeedOrderSelected(FeedOrder.NEWEST_FIRST) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsFeedOrderOldestFirst,
                    selected = state.settingsState.feedOrder == FeedOrder.OLDEST_FIRST,
                    onClick = { settings.onFeedOrderSelected(FeedOrder.OLDEST_FIRST) },
                )
            }
        }

        Menu(LocalFeedFlowStrings.current.settingsHelpTitle, mnemonic = 'B') {
            Item(
                text = LocalFeedFlowStrings.current.reportIssueButton,
                onClick = actions.onBugReportClick,
            )

            CheckboxItem(
                text = LocalFeedFlowStrings.current.settingsCrashReporting,
                checked = state.settingsState.isCrashReportingEnabled,
                onCheckedChange = settings.setCrashReportingEnabled,
            )

            if (getDesktopOS().isLinux()) {
                Separator()
                Item(
                    text = LocalFeedFlowStrings.current.supportTheProject,
                    onClick = {
                        runCatching {
                            Desktop.getDesktop().browse(URI("https://www.paypal.me/MarcoGomiero"))
                        }
                    },
                )
            }

            Item(
                text = LocalFeedFlowStrings.current.aboutButton,
                onClick = actions.onAboutClick,
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
