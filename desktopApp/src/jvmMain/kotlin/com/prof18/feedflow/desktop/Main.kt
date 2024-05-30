package com.prof18.feedflow.desktop

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScaleTransition
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.ui.components.scrollbarStyle
import com.prof18.feedflow.desktop.utils.initSentry
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import java.io.InputStream
import java.util.Properties
import javax.swing.UIManager

@OptIn(ExperimentalCoilApi::class)
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
        val resourcesPath = System.getProperty("compose.application.resources.dir")

        // jna
        System.setProperty("jna.nounpack", "true")
        System.setProperty("jna.boot.library.path", resourcesPath)

        // sqlite-jdbc
        System.setProperty("org.sqlite.lib.path", resourcesPath)
        System.setProperty("org.sqlite.lib.name", "libsqlitejdbc.dylib")
    }

    DI.initKoin(
        appEnvironment = appEnvironment,
    )

    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

    val windowState = rememberWindowState()

    val koin = DI.koin
    setSingletonImageLoaderFactory { koin.get<ImageLoader>() }

    val homeViewModel = desktopViewModel { DI.koin.get<HomeViewModel>() }
    val searchViewModel = desktopViewModel { DI.koin.get<SearchViewModel>() }

    FeedFlowTheme {
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "FeedFlow",
        ) {
            CompositionLocalProvider(LocalScrollbarStyle provides scrollbarStyle()) {
                Navigator(
                    MainScreen(
                        frameWindowScope = this,
                        appEnvironment = appEnvironment,
                        version = version,
                        homeViewModel = homeViewModel,
                        searchViewModel = searchViewModel,
                    ),
                ) { navigator ->
                    ScaleTransition(navigator)
                }
            }
        }
    }
}
