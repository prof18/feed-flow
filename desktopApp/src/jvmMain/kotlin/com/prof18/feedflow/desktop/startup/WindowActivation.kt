package com.prof18.feedflow.desktop.startup

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.WindowState
import java.awt.Desktop

internal fun ComposeWindow.bringToFront(windowState: WindowState) {
    windowState.isMinimized = false
    // macOS only raises the window of the active app.
    runCatching {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)) {
            Desktop.getDesktop().requestForeground(true)
        }
    }
    // Windows and several Linux window managers refuse toFront() from a background app; a brief
    // always-on-top switch gets the window above the others anyway.
    isAlwaysOnTop = true
    toFront()
    requestFocus()
    isAlwaysOnTop = false
}
