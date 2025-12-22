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
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowState
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScaleTransition
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.utils.DesktopDatabaseErrorState
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.DesktopConfig
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.home.FeedFlowMenuBar
import com.prof18.feedflow.desktop.home.MenuBarActions
import com.prof18.feedflow.desktop.home.MenuBarSettings
import com.prof18.feedflow.desktop.home.MenuBarState
import com.prof18.feedflow.desktop.importexport.ImportExportScreen
import com.prof18.feedflow.desktop.ui.components.scrollbarStyle
import com.prof18.feedflow.desktop.utils.disableSentry
import com.prof18.feedflow.desktop.utils.initSentry
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.SettingsViewModel
import com.prof18.feedflow.shared.presentation.model.SettingsState
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.utils.UserFeedbackReporter
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.net.URI
import kotlin.math.roundToInt

@Suppress("ViewModelForwarding")
@Composable
internal fun FrameWindowScope.MainWindow(
    windowState: WindowState,
    isDarkTheme: Boolean,
    appConfig: DesktopConfig,
    settingsViewModel: SettingsViewModel,
    settingsState: SettingsState,
    showBackupLoader: Boolean,
) {
    val settingsRepository = DI.koin.get<SettingsRepository>()
    val userFeedbackReporter = DI.koin.get<UserFeedbackReporter>()
    val feedSyncRepo = DI.koin.get<FeedSyncRepository>()
    val snackbarHostState = remember { SnackbarHostState() }
    val messageQueue = DI.koin.get<FeedSyncMessageQueue>()

    MainWindowEffects(
        composeWindow = window,
        feedSyncRepo = feedSyncRepo,
        windowState = windowState,
        settingsRepository = settingsRepository,
        messageQueue = messageQueue,
        snackbarHostState = snackbarHostState,
    )

    if (showBackupLoader) {
        BackupInProgressScreen(isDarkTheme = isDarkTheme)
    } else {
        MainWindowContent(
            snackbarHostState = snackbarHostState,
            isDarkTheme = isDarkTheme,
            appConfig = appConfig,
            settingsViewModel = settingsViewModel,
            settingsState = settingsState,
            userFeedbackReporter = userFeedbackReporter,
        )
    }
}

@Composable
private fun BackupInProgressScreen(
    isDarkTheme: Boolean,
) {
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
}

@Composable
private fun MainWindowEffects(
    composeWindow: ComposeWindow,
    feedSyncRepo: FeedSyncRepository,
    windowState: WindowState,
    settingsRepository: SettingsRepository,
    messageQueue: FeedSyncMessageQueue,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    val flowStrings = LocalFeedFlowStrings.current

    val listener = remember(feedSyncRepo) {
        object : WindowFocusListener {
            override fun windowGainedFocus(e: WindowEvent) {
                // Do nothing
            }

            override fun windowLostFocus(e: WindowEvent) {
                try {
                    scope.launch {
                        feedSyncRepo.performBackup()
                    }
                } catch (_: Exception) {
                    // Ignore ForgottenCoroutineScopeException when window is closing
                }
            }
        }
    }

    LaunchedEffect(composeWindow.rootPane) {
        if (getDesktopOS().isMacOs()) {
            with(composeWindow.rootPane) {
                putClientProperty("apple.awt.transparentTitleBar", true)
                putClientProperty("apple.awt.fullWindowContent", true)
            }
        }
    }

    DisposableEffect(listener) {
        composeWindow.addWindowFocusListener(listener)
        onDispose {
            composeWindow.removeWindowFocusListener(listener)
        }
    }

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

    LaunchedEffect(messageQueue) {
        messageQueue.messageQueue.collect { message ->
            if (message is SyncResult.Error) {
                snackbarHostState.showSnackbar(
                    message = flowStrings.errorAccountSync(message.errorCode.code),
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        DesktopDatabaseErrorState.errorState.collect { show ->
            if (show) {
                snackbarHostState.showSnackbar(
                    message = flowStrings.databaseErrorReset,
                )
                DesktopDatabaseErrorState.setError(false)
            }
        }
    }
}

@Composable
private fun FrameWindowScope.MainWindowContent(
    snackbarHostState: SnackbarHostState,
    isDarkTheme: Boolean,
    appConfig: DesktopConfig,
    settingsViewModel: SettingsViewModel,
    settingsState: SettingsState,
    userFeedbackReporter: UserFeedbackReporter,
) {
    val homeViewModel = desktopViewModel { DI.koin.get<HomeViewModel>() }

    CompositionLocalProvider(LocalScrollbarStyle provides scrollbarStyle()) {
        val scope = rememberCoroutineScope()
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

        MarkAllReadDialog(
            visible = showMarkAllReadDialog,
            onDismiss = { showMarkAllReadDialog = false },
            onConfirm = {
                homeViewModel.markAllRead()
                showMarkAllReadDialog = false
            },
        )

        ClearOldArticlesDialog(
            visible = showClearOldArticlesDialog,
            onDismiss = { showClearOldArticlesDialog = false },
            onConfirm = {
                homeViewModel.deleteOldFeedItems()
                showClearOldArticlesDialog = false
            },
        )

        PrefetchWarningDialog(
            visible = showPrefetchWarningDialog,
            onDismiss = { showPrefetchWarningDialog = false },
            onConfirm = {
                settingsViewModel.updatePrefetchArticleContent(true)
                showPrefetchWarningDialog = false
            },
        )

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
                    frameWindowScope = this@MainWindowContent,
                    appEnvironment = appConfig.appEnvironment,
                    version = appConfig.version,
                    homeViewModel = homeViewModel,
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

@Composable
private fun MarkAllReadDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(LocalFeedFlowStrings.current.markAllReadButton) },
            text = { Text(LocalFeedFlowStrings.current.markAllReadDialogMessage) },
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
private fun ClearOldArticlesDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(LocalFeedFlowStrings.current.clearOldArticlesButton) },
            text = { Text(LocalFeedFlowStrings.current.clearOldArticlesDialogMessage) },
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
