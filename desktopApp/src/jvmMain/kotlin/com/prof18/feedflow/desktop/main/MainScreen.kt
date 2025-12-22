package com.prof18.feedflow.desktop.main

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
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
import com.prof18.feedflow.desktop.utils.generateUniqueKey
import com.prof18.feedflow.shared.presentation.HomeViewModel

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
internal data class MainScreen(
    private val homeViewModel: HomeViewModel,
    private val frameWindowScope: FrameWindowScope,
    private val version: String?,
    private val appEnvironment: AppEnvironment,
    private val listState: LazyListState,
) : Screen {
    override val key: String = generateUniqueKey()

    @Composable
    override fun Content() {
        val snackbarHostState = remember { SnackbarHostState() }

        val navigator = LocalNavigator.currentOrThrow

        @Suppress("ViewModelForwarding")
        HomeScreen(
            homeViewModel = homeViewModel,
            snackbarHostState = snackbarHostState,
            listState = listState,
            onImportExportClick = {
                navigator.push(
                    ImportExportScreen(
                        composeWindow = frameWindowScope.window,
                        triggerFeedFetch = {
                            homeViewModel.getNewFeeds()
                        },
                    ),
                )
            },
            onSearchClick = {
                navigator.push(SearchScreen())
            },
            navigateToReaderMode = { feedItemUrlInfo ->
                navigator.push(ReaderModeScreen(feedItemUrlInfo))
            },
            onAccountsClick = {
                navigator.push(AccountsScreen())
            },
            onSettingsButtonClicked = {
                // There's not settings screen in desktop
            },
        )
    }
}
