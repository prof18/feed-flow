package com.prof18.feedflow.shared.domain.browser

import com.prof18.feedflow.shared.data.SettingsHelper
import com.prof18.feedflow.shared.domain.model.Browser

class BrowserSettingsRepository internal constructor(
    private val settingsHelper: SettingsHelper,
) {
    fun getFavouriteBrowserId(): String? =
        settingsHelper.getFavouriteBrowserId()

    fun setFavouriteBrowser(browser: Browser) {
        settingsHelper.saveFavouriteBrowserId(browser.id)
    }
}
