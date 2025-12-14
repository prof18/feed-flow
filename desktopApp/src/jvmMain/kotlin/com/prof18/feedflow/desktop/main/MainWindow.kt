package com.prof18.feedflow.desktop.main

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScaleTransition
import co.touchlab.kermit.Logger
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.DesktopConfig
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.home.FeedFlowMenuBar
import com.prof18.feedflow.desktop.home.MenuBarActions
import com.prof18.feedflow.desktop.home.MenuBarSettings
import com.prof18.feedflow.desktop.home.MenuBarState
import com.prof18.feedflow.desktop.importexport.ImportExportScreen
import com.prof18.feedflow.desktop.resources.Res
import com.prof18.feedflow.desktop.resources.icon
import com.prof18.feedflow.desktop.ui.components.scrollbarStyle
import com.prof18.feedflow.desktop.utils.disableSentry
import com.prof18.feedflow.desktop.utils.initSentry
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.DatabaseCloser
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.presentation.SettingsViewModel
import com.prof18.feedflow.shared.presentation.model.SettingsState
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.utils.UserFeedbackReporter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import java.awt.Desktop
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.net.URI
import kotlin.math.roundToInt

@Composable
internal fun ApplicationScope.MainWindow(
    feedSyncRepo: FeedSyncRepository,
    windowState: WindowState,
    settingsRepository: SettingsRepository,
    messageQueue: FeedSyncMessageQueue,
    isDarkTheme: Boolean,
    appConfig: DesktopConfig,
    settingsViewModel: SettingsViewModel,
    settingsState: SettingsState,
    homeViewModel: HomeViewModel,
    searchViewModel: SearchViewModel,
) {
    val scope = rememberCoroutineScope()
    var showBackupLoader by remember { mutableStateOf(false) }
    val icon = painterResource(Res.drawable.icon)

    val userFeedbackReporter = DI.koin.get<UserFeedbackReporter>()

    Window(
        onCloseRequest = {
            scope.launch {
                showBackupLoader = true
                try {
                    feedSyncRepo.performBackup()
                    DI.koin.get<DatabaseCloser>().close()
                } catch (e: Exception) {
                    DI.koin.get<Logger>().e("Error during cleanup", e)
                } finally {
                    exitApplication()
                }
            }
        },
        title = "",
        state = windowState,
        icon = icon,
    ) {
        val listener = object : WindowFocusListener {
            override fun windowGainedFocus(e: WindowEvent) {
                // Do nothing
            }

            override fun windowLostFocus(e: WindowEvent) {
                scope.launch {
                    feedSyncRepo.performBackup()
                }
            }
        }

        LaunchedEffect(window.rootPane) {
            if (getDesktopOS().isMacOs()) {
                with(window.rootPane) {
                    putClientProperty("apple.awt.transparentTitleBar", true)
                    putClientProperty("apple.awt.fullWindowContent", true)
                }
            }
        }

        DisposableEffect(Unit) {
            window.addWindowFocusListener(listener)
            onDispose {
                window.removeWindowFocusListener(listener)
            }
        }

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(windowState) {
            snapshotFlow { windowState.size }
                .collect { size ->
                    settingsRepository.setDesktopWindowWidthDp(size.width.value.roundToInt())
                    settingsRepository.setDesktopWindowHeightDp(size.height.value.roundToInt())
                }
        }

        LaunchedEffect(windowState) {
            snapshotFlow { windowState.position }
                .collect { position ->
                    settingsRepository.setDesktopWindowXPositionDp(position.x.value)
                    settingsRepository.setDesktopWindowYPositionDp(position.y.value)
                }
        }

        val flowStrings = LocalFeedFlowStrings.current
        LaunchedEffect(Unit) {
            messageQueue.messageQueue.collect { message ->
                if (message is SyncResult.Error) {
                    snackbarHostState.showSnackbar(
                        message = flowStrings.errorAccountSync(message.errorCode.code),
                    )
                }
            }
        }

        if (showBackupLoader) {
            FeedFlowTheme(darkTheme = isDarkTheme) {
                Scaffold {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Text(
                            modifier = Modifier
                                .padding(top = Spacing.regular),
                            text = LocalFeedFlowStrings.current.feedSyncInProgress,
                        )
                    }
                }
            }
        } else {
            CompositionLocalProvider(LocalScrollbarStyle provides scrollbarStyle()) {
                val emailSubject = LocalFeedFlowStrings.current.issueContentTitle
                val emailContent = LocalFeedFlowStrings.current.issueContentTemplate

                val listState = rememberLazyListState()

                var aboutDialogVisibility by remember { mutableStateOf(false) }
                var showMarkAllReadDialog by remember { mutableStateOf(false) }
                var showClearOldArticlesDialog by remember { mutableStateOf(false) }
                var showPrefetchWarningDialog by remember { mutableStateOf(false) }
                var showClearDownloadedArticlesDialog by remember { mutableStateOf(false) }
                var showClearImageCacheDialog by remember { mutableStateOf(false) }

                AboutDialog(
                    visible = aboutDialogVisibility,
                    onCloseRequest = { aboutDialogVisibility = false },
                    version = appConfig.version ?: "N/A",
                    isDarkTheme = isDarkTheme,
                )

                var feedListFontDialogState by remember { mutableStateOf(false) }
                val fontSizesState by settingsViewModel.feedFontSizeState.collectAsState()

                FeedListAppearanceDialog(
                    visible = feedListFontDialogState,
                    onCloseRequest = { feedListFontDialogState = false },
                    fontSizesState = fontSizesState,
                    settingsState = settingsState,
                    callbacks = FeedListAppearanceCallbacks(
                        onFontScaleUpdate = { fontScale ->
                            settingsViewModel.updateFontScale(fontScale)
                        },
                        onFeedLayoutUpdate = { feedLayout ->
                            settingsViewModel.updateFeedLayout(feedLayout)
                        },
                        onHideDescriptionUpdate = { enabled ->
                            settingsViewModel.updateHideDescription(enabled)
                        },
                        onHideImagesUpdate = { enabled ->
                            settingsViewModel.updateHideImages(enabled)
                        },
                        onHideDateUpdate = { enabled ->
                            settingsViewModel.updateHideDate(enabled)
                        },
                        onRemoveTitleFromDescUpdate = { enabled ->
                            settingsViewModel.updateRemoveTitleFromDescription(enabled)
                        },
                        onDateFormatUpdate = { format ->
                            settingsViewModel.updateDateFormat(format)
                        },
                        onTimeFormatUpdate = { format ->
                            settingsViewModel.updateTimeFormat(format)
                        },
                        onSwipeActionUpdate = { direction, action ->
                            settingsViewModel.updateSwipeAction(direction, action)
                        },
                    ),
                )

                if (showMarkAllReadDialog) {
                    AlertDialog(
                        onDismissRequest = { showMarkAllReadDialog = false },
                        title = { Text(LocalFeedFlowStrings.current.markAllReadButton) },
                        text = { Text(LocalFeedFlowStrings.current.markAllReadDialogMessage) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    homeViewModel.markAllRead()
                                    showMarkAllReadDialog = false
                                },
                            ) {
                                Text(LocalFeedFlowStrings.current.confirmButton)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showMarkAllReadDialog = false },
                            ) {
                                Text(LocalFeedFlowStrings.current.cancelButton)
                            }
                        },
                    )
                }

                if (showClearOldArticlesDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearOldArticlesDialog = false },
                        title = { Text(LocalFeedFlowStrings.current.clearOldArticlesButton) },
                        text = { Text(LocalFeedFlowStrings.current.clearOldArticlesDialogMessage) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    homeViewModel.deleteOldFeedItems()
                                    showClearOldArticlesDialog = false
                                },
                            ) {
                                Text(LocalFeedFlowStrings.current.confirmButton)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showClearOldArticlesDialog = false },
                            ) {
                                Text(LocalFeedFlowStrings.current.cancelButton)
                            }
                        },
                    )
                }

                if (showPrefetchWarningDialog) {
                    AlertDialog(
                        onDismissRequest = { showPrefetchWarningDialog = false },
                        title = { Text(LocalFeedFlowStrings.current.settingsPrefetchArticleContent) },
                        text = { Text(LocalFeedFlowStrings.current.settingsPrefetchArticleContentWarning) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    settingsViewModel.updatePrefetchArticleContent(true)
                                    showPrefetchWarningDialog = false
                                },
                            ) {
                                Text(LocalFeedFlowStrings.current.confirmButton)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showPrefetchWarningDialog = false },
                            ) {
                                Text(LocalFeedFlowStrings.current.cancelButton)
                            }
                        },
                    )
                }

                ClearDownloadedArticlesDialog(
                    visible = showClearDownloadedArticlesDialog,
                    onDismiss = { showClearDownloadedArticlesDialog = false },
                    onConfirm = {
                        settingsViewModel.clearDownloadedArticleContent()
                        showClearDownloadedArticlesDialog = false
                    },
                )

                ClearImageCacheDialog(
                    visible = showClearImageCacheDialog,
                    onDismiss = { showClearImageCacheDialog = false },
                    onConfirm = {
                        val imageLoader = DI.koin.get<ImageLoader>()
                        imageLoader.diskCache?.clear()
                        imageLoader.memoryCache?.clear()
                        showClearImageCacheDialog = false
                    },
                )

                val currentFeedFilter by homeViewModel.currentFeedFilter.collectAsState()

                Surface(
                    modifier = Modifier
                        .then(
                            if (getDesktopOS().isMacOs()) {
                                Modifier
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(top = Spacing.regular)
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Navigator(
                        @Suppress("ViewModelForwarding")
                        MainScreen(
                            frameWindowScope = this@Window,
                            appEnvironment = appConfig.appEnvironment,
                            version = appConfig.version,
                            homeViewModel = homeViewModel,
                            searchViewModel = searchViewModel,
                            listState = listState,
                        ),
                    ) { navigator ->
                        FeedFlowMenuBar(
                            state = MenuBarState(
                                showDebugMenu = appConfig.appEnvironment.isDebug(),
                                feedFilter = currentFeedFilter,
                                settingsState = settingsState,
                            ),
                            actions = MenuBarActions(
                                onRefreshClick = {
                                    scope.launch {
                                        listState.animateScrollToItem(0)
                                        homeViewModel.getNewFeeds()
                                    }
                                },
                                onMarkAllReadClick = {
                                    showMarkAllReadDialog = true
                                },
                                onImportExportClick = {
                                    navigator.push(
                                        ImportExportScreen(
                                            composeWindow = window,
                                            triggerFeedFetch = {
                                                homeViewModel.getNewFeeds()
                                            },
                                        ),
                                    )
                                },
                                onClearOldFeedClick = {
                                    showClearOldArticlesDialog = true
                                },
                                onAboutClick = {
                                    aboutDialogVisibility = true
                                },
                                onBugReportClick = {
                                    val desktop = Desktop.getDesktop()
                                    val uri = URI.create(
                                        userFeedbackReporter.getEmailUrl(
                                            subject = emailSubject,
                                            content = emailContent,
                                        ),
                                    )
                                    desktop.mail(uri)
                                },
                                onForceRefreshClick = {
                                    scope.launch {
                                        listState.animateScrollToItem(0)
                                        homeViewModel.forceFeedRefresh()
                                    }
                                },
                                onFeedFontScaleClick = {
                                    feedListFontDialogState = true
                                },
                                deleteFeeds = {
                                    homeViewModel.deleteAllFeeds()
                                },
                            ),
                            settings = MenuBarSettings(
                                onFeedOrderSelected = { order ->
                                    settingsViewModel.updateFeedOrder(order)
                                },
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
                                    if (enabled) {
                                        showPrefetchWarningDialog = true
                                    } else {
                                        settingsViewModel.updatePrefetchArticleContent(false)
                                    }
                                },
                                onAutoDeletePeriodSelected = { period ->
                                    settingsViewModel.updateAutoDeletePeriod(period)
                                },
                                setCrashReportingEnabled = { enabled ->
                                    settingsViewModel.updateCrashReporting(enabled)
                                    if (enabled) {
                                        if (
                                            appConfig.appEnvironment.isRelease() &&
                                            appConfig.sentryDns != null &&
                                            appConfig.version != null
                                        ) {
                                            initSentry(
                                                dns = appConfig.sentryDns,
                                                version = appConfig.version,
                                            )
                                        }
                                    } else {
                                        disableSentry()
                                    }
                                },
                                onThemeModeSelected = { mode ->
                                    settingsViewModel.updateThemeMode(mode)
                                },
                                onClearDownloadedArticles = {
                                    showClearDownloadedArticlesDialog = true
                                },
                                onClearImageCache = {
                                    showClearImageCacheDialog = true
                                },
                            ),
                        )

                        ScaleTransition(navigator)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    SnackbarHost(snackbarHostState)
                }
            }
        }
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
