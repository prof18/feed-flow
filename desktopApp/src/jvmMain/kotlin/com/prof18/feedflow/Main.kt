@file:OptIn(ExperimentalMaterial3Api::class)

package com.prof18.feedflow

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
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
import com.prof18.feedflow.about.AboutContent
import com.prof18.feedflow.di.DI
import com.prof18.feedflow.domain.opml.OpmlInput
import com.prof18.feedflow.domain.opml.OpmlOutput
import com.prof18.feedflow.feedsourcelist.FeedSourceListScreen
import com.prof18.feedflow.home.FeedFlowMenuBar
import com.prof18.feedflow.home.HomeScreen
import com.prof18.feedflow.navigation.ChildStack
import com.prof18.feedflow.navigation.ProvideComponentContext
import com.prof18.feedflow.navigation.Screen
import com.prof18.feedflow.presentation.HomeViewModel
import com.prof18.feedflow.presentation.SettingsViewModel
import com.prof18.feedflow.ui.style.FeedFlowTheme
import com.prof18.feedflow.ui.style.rememberDesktopDarkTheme
import com.prof18.feedflow.utils.AppEnvironment
import com.prof18.feedflow.utils.UserFeedbackReporter
import com.prof18.feedflow.utils.initSentry
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.Properties
import javax.swing.UIManager

@Suppress("LongMethod")
@OptIn(ExperimentalDecomposeApi::class)
fun main() = application {
    val properties = Properties()
    val propsFile = DI::class.java.classLoader?.getResourceAsStream("props.properties")
        ?: InputStream.nullInputStream()
    properties.load(propsFile)

    val sentryDns = properties["sentry_dns"]
        ?.toString()
    val version = properties["version"]
        ?.toString()

    val isRelease = properties["is_release"]
        ?.toString()
        ?.toBooleanStrictOrNull()
        ?: false

    val appEnvironment = if (isRelease) {
        AppEnvironment.Release
    } else {
        AppEnvironment.Debug
    }

    if (appEnvironment.isRelease() && sentryDns != null && version != null) {
        initSentry(
            dns = sentryDns,
            version = version,
        )
    }

    DI.initKoin(
        appEnvironment = appEnvironment,
    )

    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

    val lifecycle = LifecycleRegistry()
    val rootComponentContext = DefaultComponentContext(lifecycle = lifecycle)

    val windowState = rememberWindowState()

    LifecycleController(lifecycle, windowState)

    val settingsViewModel = desktopViewModel { DI.koin.get<SettingsViewModel>() }
    val homeViewModel = desktopViewModel { DI.koin.get<HomeViewModel>() }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "FeedFlow",
    ) {
        val navigation: StackNavigation<Screen> = remember { StackNavigation() }

        val isImportDone by settingsViewModel.isImportDoneState.collectAsState()
        val importDoneMessage = stringResource(resource = MR.strings.feeds_import_done_message)

        if (isImportDone) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    importDoneMessage,
                    duration = SnackbarDuration.Short,
                )
            }
        }

        val isExportDone by settingsViewModel.isExportDoneState.collectAsState()
        val exportDoneMessage = stringResource(resource = MR.strings.feeds_export_done_message)

        if (isExportDone) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    exportDoneMessage,
                    duration = SnackbarDuration.Short,
                )
            }
        }

        var aboutDialogState by remember { mutableStateOf(false) }
        val dialogTitle = stringResource(MR.strings.app_name)
        DialogWindow(
            title = dialogTitle,
            visible = aboutDialogState,
            onCloseRequest = {
                aboutDialogState = false
            },
        ) {
            AboutContent()
        }

        FeedFlowMenuBar(
            onRefreshClick = {
                scope.launch {
                    listState.animateScrollToItem(0)
                    homeViewModel.getNewFeeds()
                }
            },
            onMarkAllReadClick = {
                homeViewModel.markAllRead()
            },
            onImportOPMLCLick = { file ->
                settingsViewModel.importFeed(OpmlInput(file))
            },
            onExportOPMLClick = { file ->
                settingsViewModel.exportFeed(OpmlOutput(file))
            },
            onFeedsListClick = {
                navigation.push(Screen.FeedList)
            },
            onClearOldFeedClick = {
                homeViewModel.deleteOldFeedItems()
            },
            onAboutClick = {
                aboutDialogState = true
            },
            onBugReportClick = {
                openInBrowser(UserFeedbackReporter.getFeedbackUrl())
            },
            onForceRefreshClick = {
                scope.launch {
                    listState.animateScrollToItem(0)
                    homeViewModel.forceFeedRefresh()
                }
            },
        )

        MainContent(
            rootComponentContext = rootComponentContext,
            snackbarHostState = snackbarHostState,
            navigation = navigation,
            homeViewModel = homeViewModel,
            listState = listState,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    rootComponentContext: DefaultComponentContext,
    snackbarHostState: SnackbarHostState,
    navigation: StackNavigation<Screen>,
    homeViewModel: HomeViewModel,
    listState: LazyListState,
) {
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
                                    },
                                )

                                is Screen.FeedList -> FeedSourceListScreen(navigateBack = { navigation.pop() })
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
        },
    )
}
