package com.prof18.feedflow.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.FlatPropertiesLaf
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.core.utils.isNotMacOs
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.main.MainWindow
import com.prof18.feedflow.desktop.telemetry.TelemetryDeckClient
import com.prof18.feedflow.desktop.utils.initSentry
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.presentation.SettingsViewModel
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.theme.rememberDesktopDarkTheme
import com.prof18.feedflow.shared.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.rememberFeedFlowStrings
import org.jetbrains.skiko.SkikoProperties.libraryPath
import java.awt.Toolkit
import java.io.File
import java.io.InputStream
import java.util.Properties
import javax.swing.UIManager

fun main() {
    val appConfig = initializeApp()

    application {
        val settingsRepository = DI.koin.get<SettingsRepository>()
        val windowState = windowState(settingsRepository)

        val koin = DI.koin
        setSingletonImageLoaderFactory { koin.get<ImageLoader>() }

        val homeViewModel = desktopViewModel { DI.koin.get<HomeViewModel>() }
        val searchViewModel = desktopViewModel { DI.koin.get<SearchViewModel>() }

        val feedSyncRepo = DI.koin.get<FeedSyncRepository>()
        val messageQueue = DI.koin.get<FeedSyncMessageQueue>()

        val settingsViewModel = desktopViewModel { DI.koin.get<SettingsViewModel>() }
        val settingsState by settingsViewModel.settingsState.collectAsState()
        val isDarkTheme = when (settingsState.themeMode) {
            ThemeMode.SYSTEM -> rememberDesktopDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

        FeedFlowTheme(
            darkTheme = isDarkTheme,
            reduceMotion = settingsState.isReduceMotionEnabled,
        ) {
            LaunchedEffect(isDarkTheme) {
                if (getDesktopOS().isNotMacOs()) {
                    setupLookAndFeel(isDarkTheme)
                }
            }

            val lyricist = rememberFeedFlowStrings()
            ProvideFeedFlowStrings(lyricist) {
                MainWindow(
                    feedSyncRepo = feedSyncRepo,
                    windowState = windowState,
                    settingsRepository = settingsRepository,
                    messageQueue = messageQueue,
                    isDarkTheme = isDarkTheme,
                    appConfig = appConfig,
                    settingsViewModel = settingsViewModel,
                    settingsState = settingsState,
                    homeViewModel = homeViewModel,
                    searchViewModel = searchViewModel,
                )
            }
        }
    }
}

private fun initializeApp(): DesktopConfig {
    val properties = loadProperties()
    val appConfig = createAppConfig(properties)

    setupSandboxEnvironment()
    initializeDependencies(appConfig)
    setupTelemetryAndCrashReporting(appConfig)

    return appConfig
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

    val windowState = rememberWindowState(
        size = DpSize(width, height),
        position = position,
    )
    return windowState
}

private fun setupLookAndFeel(isDarkMode: Boolean) {
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

internal data class DesktopConfig(
    val sentryDns: String?,
    val version: String?,
    val appEnvironment: AppEnvironment,
    val isIcloudEnabled: Boolean,
    val isDropboxEnabled: Boolean,
)
