package com.prof18.feedflow.desktop

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.FrameWindowScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.desktop.accounts.AccountsScreen
import com.prof18.feedflow.desktop.home.HomeScreen
import com.prof18.feedflow.desktop.importexport.ImportExportScreen
import com.prof18.feedflow.desktop.reaadermode.ReaderModeScreen
import com.prof18.feedflow.desktop.search.SearchScreen
import com.prof18.feedflow.desktop.utils.calculateWindowSizeClass
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
        val snackbarHostState = remember { SnackbarHostState() }

        val navigator = LocalNavigator.currentOrThrow

        val windowSize = calculateWindowSizeClass(frameWindowScope.window)

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
                },
                onSearchClick = {
                    navigator.push(SearchScreen(searchViewModel))
                },
                navigateToReaderMode = { feedItemUrlInfo ->
                    navigator.push(ReaderModeScreen(feedItemUrlInfo))
                },
                onAccountsClick = {
                    navigator.push(AccountsScreen())
                },
            )
        }
    }
}
