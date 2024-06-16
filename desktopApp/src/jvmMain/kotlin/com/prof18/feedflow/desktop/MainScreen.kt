package com.prof18.feedflow.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.FrameWindowScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.desktop.di.DI
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

internal data class MainScreen(
    private val homeViewModel: HomeViewModel,
    private val searchViewModel: SearchViewModel,
    private val frameWindowScope: FrameWindowScope,
    private val version: String?,
    private val appEnvironment: AppEnvironment,
    private val listState: LazyListState,
) : Screen {
    @Composable
    override fun Content() {
        val newVersionChecker = DI.koin.get<NewVersionChecker>()
        val newVersionState by newVersionChecker.newVersionState.collectAsState()

        LaunchedEffect(Unit) {
            newVersionChecker.notifyIfNewVersionIsAvailable()
        }

        val snackbarHostState = remember { SnackbarHostState() }

        val navigator = LocalNavigator.currentOrThrow

        val windowSize = calculateWindowSizeClass(frameWindowScope.window)

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
