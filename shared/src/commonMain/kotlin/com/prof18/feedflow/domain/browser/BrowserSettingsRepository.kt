package com.prof18.feedflow.domain.browser

import com.prof18.feedflow.domain.model.Browser

interface BrowserSettingsRepository {
    fun getFavouriteBrowserId(): String?
    fun setFavouriteBrowser(browser: Browser)
}
