package com.prof18.feedflow

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
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
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.prof18.feedflow.home.HomeScreen
import com.prof18.feedflow.navigation.ChildStack
import com.prof18.feedflow.navigation.ProvideComponentContext
import com.prof18.feedflow.navigation.Screen
import com.prof18.feedflow.settings.SettingsScreen
import com.prof18.feedflow.ui.FeedFlowTheme

val koin = initKoinDesktop().koin

@OptIn(ExperimentalComposeUiApi::class, ExperimentalDecomposeApi::class)
fun main() = application {

    val lifecycle = LifecycleRegistry()
    val rootComponentContext = DefaultComponentContext(lifecycle = lifecycle)

    val windowState = rememberWindowState()

    LifecycleController(lifecycle, windowState)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "FeedFlow"
    ) {

        val navigation: StackNavigation<Screen> = remember { StackNavigation<Screen>() }

        FeedFlowMenuBar(navigation)

        Surface(modifier = Modifier.fillMaxSize()) {
            FeedFlowTheme {
                CompositionLocalProvider(LocalScrollbarStyle provides defaultScrollbarStyle()) {
                    ProvideComponentContext(rootComponentContext) {
                        MainContent(navigation)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FrameWindowScope.FeedFlowMenuBar(navigation: StackNavigation<Screen>) {
    MenuBar {
        Menu("File", mnemonic = 'F') {

            Item(
                text = "Mark all read",
                onClick = {
                    // TODO
                },
            )

            Separator()

            Item(
                text = "Import Feed from OPML",
                onClick = {
                          // TODO
                },
            )

            Item(
                text = "Add Feed",
                onClick = {
                    // TODO
                },
            )

            Item(
                text = "Feeds",
                onClick = {
                    // TODO
                },
            )
        }
    }
}

@Composable
fun MainContent(
    navigation: StackNavigation<Screen>,
) {

    ChildStack(
        source = navigation,
        initialStack = { listOf(Screen.Home) },
        handleBackButton = true,
        animation = stackAnimation(fade() + scale()),
    ) { screen ->
        when (screen) {
            is Screen.Home -> HomeScreen()
            is Screen.Settings -> SettingsScreen()
            else -> TODO()
        }
    }
}
