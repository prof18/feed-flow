package com.prof18.feedflow.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.core.utils.AppDataPathBuilder
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DesktopOS
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.resources.Res
import com.prof18.feedflow.desktop.resources.icon
import com.prof18.feedflow.desktop.startup.AlreadyRunningWindow
import com.prof18.feedflow.desktop.startup.DatabaseStartupErrorWindow
import com.prof18.feedflow.desktop.startup.DesktopStartupCoordinator
import com.prof18.feedflow.desktop.startup.InstanceActivation
import com.prof18.feedflow.desktop.startup.SingleInstanceLock
import com.prof18.feedflow.desktop.startup.StartupState
import com.prof18.feedflow.desktop.startup.bringToFront
import com.prof18.feedflow.desktop.startup.handleStartupCrash
import com.prof18.feedflow.desktop.telemetry.TelemetryDeckClient
import com.prof18.feedflow.desktop.ui.DesktopWindowBackground
import com.prof18.feedflow.desktop.ui.components.restoreInitialLinuxWindowSize
import com.prof18.feedflow.desktop.ui.desktopMainWindowTitle
import com.prof18.feedflow.desktop.ui.desktopWindowBackgroundColor
import com.prof18.feedflow.desktop.ui.rememberDesktopDarkTheme
import com.prof18.feedflow.desktop.utils.initSentry
import com.prof18.feedflow.desktop.utils.integrateAppImageIfNeeded
import com.prof18.feedflow.shared.data.DesktopWindowSettingsRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.DatabaseCloser
import com.prof18.feedflow.shared.domain.DesktopAutoRefreshScheduler
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.logging.DatabaseFailureReporterConfig
import com.prof18.feedflow.shared.logging.reportDesktopDatabaseFailure
import io.sentry.Sentry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import java.awt.Toolkit
import java.io.File
import java.io.InputStream
import java.util.Properties
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds

fun main() {
    var appConfig: DesktopConfig? = null
    try {
        runDesktopApp(onConfigReady = { appConfig = it })
    } catch (e: Throwable) {
        reportStartupCrash(e, appConfig)
        exitProcess(1)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun runDesktopApp(onConfigReady: (DesktopConfig) -> Unit) {
    integrateAppImageIfNeeded()
    setupSandboxEnvironment()

    val appConfig = createAppConfig(loadProperties(), isIcloudEnabled = setupICloudSupport())
    onConfigReady(appConfig)
    val dataFolder = File(AppDataPathBuilder.getAppDataPath(appConfig.appEnvironment))
    val lockResult = singleInstanceLock.acquire(dataFolder)
    DatabaseFailureReporterConfig.singleInstanceLockState = singleInstanceLock.state

    if (lockResult == SingleInstanceLock.Result.HeldByOther) {
        if (InstanceActivation.requestActivation(dataFolder)) return
        application {
            AlreadyRunningWindow(onQuit = ::exitApplication)
        }
        return
    }
    if (lockResult == SingleInstanceLock.Result.Acquired) {
        instanceActivation.listen(dataFolder) { activationRequests.tryEmit(Unit) }
    }

    application {
        var showMainWindow by remember { mutableStateOf(false) }
        var initProgress by remember { mutableFloatStateOf(0.1f) }
        var startupThemeMode by remember { mutableStateOf<ThemeMode?>(null) }
        val startupCoordinator = remember {
            DesktopStartupCoordinator(
                initDependencies = {
                    initializeDependencies(appConfig)
                    val initialThemeMode = DI.koin.get<SettingsRepository>().getThemeMode()
                    withContext(Dispatchers.Main) { startupThemeMode = initialThemeMode }
                    // Open the database here, so a failure reaches the coordinator instead of a composable.
                    DI.koin.get<SqlDriver>()
                },
                afterDatabaseReady = {
                    DI.koin.get<DesktopAutoRefreshScheduler>().start()
                    @Suppress("MagicNumber")
                    withContext(Dispatchers.Main) { initProgress = 0.6f }
                    setupTelemetryAndCrashReporting(appConfig)
                },
                reportStartupFailure = { error ->
                    runCatching { setupTelemetryAndCrashReporting(appConfig) }
                    runCatching { reportDesktopDatabaseFailure("startup", error) }
                },
            )
        }
        val startupState by startupCoordinator.state.collectAsState()

        @Suppress("MagicNumber")
        LaunchedEffect(Unit) {
            delay(100.milliseconds)
            initProgress = 0.2f
            launch(Dispatchers.IO) {
                try {
                    startupCoordinator.start()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    reportStartupCrash(e, appConfig)
                    exitProcess(1)
                }
            }
        }

        val scope = rememberCoroutineScope()
        val icon = painterResource(Res.drawable.icon)
        val systemDarkTheme = rememberDesktopDarkTheme()

        if (!showMainWindow && startupState !is StartupState.DatabaseFailed) {
            DesktopStartupSplashWindow(
                themeMode = startupThemeMode,
                systemDarkTheme = systemDarkTheme,
                progress = initProgress,
                onCloseRequest = ::exitApplication,
            )
        }

        (startupState as? StartupState.DatabaseFailed)?.let { failed ->
            DatabaseStartupErrorWindow(
                failure = failed.failure,
                dataFolder = File(AppDataPathBuilder.getAppDataPath(appConfig.appEnvironment)),
                onQuit = ::exitApplication,
                activationRequests = activationRequests,
            )
        }

        if (startupState == StartupState.Ready) {
            val desktopWindowSettingsRepository = remember { DI.koin.get<DesktopWindowSettingsRepository>() }
            val settingsRepository = remember { DI.koin.get<SettingsRepository>() }
            val windowState = windowState(desktopWindowSettingsRepository)
            var showBackupLoader by remember { mutableStateOf(false) }

            val themeMode by settingsRepository.themeModeFlow.collectAsState()
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDarkTheme
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.OLED -> true
            }

            SwingWindow(
                onCloseRequest = {
                    scope.launch {
                        showBackupLoader = true
                        try {
                            DI.koin.get<DesktopAutoRefreshScheduler>().stop()
                            DI.koin.get<FeedSyncRepository>().performBackup()
                            DI.koin.get<DatabaseCloser>().close()
                        } catch (e: Exception) {
                            DI.koin.get<Logger>().e("Error during cleanup", e)
                        } finally {
                            exitApplication()
                        }
                    }
                },
                title = desktopMainWindowTitle(),
                state = windowState,
                init = { window ->
                    window.restoreInitialLinuxWindowSize(windowState.size) {
                        windowState.placement == WindowPlacement.Floating
                    }
                },
                icon = icon,
                visible = showMainWindow,
                onPreviewKeyEvent = { false },
            ) {
                DesktopWindowBackground(window, desktopWindowBackgroundColor(isDarkTheme, themeMode == ThemeMode.OLED))

                LaunchedEffect(window) {
                    activationRequests.collect { window.bringToFront(windowState) }
                }

                LaunchedEffect(Unit) {
                    initProgress = 1f
                    delay(100.milliseconds)
                    showMainWindow = true
                }

                AppContent(
                    showBackupLoader = showBackupLoader,
                    windowState = windowState,
                    appConfig = appConfig,
                )
            }
        }
    }
}

private fun reportStartupCrash(error: Throwable, appConfig: DesktopConfig?) {
    val dataFolder = appConfig?.let { File(AppDataPathBuilder.getAppDataPath(it.appEnvironment)) }
    handleStartupCrash(error = error, appVersion = appConfig?.version, dataFolder = dataFolder)
    // Only reaches Sentry when startup got far enough to read the crash reporting preference.
    runCatching {
        Sentry.captureException(error)
        Sentry.flush(SENTRY_FLUSH_TIMEOUT_MILLIS)
    }
}

private const val SENTRY_FLUSH_TIMEOUT_MILLIS = 2_000L

// Held for the whole process lifetime so the OS keeps the lock until FeedFlow exits.
private val singleInstanceLock = SingleInstanceLock()
private val instanceActivation = InstanceActivation()

// Requests that arrive before any window exists are dropped: the splash or main window is about to appear.
private val activationRequests = MutableSharedFlow<Unit>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
)

@Composable
internal fun DesktopStartupSplashWindow(
    themeMode: ThemeMode?,
    systemDarkTheme: Boolean,
    progress: Float,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (themeMode == null) return
    val isLinux = getDesktopOS() == DesktopOS.LINUX
    Window(
        onCloseRequest = onCloseRequest,
        title = "FeedFlow",
        state = rememberWindowState(
            size = DpSize(400.dp, 300.dp),
            position = WindowPosition.Aligned(Alignment.Center),
        ),
        icon = painterResource(Res.drawable.icon),
        resizable = false,
        undecorated = true,
        transparent = !isLinux,
    ) {
        DesktopStartupSplashContent(
            themeMode = themeMode,
            systemDarkTheme = systemDarkTheme,
            progress = progress,
            useRoundedCorners = !isLinux,
            modifier = modifier,
        )
    }
}

@Composable
internal fun DesktopStartupSplashContent(
    themeMode: ThemeMode,
    systemDarkTheme: Boolean,
    progress: Float,
    useRoundedCorners: Boolean,
    modifier: Modifier = Modifier,
) {
    SplashContent(
        modifier = modifier,
        progress = progress,
        useRoundedCorners = useRoundedCorners,
        darkTheme = when (themeMode) {
            ThemeMode.SYSTEM -> systemDarkTheme
            ThemeMode.LIGHT -> false
            ThemeMode.DARK, ThemeMode.OLED -> true
        },
        useOledTheme = themeMode == ThemeMode.OLED,
    )
}

private fun loadProperties(): Properties {
    val properties = Properties()
    val propsFile = DI::class.java.classLoader?.getResourceAsStream("props.properties")
        ?: InputStream.nullInputStream()
    properties.load(propsFile)
    return properties
}

internal fun createAppConfig(properties: Properties, isIcloudEnabled: Boolean): DesktopConfig {
    val sentryDns = properties["sentry_dns"]?.toString()
    val version = properties["version"]?.toString()?.takeIf { it.isNotBlank() } ?: DesktopBuildInfo.VERSION
    val isRelease = properties["is_release"]?.toString()?.toBooleanStrictOrNull() ?: false
    val dropboxKey = properties["dropbox_key"]?.toString()
    val isFlatpak = properties["flatpak"]?.toString()?.toBooleanStrictOrNull() ?: false

    val appEnvironment = if (isRelease) AppEnvironment.Release else AppEnvironment.Debug

    return DesktopConfig(
        sentryDns = sentryDns,
        version = version,
        appEnvironment = appEnvironment,
        isIcloudEnabled = isIcloudEnabled,
        isDropboxEnabled = dropboxKey != null,
        isGoogleDriveEnabled = !isFlatpak,
    )
}

private fun setupSandboxEnvironment() {
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
}

@Suppress("UnsafeDynamicallyLoadedCode")
private fun setupICloudSupport(): Boolean {
    if (!getDesktopOS().isMacOs()) return false

    val resourcesDir = System.getProperty("compose.application.resources.dir")
    val libraryPath = resourcesDir + File.separator + System.mapLibraryName("ikloud")
    return try {
        System.load(libraryPath)
        true
    } catch (_: UnsatisfiedLinkError) {
        System.err.println("Failed to load iCloud library")
        System.err.println("Failed to load library. Path: $libraryPath")
        false
    }
}

private fun initializeDependencies(appConfig: DesktopConfig) {
    DI.initKoin(
        appConfig = appConfig,
    )
}

private fun setupTelemetryAndCrashReporting(appConfig: DesktopConfig) {
    val isCrashReportEnabled = DI.koin.get<SettingsRepository>().getCrashReportingEnabled()

    if (appConfig.appEnvironment.isRelease() &&
        appConfig.sentryDns != null &&
        appConfig.version != null &&
        isCrashReportEnabled
    ) {
        initSentry(
            dns = appConfig.sentryDns,
            version = appConfig.version,
        )
    }

    val telemetryClient = DI.koin.get<TelemetryDeckClient>()
    telemetryClient.signal("TelemetryDeck.Session.started")
}

@Composable
private fun windowState(desktopWindowSettingsRepository: DesktopWindowSettingsRepository): WindowState {
    val savedWidthDp = desktopWindowSettingsRepository.getDesktopWindowWidthDp()
    val savedHeightDp = desktopWindowSettingsRepository.getDesktopWindowHeightDp()
    val savedPositionX = desktopWindowSettingsRepository.getDesktopWindowXPositionDp()
    val savedPositionY = desktopWindowSettingsRepository.getDesktopWindowYPositionDp()

    val toolkit = Toolkit.getDefaultToolkit()
    val screenSize = toolkit.screenSize
    val maxWidth = screenSize.width.dp
    val maxHeight = screenSize.height.dp

    val width = savedWidthDp.dp.coerceAtMost(maxWidth)
    val height = savedHeightDp.dp.coerceAtMost(maxHeight)

    val xPos = savedPositionX?.dp
        ?.coerceIn(0.dp, (maxWidth - width).coerceAtLeast(minimumValue = 0.dp))
        ?: Dp.Unspecified

    val yPos = savedPositionY?.dp
        ?.coerceIn(0.dp, (maxHeight - height).coerceAtLeast(minimumValue = 0.dp))
        ?: Dp.Unspecified

    val position = if (xPos != Dp.Unspecified && yPos != Dp.Unspecified) {
        WindowPosition.Absolute(
            x = xPos,
            y = yPos,
        )
    } else {
        WindowPosition.PlatformDefault
    }

    return rememberWindowState(
        size = DpSize(width, height),
        position = position,
        placement = savedPlacement(desktopWindowSettingsRepository),
    )
}

private fun savedPlacement(desktopWindowSettingsRepository: DesktopWindowSettingsRepository): WindowPlacement =
    when (desktopWindowSettingsRepository.getDesktopWindowPlacement()) {
        WindowPlacement.Maximized.name -> WindowPlacement.Maximized
        WindowPlacement.Fullscreen.name -> WindowPlacement.Fullscreen
        else -> WindowPlacement.Floating
    }

data class DesktopConfig(
    val sentryDns: String?,
    val version: String?,
    val appEnvironment: AppEnvironment,
    val isIcloudEnabled: Boolean,
    val isDropboxEnabled: Boolean,
    val isGoogleDriveEnabled: Boolean,
)
