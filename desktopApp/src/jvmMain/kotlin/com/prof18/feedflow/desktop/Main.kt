package com.prof18.feedflow.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.resources.Res
import com.prof18.feedflow.desktop.resources.icon
import com.prof18.feedflow.desktop.telemetry.TelemetryDeckClient
import com.prof18.feedflow.desktop.utils.initSentry
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skiko.SkikoProperties.libraryPath
import java.awt.Toolkit
import java.io.File
import java.io.InputStream
import java.util.Properties

fun main() {
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
                setupSandboxEnvironment()
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
                transparent = true,
            ) {
                SplashContent(progress = initProgress)
            }
        }

        if (isInitialized) {
            val settingsRepository = remember { DI.koin.get<SettingsRepository>() }
            val windowState = windowState(settingsRepository)
            var showBackupLoader by remember { mutableStateOf(false) }

            Window(
                onCloseRequest = {
                    scope.launch {
                        showBackupLoader = true
                        try {
                            DI.koin.get<FeedSyncRepository>().performBackup()
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
                visible = showMainWindow,
                onPreviewKeyEvent = { false },
            ) {
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

    val appEnvironment = if (isRelease) AppEnvironment.Release else AppEnvironment.Debug
    val isIcloudEnabled = setupICloudSupport()

    return DesktopConfig(
        sentryDns = sentryDns,
        version = version,
        appEnvironment = appEnvironment,
        isIcloudEnabled = isIcloudEnabled,
        isDropboxEnabled = dropboxKey != null,
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

    return try {
        val resourcesDir = System.getProperty("compose.application.resources.dir")
        val libraryPath = resourcesDir + File.separator + System.mapLibraryName("ikloud")
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
        appEnvironment = appConfig.appEnvironment,
        isICloudEnabled = appConfig.isIcloudEnabled,
        version = appConfig.version ?: "",
        isDropboxEnabled = appConfig.isDropboxEnabled,
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
private fun windowState(settingsRepository: SettingsRepository): WindowState {
    val savedWidthDp = settingsRepository.getDesktopWindowWidthDp()
    val savedHeightDp = settingsRepository.getDesktopWindowHeightDp()
    val savedPositionX = settingsRepository.getDesktopWindowXPositionDp()
    val savedPositionY = settingsRepository.getDesktopWindowYPositionDp()

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

internal data class DesktopConfig(
    val sentryDns: String?,
    val version: String?,
    val appEnvironment: AppEnvironment,
    val isIcloudEnabled: Boolean,
    val isDropboxEnabled: Boolean,
)
