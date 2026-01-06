package com.prof18.feedflow.desktop.home.menubar

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.DesktopConfig
import com.prof18.feedflow.desktop.accounts.AccountsScreen
import com.prof18.feedflow.desktop.addfeed.AddFeedFullScreen
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.editfeed.EditFeedScreen
import com.prof18.feedflow.desktop.feedsourcelist.FeedSourceListScreen
import com.prof18.feedflow.desktop.settings.blocked.BlockedWordsScreen
import com.prof18.feedflow.shared.presentation.MenuBarViewModel
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.utils.UserFeedbackReporter
import kotlinx.coroutines.launch

@Composable
fun FrameWindowScope.FeedFlowMenuBar(
    state: MenuBarState,
    actions: MenuBarActions,
) {
    val isMacOS = getDesktopOS().isMacOs()
    val appConfig = DI.koin.get<DesktopConfig>()
    val userFeedbackReporter = DI.koin.get<UserFeedbackReporter>()
    val navigator = LocalNavigator.currentOrThrow
    val menuBarViewModel = desktopViewModel { DI.koin.get<MenuBarViewModel>() }
    val settingsState by menuBarViewModel.state.collectAsState()
    var showPrefetchWarningDialog by remember { mutableStateOf(false) }
    var showClearDownloadedArticlesDialog by remember { mutableStateOf(false) }
    var showClearImageCacheDialog by remember { mutableStateOf(false) }
    val emailSubject = LocalFeedFlowStrings.current.issueContentTitle
    val emailContent = LocalFeedFlowStrings.current.issueContentTemplate
    val feedMenuCallbacks = FeedMenuCallbacks(
        onAddFeed = {
            navigator.push(
                AddFeedFullScreen(
                    onFeedAdded = actions.onRefreshClick,
                ),
            )
        },
        onEditFeed = { feedSource ->
            navigator.push(EditFeedScreen(feedSource))
        },
        onFeedsClick = {
            navigator.push(FeedSourceListScreen())
        },
        onBlockedWordsClick = {
            navigator.push(BlockedWordsScreen())
        },
    )
    val viewMenuCallbacks = ViewMenuCallbacks(
        onThemeModeSelected = menuBarViewModel::updateThemeMode,
        onFeedListAppearanceClick = actions.onFeedFontScaleClick,
        onShowReadItemsToggled = menuBarViewModel::updateShowReadItemsOnTimeline,
        onFeedOrderSelected = menuBarViewModel::updateFeedOrder,
    )
    val behaviorMenuCallbacks = BehaviorMenuCallbacks(
        onAccountsClick = {
            navigator.push(AccountsScreen())
        },
        onReaderModeToggled = menuBarViewModel::updateReaderMode,
        onSaveReaderModeContentToggled = menuBarViewModel::updateSaveReaderModeContent,
        onPrefetchToggle = { enabled ->
            if (enabled) {
                showPrefetchWarningDialog = true
            } else {
                menuBarViewModel.updatePrefetchArticleContent(false)
            }
        },
        onRefreshFeedsOnLaunchToggled = menuBarViewModel::updateRefreshFeedsOnLaunch,
        onMarkReadWhenScrollingToggled = menuBarViewModel::updateMarkReadWhenScrolling,
        onAutoDeletePeriodSelected = menuBarViewModel::updateAutoDeletePeriod,
        onClearDownloadedArticles = { showClearDownloadedArticlesDialog = true },
        onClearImageCache = { showClearImageCacheDialog = true },
    )
    val helpMenuCallbacks = HelpMenuCallbacks(
        onBugReportClick = {
            val desktop = java.awt.Desktop.getDesktop()
            val uri = java.net.URI.create(
                userFeedbackReporter.getEmailUrl(
                    subject = emailSubject,
                    content = emailContent,
                ),
            )
            desktop.mail(uri)
        },
        onCrashReportingToggled = { enabled ->
            menuBarViewModel.updateCrashReporting(enabled)
            if (enabled) {
                if (
                    appConfig.appEnvironment.isRelease() &&
                    appConfig.sentryDns != null &&
                    appConfig.version != null
                ) {
                    com.prof18.feedflow.desktop.utils.initSentry(
                        dns = appConfig.sentryDns,
                        version = appConfig.version,
                    )
                }
            } else {
                com.prof18.feedflow.desktop.utils.disableSentry()
            }
        },
        onAboutClick = actions.onAboutClick,
    )

    PrefetchWarningDialog(
        visible = showPrefetchWarningDialog,
        onDismiss = { showPrefetchWarningDialog = false },
        onConfirm = {
            menuBarViewModel.updatePrefetchArticleContent(true)
            showPrefetchWarningDialog = false
        },
    )

    ClearDownloadedArticlesDialog(
        visible = showClearDownloadedArticlesDialog,
        onDismiss = { showClearDownloadedArticlesDialog = false },
        onConfirm = {
            menuBarViewModel.clearDownloadedArticleContent()
            showClearDownloadedArticlesDialog = false
        },
    )

    ClearImageCacheDialog(
        visible = showClearImageCacheDialog,
        onDismiss = { showClearImageCacheDialog = false },
        onConfirm = {
            showClearImageCacheDialog = false
        },
    )

    MenuBar {
        FileMenu(
            isMacOS = isMacOS,
            state = state,
            actions = actions,
        )

        FeedMenu(
            isMacOS = isMacOS,
            state = state,
            callbacks = feedMenuCallbacks,
        )

        ViewMenu(
            settingsState = settingsState,
            callbacks = viewMenuCallbacks,
        )

        BehaviorMenu(
            isMacOS = isMacOS,
            settingsState = settingsState,
            callbacks = behaviorMenuCallbacks,
        )

        HelpMenu(
            settingsState = settingsState,
            callbacks = helpMenuCallbacks,
        )
    }
}

@Composable
private fun PrefetchWarningDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(LocalFeedFlowStrings.current.settingsPrefetchArticleContent) },
            text = { Text(LocalFeedFlowStrings.current.settingsPrefetchArticleContentWarning) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(LocalFeedFlowStrings.current.confirmButton)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(LocalFeedFlowStrings.current.cancelButton)
                }
            },
        )
    }
}

@Composable
private fun ClearDownloadedArticlesDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(LocalFeedFlowStrings.current.settingsClearDownloadedArticlesDialogTitle) },
            text = { Text(LocalFeedFlowStrings.current.settingsClearDownloadedArticlesDialogMessage) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(LocalFeedFlowStrings.current.confirmButton)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(LocalFeedFlowStrings.current.cancelButton)
                }
            },
        )
    }
}

@Composable
private fun ClearImageCacheDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (visible) {
        val context = LocalPlatformContext.current
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(LocalFeedFlowStrings.current.settingsClearImageCacheDialogTitle) },
            text = { Text(LocalFeedFlowStrings.current.settingsClearImageCacheDialogMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val imageLoader = SingletonImageLoader.get(context)
                            imageLoader.memoryCache?.clear()
                            imageLoader.diskCache?.clear()
                        }
                        onConfirm()
                    },
                ) {
                    Text(LocalFeedFlowStrings.current.confirmButton)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(LocalFeedFlowStrings.current.cancelButton)
                }
            },
        )
    }
}

data class MenuBarActions(
    val onRefreshClick: () -> Unit,
    val onMarkAllReadClick: () -> Unit,
    val onImportExportClick: () -> Unit,
    val onClearOldFeedClick: () -> Unit,
    val onAboutClick: () -> Unit,
    val onForceRefreshClick: () -> Unit,
    val onFeedFontScaleClick: () -> Unit,
    val deleteFeeds: () -> Unit,
    val onBackupClick: () -> Unit,
)

data class MenuBarState(
    val showDebugMenu: Boolean,
    val feedFilter: FeedFilter,
    val isSyncUploadRequired: Boolean,
)
