package com.prof18.feedflow.shared.domain.browser

import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.model.Browser

class BrowserSettingsRepository internal constructor(
    private val settingsRepository: SettingsRepository,
) {
    fun getFavouriteBrowserId(): String? =
        settingsRepository.getFavouriteBrowserId()

    fun setFavouriteBrowser(browser: Browser) {
        settingsRepository.saveFavouriteBrowserId(browser.id)
    }
}
