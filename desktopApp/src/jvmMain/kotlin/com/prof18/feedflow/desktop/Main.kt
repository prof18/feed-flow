package com.prof18.feedflow.desktop

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
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
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.desktop.about.AboutContent
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.feedsourcelist.FeedSourceListScreen
import com.prof18.feedflow.desktop.home.FeedFlowMenuBar
import com.prof18.feedflow.desktop.home.HomeScreen
import com.prof18.feedflow.desktop.importexport.ImportExportScreen
import com.prof18.feedflow.desktop.navigation.ChildStack
import com.prof18.feedflow.desktop.navigation.ProvideComponentContext
import com.prof18.feedflow.desktop.navigation.Screen
import com.prof18.feedflow.desktop.search.SearchScreen
import com.prof18.feedflow.desktop.ui.components.NewVersionBanner
import com.prof18.feedflow.desktop.utils.initSentry
import com.prof18.feedflow.desktop.versionchecker.NewVersionChecker
import com.prof18.feedflow.desktop.versionchecker.NewVersionState
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.SettingsViewModel
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.theme.rememberDesktopDarkTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.utils.UserFeedbackReporter
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.File
import java.io.InputStream
import java.net.URI
import java.util.Properties
import javax.swing.UIManager

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

    val isSandboxed = System.getenv("APP_SANDBOX_CONTAINER_ID") != null
    if (isSandboxed) {
        val resources = File(System.getProperty("compose.application.resources.dir"))

        // jna
        System.setProperty("jna.nounpack", "true")
        System.setProperty("jna.boot.library.path", resources.absolutePath)

        // sqlite-jdbc
        System.setProperty("org.sqlite.lib.path", resources.absolutePath)
        System.setProperty("org.sqlite.lib.name", "libsqlitejdbc.dylib")
    }

    DI.initKoin(
        appEnvironment = appEnvironment,
    )

    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

    val lifecycle = LifecycleRegistry()
    val rootComponentContext = DefaultComponentContext(lifecycle = lifecycle)

    val windowState = rememberWindowState()

    LifecycleController(lifecycle, windowState)

    val homeViewModel = desktopViewModel { DI.koin.get<HomeViewModel>() }
    val settingsViewModel = desktopViewModel { DI.koin.get<SettingsViewModel>() }
    val newVersionChecker = DI.koin.get<NewVersionChecker>()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    val newVersionState by newVersionChecker.newVersionState.collectAsState()

    scope.launch {
        newVersionChecker.notifyIfNewVersionIsAvailable()
    }

    val koin = DI.koin
    setSingletonImageLoaderFactory { koin.get<ImageLoader>() }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "FeedFlow",
    ) {
        val navigation: StackNavigation<Screen> = remember { StackNavigation() }

        var aboutDialogState by remember { mutableStateOf(false) }
        val dialogTitle = LocalFeedFlowStrings.current.appName
        DialogWindow(
            title = dialogTitle,
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

        val settingsState by settingsViewModel.settingsState.collectAsState()

        FeedFlowMenuBar(
            showDebugMenu = appEnvironment.isDebug(),
            isMarkReadWhenScrollingEnabled = settingsState.isMarkReadWhenScrollingEnabled,
            isShowReadItemEnabled = settingsState.isShowReadItemsEnabled,
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
                navigation.push(Screen.ImportExport)
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
        )

        MainContent(
            rootComponentContext = rootComponentContext,
            snackbarHostState = snackbarHostState,
            navigation = navigation,
            homeViewModel = homeViewModel,
            listState = listState,
            window = window,
            newVersionState = newVersionState,
            onCloseDownloadBannerClick = {
                newVersionChecker.clearNewVersionState()
            },
        )
    }
}

@Composable
private fun MainContent(
    rootComponentContext: DefaultComponentContext,
    snackbarHostState: SnackbarHostState,
    navigation: StackNavigation<Screen>,
    homeViewModel: HomeViewModel,
    listState: LazyListState,
    window: ComposeWindow,
    newVersionState: NewVersionState,
    onCloseDownloadBannerClick: () -> Unit,
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
                                is Screen.Home -> {
                                    Column {
                                        if (newVersionState is NewVersionState.NewVersion) {
                                            NewVersionBanner(
                                                window = window,
                                                onDownloadLinkClick = {
                                                    openInBrowser(newVersionState.downloadLink)
                                                },
                                                onCloseClick = onCloseDownloadBannerClick,
                                            )
                                        }

                                        @Suppress("ViewModelForwarding")
                                        HomeScreen(
                                            window = window,
                                            paddingValues = paddingValues,
                                            homeViewModel = homeViewModel,
                                            snackbarHostState = snackbarHostState,
                                            listState = listState,
                                            onAddFeedClick = {
                                                navigation.push(Screen.FeedList)
                                            },
                                            onImportExportClick = {
                                                navigation.push(Screen.ImportExport)
                                            },
                                            onSearchClick = {
                                                navigation.push(Screen.Search)
                                            }
                                        )
                                    }
                                }

                                is Screen.FeedList ->
                                    FeedSourceListScreen(
                                        navigateBack = {
                                            navigation.pop()
                                        },
                                    )

                                is Screen.ImportExport ->
                                    ImportExportScreen(
                                        composeWindow = window,
                                        navigateBack = {
                                            navigation.pop()
                                        },
                                    )

                                is Screen.Search ->
                                    SearchScreen(
                                        navigateBack = {
                                            navigation.pop()
                                        }
                                    )
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
