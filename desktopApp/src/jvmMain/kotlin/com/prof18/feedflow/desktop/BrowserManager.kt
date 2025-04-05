package com.prof18.feedflow.desktop

import com.prof18.feedflow.shared.data.SettingsRepository

internal class BrowserManager(
    private val settingsRepository: SettingsRepository,
) {
    fun openReaderMode(): Boolean =
        settingsRepository.isUseReaderModeEnabled()
}
