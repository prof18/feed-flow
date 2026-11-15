package com.prof18.feedflow.core.utils

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.resolveWith

object BrowserResolver {
    // The browser for comments, links inside articles and feed websites.
    fun linksBrowserId(favouriteId: String?, availableIds: List<String>): String =
        favouriteId?.takeIf { it == BrowserIds.IN_APP_BROWSER || it in availableIds }
            ?: BrowserIds.IN_APP_BROWSER

    // The browser PREFERRED_BROWSER opens; falls back to the links browser.
    fun articleBrowserId(
        articleBrowserId: String?,
        favouriteId: String?,
        availableIds: List<String>,
    ): String =
        articleBrowserId?.takeIf { it != BrowserIds.IN_APP_BROWSER && it in availableIds }
            ?: linksBrowserId(favouriteId, availableIds)

    // Where an explicit "Open in browser" action on an article goes.
    fun browserActionMode(feedMode: ArticleOpenMode, globalMode: ArticleOpenMode): ArticleOpenMode =
        if (feedMode.resolveWith(globalMode) == ArticleOpenMode.INTERNAL_BROWSER) {
            ArticleOpenMode.INTERNAL_BROWSER
        } else {
            ArticleOpenMode.PREFERRED_BROWSER
        }
}
