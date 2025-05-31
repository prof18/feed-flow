package com.prof18.feedflow.android.settings

import FeedFlowTheme
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MarkAsUnread
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.CrashlyticsHelper
import com.prof18.feedflow.android.settings.components.AutoDeletePeriodDialog
import com.prof18.feedflow.android.settings.components.BrowserSelector
import com.prof18.feedflow.android.settings.components.FeedOrderSelectionDialog
import com.prof18.feedflow.android.settings.components.SyncPeriodDialog
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.shared.domain.FeedDownloadWorkerEnqueuer
import com.prof18.feedflow.shared.domain.model.Browser
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.presentation.SettingsViewModel
import com.prof18.feedflow.shared.presentation.model.SettingsState
import com.prof18.feedflow.shared.presentation.preview.browsersForPreview
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.settings.DateFormatSelector
import com.prof18.feedflow.shared.ui.settings.FeedLayoutSelector
import com.prof18.feedflow.shared.ui.settings.FeedListFontSettings
import com.prof18.feedflow.shared.ui.settings.HideDescriptionSwitch
import com.prof18.feedflow.shared.ui.settings.HideImagesSwitch
import com.prof18.feedflow.shared.ui.settings.RemoveTitleFromDescSwitch
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.settings.SwipeActionSelector
import com.prof18.feedflow.shared.ui.style.Spacing
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
) {
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val feedDownloadWorkerEnqueuer = koinInject<FeedDownloadWorkerEnqueuer>()
    val context = LocalContext.current
    val browserManager = koinInject<BrowserManager>()
    val appConfig = koinInject<AppConfig>()

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
            val uri = UserFeedbackReporter.getEmailUrl(
                subject = emailSubject,
                content = emailContent,
            ).toUri()
            val emailIntent = Intent(Intent.ACTION_SENDTO, uri)
            context.startActivity(Intent.createChooser(emailIntent, chooserTitle))
        },
        navigateToImportExport = navigateToImportExport,
        navigateToAccounts = navigateToAccounts,
        setMarkReadWhenScrolling = { enabled ->
            settingsViewModel.updateMarkReadWhenScrolling(enabled)
        },
        setShowReadItem = { enabled ->
            settingsViewModel.updateShowReadItemsOnTimeline(enabled)
        },
        setReaderMode = { enabled ->
            settingsViewModel.updateReaderMode(enabled)
        },
        setExperimentalParsing = { value ->
            settingsViewModel.updateExperimentalParsing(value)
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
        navigateToNotifications = navigateToNotifications,
        onFeedOrderSelected = { order ->
            settingsViewModel.updateFeedOrder(order)
        },
        setFeedLayout = { feedLayout ->
            settingsViewModel.updateFeedLayout(feedLayout)
        },
    )
}

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
    setMarkReadWhenScrolling: (Boolean) -> Unit,
    setShowReadItem: (Boolean) -> Unit,
    setReaderMode: (Boolean) -> Unit,
    setExperimentalParsing: (Boolean) -> Unit,
    setRemoveTitleFromDescription: (Boolean) -> Unit,
    setHideDescription: (Boolean) -> Unit,
    setHideImages: (Boolean) -> Unit,
    updateFontScale: (Int) -> Unit,
    onAutoDeletePeriodSelected: (AutoDeletePeriod) -> Unit,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
    onCrashReportingEnabled: (Boolean) -> Unit,
    onSwipeActionSelected: (SwipeDirection, SwipeActionType) -> Unit,
    onDateFormatSelected: (DateFormat) -> Unit,
    navigateToNotifications: () -> Unit,
    onFeedOrderSelected: (FeedOrder) -> Unit,
    setFeedLayout: (FeedLayout) -> Unit,
) {
    Scaffold(
        topBar = {
            SettingsNavBar(navigateBack)
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues),
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
                Text(
                    text = LocalFeedFlowStrings.current.settingsBehaviourTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(Spacing.regular),
                )
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
                SettingItem(
                    title = LocalFeedFlowStrings.current.settingsNotificationsTitle,
                    icon = Icons.Outlined.Notifications,
                    onClick = navigateToNotifications,
                )
            }

            item {
                AutoDeletePeriodSelector(
                    currentPeriod = settingsState.autoDeletePeriod,
                    onPeriodSelected = onAutoDeletePeriodSelected,
                )
            }

            item {
                ReaderModeSwitch(
                    setReaderMode = setReaderMode,
                    isReaderModeEnabled = settingsState.isReaderModeEnabled,
                )
            }

            item {
                ExperimentalParsingSwitch(
                    setExperimentalParsing = setExperimentalParsing,
                    isExperimentalParsingEnabled = settingsState.isExperimentalParsingEnabled,
                )
            }

            item {
                MarkReadWhenScrollingSwitch(
                    setMarkReadWhenScrolling = setMarkReadWhenScrolling,
                    isMarkReadWhenScrollingEnabled = settingsState.isMarkReadWhenScrollingEnabled,
                )
            }

            item {
                ShowReadItemOnTimelineSwitch(
                    isShowReadItemEnabled = settingsState.isShowReadItemsEnabled,
                    setShowReadItem = setShowReadItem,
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
                    fontSizes = fontSizes,
                    updateFontScale = updateFontScale,
                    dateFormat = settingsState.dateFormat,
                    feedLayout = settingsState.feedLayout,
                )
            }

            item {
                Spacer(modifier = Modifier.padding(top = Spacing.regular))
                FeedLayoutSelector(
                    feedLayout = settingsState.feedLayout,
                    onFormatSelected = setFeedLayout,
                )
            }

            item {
                HideDescriptionSwitch(
                    isHideDescriptionEnabled = settingsState.isHideDescriptionEnabled,
                    setHideDescription = setHideDescription,
                )
            }

            item {
                HideImagesSwitch(
                    isHideImagesEnabled = settingsState.isHideImagesEnabled,
                    setHideImages = setHideImages,
                )
            }

            item {
                RemoveTitleFromDescSwitch(
                    isRemoveTitleFromDescriptionEnabled = settingsState.isRemoveTitleFromDescriptionEnabled,
                    setRemoveTitleFromDescription = setRemoveTitleFromDescription,
                )
            }

            item {
                DateFormatSelector(
                    currentFormat = settingsState.dateFormat,
                    onFormatSelected = onDateFormatSelected,
                )
            }

            item {
                FeedOrderSelector(
                    currentFeedOrder = settingsState.feedOrder,
                    onFeedOrderSelected = onFeedOrderSelected,
                )
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                onCrashReportingEnabled(!settingsState.isCrashReportingEnabled)
                            }
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xsmall)
                            .padding(horizontal = Spacing.regular),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
                    ) {
                        Icon(
                            Icons.Outlined.Report,
                            contentDescription = null,
                        )

                        Text(
                            text = LocalFeedFlowStrings.current.settingsCrashReporting,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = settingsState.isCrashReportingEnabled,
                            onCheckedChange = onCrashReportingEnabled,
                            interactionSource = remember { MutableInteractionSource() },
                        )
                    }
                }
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.aboutButton,
                    icon = Icons.Outlined.Info,
                    onClick = onAboutClick,
                )
            }
        }
    }
}

@Composable
private fun FeedOrderSelector(
    currentFeedOrder: FeedOrder,
    onFeedOrderSelected: (FeedOrder) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { showDialog = true }
            .fillMaxWidth()
            .padding(vertical = Spacing.small)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.Sort,
            contentDescription = null,
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = strings.settingsFeedOrderTitle,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = when (currentFeedOrder) {
                    FeedOrder.NEWEST_FIRST -> strings.settingsFeedOrderNewestFirst
                    FeedOrder.OLDEST_FIRST -> strings.settingsFeedOrderOldestFirst
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDialog) {
        FeedOrderSelectionDialog(
            currentFeedOrder = currentFeedOrder,
            onFeedOrderSelected = onFeedOrderSelected,
            dismissDialog = { showDialog = false },
        )
    }
}

@Composable
private fun ReaderModeSwitch(
    setReaderMode: (Boolean) -> Unit,
    isReaderModeEnabled: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable {
                setReaderMode(!isReaderModeEnabled)
            }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.Article,
            contentDescription = null,
        )

        Text(
            text = LocalFeedFlowStrings.current.settingsReaderMode,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f),
        )
        Switch(
            interactionSource = interactionSource,
            checked = isReaderModeEnabled,
            onCheckedChange = setReaderMode,
        )
    }
}

@Composable
private fun ExperimentalParsingSwitch(
    setExperimentalParsing: (Boolean) -> Unit,
    isExperimentalParsingEnabled: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable {
                setExperimentalParsing(!isExperimentalParsingEnabled)
            }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.Default.Construction,
            contentDescription = null,
        )

        Text(
            text = LocalFeedFlowStrings.current.settingsExperimentalParsing,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f),
        )
        Switch(
            interactionSource = interactionSource,
            checked = isExperimentalParsingEnabled,
            onCheckedChange = setExperimentalParsing,
        )
    }
}

@Composable
private fun MarkReadWhenScrollingSwitch(
    setMarkReadWhenScrolling: (Boolean) -> Unit,
    isMarkReadWhenScrollingEnabled: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable {
                setMarkReadWhenScrolling(!isMarkReadWhenScrollingEnabled)
            }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.Outlined.MarkAsUnread,
            contentDescription = null,
        )

        Text(
            text = LocalFeedFlowStrings.current.toggleMarkReadWhenScrolling,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f),
        )
        Switch(
            interactionSource = interactionSource,
            checked = isMarkReadWhenScrollingEnabled,
            onCheckedChange = setMarkReadWhenScrolling,
        )
    }
}

@Composable
private fun ShowReadItemOnTimelineSwitch(
    setShowReadItem: (Boolean) -> Unit,
    isShowReadItemEnabled: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable {
                setShowReadItem(!isShowReadItemEnabled)
            }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.PlaylistAddCheck,
            contentDescription = null,
        )

        Text(
            text = LocalFeedFlowStrings.current.settingsToggleShowReadArticles,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f),
        )
        Switch(
            interactionSource = interactionSource,
            checked = isShowReadItemEnabled,
            onCheckedChange = setShowReadItem,
        )
    }
}

@Composable
private fun SyncPeriodSelector(
    currentPeriod: SyncPeriod,
    onPeriodSelected: (SyncPeriod) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { showDialog = true }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.Outlined.Sync,
            contentDescription = null,
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = strings.settingsSyncPeriod,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = when (currentPeriod) {
                    SyncPeriod.NEVER -> strings.settingsSyncPeriodNever
                    SyncPeriod.FIFTEEN_MINUTES -> strings.settingsSyncPeriodFifteenMinutes
                    SyncPeriod.THIRTY_MINUTES -> strings.settingsSyncPeriodThirtyMinutes
                    SyncPeriod.ONE_HOUR -> strings.settingsSyncPeriodOneHour
                    SyncPeriod.TWO_HOURS -> strings.settingsSyncPeriodTwoHours
                    SyncPeriod.SIX_HOURS -> strings.settingsSyncPeriodSixHours
                    SyncPeriod.TWELVE_HOURS -> strings.settingsSyncPeriodTwelveHours
                    SyncPeriod.ONE_DAY -> strings.settingsSyncPeriodOneDay
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDialog) {
        SyncPeriodDialog(
            currentPeriod = currentPeriod,
            onPeriodSelected = onPeriodSelected,
            dismissDialog = { showDialog = false },
        )
    }
}

@Composable
private fun AutoDeletePeriodSelector(
    currentPeriod: AutoDeletePeriod,
    onPeriodSelected: (AutoDeletePeriod) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { showDialog = true }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.Outlined.DeleteSweep,
            contentDescription = null,
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = strings.settingsAutoDelete,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = when (currentPeriod) {
                    AutoDeletePeriod.DISABLED -> strings.settingsAutoDeletePeriodDisabled
                    AutoDeletePeriod.ONE_WEEK -> strings.settingsAutoDeletePeriodOneWeek
                    AutoDeletePeriod.TWO_WEEKS -> strings.settingsAutoDeletePeriodTwoWeeks
                    AutoDeletePeriod.ONE_MONTH -> strings.settingsAutoDeletePeriodOneMonth
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDialog) {
        AutoDeletePeriodDialog(
            currentPeriod = currentPeriod,
            onPeriodSelected = onPeriodSelected,
            dismissDialog = { showDialog = false },
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
            setMarkReadWhenScrolling = {},
            setShowReadItem = {},
            setReaderMode = {},
            setRemoveTitleFromDescription = {},
            setHideDescription = {},
            setHideImages = {},
            updateFontScale = {},
            onAutoDeletePeriodSelected = {},
            onSyncPeriodSelected = {},
            onCrashReportingEnabled = {},
            onSwipeActionSelected = { _, _ -> },
            onDateFormatSelected = {},
            navigateToNotifications = {},
            setExperimentalParsing = {},
            onFeedOrderSelected = {},
            setFeedLayout = {},
        )
    }
}
