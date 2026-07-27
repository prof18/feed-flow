package com.prof18.feedflow.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the dispatch table the platform screens rely on. Before the enums were unified each screen
 * repeated this logic inline, so these cases mirror the old per-platform `when` branches.
 */
class ArticleOpenModeResolverTest {

    private fun urlInfo(
        url: String = ELIGIBLE_URL,
        articleOpenMode: ArticleOpenMode,
        openOnlyOnBrowser: Boolean = false,
    ) = FeedItemUrlInfo(
        id = "id",
        url = url,
        title = "Title",
        openOnlyOnBrowser = openOnlyOnBrowser,
        isBookmarked = false,
        articleOpenMode = articleOpenMode,
        commentsUrl = null,
    )

    @Test
    fun `url-less items always open in the reader from feed content`() {
        for (global in globalArticleOpenModes) {
            assertEquals(
                ArticleOpenMode.FEED_CONTENT,
                urlInfo(url = "", articleOpenMode = ArticleOpenMode.PREFERRED_BROWSER).resolveArticleOpenMode(global),
            )
        }
    }

    @Test
    fun `a feed override wins over the global default`() {
        assertEquals(
            ArticleOpenMode.INTERNAL_BROWSER,
            urlInfo(articleOpenMode = ArticleOpenMode.INTERNAL_BROWSER)
                .resolveArticleOpenMode(ArticleOpenMode.FULL_ARTICLE),
        )
        assertEquals(
            ArticleOpenMode.FEED_CONTENT,
            urlInfo(articleOpenMode = ArticleOpenMode.FEED_CONTENT)
                .resolveArticleOpenMode(ArticleOpenMode.PREFERRED_BROWSER),
        )
    }

    @Test
    fun `DEFAULT follows the global default`() {
        for (global in globalArticleOpenModes) {
            assertEquals(
                global,
                urlInfo(articleOpenMode = ArticleOpenMode.DEFAULT).resolveArticleOpenMode(global),
            )
        }
    }

    @Test
    fun `reader modes fall back to the favourite browser when the page cannot be parsed`() {
        for (readerMode in listOf(ArticleOpenMode.FULL_ARTICLE, ArticleOpenMode.FEED_CONTENT)) {
            assertEquals(
                ArticleOpenMode.PREFERRED_BROWSER,
                urlInfo(url = PDF_URL, articleOpenMode = readerMode)
                    .resolveArticleOpenMode(ArticleOpenMode.FULL_ARTICLE),
            )
            assertEquals(
                ArticleOpenMode.PREFERRED_BROWSER,
                urlInfo(articleOpenMode = readerMode, openOnlyOnBrowser = true)
                    .resolveArticleOpenMode(ArticleOpenMode.FULL_ARTICLE),
            )
        }
    }

    @Test
    fun `browser modes are unaffected by reader eligibility`() {
        for (browserMode in listOf(ArticleOpenMode.INTERNAL_BROWSER, ArticleOpenMode.PREFERRED_BROWSER)) {
            assertEquals(
                browserMode,
                urlInfo(url = PDF_URL, articleOpenMode = browserMode)
                    .resolveArticleOpenMode(ArticleOpenMode.FULL_ARTICLE),
            )
        }
    }

    @Test
    fun `resolution never returns DEFAULT`() {
        val allModes = ArticleOpenMode.entries
        for (feedMode in allModes) {
            for (global in globalArticleOpenModes) {
                for (url in listOf(ELIGIBLE_URL, PDF_URL, "")) {
                    val resolved = urlInfo(url = url, articleOpenMode = feedMode).resolveArticleOpenMode(global)
                    assertEquals(false, resolved == ArticleOpenMode.DEFAULT, "feed=$feedMode global=$global url=$url")
                }
            }
        }
    }

    private companion object {
        const val ELIGIBLE_URL = "https://example.com/article"
        const val PDF_URL = "https://example.com/file.pdf"
    }
}
