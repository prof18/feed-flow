package com.prof18.feedflow.desktop

import java.awt.Desktop
import java.net.URI

fun openInBrowser(url: String) {
    @Suppress("SwallowedException")
    try {
        val desktop = Desktop.getDesktop()
        desktop.browse(URI.create(url))
    } catch (e: Exception) {
        // do nothing
    }
}
