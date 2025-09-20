package com.prof18.feedflow.desktop

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScaleTransition
import co.touchlab.kermit.Logger
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.FlatPropertiesLaf
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.core.utils.isNotMacOs
import com.prof18.feedflow.desktop.about.AboutContent
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.home.FeedFlowMenuBar
import com.prof18.feedflow.desktop.importexport.ImportExportScreen
import com.prof18.feedflow.desktop.resources.Res
import com.prof18.feedflow.desktop.resources.icon
import com.prof18.feedflow.desktop.telemetry.TelemetryDeckClient
import com.prof18.feedflow.desktop.ui.components.scrollbarStyle
import com.prof18.feedflow.desktop.utils.disableSentry
import com.prof18.feedflow.desktop.utils.initSentry
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.DatabaseCloser
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.presentation.SettingsViewModel
import com.prof18.feedflow.shared.ui.settings.DateFormatSelector
import com.prof18.feedflow.shared.ui.settings.FeedLayoutSelector
import com.prof18.feedflow.shared.ui.settings.FeedListFontSettings
import com.prof18.feedflow.shared.ui.settings.HideDescriptionSwitch
import com.prof18.feedflow.shared.ui.settings.HideImagesSwitch
import com.prof18.feedflow.shared.ui.settings.RemoveTitleFromDescSwitch
import com.prof18.feedflow.shared.ui.settings.SwipeActionSelector
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.theme.rememberDesktopDarkTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.rememberFeedFlowStrings
import com.prof18.feedflow.shared.utils.UserFeedbackReporter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skiko.SkikoProperties.libraryPath
import java.awt.Desktop
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.io.File
import java.io.InputStream
import java.net.URI
import java.util.Properties
import javax.swing.UIManager
import kotlin.math.roundToInt

@Suppress("UnsafeDynamicallyLoadedCode", "CyclomaticComplexMethod")
fun main() {
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

    val dropboxKey = properties["dropbox_key"]
        ?.toString()

    val appEnvironment = if (isRelease) {
        AppEnvironment.Release
    } else {
        AppEnvironment.Debug
    }

    val isSandboxed = System.getenv("APP_SANDBOX_CONTAINER_ID") != null
    val resourcesPath = System.getProperty("compose.application.resources.dir")
    if (isSandboxed) {
        // jna
        System.setProperty("jna.nounpack", "true")
        System.setProperty("jna.boot.library.path", resourcesPath)

        // sqlite-jdbc
        System.setProperty("org.sqlite.lib.path", resourcesPath)
        System.setProperty("org.sqlite.lib.name", "libsqlitejdbc.dylib")
    }

    var isIcloudEnabled = false
    if (getDesktopOS().isMacOs()) {
        try {
            val resourcesDir = System.getProperty("compose.application.resources.dir")
            val libraryPath = resourcesDir + File.separator + System.mapLibraryName("ikloud")
            System.load(libraryPath)
            isIcloudEnabled = true
        } catch (_: UnsatisfiedLinkError) {
            System.err.println("Failed to load library. Path: $libraryPath")
            isIcloudEnabled = false
        }
    }

    DI.initKoin(
        appEnvironment = appEnvironment,
        isICloudEnabled = isIcloudEnabled,
        version = version ?: "",
        isDropboxEnabled = dropboxKey != null,
    )

    val isCrashReportEnabled = DI.koin.get<SettingsRepository>().getCrashReportingEnabled()

    if (appEnvironment.isRelease() && sentryDns != null && version != null && isCrashReportEnabled) {
        initSentry(
            dns = sentryDns,
            version = version,
        )
    }

    val telemetryClient = DI.koin.get<TelemetryDeckClient>()
    telemetryClient.signal("TelemetryDeck.Session.started")

    application {
        val settingsRepository = DI.koin.get<SettingsRepository>()
        val savedWidthDp = settingsRepository.getDesktopWindowWidthDp()
        val savedHeightDp = settingsRepository.getDesktopWindowHeightDp()
        val windowState = rememberWindowState(size = DpSize(savedWidthDp.dp, savedHeightDp.dp))

        val koin = DI.koin
        setSingletonImageLoaderFactory { koin.get<ImageLoader>() }

        val homeViewModel = desktopViewModel { DI.koin.get<HomeViewModel>() }
        val searchViewModel = desktopViewModel { DI.koin.get<SearchViewModel>() }

        val feedSyncRepo = DI.koin.get<FeedSyncRepository>()
        val messageQueue = DI.koin.get<FeedSyncMessageQueue>()

        val scope = rememberCoroutineScope()
        var showBackupLoader by remember { mutableStateOf(false) }

        val settingsViewModel = desktopViewModel { DI.koin.get<SettingsViewModel>() }
        val settingsState by settingsViewModel.settingsState.collectAsState()
        val isDarkTheme = when (settingsState.themeMode) {
            ThemeMode.SYSTEM -> rememberDesktopDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

        FeedFlowTheme(darkTheme = isDarkTheme) {
            LaunchedEffect(isDarkTheme, ) {
                setupLookAndFeel(isDarkTheme)
            }

            val lyricist = rememberFeedFlowStrings()
            val icon = painterResource(Res.drawable.icon)
            ProvideFeedFlowStrings(lyricist) {
                Window(
                    onCloseRequest = {
                        scope.launch {
                            showBackupLoader = true
                            try {
                                feedSyncRepo.performBackup()
                                DI.koin.get<DatabaseCloser>().close()
                            } catch (e: Exception) {
                                DI.koin.get<Logger>().e("Error during cleanup", e)
                            } finally {
                                exitApplication()
                            }
                        }
                    },
                    title = "",
                    state = windowState,
                    icon = icon,
                ) {
                    val listener = object : WindowFocusListener {
                        override fun windowGainedFocus(e: WindowEvent) {
                            // Do nothing
                        }

                        override fun windowLostFocus(e: WindowEvent) {
                            scope.launch {
                                feedSyncRepo.performBackup()
                            }
                        }
                    }

                    LaunchedEffect(window.rootPane) {
                        if (getDesktopOS().isMacOs()) {
                            with(window.rootPane) {
                                putClientProperty("apple.awt.transparentTitleBar", true)
                                putClientProperty("apple.awt.fullWindowContent", true)
                            }
                        }
                    }

                    DisposableEffect(Unit) {
                        window.addWindowFocusListener(listener)
                        onDispose {
                            window.removeWindowFocusListener(listener)
                        }
                    }

                    val snackbarHostState = remember { SnackbarHostState() }

                    LaunchedEffect(windowState) {
                        snapshotFlow { windowState.size }
                            .collect { size ->
                                settingsRepository.setDesktopWindowWidthDp(size.width.value.roundToInt())
                                settingsRepository.setDesktopWindowHeightDp(size.height.value.roundToInt())
                            }
                    }

                    val errorMessage = LocalFeedFlowStrings.current.errorAccountSync
                    LaunchedEffect(Unit) {
                        messageQueue.messageQueue.collect { message ->
                            if (message is SyncResult.Error) {
                                snackbarHostState.showSnackbar(
                                    message = errorMessage,
                                )
                            }
                        }
                    }

                    if (showBackupLoader) {
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
                    } else {
                        CompositionLocalProvider(LocalScrollbarStyle provides scrollbarStyle()) {
                            val emailSubject = LocalFeedFlowStrings.current.issueContentTitle
                            val emailContent = LocalFeedFlowStrings.current.issueContentTemplate

                            val listState = rememberLazyListState()

                            var aboutDialogVisibility by remember { mutableStateOf(false) }
                            val aboutDialogState = rememberDialogState(
                                size = DpSize(500.dp, 500.dp),
                            )
                            DialogWindow(
                                state = aboutDialogState,
                                title = LocalFeedFlowStrings.current.appName,
                                visible = aboutDialogVisibility,
                                onCloseRequest = {
                                    aboutDialogVisibility = false
                                },
                            ) {
                                AboutContent(
                                    versionLabel = LocalFeedFlowStrings.current.aboutAppVersion(version ?: "N/A"),
                                    isDarkTheme = isDarkTheme,
                                )
                            }

                            var feedListFontDialogState by remember { mutableStateOf(false) }
                            val fontSizesState by settingsViewModel.feedFontSizeState.collectAsState()

                            val dialogState = rememberDialogState(
                                size = DpSize(500.dp, 720.dp),
                            )
                            DialogWindow(
                                state = dialogState,
                                title = LocalFeedFlowStrings.current.feedListAppearance,
                                visible = feedListFontDialogState,
                                onCloseRequest = {
                                    feedListFontDialogState = false
                                },
                            ) {
                                Scaffold { paddingValues ->
                                    val scrollableState = rememberScrollState()
                                    Column(
                                        modifier = Modifier
                                            .verticalScroll(scrollableState),
                                    ) {
                                        FeedListFontSettings(
                                            fontSizes = fontSizesState,
                                            modifier = Modifier
                                                .padding(paddingValues),
                                            updateFontScale = { fontScale ->
                                                settingsViewModel.updateFontScale(fontScale)
                                            },
                                            isHideDescriptionEnabled = settingsState.isHideDescriptionEnabled,
                                            isHideImagesEnabled = settingsState.isHideImagesEnabled,
                                            dateFormat = settingsState.dateFormat,
                                            feedLayout = settingsState.feedLayout,
                                        )

                                        Spacer(modifier = Modifier.padding(top = Spacing.regular))

                                        FeedLayoutSelector(
                                            feedLayout = settingsState.feedLayout,
                                            onFeedLayoutSelected = { feedLayout ->
                                                settingsViewModel.updateFeedLayout(feedLayout)
                                            },
                                        )

                                        HideDescriptionSwitch(
                                            isHideDescriptionEnabled = settingsState.isHideDescriptionEnabled,
                                            setHideDescription = {
                                                settingsViewModel.updateHideDescription(
                                                    !settingsState.isHideDescriptionEnabled,
                                                )
                                            },
                                        )

                                        HideImagesSwitch(
                                            isHideImagesEnabled = settingsState.isHideImagesEnabled,
                                            setHideImages = {
                                                settingsViewModel.updateHideImages(!settingsState.isHideImagesEnabled)
                                            },
                                        )

                                        RemoveTitleFromDescSwitch(
                                            isRemoveTitleFromDescriptionEnabled =
                                            settingsState.isRemoveTitleFromDescriptionEnabled,
                                            setRemoveTitleFromDescription = {
                                                settingsViewModel.updateRemoveTitleFromDescription(
                                                    !settingsState.isRemoveTitleFromDescriptionEnabled,
                                                )
                                            },
                                        )

                                        DateFormatSelector(
                                            currentFormat = settingsState.dateFormat,
                                            onFormatSelected = { format ->
                                                settingsViewModel.updateDateFormat(format)
                                            },
                                        )

                                        SwipeActionSelector(
                                            direction = SwipeDirection.LEFT,
                                            currentAction = settingsState.leftSwipeActionType,
                                            onActionSelected = { action ->
                                                settingsViewModel.updateSwipeAction(SwipeDirection.LEFT, action)
                                            },
                                        )

                                        SwipeActionSelector(
                                            direction = SwipeDirection.RIGHT,
                                            currentAction = settingsState.rightSwipeActionType,
                                            onActionSelected = { action ->
                                                settingsViewModel.updateSwipeAction(SwipeDirection.RIGHT, action)
                                            },
                                        )
                                    }
                                }
                            }

                            val currentFeedFilter by homeViewModel.currentFeedFilter.collectAsState()

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
                                    MainScreen(
                                        frameWindowScope = this@Window,
                                        appEnvironment = appEnvironment,
                                        version = version,
                                        homeViewModel = homeViewModel,
                                        searchViewModel = searchViewModel,
                                        listState = listState,
                                    ),
                                ) { navigator ->
                                    FeedFlowMenuBar(
                                        showDebugMenu = appEnvironment.isDebug(),
                                        settingsState = settingsState,
                                        feedFilter = currentFeedFilter,
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
                                            homeViewModel.deleteOldFeedItems()
                                        },
                                        onAboutClick = {
                                            aboutDialogVisibility = true
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
                                        onFeedOrderSelected = { order ->
                                            settingsViewModel.updateFeedOrder(order)
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
                                        onFeedFontScaleClick = {
                                            feedListFontDialogState = true
                                        },
                                        onAutoDeletePeriodSelected = { period ->
                                            settingsViewModel.updateAutoDeletePeriod(period)
                                        },
                                        setCrashReportingEnabled = { enabled ->
                                            settingsViewModel.updateCrashReporting(enabled)
                                            if (enabled) {
                                                if (appEnvironment.isRelease() && sentryDns != null && version != null) {
                                                    initSentry(
                                                        dns = sentryDns,
                                                        version = version,
                                                    )
                                                }
                                            } else {
                                                disableSentry()
                                            }
                                        },
                                        onThemeModeSelected = { mode ->
                                            settingsViewModel.updateThemeMode(mode)
                                        },
                                    )

                                    ScaleTransition(navigator)
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
                }
            }
        }
    }
}

private fun setupLookAndFeel(isDarkMode: Boolean) {
    if (getDesktopOS().isNotMacOs()) {
        System.setProperty("flatlaf.useWindowDecorations", "true")
        System.setProperty("flatlaf.menuBarEmbedded", "false")

        try {
            val themeFileName = if (isDarkMode) {
                "feedflow-dark.properties"
            } else {
                "feedflow-light.properties"
            }

            // Load custom properties theme
            val themeStream = DI::class.java.classLoader?.getResourceAsStream(themeFileName)

            if (themeStream != null) {
                val customLaf = FlatPropertiesLaf(themeFileName, themeStream)
                UIManager.setLookAndFeel(customLaf)
            } else {
                // Fallback to standard themes if properties file not found
                val newLaf = if (isDarkMode) {
                    FlatDarkLaf()
                } else {
                    FlatLightLaf()
                }
                UIManager.setLookAndFeel(newLaf)
                UIManager.put("MenuBar.border", null)
            }

            FlatLaf.updateUI()
        } catch (_: Exception) {
            // Fallback to default theme
            val newLaf = if (isDarkMode) {
                FlatDarkLaf()
            } else {
                FlatLightLaf()
            }
            UIManager.setLookAndFeel(newLaf)
        }
    }
}

