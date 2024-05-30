package com.prof18.feedflow.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.FrameWindowScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.desktop.about.AboutContent
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.feedsourcelist.FeedSourceListScreen
import com.prof18.feedflow.desktop.home.FeedFlowMenuBar
import com.prof18.feedflow.desktop.home.HomeScreen
import com.prof18.feedflow.desktop.importexport.ImportExportScreen
import com.prof18.feedflow.desktop.reaadermode.ReaderModeScreen
import com.prof18.feedflow.desktop.search.SearchScreen
import com.prof18.feedflow.desktop.ui.components.NewVersionBanner
import com.prof18.feedflow.desktop.utils.calculateWindowSizeClass
import com.prof18.feedflow.desktop.versionchecker.NewVersionChecker
import com.prof18.feedflow.desktop.versionchecker.NewVersionState
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.presentation.SettingsViewModel
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.utils.UserFeedbackReporter
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

internal data class MainScreen(
    private val homeViewModel: HomeViewModel,
    private val searchViewModel: SearchViewModel,
    private val frameWindowScope: FrameWindowScope,
    private val version: String?,
    private val appEnvironment: AppEnvironment,
) : Screen {
    @Composable
    override fun Content() {
        val listState = rememberLazyListState()

        val settingsViewModel = desktopViewModel { DI.koin.get<SettingsViewModel>() }
        val settingsState by settingsViewModel.settingsState.collectAsState()

        val newVersionChecker = DI.koin.get<NewVersionChecker>()
        val newVersionState by newVersionChecker.newVersionState.collectAsState()

        LaunchedEffect(Unit) {
            newVersionChecker.notifyIfNewVersionIsAvailable()
        }

        val snackbarHostState = remember { SnackbarHostState() }
        var aboutDialogState by remember { mutableStateOf(false) }
        DialogWindow(
            title = LocalFeedFlowStrings.current.appName,
            visible = aboutDialogState,
            onCloseRequest = {
                aboutDialogState = false
            },
        ) {
            AboutContent(
                versionLabel = LocalFeedFlowStrings.current.aboutAppVersion(version ?: "N/A"),
            )
        }

        val emailSubject = LocalFeedFlowStrings.current.issueContentTitle
        val emailContent = LocalFeedFlowStrings.current.issueContentTemplate

        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        val windowSize = calculateWindowSizeClass(frameWindowScope.window)

        frameWindowScope.FeedFlowMenuBar(
            showDebugMenu = appEnvironment.isDebug(),
            isMarkReadWhenScrollingEnabled = settingsState.isMarkReadWhenScrollingEnabled,
            isShowReadItemEnabled = settingsState.isShowReadItemsEnabled,
            isReaderModeEnabled = settingsState.isReaderModeEnabled,
            onRefreshClick = {
                scope.launch {
                    listState.animateScrollToItem(0)
                    homeViewModel.getNewFeeds()
                }
            },
            onMarkAllReadClick = {
                homeViewModel.markAllRead()
            },
            onImportExportClick = {
                navigator.push(ImportExportScreen(frameWindowScope.window))
            },
            onFeedsListClick = {
                navigator.push(FeedSourceListScreen())
            },
            onClearOldFeedClick = {
                homeViewModel.deleteOldFeedItems()
            },
            onAboutClick = {
                aboutDialogState = true
            },
            onBugReportClick = {
                val desktop = Desktop.getDesktop()
                val uri = URI.create(
                    UserFeedbackReporter.getEmailUrl(
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
            deleteFeeds = {
                homeViewModel.deleteAllFeeds()
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
        )

        Column {
            if (newVersionState is NewVersionState.NewVersion) {
                NewVersionBanner(
                    window = frameWindowScope.window,
                    onDownloadLinkClick = {
                        openInBrowser((newVersionState as NewVersionState.NewVersion).downloadLink)
                    },
                    onCloseClick = {
                        newVersionChecker.clearNewVersionState()
                    },
                )
            }

            @Suppress("ViewModelForwarding")
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { paddingValues ->
                HomeScreen(
                    windowSizeClass = windowSize,
                    paddingValues = paddingValues,
                    homeViewModel = homeViewModel,
                    snackbarHostState = snackbarHostState,
                    listState = listState,
                    onImportExportClick = {
                        navigator.push(ImportExportScreen(frameWindowScope.window))
                        //                    navigation.push(ScreenType.ImportExport)
                    },
                    onSearchClick = {
                        navigator.push(SearchScreen(searchViewModel))
                    },
                    navigateToReaderMode = { feedItemUrlInfo ->
                        navigator.push(ReaderModeScreen(feedItemUrlInfo))
                    },
                )
            }
        }
    }
}
