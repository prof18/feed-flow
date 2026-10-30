package com.prof18.feedflow.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import java.awt.Color
import java.awt.Window
import javax.swing.RootPaneContainer

@Composable
internal fun DesktopWindowBackground(window: Window, color: Color) {
    DisposableEffect(window, color) {
        window.background = color
        (window as? RootPaneContainer)?.let { root ->
            root.contentPane.background = color
            root.rootPane.background = color
        }
        onDispose { }
    }
}
