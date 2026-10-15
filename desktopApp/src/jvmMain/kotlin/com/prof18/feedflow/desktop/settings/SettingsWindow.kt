package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDialogWindow
import com.prof18.feedflow.shared.data.DesktopHomeSettingsRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.presentation.FeedListSettingsViewModel
import com.prof18.feedflow.shared.presentation.MenuBarViewModel
import org.koin.compose.viewmodel.koinViewModel

private val SETTINGS_WINDOW_SIZE = DpSize(width = 960.dp, height = 720.dp)
private val SETTINGS_WINDOW_MINIMUM_SIZE = DpSize(width = 680.dp, height = 520.dp)

@Composable
internal fun DesktopSettingsDialog(
    onCloseRequest: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    DesktopDialogWindow(
        title = LocalFeedFlowStrings.current.settingsTitle,
        size = SETTINGS_WINDOW_SIZE,
        minimumSize = SETTINGS_WINDOW_MINIMUM_SIZE,
        onCloseRequest = onCloseRequest,
        resizable = true,
    ) { modifier -> content(modifier) }
}

@Composable
internal fun SettingsWindow(
    onCloseRequest: () -> Unit,
    initialCategory: DesktopSettingsCategory = DesktopSettingsCategory.GENERAL,
) {
    DesktopSettingsDialog(onCloseRequest) { modifier ->
        val menuBarViewModel = koinViewModel<MenuBarViewModel>()
        val feedListSettingsViewModel = koinViewModel<FeedListSettingsViewModel>()
        val desktopHomeSettingsRepository = remember { DI.koin.get<DesktopHomeSettingsRepository>() }
        val settingsRepository = remember { DI.koin.get<SettingsRepository>() }
        val settingsState by menuBarViewModel.state.collectAsState()
        val isForceEnglishEnabled by settingsRepository.forceEnglishEnabledFlow.collectAsState()
        val isMultiPaneEnabled by desktopHomeSettingsRepository.isMultiPaneLayoutEnabledFlow.collectAsState()
        val fontSizesState by feedListSettingsViewModel.feedFontSizeState.collectAsState()
        val feedListSettingsState by feedListSettingsViewModel.state.collectAsState()
        SettingsContent(initialCategory = initialCategory, modifier = modifier) { category ->
            when (category) {
                DesktopSettingsCategory.GENERAL -> GeneralPane(
                    themeMode = settingsState.themeMode,
                    onThemeModeSelected = menuBarViewModel::updateThemeMode,
                    isMultiPaneEnabled = isMultiPaneEnabled,
                    onMultiPaneToggled = desktopHomeSettingsRepository::setMultiPaneLayoutEnabled,
                    isReduceMotionEnabled = settingsState.isReduceMotionEnabled,
                    onReduceMotionToggled = menuBarViewModel::updateReduceMotionEnabled,
                    isForceEnglishEnabled = isForceEnglishEnabled,
                    onForceEnglishToggled = settingsRepository::setForceEnglishEnabled,
                    isHideUnreadCountEnabled = settingsState.isHideUnreadCountEnabled,
                    onHideUnreadCountToggled = menuBarViewModel::updateHideUnreadCount,
                )
                DesktopSettingsCategory.ACCOUNTS -> AccountsPane()
                DesktopSettingsCategory.FEED_LIST -> FeedListPane(
                    fontSizesState = fontSizesState,
                    settingsState = feedListSettingsState,
                    feedOrder = settingsState.feedOrder,
                    onFontScaleUpdate = feedListSettingsViewModel::updateFontScale,
                    onFeedLayoutUpdate = feedListSettingsViewModel::updateFeedLayout,
                    onGridLayoutEnabledUpdate = feedListSettingsViewModel::updateGridLayoutEnabled,
                    onHideDescriptionUpdate = feedListSettingsViewModel::updateHideDescription,
                    onHideImagesUpdate = feedListSettingsViewModel::updateHideImages,
                    onHideDateUpdate = feedListSettingsViewModel::updateHideDate,
                    onRemoveTitleFromDescUpdate = feedListSettingsViewModel::updateRemoveTitleFromDescription,
                    onDateFormatUpdate = feedListSettingsViewModel::updateDateFormat,
                    onTimeFormatUpdate = feedListSettingsViewModel::updateTimeFormat,
                    onSwipeActionUpdate = feedListSettingsViewModel::updateSwipeAction,
                    onFeedOrderSelected = menuBarViewModel::updateFeedOrder,
                    onHideUnreadDotUpdate = feedListSettingsViewModel::updateHideUnreadDot,
                    onHideFeedSourceUpdate = feedListSettingsViewModel::updateHideFeedSource,
                    onDescriptionLineLimitUpdate = feedListSettingsViewModel::updateDescriptionLineLimit,
                )
                DesktopSettingsCategory.READING -> ReadingPane(
                    articleOpenMode = settingsState.articleOpenMode,
                    isSaveReaderModeContentEnabled = settingsState.isSaveReaderModeContentEnabled,
                    isPrefetchArticleContentEnabled = settingsState.isPrefetchArticleContentEnabled,
                    isMarkReadWhenScrollingEnabled = settingsState.isMarkReadWhenScrollingEnabled,
                    isShowReadItemsEnabled = settingsState.isShowReadItemsEnabled,
                    isHideReadItemsEnabled = settingsState.isHideReadItemsEnabled,
                    onArticleOpenModeSelected = menuBarViewModel::updateArticleOpenMode,
                    onSaveReaderModeContentToggled = menuBarViewModel::updateSaveReaderModeContent,
                    onPrefetchToggled = menuBarViewModel::updatePrefetchArticleContent,
                    onMarkReadWhenScrollingToggled = menuBarViewModel::updateMarkReadWhenScrolling,
                    onShowReadItemsToggled = menuBarViewModel::updateShowReadItemsOnTimeline,
                    onHideReadItemsToggled = menuBarViewModel::updateHideReadItems,
                )
                DesktopSettingsCategory.SYNC_STORAGE -> SyncStoragePane(
                    isRefreshFeedsOnLaunchEnabled = settingsState.isRefreshFeedsOnLaunchEnabled,
                    autoDeletePeriod = settingsState.autoDeletePeriod,
                    syncPeriod = settingsState.syncPeriod,
                    onRefreshFeedsOnLaunchToggled = menuBarViewModel::updateRefreshFeedsOnLaunch,
                    onSyncPeriodSelected = menuBarViewModel::updateSyncPeriod,
                    onAutoDeletePeriodSelected = menuBarViewModel::updateAutoDeletePeriod,
                    onClearDownloadedArticles = menuBarViewModel::clearDownloadedArticleContent,
                )
                DesktopSettingsCategory.ABOUT -> AboutPane(
                    isCrashReportingEnabled = settingsState.isCrashReportingEnabled,
                    onCrashReportingToggled = { enabled ->
                        menuBarViewModel.updateCrashReporting(enabled)
                        updateDesktopCrashReporting(enabled)
                    },
                )
            }
        }
    }
}
