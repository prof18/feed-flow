package com.prof18.feedflow

import java.awt.Desktop
import java.net.URI

fun openInBrowser(url: String) {
    val desktop = Desktop.getDesktop()
    desktop.browse(URI.create(url))
}
