package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.rssparser.exception.HttpException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class RssParserWrapperTest {

    private val feedUrl = "https://example.com/feed.xml"

    @Test
    fun `primary response is returned without calling fallback`() = runTest {
        val expectedChannel = RssChannelGenerator.rssChannel()
        val wrapper = RssParserWrapperImpl(
            primaryParser = { expectedChannel },
            forbiddenFallbackParser = { error("Fallback should not be called") },
        )

        val channel = wrapper.getRssChannel(feedUrl)

        assertSame(expectedChannel, channel)
    }

    @Test
    fun `forbidden primary response is retried with fallback`() = runTest {
        val requestedUrls = mutableListOf<String>()
        val expectedChannel = RssChannelGenerator.rssChannel()
        val wrapper = RssParserWrapperImpl(
            primaryParser = { throw HttpException(code = 403, message = "Forbidden") },
            forbiddenFallbackParser = { url ->
                requestedUrls.add(url)
                expectedChannel
            },
        )

        val channel = wrapper.getRssChannel(feedUrl)

        assertSame(expectedChannel, channel)
        assertEquals(listOf(feedUrl), requestedUrls)
    }

    @Test
    fun `non-forbidden HTTP error is propagated without calling fallback`() = runTest {
        val expectedError = HttpException(code = 404, message = "Not Found")
        val wrapper = RssParserWrapperImpl(
            primaryParser = { throw expectedError },
            forbiddenFallbackParser = { error("Fallback should not be called") },
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
            primaryParser = { throw HttpException(code = 403, message = "Forbidden") },
            forbiddenFallbackParser = { throw expectedError },
        )

        val error = assertFailsWith<HttpException> {
            wrapper.getRssChannel(feedUrl)
        }

        assertSame(expectedError, error)
    }
}
