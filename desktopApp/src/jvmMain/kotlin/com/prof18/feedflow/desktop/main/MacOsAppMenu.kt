package com.prof18.feedflow.desktop.main

import java.awt.Desktop

/**
 * Routes the native macOS application menu ("Settings…" with ⌘, and "About") to the Settings window.
 * Without a preferences handler macOS hides the Settings item from the application menu.
 */
internal fun installMacOsAppMenuHandlers(onSettings: () -> Unit, onAbout: () -> Unit) {
    val desktop = desktopOrNull() ?: return
    if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) {
        desktop.setPreferencesHandler { onSettings() }
    }
    if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
        desktop.setAboutHandler { onAbout() }
    }
}

internal fun uninstallMacOsAppMenuHandlers() {
    val desktop = desktopOrNull() ?: return
    if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) desktop.setPreferencesHandler(null)
    if (desktop.isSupported(Desktop.Action.APP_ABOUT)) desktop.setAboutHandler(null)
}

private fun desktopOrNull(): Desktop? = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
