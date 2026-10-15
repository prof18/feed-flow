package com.prof18.feedflow.desktop.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.utils.DesktopDatabaseErrorState
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.DesktopConfig
import com.prof18.feedflow.desktop.Home
import com.prof18.feedflow.desktop.ReaderMode
import com.prof18.feedflow.desktop.Search
import com.prof18.feedflow.desktop.addfeed.AddFeedScreen
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.editfeed.EditFeedScreen
import com.prof18.feedflow.desktop.feedsuggestions.FeedSuggestionsScreen
import com.prof18.feedflow.desktop.home.DesktopHomeListStateStore
import com.prof18.feedflow.desktop.home.DesktopWorkspaceCommand
import com.prof18.feedflow.desktop.home.HomeScreen
import com.prof18.feedflow.desktop.home.desktopWorkspaceAction
import com.prof18.feedflow.desktop.home.desktopWorkspaceCommandAvailability
import com.prof18.feedflow.desktop.home.menubar.FeedFlowMenuBar
import com.prof18.feedflow.desktop.home.menubar.MenuBarActions
import com.prof18.feedflow.desktop.home.menubar.MenuBarState
import com.prof18.feedflow.desktop.home.rememberDesktopHomeListStateStore
import com.prof18.feedflow.desktop.reaadermode.ReaderModeScreen
import com.prof18.feedflow.desktop.settings.DesktopSettingsCategory
import com.prof18.feedflow.desktop.settings.DesktopSettingsSection
import com.prof18.feedflow.desktop.settings.SettingsWindow
import com.prof18.feedflow.desktop.toReaderMode
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.LocalReduceMotion
import com.prof18.feedflow.desktop.ui.components.ConfirmationDialog
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.desktop.ui.components.SnackbarHost
import com.prof18.feedflow.desktop.ui.components.scrollbarStyle
import com.prof18.feedflow.shared.data.DesktopWindowSettingsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.presentation.HomeViewModel
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Text
import org.koin.compose.viewmodel.koinViewModel
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
    val snackbarHostState = remember { DesktopFeedbackState() }

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
        Column(
            modifier = Modifier.fillMaxSize().background(JewelTheme.globalColors.panelBackground),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Text(
                modifier = Modifier.padding(top = DesktopDesignTokens.spaceSection),
                text = LocalFeedFlowStrings.current.feedSyncInProgress,
            )
        }
    }
}

@Composable
private fun MainWindowEffects(
    composeWindow: ComposeWindow,
    windowState: WindowState,
    snackbarHostState: DesktopFeedbackState,
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
                putClientProperty("apple.awt.windowTitleVisible", false)
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
        snapshotFlow { windowState.placement }
            .collect { placement ->
                // The window reports Floating again while it is being disposed on exit,
                // so ignore placement changes that happen after it stopped showing.
                if (!composeWindow.isShowing) {
                    return@collect
                }
                desktopWindowSettingsRepository.setDesktopWindowPlacement(placement.name)
            }
    }

    LaunchedEffect(windowState) {
        snapshotFlow { windowState.placement to windowState.size }
            .collect { (placement, size) ->
                if (placement != WindowPlacement.Floating) {
                    return@collect
                }
                desktopWindowSettingsRepository.setDesktopWindowWidthDp(size.width.value.roundToInt())
                desktopWindowSettingsRepository.setDesktopWindowHeightDp(size.height.value.roundToInt())
            }
    }

    LaunchedEffect(windowState) {
        snapshotFlow { windowState.placement to windowState.position }
            .collect { (placement, position) ->
                if (placement != WindowPlacement.Floating) {
                    return@collect
                }
                desktopWindowSettingsRepository.setDesktopWindowXPositionDp(position.x.value)
                desktopWindowSettingsRepository.setDesktopWindowYPositionDp(position.y.value)
            }
    }

    LaunchedEffect(messageQueue) {
        messageQueue.userMessages.collect { message ->
            if (message is SyncResult.GoogleDriveNeedReAuth) {
                snackbarHostState.showFeedback(
                    message = flowStrings.googleDriveAuthRetry,
                )
            } else if (message is SyncResult.Error) {
                snackbarHostState.showFeedback(
                    message = flowStrings.errorAccountSync(message.errorCode.code),
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        DesktopDatabaseErrorState.errorState.collect { show ->
            if (show) {
                snackbarHostState.showFeedback(
                    message = flowStrings.databaseErrorReset,
                )
                DesktopDatabaseErrorState.setError(false)
            }
        }
    }
}

@Composable
private fun FrameWindowScope.MainWindowContent(
    snackbarHostState: DesktopFeedbackState,
    appConfig: DesktopConfig,
) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val homeListStateStore = rememberDesktopHomeListStateStore()

    LaunchedEffect(Unit) {
        homeViewModel.onAppLaunch()
    }

    val menuCommands = remember { DesktopMenuCommands() }
    CompositionLocalProvider(
        LocalScrollbarStyle provides scrollbarStyle(),
        LocalDesktopMenuCommands provides menuCommands,
    ) {
        val reduceMotionEnabled = LocalReduceMotion.current

        var showMarkAllReadDialog by remember { mutableStateOf(false) }
        val dialogWindowNavigator = rememberDesktopDialogWindowNavigator()

        MarkAllReadDialog(
            visible = showMarkAllReadDialog,
            onDismiss = { showMarkAllReadDialog = false },
            onConfirm = {
                desktopWorkspaceAction(
                    DesktopWorkspaceCommand.BulkAction,
                    homeViewModel::desktopWorkspaceCommandAvailability,
                    homeViewModel::markAllRead,
                )()
                showMarkAllReadDialog = false
            },
        )

        if (dialogWindowNavigator.isOpen(DesktopDialogWindowDestination.AddFeed)) {
            AddFeedScreen(
                onCloseRequest = { dialogWindowNavigator.close(DesktopDialogWindowDestination.AddFeed) },
                onFeedAdded = { homeViewModel.getNewFeeds() },
            )
        }

        if (dialogWindowNavigator.isOpen(DesktopDialogWindowDestination.FeedSuggestions)) {
            FeedSuggestionsScreen(
                onCloseRequest = { dialogWindowNavigator.close(DesktopDialogWindowDestination.FeedSuggestions) },
            )
        }

        var feedSourceToEdit: FeedSource? by remember { mutableStateOf(null) }

        if (dialogWindowNavigator.isOpen(DesktopDialogWindowDestination.EditFeed)) {
            EditFeedScreen(
                feedSource = feedSourceToEdit,
                onCloseRequest = {
                    dialogWindowNavigator.close(DesktopDialogWindowDestination.EditFeed)
                    feedSourceToEdit = null
                },
            )
        }

        val currentFeedFilter by homeViewModel.currentFeedFilter.collectAsState()
        val isSyncUploadRequired by homeViewModel.isSyncUploadRequired.collectAsState()
        val loadingState by homeViewModel.loadingState.collectAsState()
        val feedOperation by homeViewModel.feedOperationState.collectAsState()
        val commands = desktopWorkspaceCommandAvailability(loadingState, feedOperation)
        if (dialogWindowNavigator.isOpen(DesktopDialogWindowDestination.Settings)) {
            SettingsWindow(
                navigator = dialogWindowNavigator,
                onCloseRequest = { dialogWindowNavigator.close(DesktopDialogWindowDestination.Settings) },
                triggerFeedFetch = homeViewModel::getNewFeeds,
                onClearOldArticles = desktopWorkspaceAction(
                    DesktopWorkspaceCommand.BulkAction,
                    homeViewModel::desktopWorkspaceCommandAvailability,
                    homeViewModel::deleteOldFeedItems,
                ),
                canClearOldArticles = commands.bulkActionsEnabled,
            )
        }

        val backStack = remember { mutableStateListOf<NavKey>(Home) }
        val navigateBack: () -> Unit = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        }
        val shouldShowWindowSnackbarHost = backStack.lastOrNull() != Home

        CompositionLocalProvider(
            LocalDesktopDialogWindowNavigator provides dialogWindowNavigator,
            LocalScrollbarStyle provides scrollbarStyle(),
        ) {
            val onSearch: (() -> Unit)? = if (backStack.lastOrNull() == Home) {
                { backStack.add(Search) }
            } else {
                null
            }
            MainWindowSurface(modifier = Modifier.fillMaxSize(), onSearch = onSearch) {
                Box {
                    NavDisplay(
                        backStack = backStack,
                        onBack = navigateBack,
                        transitionSpec = {
                            if (reduceMotionEnabled) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                                    fadeOut(animationSpec = tween(durationMillis = 150))
                            }
                        },
                        popTransitionSpec = {
                            if (reduceMotionEnabled) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                                    fadeOut(animationSpec = tween(durationMillis = 150))
                            }
                        },
                        entryProvider = entryProvider {
                            screens(
                                backStack = backStack,
                                navigateBack = navigateBack,
                                homeViewModel = homeViewModel,
                                homeListStateStore = homeListStateStore,
                                snackbarHostState = snackbarHostState,
                                dialogWindowNavigator = dialogWindowNavigator,
                                onAccountsRequested = {
                                    dialogWindowNavigator.openSettings(DesktopSettingsCategory.ACCOUNTS)
                                },
                                onEditFeedRequested = { feedSource ->
                                    feedSourceToEdit = feedSource
                                    dialogWindowNavigator.open(DesktopDialogWindowDestination.EditFeed)
                                },
                            )
                        },
                    )

                    if (shouldShowWindowSnackbarHost) {
                        SnackbarHost(
                            state = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }

            if (getDesktopOS().isMacOs()) {
                DisposableEffect(dialogWindowNavigator) {
                    installMacOsAppMenuHandlers(
                        onSettings = { dialogWindowNavigator.openSettings() },
                        onAbout = { dialogWindowNavigator.openSettings(DesktopSettingsCategory.ABOUT) },
                    )
                    onDispose { uninstallMacOsAppMenuHandlers() }
                }
            }

            FeedFlowMenuBar(
                state = MenuBarState(
                    showDebugMenu = appConfig.appEnvironment.isDebug(),
                    feedFilter = currentFeedFilter,
                    isSyncUploadRequired = isSyncUploadRequired,
                    commands = commands,
                ),
                actions = MenuBarActions(
                    onRefreshClick = desktopWorkspaceAction(
                        DesktopWorkspaceCommand.Refresh,
                        homeViewModel::desktopWorkspaceCommandAvailability,
                        homeViewModel::refreshFeeds,
                    ),
                    onAddFeedClick = {
                        dialogWindowNavigator.open(DesktopDialogWindowDestination.AddFeed)
                    },
                    onMarkAllReadClick = desktopWorkspaceAction(
                        DesktopWorkspaceCommand.BulkAction,
                        homeViewModel::desktopWorkspaceCommandAvailability,
                    ) {
                        showMarkAllReadDialog = true
                    },
                    onImportExportClick = {
                        dialogWindowNavigator.openSettings(
                            DesktopSettingsCategory.SYNC_STORAGE,
                            DesktopSettingsSection.IMPORT_EXPORT,
                        )
                    },
                    onSettingsClick = {
                        dialogWindowNavigator.openSettings()
                    },
                    onEditFeedClick = { source ->
                        feedSourceToEdit = source
                        dialogWindowNavigator.open(DesktopDialogWindowDestination.EditFeed)
                    },
                    deleteFeeds = {
                        homeViewModel.deleteAllFeeds()
                    },
                    onBackupClick = {
                        homeViewModel.enqueueBackup()
                    },
                    onExitClick = {
                        window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
                    },
                    onFeedSuggestionsClick = {
                        dialogWindowNavigator.open(DesktopDialogWindowDestination.FeedSuggestions)
                    },
                ),
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
        ConfirmationDialog(
            title = LocalFeedFlowStrings.current.markAllReadButton,
            message = LocalFeedFlowStrings.current.markAllReadDialogMessage,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

private fun EntryProviderScope<NavKey>.screens(
    backStack: MutableList<NavKey>,
    navigateBack: () -> Unit,
    homeViewModel: HomeViewModel,
    homeListStateStore: DesktopHomeListStateStore,
    snackbarHostState: DesktopFeedbackState,
    dialogWindowNavigator: DesktopDialogWindowNavigator,
    onAccountsRequested: () -> Unit,
    onEditFeedRequested: (FeedSource) -> Unit,
) {
    entry<Home> {
        HomeScreen(
            homeViewModel = homeViewModel,
            listStateStore = homeListStateStore,
            snackbarHostState = snackbarHostState,
            onImportExportClick = {
                dialogWindowNavigator.openSettings(
                    DesktopSettingsCategory.SYNC_STORAGE,
                    DesktopSettingsSection.IMPORT_EXPORT,
                )
            },
            onSearchClick = { backStack.add(Search) },
            onAccountsClick = onAccountsRequested,
            navigateToReaderMode = { feedItemUrlInfo ->
                backStack.add(feedItemUrlInfo.toReaderMode())
            },
            onAddFeedClick = { dialogWindowNavigator.open(DesktopDialogWindowDestination.AddFeed) },
            onEditFeedClick = onEditFeedRequested,
            onFeedSuggestionsClick = {
                dialogWindowNavigator.open(DesktopDialogWindowDestination.FeedSuggestions)
            },
        )
    }

    entry<Search> {
        MacOsTitleBarPadding {
            com.prof18.feedflow.desktop.search.SearchScreen(
                navigateBack = navigateBack,
                navigateToReaderMode = { urlInfo ->
                    backStack.add(urlInfo.toReaderMode())
                },
                navigateToEditFeed = onEditFeedRequested,
            )
        }
    }

    entry<ReaderMode> { route ->
        MacOsTitleBarPadding {
            val feedItemUrlInfo = FeedItemUrlInfo(
                id = route.id,
                url = route.url,
                title = route.title,
                isBookmarked = route.isBookmarked,
                articleOpenMode = ArticleOpenMode.valueOf(route.articleOpenMode),
                commentsUrl = route.commentsUrl,
                imageUrl = route.imageUrl,
                feedSourceTitle = route.feedSourceTitle,
            )
            ReaderModeScreen(
                feedItemUrlInfo = feedItemUrlInfo,
                navigateBack = navigateBack,
            )
        }
    }
}

@Composable
private fun MacOsTitleBarPadding(content: @Composable () -> Unit) {
    val topPadding = if (getDesktopOS().isMacOs()) DesktopDesignTokens.spacePanel else 0.dp
    Box(modifier = Modifier.padding(top = topPadding)) {
        content()
    }
}
