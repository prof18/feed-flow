package com.prof18.feedflow.android.settings

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MarkAsUnread
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.SubtitlesOff
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.imageLoader
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.CrashlyticsHelper
import com.prof18.feedflow.android.settings.components.AutoDeletePeriodDialog
import com.prof18.feedflow.android.settings.components.BrowserSelector
import com.prof18.feedflow.android.settings.components.FeedOrderSelectionDialog
import com.prof18.feedflow.android.settings.components.SyncPeriodSelector
import com.prof18.feedflow.android.settings.components.ThemeModeDialog
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.core.utils.FeatureFlags
import com.prof18.feedflow.shared.domain.FeedDownloadWorkerEnqueuer
import com.prof18.feedflow.shared.domain.model.Browser
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.presentation.SettingsViewModel
import com.prof18.feedflow.shared.presentation.model.SettingsState
import com.prof18.feedflow.shared.presentation.preview.browsersForPreview
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.settings.ConfirmationDialogConfig
import com.prof18.feedflow.shared.ui.settings.ConfirmationSettingItem
import com.prof18.feedflow.shared.ui.settings.DateFormatSelector
import com.prof18.feedflow.shared.ui.settings.FeedLayoutSelector
import com.prof18.feedflow.shared.ui.settings.FeedListFontSettings
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.settings.SettingSelectorItem
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.settings.SwipeActionSelector
import com.prof18.feedflow.shared.ui.settings.TimeFormatSelector
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.utils.UserFeedbackReporter
import kotlinx.collections.immutable.ImmutableList
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onFeedListClick: () -> Unit,
    onAddFeedClick: () -> Unit,
    navigateBack: () -> Unit,
    onAboutClick: () -> Unit,
    navigateToImportExport: () -> Unit,
    navigateToAccounts: () -> Unit,
    navigateToNotifications: () -> Unit,
    navigateToBlockedWords: () -> Unit,
) {
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val feedDownloadWorkerEnqueuer = koinInject<FeedDownloadWorkerEnqueuer>()
    val context = LocalContext.current
    val browserManager = koinInject<BrowserManager>()
    val appConfig = koinInject<AppConfig>()
    val userFeedbackReported = koinInject<UserFeedbackReporter>()

    val browserListState by browserManager.browserListState.collectAsStateWithLifecycle()
    val settingState by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    val fontSizesState by settingsViewModel.feedFontSizeState.collectAsStateWithLifecycle()

    val emailSubject = LocalFeedFlowStrings.current.issueContentTitle
    val emailContent = LocalFeedFlowStrings.current.issueContentTemplate
    val chooserTitle = LocalFeedFlowStrings.current.issueReportTitle

    SettingsScreenContent(
        browsers = browserListState,
        settingsState = settingState,
        fontSizes = fontSizesState,
        showCrashReporting = appConfig.isLoggingEnabled,
        onFeedListClick = onFeedListClick,
        onAddFeedClick = onAddFeedClick,
        onBrowserSelected = { browser ->
            browserManager.setFavouriteBrowser(browser)
        },
        navigateBack = navigateBack,
        onAboutClick = onAboutClick,
        onBugReportClick = {
            val uri = userFeedbackReported.getEmailUrl(
                subject = emailSubject,
                content = emailContent,
            ).toUri()
            val emailIntent = Intent(Intent.ACTION_SENDTO, uri)
            context.startActivity(Intent.createChooser(emailIntent, chooserTitle))
        },
        navigateToImportExport = navigateToImportExport,
        navigateToAccounts = navigateToAccounts,
        navigateToBlockedWords = navigateToBlockedWords,
        setMarkReadWhenScrolling = { enabled ->
            settingsViewModel.updateMarkReadWhenScrolling(enabled)
        },
        setShowReadItem = { enabled ->
            settingsViewModel.updateShowReadItemsOnTimeline(enabled)
        },
        setReaderMode = { enabled ->
            settingsViewModel.updateReaderMode(enabled)
        },
        setSaveReaderModeContent = { enabled ->
            settingsViewModel.updateSaveReaderModeContent(enabled)
        },
        setPrefetchArticleContent = { enabled ->
            settingsViewModel.updatePrefetchArticleContent(enabled)
        },
        setRemoveTitleFromDescription = { enabled ->
            settingsViewModel.updateRemoveTitleFromDescription(enabled)
        },
        setHideDescription = { enabled ->
            settingsViewModel.updateHideDescription(enabled)
        },
        setHideImages = { enabled ->
            settingsViewModel.updateHideImages(enabled)
        },
        setHideDate = { enabled ->
            settingsViewModel.updateHideDate(enabled)
        },
        updateFontScale = { newFontSize ->
            settingsViewModel.updateFontScale(newFontSize)
        },
        onAutoDeletePeriodSelected = { period ->
            settingsViewModel.updateAutoDeletePeriod(period)
        },
        onSyncPeriodSelected = { period ->
            settingsViewModel.updateSyncPeriod(period)
            feedDownloadWorkerEnqueuer.updateWorker(period)
        },
        onCrashReportingEnabled = { enabled ->
            settingsViewModel.updateCrashReporting(enabled)
            if (appConfig.isLoggingEnabled) {
                CrashlyticsHelper.setCollectionEnabled(enabled)
            }
        },
        onSwipeActionSelected = { direction, action ->
            settingsViewModel.updateSwipeAction(direction, action)
        },
        onDateFormatSelected = { format ->
            settingsViewModel.updateDateFormat(format)
        },
        onTimeFormatSelected = { format ->
            settingsViewModel.updateTimeFormat(format)
        },
        navigateToNotifications = navigateToNotifications,
        onFeedOrderSelected = { order ->
            settingsViewModel.updateFeedOrder(order)
        },
        setFeedLayout = { feedLayout ->
            settingsViewModel.updateFeedLayout(feedLayout)
        },
        onThemeModeSelected = { themeMode ->
            settingsViewModel.updateThemeMode(themeMode)
        },
        onClearDownloadedArticles = {
            settingsViewModel.clearDownloadedArticleContent()
        },
        openInAppBrowser = { faqUrl ->
            browserManager.openWithInAppBrowser(faqUrl, context)
        },
    )
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun SettingsScreenContent(
    browsers: ImmutableList<Browser>,
    settingsState: SettingsState,
    fontSizes: FeedFontSizes,
    showCrashReporting: Boolean,
    onFeedListClick: () -> Unit,
    onAddFeedClick: () -> Unit,
    onBrowserSelected: (Browser) -> Unit,
    navigateBack: () -> Unit,
    onAboutClick: () -> Unit,
    onBugReportClick: () -> Unit,
    navigateToImportExport: () -> Unit,
    navigateToAccounts: () -> Unit,
    navigateToBlockedWords: () -> Unit,
    setMarkReadWhenScrolling: (Boolean) -> Unit,
    setShowReadItem: (Boolean) -> Unit,
    setReaderMode: (Boolean) -> Unit,
    setSaveReaderModeContent: (Boolean) -> Unit,
    setPrefetchArticleContent: (Boolean) -> Unit,
    setRemoveTitleFromDescription: (Boolean) -> Unit,
    setHideDescription: (Boolean) -> Unit,
    setHideImages: (Boolean) -> Unit,
    setHideDate: (Boolean) -> Unit,
    updateFontScale: (Int) -> Unit,
    onAutoDeletePeriodSelected: (AutoDeletePeriod) -> Unit,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
    onCrashReportingEnabled: (Boolean) -> Unit,
    onSwipeActionSelected: (SwipeDirection, SwipeActionType) -> Unit,
    onDateFormatSelected: (DateFormat) -> Unit,
    onTimeFormatSelected: (TimeFormat) -> Unit,
    navigateToNotifications: () -> Unit,
    onFeedOrderSelected: (FeedOrder) -> Unit,
    setFeedLayout: (FeedLayout) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onClearDownloadedArticles: () -> Unit,
    openInAppBrowser: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            SettingsNavBar(navigateBack)
        },
    ) { paddingValues ->
        val layoutDir = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .padding(start = paddingValues.calculateLeftPadding(layoutDir))
                .padding(end = paddingValues.calculateRightPadding(layoutDir)),
        ) {
            item {
                Text(
                    text = LocalFeedFlowStrings.current.settingsTitleFeed,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(Spacing.regular),
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.feedsTitle,
                    icon = Icons.AutoMirrored.Default.Feed,
                    onClick = onFeedListClick,
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.addFeed,
                    icon = Icons.Outlined.AddCircleOutline,
                    onClick = onAddFeedClick,
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.importExportOpml,
                    icon = Icons.Outlined.SwapVert,
                    onClick = navigateToImportExport,
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.settingsAccounts,
                    icon = Icons.Outlined.Sync,
                    onClick = navigateToAccounts,
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.settingsNotificationsTitle,
                    icon = Icons.Outlined.Notifications,
                    onClick = navigateToNotifications,
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.settingsBlockedWords,
                    icon = Icons.Outlined.Report,
                    onClick = navigateToBlockedWords,
                )
            }

            item {
                Text(
                    text = LocalFeedFlowStrings.current.settingsBehaviourTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(Spacing.regular),
                )
            }

            item {
                val themeModeLabel = when (settingsState.themeMode) {
                    ThemeMode.LIGHT -> LocalFeedFlowStrings.current.settingsThemeLight
                    ThemeMode.DARK -> LocalFeedFlowStrings.current.settingsThemeDark
                    ThemeMode.SYSTEM -> LocalFeedFlowStrings.current.settingsThemeSystem
                }
                var showDialog by remember { mutableStateOf(false) }

                SettingSelectorItem(
                    title = LocalFeedFlowStrings.current.settingsTheme,
                    currentValueLabel = themeModeLabel,
                    icon = Icons.Outlined.DarkMode,
                    onClick = { showDialog = true },
                )

                if (showDialog) {
                    ThemeModeDialog(
                        currentThemeMode = settingsState.themeMode,
                        onThemeModeSelected = onThemeModeSelected,
                        dismissDialog = { showDialog = false },
                    )
                }
            }

            item {
                BrowserSelector(
                    browsers = browsers,
                    onBrowserSelected = onBrowserSelected,
                )
            }

            item {
                SyncPeriodSelector(
                    currentPeriod = settingsState.syncPeriod,
                    onPeriodSelected = onSyncPeriodSelected,
                )
            }

            item {
                val autoDeleteLabel = when (settingsState.autoDeletePeriod) {
                    AutoDeletePeriod.DISABLED -> LocalFeedFlowStrings.current.settingsAutoDeletePeriodDisabled
                    AutoDeletePeriod.ONE_DAY -> LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneDay
                    AutoDeletePeriod.ONE_WEEK -> LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneWeek
                    AutoDeletePeriod.TWO_WEEKS -> LocalFeedFlowStrings.current.settingsAutoDeletePeriodTwoWeeks
                    AutoDeletePeriod.ONE_MONTH -> LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneMonth
                }
                var showDialog by remember { mutableStateOf(false) }

                SettingSelectorItem(
                    title = LocalFeedFlowStrings.current.settingsAutoDelete,
                    currentValueLabel = autoDeleteLabel,
                    icon = Icons.Outlined.DeleteSweep,
                    onClick = { showDialog = true },
                )

                if (showDialog) {
                    AutoDeletePeriodDialog(
                        currentPeriod = settingsState.autoDeletePeriod,
                        onPeriodSelected = onAutoDeletePeriodSelected,
                        dismissDialog = { showDialog = false },
                    )
                }
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.settingsReaderMode,
                    icon = Icons.AutoMirrored.Outlined.Article,
                    isChecked = settingsState.isReaderModeEnabled,
                    onCheckedChange = setReaderMode,
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.settingsSaveReaderModeContent,
                    icon = Icons.AutoMirrored.Outlined.Article,
                    isChecked = settingsState.isSaveReaderModeContentEnabled,
                    onCheckedChange = setSaveReaderModeContent,
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.settingsPrefetchArticleContent,
                    icon = Icons.Outlined.CloudDownload,
                    isChecked = settingsState.isPrefetchArticleContentEnabled,
                    onCheckedChange = setPrefetchArticleContent,
                    confirmationDialog = ConfirmationDialogConfig(
                        title = LocalFeedFlowStrings.current.settingsPrefetchArticleContent,
                        message = LocalFeedFlowStrings.current.settingsPrefetchArticleContentWarning,
                    ),
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.toggleMarkReadWhenScrolling,
                    icon = Icons.Outlined.MarkAsUnread,
                    isChecked = settingsState.isMarkReadWhenScrollingEnabled,
                    onCheckedChange = setMarkReadWhenScrolling,
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.settingsToggleShowReadArticles,
                    icon = Icons.AutoMirrored.Outlined.PlaylistAddCheck,
                    isChecked = settingsState.isShowReadItemsEnabled,
                    onCheckedChange = setShowReadItem,
                )
            }

            item {
                Text(
                    text = LocalFeedFlowStrings.current.settingsFeedListTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(Spacing.regular),
                )
            }

            item {
                FeedListFontSettings(
                    isHideDescriptionEnabled = settingsState.isHideDescriptionEnabled,
                    isHideImagesEnabled = settingsState.isHideImagesEnabled,
                    isHideDateEnabled = settingsState.isHideDateEnabled,
                    fontSizes = fontSizes,
                    updateFontScale = updateFontScale,
                    dateFormat = settingsState.dateFormat,
                    timeFormat = settingsState.timeFormat,
                    feedLayout = settingsState.feedLayout,
                )
            }

            item {
                Spacer(modifier = Modifier.padding(top = Spacing.regular))
                FeedLayoutSelector(
                    feedLayout = settingsState.feedLayout,
                    onFeedLayoutSelected = setFeedLayout,
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.settingsHideDescription,
                    icon = Icons.Outlined.SubtitlesOff,
                    isChecked = settingsState.isHideDescriptionEnabled,
                    onCheckedChange = setHideDescription,
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.settingsHideImages,
                    icon = Icons.Outlined.HideImage,
                    isChecked = settingsState.isHideImagesEnabled,
                    onCheckedChange = setHideImages,
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.settingsHideDate,
                    icon = Icons.Outlined.EventBusy,
                    isChecked = settingsState.isHideDateEnabled,
                    onCheckedChange = setHideDate,
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.settingsHideDuplicatedTitleFromDesc,
                    icon = Icons.Outlined.HideSource,
                    isChecked = settingsState.isRemoveTitleFromDescriptionEnabled,
                    onCheckedChange = setRemoveTitleFromDescription,
                )
            }

            item {
                DateFormatSelector(
                    currentFormat = settingsState.dateFormat,
                    onFormatSelected = onDateFormatSelected,
                )
            }

            item {
                TimeFormatSelector(
                    currentFormat = settingsState.timeFormat,
                    onFormatSelected = onTimeFormatSelected,
                )
            }

            item {
                val feedOrderLabel = when (settingsState.feedOrder) {
                    FeedOrder.NEWEST_FIRST -> LocalFeedFlowStrings.current.settingsFeedOrderNewestFirst
                    FeedOrder.OLDEST_FIRST -> LocalFeedFlowStrings.current.settingsFeedOrderOldestFirst
                }
                var showDialog by remember { mutableStateOf(false) }

                SettingSelectorItem(
                    title = LocalFeedFlowStrings.current.settingsFeedOrderTitle,
                    currentValueLabel = feedOrderLabel,
                    icon = Icons.AutoMirrored.Outlined.Sort,
                    onClick = { showDialog = true },
                )

                if (showDialog) {
                    FeedOrderSelectionDialog(
                        currentFeedOrder = settingsState.feedOrder,
                        onFeedOrderSelected = onFeedOrderSelected,
                        dismissDialog = { showDialog = false },
                    )
                }
            }

            item {
                SwipeActionSelector(
                    direction = SwipeDirection.LEFT,
                    currentAction = settingsState.leftSwipeActionType,
                    onActionSelected = { action ->
                        onSwipeActionSelected(SwipeDirection.LEFT, action)
                    },
                )
            }

            item {
                SwipeActionSelector(
                    direction = SwipeDirection.RIGHT,
                    currentAction = settingsState.rightSwipeActionType,
                    onActionSelected = { action ->
                        onSwipeActionSelected(SwipeDirection.RIGHT, action)
                    },
                )
            }

            item {
                DangerSection(
                    onClearDownloadedArticles = onClearDownloadedArticles,
                )
            }

            item {
                Text(
                    text = LocalFeedFlowStrings.current.settingsAppTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(Spacing.regular),
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.reportIssueButton,
                    icon = Icons.Outlined.BugReport,
                    onClick = onBugReportClick,
                )
            }

            if (showCrashReporting) {
                item {
                    SettingSwitchItem(
                        title = LocalFeedFlowStrings.current.settingsCrashReporting,
                        icon = Icons.Outlined.Report,
                        isChecked = settingsState.isCrashReportingEnabled,
                        onCheckedChange = onCrashReportingEnabled,
                    )
                }
            }

            if (FeatureFlags.ENABLE_FAQ) {
                item {
                    SettingItem(
                        title = LocalFeedFlowStrings.current.aboutMenuFaq,
                        icon = Icons.Outlined.QuestionMark,
                        onClick = {
                            val languageCode = java.util.Locale.getDefault().language
                            val faqUrl = "https://feedflow.dev/$languageCode/faq"
                            openInAppBrowser(faqUrl)
                        },
                    )
                }
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.aboutButton,
                    icon = Icons.Outlined.Info,
                    onClick = onAboutClick,
                )
            }

            item {
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}

@Composable
private fun DangerSection(
    onClearDownloadedArticles: () -> Unit,
) {
    val context = LocalContext.current

    Column {
        Text(
            text = LocalFeedFlowStrings.current.settingsDangerTitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
            modifier = Modifier.padding(Spacing.regular),
        )

        ConfirmationSettingItem(
            title = LocalFeedFlowStrings.current.settingsClearDownloadedArticles,
            icon = Icons.Outlined.DeleteSweep,
            dialogTitle = LocalFeedFlowStrings.current.settingsClearDownloadedArticlesDialogTitle,
            dialogMessage = LocalFeedFlowStrings.current.settingsClearDownloadedArticlesDialogMessage,
            onConfirm = onClearDownloadedArticles,
        )

        ConfirmationSettingItem(
            title = LocalFeedFlowStrings.current.settingsClearImageCache,
            icon = Icons.Outlined.Image,
            dialogTitle = LocalFeedFlowStrings.current.settingsClearImageCacheDialogTitle,
            dialogMessage = LocalFeedFlowStrings.current.settingsClearImageCacheDialogMessage,
            onConfirm = {
                val imageLoader = context.applicationContext.imageLoader
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()
            },
        )
    }
}

@Composable
private fun SettingsNavBar(navigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(LocalFeedFlowStrings.current.settingsTitle)
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navigateBack()
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = null,
                )
            }
        },
    )
}

@PreviewPhone
@Composable
private fun SettingsScreenPreview() {
    FeedFlowTheme {
        SettingsScreenContent(
            browsers = browsersForPreview,
            settingsState = SettingsState(),
            fontSizes = FeedFontSizes(),
            showCrashReporting = true,
            onFeedListClick = {},
            onAddFeedClick = {},
            onBrowserSelected = {},
            navigateBack = {},
            onAboutClick = {},
            onBugReportClick = {},
            navigateToImportExport = {},
            navigateToAccounts = {},
            navigateToBlockedWords = {},
            setMarkReadWhenScrolling = {},
            setShowReadItem = {},
            setReaderMode = {},
            setSaveReaderModeContent = {},
            setPrefetchArticleContent = {},
            setRemoveTitleFromDescription = {},
            setHideDescription = {},
            setHideImages = {},
            updateFontScale = {},
            onAutoDeletePeriodSelected = {},
            onSyncPeriodSelected = {},
            onCrashReportingEnabled = {},
            onSwipeActionSelected = { _, _ -> },
            onDateFormatSelected = {},
            onTimeFormatSelected = {},
            navigateToNotifications = {},
            onFeedOrderSelected = {},
            setFeedLayout = {},
            onThemeModeSelected = {},
            onClearDownloadedArticles = {},
            setHideDate = {},
            openInAppBrowser = {},
        )
    }
}
