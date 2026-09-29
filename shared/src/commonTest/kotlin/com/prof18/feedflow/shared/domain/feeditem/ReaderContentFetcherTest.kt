package com.prof18.feedflow.shared.domain.feeditem

import com.prof18.feedflow.shared.test.FeedItemContentFileHandlerTestImpl
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ReaderContentFetcherTest {

    private val fileHandler = FeedItemContentFileHandlerTestImpl()

    @Test
    fun `content is returned and saved when save is true`() = runTest {
        val fetcher = fetcher(parsed = "Content")

        val content = fetcher.fetch(feedItemId = "id", url = "https://example.com", save = true)

        assertEquals("Content", content)
        assertEquals("Content", fileHandler.loadFeedItemContent("id"))
    }

    @Test
    fun `content is returned but not saved when save is false`() = runTest {
        val fetcher = fetcher(parsed = "Content")

        val content = fetcher.fetch(feedItemId = "id", url = "https://example.com", save = false)

        assertEquals("Content", content)
        assertFalse(fileHandler.isContentAvailable("id"))
    }

    @Test
    fun `blank content returns null and saves nothing`() = runTest {
        val fetcher = fetcher(parsed = "   ")

        assertNull(fetcher.fetch(feedItemId = "id", url = "https://example.com", save = true))
        assertFalse(fileHandler.isContentAvailable("id"))
    }

    @Test
    fun `null parse result returns null`() = runTest {
        val fetcher = fetcher(parsed = null)

        assertNull(fetcher.fetch(feedItemId = "id", url = "https://example.com", save = true))
        assertFalse(fileHandler.isContentAvailable("id"))
    }

    private fun fetcher(parsed: String?) = ReaderContentFetcher(
        articleContentParser = object : ArticleContentParser {
            override suspend fun parse(url: String): String? = parsed
            override suspend fun prepareFeedContent(html: String, baseUrl: String?): String = html
        },
        feedItemContentFileHandler = fileHandler,
    )
}
