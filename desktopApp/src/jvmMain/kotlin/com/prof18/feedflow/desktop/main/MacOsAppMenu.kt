package com.prof18.feedflow.desktop.main

import java.awt.Desktop
import java.awt.desktop.QuitStrategy

/**
 * Routes the native macOS application menu ("Settings…" with ⌘, and "About") to the Settings window.
 * Without a preferences handler macOS hides the Settings item from the application menu.
 *
 * Quit (⌘Q) closes every window instead of exiting the JVM right away, so the main window goes through
 * its close request: it saves its geometry and runs the backup before the app exits.
 */
internal fun installMacOsAppMenuHandlers(onSettings: () -> Unit, onAbout: () -> Unit) {
    val desktop = desktopOrNull() ?: return
    if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) {
        desktop.setPreferencesHandler { onSettings() }
    }
    if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
        desktop.setAboutHandler { onAbout() }
    }
    if (desktop.isSupported(Desktop.Action.APP_QUIT_STRATEGY)) {
        desktop.setQuitStrategy(QuitStrategy.CLOSE_ALL_WINDOWS)
    }
}

internal fun uninstallMacOsAppMenuHandlers() {
    val desktop = desktopOrNull() ?: return
    if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) desktop.setPreferencesHandler(null)
    if (desktop.isSupported(Desktop.Action.APP_ABOUT)) desktop.setAboutHandler(null)
}

private fun desktopOrNull(): Desktop? = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
