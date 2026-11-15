package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.utils.BrowserIds
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsRepositoryArticleBrowserTest : KoinTestBase() {

    private val repository: SettingsRepository by inject()

    @Test
    fun `article browser is absent on a fresh install`() = runTest {
        assertNull(repository.getArticleBrowserId())
    }

    @Test
    fun `selecting a browser stores it and opens articles in a browser`() = runTest {
        repository.selectArticleBrowser(DUCKDUCKGO)

        assertEquals(DUCKDUCKGO, repository.getArticleBrowserId())
        assertEquals(ArticleOpenMode.PREFERRED_BROWSER, repository.getArticleOpenMode())
    }

    @Test
    fun `selecting the in-app browser stores the in-app mode and keeps the article browser`() = runTest {
        repository.selectArticleBrowser(DUCKDUCKGO)

        repository.selectArticleBrowser(BrowserIds.IN_APP_BROWSER)

        assertEquals(ArticleOpenMode.INTERNAL_BROWSER, repository.getArticleOpenMode())
        assertEquals(DUCKDUCKGO, repository.getArticleBrowserId())
    }

    @Test
    fun `changing the links browser keeps articles in the old favourite browser`() = runTest {
        repository.saveFavouriteBrowserId(CHROME)
        repository.setArticleOpenMode(ArticleOpenMode.PREFERRED_BROWSER)

        repository.saveFavouriteBrowserId(FIREFOX)

        assertEquals(CHROME, repository.getArticleBrowserId())
        assertEquals(FIREFOX, repository.getFavouriteBrowserId())
        assertEquals(ArticleOpenMode.PREFERRED_BROWSER, repository.getArticleOpenMode())
    }

    @Test
    fun `changing the links browser away from in-app keeps articles in the in-app browser`() = runTest {
        repository.saveFavouriteBrowserId(BrowserIds.IN_APP_BROWSER)
        repository.setArticleOpenMode(ArticleOpenMode.PREFERRED_BROWSER)

        repository.saveFavouriteBrowserId(FIREFOX)

        assertEquals(ArticleOpenMode.INTERNAL_BROWSER, repository.getArticleOpenMode())
        assertNull(repository.getArticleBrowserId())
    }

    @Test
    fun `changing the links browser with no favourite keeps articles in the in-app browser`() = runTest {
        repository.setArticleOpenMode(ArticleOpenMode.PREFERRED_BROWSER)

        repository.saveFavouriteBrowserId(FIREFOX)

        assertEquals(ArticleOpenMode.INTERNAL_BROWSER, repository.getArticleOpenMode())
        assertNull(repository.getArticleBrowserId())
    }

    @Test
    fun `changing the links browser leaves reader modes alone`() = runTest {
        repository.setArticleOpenMode(ArticleOpenMode.FULL_ARTICLE)

        repository.saveFavouriteBrowserId(FIREFOX)

        assertEquals(ArticleOpenMode.FULL_ARTICLE, repository.getArticleOpenMode())
        assertNull(repository.getArticleBrowserId())
    }

    @Test
    fun `only the first links browser change is copied`() = runTest {
        repository.saveFavouriteBrowserId(CHROME)
        repository.setArticleOpenMode(ArticleOpenMode.PREFERRED_BROWSER)

        repository.saveFavouriteBrowserId(FIREFOX)
        repository.saveFavouriteBrowserId(DUCKDUCKGO)

        assertEquals(CHROME, repository.getArticleBrowserId())
        assertEquals(DUCKDUCKGO, repository.getFavouriteBrowserId())
    }

    private companion object {
        const val CHROME = "com.android.chrome"
        const val FIREFOX = "org.mozilla.firefox"
        const val DUCKDUCKGO = "com.duckduckgo.mobile.android"
    }
}
