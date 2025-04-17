package com.prof18.feedflow.desktop.utils

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

fun copyToClipboard(contentToCopy: String): Boolean =
    try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(contentToCopy), null)
        true
    } catch (_: Exception) {
        false
    }
