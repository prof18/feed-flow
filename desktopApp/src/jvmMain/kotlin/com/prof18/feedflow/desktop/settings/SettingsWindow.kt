package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.ui.components.DesktopDialogWindow
import com.prof18.feedflow.desktop.ui.components.MacToolbarTitleAreaHeight
import com.prof18.feedflow.desktop.ui.components.drawerHazeStyle
import com.prof18.feedflow.shared.data.DesktopHomeSettingsRepository
import com.prof18.feedflow.shared.presentation.FeedListSettingsViewModel
import com.prof18.feedflow.shared.presentation.MenuBarViewModel
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SettingsWindow(
    onCloseRequest: () -> Unit,
    isDarkTheme: Boolean,
    initialCategory: DesktopSettingsCategory = DesktopSettingsCategory.GENERAL,
) {
    val strings = LocalFeedFlowStrings.current
    DesktopDialogWindow(
        title = strings.settingsTitle,
        size = DpSize(860.dp, 680.dp),
        onCloseRequest = onCloseRequest,
        resizable = false,
        extendBehindTitleBar = true,
    ) { modifier ->
        val menuBarViewModel = koinViewModel<MenuBarViewModel>()
        val feedListSettingsViewModel = koinViewModel<FeedListSettingsViewModel>()
        val desktopHomeSettingsRepository = remember { DI.koin.get<DesktopHomeSettingsRepository>() }

        val settingsState by menuBarViewModel.state.collectAsState()
        val isMultiPaneEnabled by desktopHomeSettingsRepository.isMultiPaneLayoutEnabledFlow.collectAsState()
        val fontSizesState by feedListSettingsViewModel.feedFontSizeState.collectAsState()
        val feedListSettingsState by feedListSettingsViewModel.state.collectAsState()

        var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }

        val hazeState = rememberHazeState()
        val hazeStyle = drawerHazeStyle()

        Row(modifier = modifier) {
            // Sidebar
            LazyColumn(
                modifier = Modifier
                    .width(SidebarWidth)
                    .fillMaxHeight()
                    .let { base ->
                        if (hazeStyle != null) {
                            base.hazeEffect(state = hazeState, style = hazeStyle)
                        } else {
                            base.background(MaterialTheme.colorScheme.surface)
                        }
                    },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = Spacing.small,
                    end = Spacing.small,
                    top = MacToolbarTitleAreaHeight + Spacing.small,
                    bottom = Spacing.small,
                ),
            ) {
                item {
                    CategoryItem(
                        label = strings.settingsAppTitle,
                        icon = Icons.Outlined.Tune,
                        selected = selectedCategory == DesktopSettingsCategory.GENERAL,
                        onSelected = { selectedCategory = DesktopSettingsCategory.GENERAL },
                    )
                }
                item {
                    CategoryItem(
                        label = strings.settingsAccounts,
                        icon = Icons.Outlined.Sync,
                        selected = selectedCategory == DesktopSettingsCategory.ACCOUNTS,
                        onSelected = { selectedCategory = DesktopSettingsCategory.ACCOUNTS },
                    )
                }
                item {
                    CategoryItem(
                        label = strings.settingsFeedListTitle,
                        icon = Icons.Outlined.Layers,
                        selected = selectedCategory == DesktopSettingsCategory.FEED_LIST,
                        onSelected = { selectedCategory = DesktopSettingsCategory.FEED_LIST },
                    )
                }
                item {
                    CategoryItem(
                        label = strings.settingsReadingBehavior,
                        icon = Icons.Outlined.LocalLibrary,
                        selected = selectedCategory == DesktopSettingsCategory.READING,
                        onSelected = { selectedCategory = DesktopSettingsCategory.READING },
                    )
                }
                item {
                    CategoryItem(
                        label = strings.settingsSyncAndStorage,
                        icon = Icons.Outlined.Storage,
                        selected = selectedCategory == DesktopSettingsCategory.SYNC_STORAGE,
                        onSelected = { selectedCategory = DesktopSettingsCategory.SYNC_STORAGE },
                    )
                }
                item {
                    CategoryItem(
                        label = strings.settingsAboutAndSupport,
                        icon = Icons.Outlined.Info,
                        selected = selectedCategory == DesktopSettingsCategory.ABOUT,
                        onSelected = { selectedCategory = DesktopSettingsCategory.ABOUT },
                    )
                }
            }

            VerticalDivider()

            // Detail pane
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = MacToolbarTitleAreaHeight)
                    .hazeSource(state = hazeState),
            ) {
                when (selectedCategory) {
                    DesktopSettingsCategory.GENERAL -> GeneralPane(
                        themeMode = settingsState.themeMode,
                        onThemeModeSelected = menuBarViewModel::updateThemeMode,
                        isMultiPaneEnabled = isMultiPaneEnabled,
                        onMultiPaneToggled = desktopHomeSettingsRepository::setMultiPaneLayoutEnabled,
                        isReduceMotionEnabled = settingsState.isReduceMotionEnabled,
                        onReduceMotionToggled = menuBarViewModel::updateReduceMotionEnabled,
                    )

                    DesktopSettingsCategory.ACCOUNTS -> AccountsPane()

                    DesktopSettingsCategory.FEED_LIST -> FeedListPane(
                        fontSizesState = fontSizesState,
                        settingsState = feedListSettingsState,
                        feedOrder = settingsState.feedOrder,
                        onFontScaleUpdate = feedListSettingsViewModel::updateFontScale,
                        onFeedLayoutUpdate = feedListSettingsViewModel::updateFeedLayout,
                        onHideDescriptionUpdate = feedListSettingsViewModel::updateHideDescription,
                        onHideImagesUpdate = feedListSettingsViewModel::updateHideImages,
                        onHideDateUpdate = feedListSettingsViewModel::updateHideDate,
                        onRemoveTitleFromDescUpdate = feedListSettingsViewModel::updateRemoveTitleFromDescription,
                        onDateFormatUpdate = feedListSettingsViewModel::updateDateFormat,
                        onTimeFormatUpdate = feedListSettingsViewModel::updateTimeFormat,
                        onSwipeActionUpdate = feedListSettingsViewModel::updateSwipeAction,
                        onFeedOrderSelected = menuBarViewModel::updateFeedOrder,
                    )

                    DesktopSettingsCategory.READING -> ReadingPane(
                        isReaderModeEnabled = settingsState.isReaderModeEnabled,
                        isSaveReaderModeContentEnabled = settingsState.isSaveReaderModeContentEnabled,
                        isPrefetchArticleContentEnabled = settingsState.isPrefetchArticleContentEnabled,
                        isMarkReadWhenScrollingEnabled = settingsState.isMarkReadWhenScrollingEnabled,
                        isShowReadItemsEnabled = settingsState.isShowReadItemsEnabled,
                        isHideReadItemsEnabled = settingsState.isHideReadItemsEnabled,
                        onReaderModeToggled = menuBarViewModel::updateReaderMode,
                        onSaveReaderModeContentToggled = menuBarViewModel::updateSaveReaderModeContent,
                        onPrefetchToggled = menuBarViewModel::updatePrefetchArticleContent,
                        onMarkReadWhenScrollingToggled = menuBarViewModel::updateMarkReadWhenScrolling,
                        onShowReadItemsToggled = menuBarViewModel::updateShowReadItemsOnTimeline,
                        onHideReadItemsToggled = menuBarViewModel::updateHideReadItems,
                    )

                    DesktopSettingsCategory.SYNC_STORAGE -> SyncStoragePane(
                        isRefreshFeedsOnLaunchEnabled = settingsState.isRefreshFeedsOnLaunchEnabled,
                        isShowRssParsingErrorsEnabled = settingsState.isShowRssParsingErrorsEnabled,
                        autoDeletePeriod = settingsState.autoDeletePeriod,
                        onRefreshFeedsOnLaunchToggled = menuBarViewModel::updateRefreshFeedsOnLaunch,
                        onShowRssParsingErrorsToggled = menuBarViewModel::updateShowRssParsingErrors,
                        onAutoDeletePeriodSelected = menuBarViewModel::updateAutoDeletePeriod,
                        onClearDownloadedArticles = menuBarViewModel::clearDownloadedArticleContent,
                    )

                    DesktopSettingsCategory.ABOUT -> AboutPane(
                        isDarkTheme = isDarkTheme,
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
}

@Composable
private fun CategoryItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val selectedAlpha = if (colorScheme.surface.luminance() < DARK_THEME_LUMINANCE_THRESHOLD) {
        DARK_MODE_SELECTION_ALPHA
    } else {
        LIGHT_MODE_SELECTION_ALPHA
    }
    val selectedContainerColor = colorScheme.onSurface.copy(alpha = selectedAlpha)

    NavigationDrawerItem(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .height(44.dp),
        selected = selected,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        icon = {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        shape = RoundedCornerShape(14.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = selectedContainerColor,
            selectedIconColor = colorScheme.onSurface,
            selectedTextColor = colorScheme.onSurface,
            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
        onClick = onSelected,
    )
}

private val SidebarWidth = 240.dp

private const val DARK_THEME_LUMINANCE_THRESHOLD = 0.5f
private const val DARK_MODE_SELECTION_ALPHA = 0.14f
private const val LIGHT_MODE_SELECTION_ALPHA = 0.1f
