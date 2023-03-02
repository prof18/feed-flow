package com.prof18.feedflow

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.prof18.feedflow.di.initKoinDesktop
import com.prof18.feedflow.feedlist.FeedListScreen
import com.prof18.feedflow.home.HomeScreen
import com.prof18.feedflow.navigation.ChildStack
import com.prof18.feedflow.navigation.ProvideComponentContext
import com.prof18.feedflow.navigation.Screen
import com.prof18.feedflow.ui.style.FeedFlowTheme
import com.prof18.feedflow.ui.style.rememberDesktopDarkTheme
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

val koin = initKoinDesktop().koin

@OptIn(ExperimentalDecomposeApi::class)
fun main() = application {

    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

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
                CompositionLocalProvider(LocalScrollbarStyle provides scrollbarStyle()) {
                    ProvideComponentContext(rootComponentContext) {
                        MainContent(navigation)
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
        }
    )
}

@Composable
fun FrameWindowScope.FeedFlowMenuBar(navigation: StackNavigation<Screen>) {
    MenuBar {
        Menu("File", mnemonic = 'F') {

            Item(
                text = "Refresh Feed",
                onClick = {
                    // TODO
                },
            )

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
                    val fileChooser = JFileChooser("/").apply {
                        fileSelectionMode = JFileChooser.FILES_ONLY
                        addChoosableFileFilter(FileNameExtensionFilter("OPML", "opml"))
                        dialogTitle = "Select OPML file"
                        approveButtonText = "Import"
                    }
                    fileChooser.showOpenDialog(window /* OR null */)
                    val result = fileChooser.selectedFile
                    println(result)
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
                    navigation.push(Screen.FeedList)
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
            is Screen.FeedList -> FeedListScreen(navigateBack = { navigation.pop() })
        }
    }
}
