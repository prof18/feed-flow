package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowScope
import com.prof18.feedflow.desktop.accounts.rememberAccountDrafts
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.importexport.ImportExportFilePickers
import com.prof18.feedflow.desktop.importexport.ImportExportScreen
import com.prof18.feedflow.desktop.importexport.rememberImportExportFilePickerState
import com.prof18.feedflow.desktop.main.DesktopDialogWindowNavigator
import com.prof18.feedflow.desktop.settings.blocked.BlockedWordsScreen
import com.prof18.feedflow.desktop.settings.blocked.rememberBlockedWordDraft
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDialogWindow
import com.prof18.feedflow.shared.data.DesktopHomeSettingsRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.presentation.FeedListSettingsViewModel
import com.prof18.feedflow.shared.presentation.ImportExportViewModel
import com.prof18.feedflow.shared.presentation.MenuBarViewModel
import org.koin.compose.viewmodel.koinViewModel

private val SETTINGS_WINDOW_SIZE = DpSize(width = 960.dp, height = 720.dp)
private val SETTINGS_WINDOW_MINIMUM_SIZE = DpSize(width = 680.dp, height = 520.dp)

@Composable
internal fun DesktopSettingsDialog(
    onCloseRequest: () -> Unit,
    activationRequest: Int = 0,
    content: @Composable DialogWindowScope.(Modifier) -> Unit,
) {
    DesktopDialogWindow(
        title = LocalFeedFlowStrings.current.settingsTitle,
        size = SETTINGS_WINDOW_SIZE,
        minimumSize = SETTINGS_WINDOW_MINIMUM_SIZE,
        onCloseRequest = onCloseRequest,
        resizable = true,
        extendBehindTitleBar = true,
        activationRequest = activationRequest,
    ) { modifier -> content(modifier) }
}

@Composable
internal fun SettingsWindow(
    navigator: DesktopDialogWindowNavigator,
    onCloseRequest: () -> Unit,
    triggerFeedFetch: () -> Unit,
    onClearOldArticles: () -> Unit,
    canClearOldArticles: Boolean,
) {
    DesktopSettingsDialog(onCloseRequest, activationRequest = navigator.settingsActivationRequest) { modifier ->
        val menuBarViewModel = koinViewModel<MenuBarViewModel>()
        val feedListSettingsViewModel = koinViewModel<FeedListSettingsViewModel>()
        val desktopHomeSettingsRepository = remember { DI.koin.get<DesktopHomeSettingsRepository>() }
        val settingsRepository = remember { DI.koin.get<SettingsRepository>() }
        val readerFonts by settingsRepository.readerFontSettingsState.collectAsState()
        val accountDrafts = rememberAccountDrafts()
        val blockedWordDraft = rememberBlockedWordDraft()
        val pickerState = rememberImportExportFilePickerState()
        val importExportViewModel = koinViewModel<ImportExportViewModel>()
        ImportExportFilePickers(window, pickerState, importExportViewModel)
        val settingsState by menuBarViewModel.state.collectAsState()
        val isForceEnglishEnabled by settingsRepository.forceEnglishEnabledFlow.collectAsState()
        val isMultiPaneEnabled by desktopHomeSettingsRepository.isMultiPaneLayoutEnabledFlow.collectAsState()
        val fontSizesState by feedListSettingsViewModel.feedFontSizeState.collectAsState()
        val feedListSettingsState by feedListSettingsViewModel.state.collectAsState()
        SettingsContent(
            initialCategory = navigator.settingsCategory,
            sectionKey = navigator.settingsSection?.name,
            onCategorySelected = navigator::selectSettingsCategory,
            modifier = modifier,
        ) { category ->
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
                DesktopSettingsCategory.ACCOUNTS -> AccountsPane(accountDrafts)
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
                    isShowReadItemsEnabled = settingsState.isShowReadItemsEnabled,
                    isHideReadItemsEnabled = settingsState.isHideReadItemsEnabled,
                    isMarkReadWhenScrollingEnabled = settingsState.isMarkReadWhenScrollingEnabled,
                    onShowReadItemsToggled = menuBarViewModel::updateShowReadItemsOnTimeline,
                    onHideReadItemsToggled = menuBarViewModel::updateHideReadItems,
                )
                DesktopSettingsCategory.READING -> ReadingPane(
                    articleOpenMode = settingsState.articleOpenMode,
                    isSaveReaderModeContentEnabled = settingsState.isSaveReaderModeContentEnabled,
                    isPrefetchArticleContentEnabled = settingsState.isPrefetchArticleContentEnabled,
                    isMarkReadWhenScrollingEnabled = settingsState.isMarkReadWhenScrollingEnabled,
                    onArticleOpenModeSelected = menuBarViewModel::updateArticleOpenMode,
                    onSaveReaderModeContentToggled = menuBarViewModel::updateSaveReaderModeContent,
                    onPrefetchToggled = menuBarViewModel::updatePrefetchArticleContent,
                    onMarkReadWhenScrollingToggled = menuBarViewModel::updateMarkReadWhenScrolling,
                    readerFontSize = readerFonts.fontSize,
                    readerLineHeight = readerFonts.lineHeight,
                    onReaderFontSizeChanged = settingsRepository::setReaderModeFontSize,
                    onReaderLineHeightChanged = settingsRepository::setReaderModeLineHeight,
                )
                DesktopSettingsCategory.FILTERS -> BlockedWordsScreen(blockedWordDraft)
                DesktopSettingsCategory.SYNC_STORAGE -> {
                    if (navigator.settingsSection == DesktopSettingsSection.IMPORT_EXPORT) {
                        ImportExportScreen(
                            onCloseRequest = { navigator.selectSettingsSection(null) },
                            triggerFeedFetch = triggerFeedFetch,
                            pickerState = pickerState,
                            viewModel = importExportViewModel,
                        )
                    } else {
                        SyncStoragePane(
                            isRefreshFeedsOnLaunchEnabled = settingsState.isRefreshFeedsOnLaunchEnabled,
                            autoDeletePeriod = settingsState.autoDeletePeriod,
                            syncPeriod = settingsState.syncPeriod,
                            onRefreshFeedsOnLaunchToggled = menuBarViewModel::updateRefreshFeedsOnLaunch,
                            onSyncPeriodSelected = menuBarViewModel::updateSyncPeriod,
                            onAutoDeletePeriodSelected = menuBarViewModel::updateAutoDeletePeriod,
                            onClearDownloadedArticles = menuBarViewModel::clearDownloadedArticleContent,
                            onImportExport = { navigator.selectSettingsSection(DesktopSettingsSection.IMPORT_EXPORT) },
                            onClearOldArticles = onClearOldArticles,
                            canClearOldArticles = canClearOldArticles,
                        )
                    }
                }
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
