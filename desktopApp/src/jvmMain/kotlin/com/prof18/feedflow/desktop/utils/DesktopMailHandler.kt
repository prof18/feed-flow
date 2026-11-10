package com.prof18.feedflow.desktop.utils

import com.prof18.feedflow.desktop.testing.DesktopE2eHooks
import java.awt.Desktop
import java.net.URI

internal fun openDesktopMailSafely(
    url: String,
    sendMail: (URI) -> Unit = { uri ->
        val desktop = Desktop.getDesktop()
        check(desktop.isSupported(Desktop.Action.MAIL))
        desktop.mail(uri)
    },
    openFallback: () -> Boolean = {
        openDesktopUriSafely("https://github.com/prof18/feed-flow/issues/new/choose")
    },
): Boolean {
    if (DesktopE2eHooks.recordOpenedUri(url)) return true
    return runCatching { sendMail(URI.create(url)) }
        .fold(
            onSuccess = { true },
            onFailure = { runCatching { openFallback() }.getOrDefault(false) },
        )
}
