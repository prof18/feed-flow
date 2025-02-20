package com.prof18.feedflow.desktop

import com.prof18.feedflow.shared.data.SettingsRepository
import java.awt.Desktop
import java.net.URI

internal class BrowserManager(
    private val settingsRepository: SettingsRepository,
) {
    fun openReaderMode(): Boolean =
        settingsRepository.isUseReaderModeEnabled()
}

fun openInBrowser(url: String) {
    try {
        val desktop = Desktop.getDesktop()
        desktop.browse(URI.create(url))
    } catch (_: Exception) {
        // do nothing
    }
}
