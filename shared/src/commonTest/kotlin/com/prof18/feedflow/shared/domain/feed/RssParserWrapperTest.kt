package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedFetchTier
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.rssparser.exception.HttpException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class RssParserWrapperTest {

    private val store = FeedHttpCacheStore(currentTimeMillis = { 0L }, logger = Logger.withTag("WrapperTest"))
    private val feedUrl = "https://example.com/feed.xml"

    @Test
    fun `primary response is returned without calling fallback`() = runTest {
        val expectedChannel = RssChannelGenerator.rssChannel()
        val wrapper = RssParserWrapperImpl(
            feedHttpCacheStore = store,
            parsers = mapOf(
                FeedFetchTier.PRIMARY to { expectedChannel },
                FeedFetchTier.LINK_FREE to { error("Fallback should not be called") },
            ),
        )

        val channel = wrapper.getRssChannel(feedUrl)

        assertSame(expectedChannel, channel)
    }

    @Test
    fun `forbidden primary response is retried with fallback`() = runTest {
        val requestedUrls = mutableListOf<String>()
        val expectedChannel = RssChannelGenerator.rssChannel()
        val wrapper = RssParserWrapperImpl(
            feedHttpCacheStore = store,
            parsers = mapOf(
                FeedFetchTier.PRIMARY to { throw HttpException(code = 403, message = "Forbidden") },
                FeedFetchTier.LINK_FREE to { url ->
                    requestedUrls.add(url)
                    expectedChannel
                },
            ),
        )

        val channel = wrapper.getRssChannel(feedUrl)

        assertSame(expectedChannel, channel)
        assertEquals(listOf(feedUrl), requestedUrls)
    }

    @Test
    fun `non-forbidden HTTP error is propagated without calling fallback`() = runTest {
        val expectedError = HttpException(code = 404, message = "Not Found")
        val wrapper = RssParserWrapperImpl(
            feedHttpCacheStore = store,
            parsers = mapOf(
                FeedFetchTier.PRIMARY to { throw expectedError },
                FeedFetchTier.LINK_FREE to { error("Fallback should not be called") },
            ),
        )

        val error = assertFailsWith<HttpException> {
            wrapper.getRssChannel(feedUrl)
        }

        assertSame(expectedError, error)
    }

    @Test
    fun `fallback error is propagated without another retry`() = runTest {
        val expectedError = HttpException(code = 403, message = "Still forbidden")
        val wrapper = RssParserWrapperImpl(
            feedHttpCacheStore = store,
            parsers = mapOf(
                FeedFetchTier.PRIMARY to { throw HttpException(code = 403, message = "Forbidden") },
                FeedFetchTier.LINK_FREE to { throw expectedError },
            ),
        )

        val error = assertFailsWith<HttpException> {
            wrapper.getRssChannel(feedUrl)
        }

        assertSame(expectedError, error)
    }

    @Test
    fun `two forbidden responses reach browser tier`() = runTest {
        val attempted = mutableListOf<FeedFetchTier>()
        val expected = RssChannelGenerator.rssChannel()
        val wrapper = RssParserWrapperImpl(
            feedHttpCacheStore = store,
            parsers = FeedFetchTier.entries.associateWith { tier ->
                {
                        _: String ->
                    attempted.add(tier)
                    if (tier != FeedFetchTier.BROWSER) {
                        throw HttpException(code = 403, message = "Forbidden")
                    }
                    expected
                }
            },
        )
        assertSame(expected, wrapper.getRssChannel(feedUrl))
        assertEquals(FeedFetchTier.entries.toList(), attempted)
    }

    @Test
    fun `not found never reaches another tier`() = runTest {
        val expected = HttpException(code = 404, message = "Not Found")
        val wrapper = RssParserWrapperImpl(
            feedHttpCacheStore = store,
            parsers = mapOf(
                FeedFetchTier.PRIMARY to { throw expected },
                FeedFetchTier.LINK_FREE to { error("Link-free must not run") },
                FeedFetchTier.BROWSER to { error("Browser must not run") },
            ),
        )
        assertSame(expected, assertFailsWith<HttpException> { wrapper.getRssChannel(feedUrl) })
    }

    @Test
    fun `browser can be excluded from forbidden retries`() = runTest {
        val expected = HttpException(code = 403, message = "Still Forbidden")
        val wrapper = RssParserWrapperImpl(
            feedHttpCacheStore = store,
            parsers = mapOf(
                FeedFetchTier.PRIMARY to { throw HttpException(code = 403, message = "Forbidden") },
                FeedFetchTier.LINK_FREE to { throw expected },
                FeedFetchTier.BROWSER to { error("Browser must not run") },
            ),
        )
        assertSame(
            expected,
            assertFailsWith<HttpException> {
                wrapper.getRssChannel(feedUrl, allowBrowserTier = false)
            },
        )
    }

    @Test
    fun `last browser refusal propagates after all three tiers`() = runTest {
        val expected = HttpException(code = 403, message = "Browser forbidden")
        val attempted = mutableListOf<FeedFetchTier>()
        val wrapper = RssParserWrapperImpl(
            feedHttpCacheStore = store,
            parsers = FeedFetchTier.entries.associateWith { tier ->
                {
                        _: String ->
                    attempted.add(tier)
                    if (tier == FeedFetchTier.BROWSER) {
                        throw expected
                    }
                    throw HttpException(code = 403, message = "Forbidden")
                }
            },
        )

        assertSame(expected, assertFailsWith<HttpException> { wrapper.getRssChannel(feedUrl) })
        assertEquals(FeedFetchTier.entries.toList(), attempted)
        assertEquals(null, store.tierFor(feedUrl))
    }

    @Test
    fun `remembered browser tier succeeds in one request and is recorded`() = runTest {
        store.seedTiers(mapOf(feedUrl to FeedFetchTier.BROWSER))
        val expected = RssChannelGenerator.rssChannel()
        val wrapper = RssParserWrapperImpl(
            feedHttpCacheStore = store,
            parsers = mapOf(
                FeedFetchTier.PRIMARY to { error("Primary must not run") },
                FeedFetchTier.LINK_FREE to { error("Link-free must not run") },
                FeedFetchTier.BROWSER to { expected },
            ),
        )
        assertSame(expected, wrapper.getRssChannel(feedUrl))
        assertEquals(FeedFetchTier.BROWSER, store.tierFor(feedUrl))
    }

    @Test
    fun `remembered forbidden browser still tries remaining tiers in order`() = runTest {
        store.seedTiers(mapOf(feedUrl to FeedFetchTier.BROWSER))
        val attempted = mutableListOf<FeedFetchTier>()
        val expected = RssChannelGenerator.rssChannel()
        val wrapper = RssParserWrapperImpl(
            feedHttpCacheStore = store,
            parsers = FeedFetchTier.entries.associateWith { tier ->
                {
                        _: String ->
                    attempted.add(tier)
                    if (tier != FeedFetchTier.LINK_FREE) {
                        throw HttpException(code = 403, message = "Forbidden")
                    }
                    expected
                }
            },
        )
        assertSame(expected, wrapper.getRssChannel(feedUrl))
        assertEquals(listOf(FeedFetchTier.BROWSER, FeedFetchTier.PRIMARY, FeedFetchTier.LINK_FREE), attempted)
        assertEquals(FeedFetchTier.LINK_FREE, store.tierFor(feedUrl))
    }

    @Test
    fun `remembered browser is ignored when disallowed and primary success is recorded`() = runTest {
        store.seedTiers(mapOf(feedUrl to FeedFetchTier.BROWSER))
        val expected = RssChannelGenerator.rssChannel()
        val wrapper = RssParserWrapperImpl(
            feedHttpCacheStore = store,
            parsers = mapOf(
                FeedFetchTier.PRIMARY to { expected },
                FeedFetchTier.BROWSER to { error("Browser must not run") },
            ),
        )
        assertSame(expected, wrapper.getRssChannel(feedUrl, allowBrowserTier = false))
        assertEquals(FeedFetchTier.PRIMARY, store.tierFor(feedUrl))
    }
}
