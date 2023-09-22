package com.prof18.feedflow.domain.browser

import com.prof18.feedflow.data.SettingsHelper
import com.prof18.feedflow.domain.model.Browser

internal class BrowserSettingsRepositoryImpl(
    private val settingsHelper: SettingsHelper,
) : BrowserSettingsRepository {
    override fun getFavouriteBrowserId(): String? =
        settingsHelper.getFavouriteBrowserId()

    override fun setFavouriteBrowser(browser: Browser) {
        settingsHelper.saveFavouriteBrowserId(browser.id)
    }
}
