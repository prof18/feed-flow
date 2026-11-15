package com.prof18.feedflow.core.utils

import com.prof18.feedflow.core.model.ArticleOpenMode
import kotlin.test.Test
import kotlin.test.assertEquals

class BrowserResolverTest {
    private val available = listOf(BrowserIds.IN_APP_BROWSER, CHROME, FIREFOX)

    @Test
    fun linksBrowserFallsBackToInAppWhenNothingIsSaved() {
        assertEquals(BrowserIds.IN_APP_BROWSER, BrowserResolver.linksBrowserId(null, available))
    }

    @Test
    fun linksBrowserReturnsAnInstalledBrowser() {
        assertEquals(CHROME, BrowserResolver.linksBrowserId(CHROME, available))
    }

    @Test
    fun linksBrowserFallsBackToInAppWhenTheBrowserIsNotInstalled() {
        assertEquals(BrowserIds.IN_APP_BROWSER, BrowserResolver.linksBrowserId(DUCKDUCKGO, available))
    }

    @Test
    fun linksBrowserKeepsInAppEvenWhenItIsNotListed() {
        assertEquals(
            BrowserIds.IN_APP_BROWSER,
            BrowserResolver.linksBrowserId(BrowserIds.IN_APP_BROWSER, listOf(CHROME)),
        )
    }

    @Test
    fun linksBrowserIgnoresAnUnknownValue() {
        assertEquals(BrowserIds.IN_APP_BROWSER, BrowserResolver.linksBrowserId("garbage", available))
    }

    @Test
    fun articleBrowserReturnsAnInstalledArticleBrowser() {
        assertEquals(FIREFOX, BrowserResolver.articleBrowserId(FIREFOX, CHROME, available))
    }

    @Test
    fun articleBrowserFallsBackToTheLinksBrowserWhenAbsent() {
        assertEquals(CHROME, BrowserResolver.articleBrowserId(null, CHROME, available))
    }

    @Test
    fun articleBrowserFallsBackToTheLinksBrowserWhenNotInstalled() {
        assertEquals(CHROME, BrowserResolver.articleBrowserId(DUCKDUCKGO, CHROME, available))
    }

    @Test
    fun articleBrowserIgnoresAStoredInAppId() {
        assertEquals(CHROME, BrowserResolver.articleBrowserId(BrowserIds.IN_APP_BROWSER, CHROME, available))
    }

    @Test
    fun browserActionFollowsAGlobalInAppBrowser() {
        assertEquals(
            ArticleOpenMode.INTERNAL_BROWSER,
            BrowserResolver.browserActionMode(ArticleOpenMode.DEFAULT, ArticleOpenMode.INTERNAL_BROWSER),
        )
    }

    @Test
    fun browserActionFollowsAPerFeedInAppBrowser() {
        assertEquals(
            ArticleOpenMode.INTERNAL_BROWSER,
            BrowserResolver.browserActionMode(ArticleOpenMode.INTERNAL_BROWSER, ArticleOpenMode.FULL_ARTICLE),
        )
    }

    @Test
    fun browserActionUsesTheArticleBrowserForReaderModes() {
        assertEquals(
            ArticleOpenMode.PREFERRED_BROWSER,
            BrowserResolver.browserActionMode(ArticleOpenMode.DEFAULT, ArticleOpenMode.FULL_ARTICLE),
        )
        assertEquals(
            ArticleOpenMode.PREFERRED_BROWSER,
            BrowserResolver.browserActionMode(ArticleOpenMode.FEED_CONTENT, ArticleOpenMode.INTERNAL_BROWSER),
        )
    }

    private companion object {
        const val CHROME = "com.android.chrome"
        const val FIREFOX = "org.mozilla.firefox"
        const val DUCKDUCKGO = "com.duckduckgo.mobile.android"
    }
}
