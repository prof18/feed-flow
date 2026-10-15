package com.prof18.feedflow.desktop.testing

import java.awt.Desktop

internal fun requestNativeTestAppForeground() {
    if (!Desktop.isDesktopSupported()) return
    val desktop = Desktop.getDesktop()
    if (desktop.isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)) {
        desktop.requestForeground(false)
    }
}
