@file:OptIn(ExperimentalMaterial3Api::class)

package com.prof18.feedflow

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.lifecycle.LifecycleController
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.prof18.feedflow.di.initKoinDesktop
import com.prof18.feedflow.domain.feedmanager.FeedManagerRepository
import com.prof18.feedflow.domain.opml.OPMLImporter
import com.prof18.feedflow.domain.opml.OPMLInput
import com.prof18.feedflow.feedlist.FeedListScreen
import com.prof18.feedflow.home.FeedFlowMenuBar
import com.prof18.feedflow.home.HomeScreen
import com.prof18.feedflow.navigation.ChildStack
import com.prof18.feedflow.navigation.ProvideComponentContext
import com.prof18.feedflow.navigation.Screen
import com.prof18.feedflow.presentation.HomeViewModel
import com.prof18.feedflow.presentation.SettingsViewModel
import com.prof18.feedflow.ui.style.FeedFlowTheme
import com.prof18.feedflow.ui.style.rememberDesktopDarkTheme
import kotlinx.coroutines.launch
import javax.swing.UIManager

val koin = initKoinDesktop().koin

@OptIn(ExperimentalDecomposeApi::class)
fun main() = application {

    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

    val lifecycle = LifecycleRegistry()
    val rootComponentContext = DefaultComponentContext(lifecycle = lifecycle)

    val windowState = rememberWindowState()

    LifecycleController(lifecycle, windowState)

    val settingsViewModel = koin.get<SettingsViewModel>()
    val homeViewModel = koin.get<HomeViewModel>()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "FeedFlow"
    ) {

        val navigation: StackNavigation<Screen> = remember { StackNavigation() }

        val isImportDone by settingsViewModel.isImportDoneState.collectAsState()
        if (isImportDone) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Import Done",
                    duration = SnackbarDuration.Short,
                )
            }
        }

        FeedFlowMenuBar(
            onRefreshClick = {
                // TODO
                scope.launch {
                    listState.animateScrollToItem(0)
                    homeViewModel.getNewFeeds()
                }
            },
            onMarkAllReadClick = {
                // TODO
            },
            onImportOPMLCLick = { file ->
                settingsViewModel.importFeed(OPMLInput(file))
            },
            onFeedsListClick = {
                navigation.push(Screen.FeedList)
            },
            onAddFeedClick = {
                // TODO
            }
        )

        Surface(modifier = Modifier.fillMaxSize()) {
            FeedFlowTheme {
                CompositionLocalProvider(LocalScrollbarStyle provides scrollbarStyle()) {
                    ProvideComponentContext(rootComponentContext) {
                        Scaffold(
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                        ) { paddingValues ->
                            ChildStack(
                                source = navigation,
                                initialStack = { listOf(Screen.Home) },
                                handleBackButton = true,
                                animation = stackAnimation(fade() + scale()),
                            ) { screen ->
                                when (screen) {
                                    is Screen.Home -> HomeScreen(
                                        paddingValues = paddingValues,
                                        homeViewModel = homeViewModel,
                                        listState = listState,
                                        onAddFeedClick = {
                                            // TODO
                                        }
                                    )
                                    is Screen.FeedList -> FeedListScreen(navigateBack = { navigation.pop() })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun scrollbarStyle(): ScrollbarStyle {
    val isInDarkTheme = rememberDesktopDarkTheme()
    return ScrollbarStyle(
        minimalHeight = 16.dp,
        thickness = 8.dp,
        shape = RoundedCornerShape(4.dp),
        hoverDurationMillis = 300,
        unhoverColor = if (isInDarkTheme) {
            MaterialTheme.colors.surface.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
        },
        hoverColor = if (isInDarkTheme) {
            MaterialTheme.colors.surface.copy(alpha = 0.50f)
        } else {
            MaterialTheme.colors.onSurface.copy(alpha = 0.50f)
        }
    )
}
