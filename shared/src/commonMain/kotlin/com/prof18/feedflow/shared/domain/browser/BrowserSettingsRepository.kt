package com.prof18.feedflow.shared.domain.browser

import com.prof18.feedflow.shared.domain.model.Browser

interface BrowserSettingsRepository {
    fun getFavouriteBrowserId(): String?
    fun setFavouriteBrowser(browser: Browser)
}
