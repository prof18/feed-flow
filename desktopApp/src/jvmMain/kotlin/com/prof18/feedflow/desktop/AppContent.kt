package com.prof18.feedflow.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.FlatPropertiesLaf
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isNotMacOs
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.main.MainWindow
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.contentprefetch.ContentPrefetchRepository
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.theme.rememberDesktopDarkTheme
import com.prof18.feedflow.shared.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.rememberFeedFlowStrings
import javax.swing.UIManager

@Composable
internal fun FrameWindowScope.AppContent(
    showBackupLoader: Boolean,
    windowState: WindowState,
    appConfig: DesktopConfig,
) {
    val koin = DI.koin
    setSingletonImageLoaderFactory { koin.get<ImageLoader>() }

    LaunchedEffect(Unit) {
        val contentPrefetchRepository: ContentPrefetchRepository = koin.get()
        contentPrefetchRepository.startBackgroundFetching()
    }

    val settingsRepository = DI.koin.get<SettingsRepository>()
    val themeMode by settingsRepository.themeModeFlow.collectAsState()
    val isDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> rememberDesktopDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    FeedFlowTheme(darkTheme = isDarkTheme) {
        LaunchedEffect(isDarkTheme) {
            if (getDesktopOS().isNotMacOs()) {
                setupLookAndFeel(isDarkTheme)
            }
        }

        val lyricist = rememberFeedFlowStrings()
        ProvideFeedFlowStrings(lyricist) {
            this.MainWindow(
                showBackupLoader = showBackupLoader,
                isDarkTheme = isDarkTheme,
                windowState = windowState,
                appConfig = appConfig,
            )
        }
    }
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
