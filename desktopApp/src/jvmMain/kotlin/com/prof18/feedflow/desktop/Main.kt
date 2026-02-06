package com.prof18.feedflow.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DesktopOS
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.resources.Res
import com.prof18.feedflow.desktop.resources.icon
import com.prof18.feedflow.desktop.telemetry.TelemetryDeckClient
import com.prof18.feedflow.desktop.utils.initSentry
import com.prof18.feedflow.shared.data.DesktopWindowSettingsRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.DatabaseCloser
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.ui.theme.rememberDesktopDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import java.awt.Toolkit
import java.io.File
import java.io.InputStream
import java.util.Properties

private const val DARK_THEME_BACKGROUND = 0x1C1B1F
private const val LIGHT_THEME_BACKGROUND = 0xFFFBFE
private const val OLED_THEME_BACKGROUND = 0x000000

fun main() {
    setupSandboxEnvironment()

    application {
        val properties = loadProperties()
        val appConfig = createAppConfig(properties)

        var isInitialized by remember { mutableStateOf(false) }
        var showMainWindow by remember { mutableStateOf(false) }
        var initProgress by remember { mutableStateOf(value = 0.1f) }

        @Suppress("MagicNumber")
        LaunchedEffect(Unit) {
            delay(100)
            initProgress = 0.2f
            launch(Dispatchers.IO) {
                initializeDependencies(appConfig)
                withContext(Dispatchers.Main) { initProgress = 0.6f }
                setupTelemetryAndCrashReporting(appConfig)
                withContext(Dispatchers.Main) {
                    isInitialized = true
                }
            }
        }

        val scope = rememberCoroutineScope()
        val icon = painterResource(Res.drawable.icon)
        val isLinux = getDesktopOS() == DesktopOS.LINUX
        val requestedUiScale = remember { resolveUiScale() }

        if (!showMainWindow) {
            Window(
                onCloseRequest = ::exitApplication,
                title = "FeedFlow",
                state = rememberWindowState(
                    size = DpSize(400.dp, 300.dp),
                    position = WindowPosition.Aligned(Alignment.Center),
                ),
                icon = icon,
                resizable = false,
                undecorated = true,
                transparent = !isLinux,
            ) {
                ProvideUiScale(requestedUiScale) {
                    SplashContent(
                        progress = initProgress,
                        useRoundedCorners = !isLinux,
                    )
                }
            }
        }

        if (isInitialized) {
            val desktopWindowSettingsRepository = remember { DI.koin.get<DesktopWindowSettingsRepository>() }
            val settingsRepository = remember { DI.koin.get<SettingsRepository>() }
            val windowState = windowState(desktopWindowSettingsRepository)
            var showBackupLoader by remember { mutableStateOf(false) }

            val themeMode = remember { settingsRepository.getThemeMode() }
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> rememberDesktopDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.OLED -> true
            }

            Window(
                onCloseRequest = {
                    scope.launch {
                        showBackupLoader = true
                        try {
                            DI.koin.get<FeedSyncRepository>().performBackup()
                            DI.koin.get<DatabaseCloser>().close()
                        } catch (e: Exception) {
                            DI.koin.get<Logger>().e("Error during cleanup", e)
                        } finally {
                            exitApplication()
                        }
                    }
                },
                title = if (getDesktopOS().isMacOs()) "" else "FeedFlow",
                state = windowState,
                icon = icon,
                visible = showMainWindow,
                onPreviewKeyEvent = { false },
            ) {
                ProvideUiScale(requestedUiScale) {
                    DisposableEffect(themeMode) {
                        val backgroundColor = when (themeMode) {
                            ThemeMode.OLED -> java.awt.Color(OLED_THEME_BACKGROUND)
                            ThemeMode.DARK -> java.awt.Color(DARK_THEME_BACKGROUND)
                            ThemeMode.LIGHT -> java.awt.Color(LIGHT_THEME_BACKGROUND)
                            ThemeMode.SYSTEM -> {
                                if (isDarkTheme) {
                                    java.awt.Color(DARK_THEME_BACKGROUND)
                                } else {
                                    java.awt.Color(LIGHT_THEME_BACKGROUND)
                                }
                            }
                        }

                        window.background = backgroundColor
                        window.contentPane.background = backgroundColor
                        if (getDesktopOS().isMacOs()) {
                            window.rootPane.background = backgroundColor
                        }

                        onDispose { }
                    }

                    LaunchedEffect(Unit) {
                        initProgress = 1f
                        delay(timeMillis = 100)
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
}

private fun loadProperties(): Properties {
    val properties = Properties()
    val propsFile = DI::class.java.classLoader?.getResourceAsStream("props.properties")
        ?: InputStream.nullInputStream()
    properties.load(propsFile)
    return properties
}

private fun createAppConfig(properties: Properties): DesktopConfig {
    val sentryDns = properties["sentry_dns"]?.toString()
    val version = properties["version"]?.toString()
    val isRelease = properties["is_release"]?.toString()?.toBooleanStrictOrNull() ?: false
    val dropboxKey = properties["dropbox_key"]?.toString()
    val isFlatpak = properties["flatpak"]?.toString()?.toBooleanStrictOrNull() ?: false

    val appEnvironment = if (isRelease) AppEnvironment.Release else AppEnvironment.Debug
    val isIcloudEnabled = setupICloudSupport()

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
private fun ProvideUiScale(
    requestedUiScale: Float?,
    content: @Composable () -> Unit,
) {
    if (requestedUiScale == null) {
        content()
        return
    }

    val baseDensity = LocalDensity.current
    val clampedScale = requestedUiScale.coerceIn(MIN_UI_SCALE, MAX_UI_SCALE)
    val shouldOverride = if (clampedScale <= UI_SCALE_ONE_THRESHOLD) {
        baseDensity.density <= UI_SCALE_ONE_THRESHOLD
    } else {
        true
    }
    if (!shouldOverride) {
        content()
        return
    }
    val scaledDensity = remember(clampedScale, baseDensity) {
        Density(clampedScale, baseDensity.fontScale)
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        content()
    }
}

private fun resolveUiScale(): Float? {
    val raw = System.getProperty("feedflow.uiScale")
        ?: System.getenv("FEEDFLOW_UI_SCALE")
    return parseUiScale(raw)
}

private fun parseUiScale(raw: String?): Float? {
    if (raw.isNullOrBlank()) return null
    val value = raw.trim().toFloatOrNull() ?: return null
    if (!value.isFinite() || value <= 0f) return null
    return value
}

private const val MIN_UI_SCALE = 0.5f
private const val MAX_UI_SCALE = 4.0f
private const val UI_SCALE_ONE_THRESHOLD = 1.01f

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
    )
}

data class DesktopConfig(
    val sentryDns: String?,
    val version: String?,
    val appEnvironment: AppEnvironment,
    val isIcloudEnabled: Boolean,
    val isDropboxEnabled: Boolean,
    val isGoogleDriveEnabled: Boolean,
)
