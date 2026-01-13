package com.prof18.feedflow.desktop.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowState
import org.jetbrains.androidx.navigation3.NavBackStack
import org.jetbrains.androidx.navigation3.NavKey
import org.jetbrains.androidx.navigation3.entryProvider
import org.jetbrains.androidx.navigation3.rememberNavBackStack
import org.jetbrains.androidx.navigation3.ui.NavDisplay
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DesktopDatabaseErrorState
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.Accounts
import com.prof18.feedflow.desktop.AddFeed
import com.prof18.feedflow.desktop.BazquxSync
import com.prof18.feedflow.desktop.BlockedWords
import com.prof18.feedflow.desktop.DesktopConfig
import com.prof18.feedflow.desktop.DropboxSync
import com.prof18.feedflow.desktop.EditFeed
import com.prof18.feedflow.desktop.FeedSourceList
import com.prof18.feedflow.desktop.FeedSuggestions
import com.prof18.feedflow.desktop.FeedbinSync
import com.prof18.feedflow.desktop.FreshRssSync
import com.prof18.feedflow.desktop.GoogleDriveSync
import com.prof18.feedflow.desktop.Home
import com.prof18.feedflow.desktop.ICloudSync
import com.prof18.feedflow.desktop.ImportExport
import com.prof18.feedflow.desktop.MinifluxSync
import com.prof18.feedflow.desktop.ReaderMode
import com.prof18.feedflow.desktop.accounts.AccountsScreen
import com.prof18.feedflow.desktop.accounts.bazqux.BazquxSyncScreen
import com.prof18.feedflow.desktop.accounts.dropbox.DropboxSyncScreen
import com.prof18.feedflow.desktop.accounts.feedbin.FeedbinSyncScreen
import com.prof18.feedflow.desktop.accounts.freshrss.FreshRssSyncScreen
import com.prof18.feedflow.desktop.accounts.googledrive.GoogleDriveSyncScreen
import com.prof18.feedflow.desktop.accounts.icloud.ICloudSyncScreen
import com.prof18.feedflow.desktop.accounts.miniflux.MinifluxSyncScreen
import com.prof18.feedflow.desktop.addfeed.AddFeedFullScreen
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.editfeed.EditFeedScreen
import com.prof18.feedflow.desktop.feedsourcelist.FeedSourceListScreen
import com.prof18.feedflow.desktop.feedsuggestions.FeedSuggestionsScreen
import com.prof18.feedflow.desktop.home.HomeScreen
import com.prof18.feedflow.desktop.home.menubar.FeedFlowMenuBar
import com.prof18.feedflow.desktop.home.menubar.MenuBarActions
import com.prof18.feedflow.desktop.home.menubar.MenuBarState
import com.prof18.feedflow.desktop.importexport.ImportExportScreen
import com.prof18.feedflow.desktop.reaadermode.ReaderModeScreen
import com.prof18.feedflow.desktop.settings.blocked.BlockedWordsScreen
import com.prof18.feedflow.desktop.toEditFeed
import com.prof18.feedflow.desktop.toFeedSource
import com.prof18.feedflow.desktop.toReaderMode
import com.prof18.feedflow.desktop.ui.components.scrollbarStyle
import com.prof18.feedflow.desktop.utils.LocalComposeWindow
import com.prof18.feedflow.desktop.utils.desktopViewModel
import com.prof18.feedflow.shared.data.DesktopWindowSettingsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.presentation.FeedListSettingsViewModel
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
import com.prof18.feedflow.shared.ui.utils.scrollToItemConditionally
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import kotlin.math.roundToInt

@Suppress("ViewModelForwarding")
@Composable
internal fun FrameWindowScope.MainWindow(
    windowState: WindowState,
    isDarkTheme: Boolean,
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
        BackupInProgressScreen(isDarkTheme = isDarkTheme)
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

        val backStack = rememberNavBackStack<NavKey>(Home)

        CompositionLocalProvider(
            LocalScrollbarStyle provides scrollbarStyle(),
            LocalComposeWindow provides window,
        ) {
            Box {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    transitionSpec = {
                        if (reduceMotionEnabled) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith
                                (fadeOut() + scaleOut(targetScale = 0.9f))
                        }
                    },
                    popTransitionSpec = {
                        if (reduceMotionEnabled) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            val rightEdgeOrigin = TransformOrigin(pivotFractionX = 1f, pivotFractionY = 0.5f)
                            EnterTransition.None togetherWith scaleOut(
                                targetScale = 0.9f,
                                transformOrigin = rightEdgeOrigin,
                            )
                        }
                    },
                    entryProvider = entryProvider {
                        entry<Home> {
                            HomeScreen(
                                homeViewModel = homeViewModel,
                                snackbarHostState = snackbarHostState,
                                listState = listState,
                                onImportExportClick = { backStack.add(ImportExport) },
                                onSearchClick = { backStack.add(Search) },
                                onAccountsClick = { backStack.add(Accounts) },
                                onSettingsButtonClicked = { backStack.add(FeedSourceList) },
                                navigateToReaderMode = { feedItemUrlInfo ->
                                    backStack.add(feedItemUrlInfo.toReaderMode())
                                },
                                onAddFeedClick = { backStack.add(AddFeed) },
                                onEditFeedClick = { feedSource ->
                                    backStack.add(feedSource.toEditFeed())
                                },
                                onFeedSuggestionsClick = { backStack.add(FeedSuggestions) },
                            )
                        }

                        entry<Search> {
                            com.prof18.feedflow.desktop.search.SearchScreen(
                                navigateBack = { backStack.removeLastOrNull() },
                                navigateToReaderMode = { urlInfo ->
                                    backStack.add(urlInfo.toReaderMode())
                                },
                                navigateToEditFeed = { feedSource ->
                                    backStack.add(feedSource.toEditFeed())
                                },
                            )
                        }

                        entry<ReaderMode> { route ->
                            val feedItemUrlInfo = FeedItemUrlInfo(
                                id = route.id,
                                url = route.url,
                                title = route.title,
                            )
                            ReaderModeScreen(
                                feedItemUrlInfo = feedItemUrlInfo,
                                navigateBack = { backStack.removeLastOrNull() },
                            )
                        }

                        entry<Accounts> {
                            AccountsScreen(
                                navigateBack = { backStack.removeLastOrNull() },
                                navigateToDropboxSync = { backStack.add(DropboxSync) },
                                navigateToGoogleDriveSync = { backStack.add(GoogleDriveSync) },
                                navigateToICloudSync = { backStack.add(ICloudSync) },
                                navigateToFreshRssSync = { backStack.add(FreshRssSync) },
                                navigateToMinifluxSync = { backStack.add(MinifluxSync) },
                                navigateToBazquxSync = { backStack.add(BazquxSync) },
                                navigateToFeedbinSync = { backStack.add(FeedbinSync) },
                            )
                        }

                        entry<FeedSuggestions> {
                            FeedSuggestionsScreen(
                                navigateBack = { backStack.removeLastOrNull() },
                            )
                        }

                        entry<AddFeed> {
                            AddFeedFullScreen(
                                onFeedAdded = {
                                    homeViewModel.getNewFeeds()
                                    backStack.removeLastOrNull()
                                },
                                navigateBack = { backStack.removeLastOrNull() },
                            )
                        }

                        entry<ImportExport> {
                            val composeWindow = LocalComposeWindow.current
                            ImportExportScreen(
                                composeWindow = composeWindow,
                                triggerFeedFetch = { homeViewModel.getNewFeeds() },
                                navigateBack = { backStack.removeLastOrNull() },
                            )
                        }

                        entry<EditFeed> { route ->
                            val feedSource = route.toFeedSource()
                            EditFeedScreen(
                                feedSource = feedSource,
                                navigateBack = { backStack.removeLastOrNull() },
                            )
                        }

                        entry<DropboxSync> {
                            DropboxSyncScreen(navigateBack = { backStack.removeLastOrNull() })
                        }
                        entry<GoogleDriveSync> {
                            GoogleDriveSyncScreen(navigateBack = { backStack.removeLastOrNull() })
                        }
                        entry<ICloudSync> {
                            ICloudSyncScreen(navigateBack = { backStack.removeLastOrNull() })
                        }
                        entry<FreshRssSync> {
                            FreshRssSyncScreen(navigateBack = { backStack.removeLastOrNull() })
                        }
                        entry<MinifluxSync> {
                            MinifluxSyncScreen(navigateBack = { backStack.removeLastOrNull() })
                        }
                        entry<BazquxSync> {
                            BazquxSyncScreen(navigateBack = { backStack.removeLastOrNull() })
                        }
                        entry<FeedbinSync> {
                            FeedbinSyncScreen(navigateBack = { backStack.removeLastOrNull() })
                        }

                        entry<BlockedWords> {
                            BlockedWordsScreen(navigateBack = { backStack.removeLastOrNull() })
                        }

                        entry<FeedSourceList> {
                            FeedSourceListScreen(
                                onAddFeedClick = { backStack.add(AddFeed) },
                                navigateBack = { backStack.removeLastOrNull() },
                                onEditFeedClick = { feedSource ->
                                    backStack.add(feedSource.toEditFeed())
                                },
                            )
                        }
                    },
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    SnackbarHost(snackbarHostState)
                }
            }

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
                        backStack.add(ImportExport)
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
                backStack = backStack,
            )
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
