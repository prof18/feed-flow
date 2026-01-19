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
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScaleTransition
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.utils.DesktopDatabaseErrorState
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.DesktopConfig
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.home.menubar.FeedFlowMenuBar
import com.prof18.feedflow.desktop.home.menubar.MenuBarActions
import com.prof18.feedflow.desktop.home.menubar.MenuBarState
import com.prof18.feedflow.desktop.importexport.ImportExportScreen
import com.prof18.feedflow.desktop.ui.components.scrollbarStyle
import com.prof18.feedflow.shared.data.DesktopWindowSettingsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.presentation.FeedListSettingsViewModel
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
import com.prof18.feedflow.shared.ui.utils.scrollToItemConditionally
import kotlinx.coroutines.launch
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import kotlin.math.roundToInt

@Suppress("ViewModelForwarding")
@Composable
internal fun FrameWindowScope.MainWindow(
    windowState: WindowState,
    isDarkTheme: Boolean,
    useOledTheme: Boolean,
    appConfig: DesktopConfig,
    showBackupLoader: Boolean,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    MainWindowEffects(
        composeWindow = window,
        windowState = windowState,
        snackbarHostState = snackbarHostState,
    )

    if (showBackupLoader) {
        BackupInProgressScreen(
            isDarkTheme = isDarkTheme,
            useOledTheme = useOledTheme,
        )
    } else {
        MainWindowContent(
            snackbarHostState = snackbarHostState,
            isDarkTheme = isDarkTheme,
            appConfig = appConfig,
        )
    }
}

@Composable
private fun BackupInProgressScreen(
    isDarkTheme: Boolean,
    useOledTheme: Boolean,
) {
    FeedFlowTheme(
        darkTheme = isDarkTheme,
        useOledTheme = useOledTheme,
    ) {
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
    windowState: WindowState,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    val flowStrings = LocalFeedFlowStrings.current
    val desktopWindowSettingsRepository = DI.koin.get<DesktopWindowSettingsRepository>()
    val feedSyncRepo = DI.koin.get<FeedSyncRepository>()
    val messageQueue = DI.koin.get<FeedSyncMessageQueue>()

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
                desktopWindowSettingsRepository.setDesktopWindowWidthDp(size.width.value.roundToInt())
                desktopWindowSettingsRepository.setDesktopWindowHeightDp(size.height.value.roundToInt())
            }
    }

    LaunchedEffect(windowState) {
        snapshotFlow { windowState.position }
            .collect { position ->
                desktopWindowSettingsRepository.setDesktopWindowXPositionDp(position.x.value)
                desktopWindowSettingsRepository.setDesktopWindowYPositionDp(position.y.value)
            }
    }

    LaunchedEffect(messageQueue) {
        messageQueue.messageQueue.collect { message ->
            if (message is SyncResult.GoogleDriveNeedReAuth) {
                snackbarHostState.showSnackbar(
                    message = flowStrings.googleDriveAuthRetry,
                )
            } else if (message is SyncResult.Error) {
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
) {
    val homeViewModel = desktopViewModel { DI.koin.get<HomeViewModel>() }
    val feedListSettingsViewModel = desktopViewModel { DI.koin.get<FeedListSettingsViewModel>() }

    LaunchedEffect(Unit) {
        homeViewModel.onAppLaunch()
    }

    CompositionLocalProvider(LocalScrollbarStyle provides scrollbarStyle()) {
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()
        val reduceMotionEnabled = LocalReduceMotion.current

        var aboutDialogVisibility by remember { mutableStateOf(false) }
        var showMarkAllReadDialog by remember { mutableStateOf(false) }
        var showClearOldArticlesDialog by remember { mutableStateOf(false) }

        AboutDialog(
            visible = aboutDialogVisibility,
            onCloseRequest = { aboutDialogVisibility = false },
            version = appConfig.version ?: "N/A",
            isDarkTheme = isDarkTheme,
        )

        var feedListFontDialogState by remember { mutableStateOf(false) }
        val fontSizesState by feedListSettingsViewModel.feedFontSizeState.collectAsState()
        val feedListSettingsState by feedListSettingsViewModel.state.collectAsState()

        FeedListAppearanceDialog(
            visible = feedListFontDialogState,
            onCloseRequest = { feedListFontDialogState = false },
            fontSizesState = fontSizesState,
            settingsState = feedListSettingsState,
            callbacks = FeedListAppearanceCallbacks(
                onFontScaleUpdate = { fontScale ->
                    feedListSettingsViewModel.updateFontScale(fontScale)
                },
                onFeedLayoutUpdate = { feedLayout ->
                    feedListSettingsViewModel.updateFeedLayout(feedLayout)
                },
                onHideDescriptionUpdate = { enabled ->
                    feedListSettingsViewModel.updateHideDescription(enabled)
                },
                onHideImagesUpdate = { enabled ->
                    feedListSettingsViewModel.updateHideImages(enabled)
                },
                onHideDateUpdate = { enabled ->
                    feedListSettingsViewModel.updateHideDate(enabled)
                },
                onRemoveTitleFromDescUpdate = { enabled ->
                    feedListSettingsViewModel.updateRemoveTitleFromDescription(enabled)
                },
                onDateFormatUpdate = { format ->
                    feedListSettingsViewModel.updateDateFormat(format)
                },
                onTimeFormatUpdate = { format ->
                    feedListSettingsViewModel.updateTimeFormat(format)
                },
                onSwipeActionUpdate = { direction, action ->
                    feedListSettingsViewModel.updateSwipeAction(direction, action)
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

        val currentFeedFilter by homeViewModel.currentFeedFilter.collectAsState()
        val isSyncUploadRequired by homeViewModel.isSyncUploadRequired.collectAsState()

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
                HomeScreenContainer(
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
                        isSyncUploadRequired = isSyncUploadRequired,
                    ),
                    actions = MenuBarActions(
                        onRefreshClick = {
                            scope.launch {
                                listState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotionEnabled)
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
                        onForceRefreshClick = {
                            scope.launch {
                                listState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotionEnabled)
                                homeViewModel.forceFeedRefresh()
                            }
                        },
                        onFeedFontScaleClick = {
                            feedListFontDialogState = true
                        },
                        deleteFeeds = {
                            homeViewModel.deleteAllFeeds()
                        },
                        onBackupClick = {
                            homeViewModel.enqueueBackup()
                        },
                    ),
                )

                if (reduceMotionEnabled) {
                    CurrentScreen()
                } else {
                    ScaleTransition(navigator)
                }
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
