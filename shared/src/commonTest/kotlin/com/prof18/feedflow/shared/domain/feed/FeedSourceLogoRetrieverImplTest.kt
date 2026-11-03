package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.domain.ParsedFeedContent
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.testLogger
import com.prof18.feedflow.shared.test.unexpectedRequestHttpClient
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssImage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedSourceLogoRetrieverImplTest {

    @Test
    fun `resolves root relative favicon against website domain`() = runTest(testDispatcher) {
        val retriever = createRetriever(faviconUrl = "/public/build/images/favicon-48x48.png")

        val result = retriever.getFeedSourceLogoUrl("https://www.howtogeek.com/feed/")

        assertEquals(
            expected = "https://www.howtogeek.com/public/build/images/favicon-48x48.png",
            actual = result,
        )
    }

    @Test
    fun `resolves relative favicon against website domain`() = runTest(testDispatcher) {
        val retriever = createRetriever(faviconUrl = "favicon.ico")

        val result = retriever.getFeedSourceLogoUrl("https://example.com/feed.xml")

        assertEquals(
            expected = "https://example.com/favicon.ico",
            actual = result,
        )
    }

    @Test
    fun `drops a relative channel image when there is no link to resolve it against`() =
        runTest(testDispatcher) {
            val retriever = createRetriever(faviconUrl = null)

            // Nothing to resolve the path against, so there is no logo to store.
            val result = retriever.getFeedSourceLogoUrl(channelWithImage(link = null, imageUrl = "logo.png"))

            assertNull(result)
        }

    @Test
    fun `keeps an absolute channel image when there is no link`() = runTest(testDispatcher) {
        val retriever = createRetriever(faviconUrl = null)

        val result = retriever.getFeedSourceLogoUrl(
            channelWithImage(link = null, imageUrl = "https://cdn.example.com/logo.png"),
        )

        assertEquals(expected = "https://cdn.example.com/logo.png", actual = result)
    }

    @Test
    fun `keeps absolute favicon urls unchanged`() = runTest(testDispatcher) {
        val retriever = createRetriever(faviconUrl = "https://cdn.example.com/favicon.png")

        val result = retriever.getFeedSourceLogoUrl("https://example.com/feed.xml")

        assertEquals(
            expected = "https://cdn.example.com/favicon.png",
            actual = result,
        )
    }

    @Test
    fun `fetches the website once for feeds that share a domain`() = runTest(testDispatcher) {
        var requestCount = 0
        val retriever = createRetriever(
            faviconUrl = "/favicon.png",
            onRequest = { requestCount++ },
        )

        val first = retriever.getFeedSourceLogoUrl("https://www.youtube.com/feeds/videos.xml?channel_id=a")
        val second = retriever.getFeedSourceLogoUrl("https://www.youtube.com/feeds/videos.xml?channel_id=b")

        assertEquals(expected = "https://www.youtube.com/favicon.png", actual = first)
        assertEquals(expected = first, actual = second)
        assertEquals(expected = 1, actual = requestCount)
    }

    @Test
    fun `reports a stored logo as gone when the server rejects it`() = runTest(testDispatcher) {
        val retriever = createRetriever(faviconUrl = null, responseStatus = HttpStatusCode.NotFound)

        assertFalse(retriever.isStoredLogoStillAvailable("https://www.youtube.com/s/desktop/old/favicon.png"))
    }

    @Test
    fun `keeps a stored logo when HEAD is refused but a GET finds it`() = runTest(testDispatcher) {
        // Hacker News answers HEAD with 405 while serving y18.svg to GET.
        var requestCount = 0
        val retriever = createRetriever(
            faviconUrl = null,
            headStatus = HttpStatusCode.MethodNotAllowed,
            onRequest = { requestCount++ },
        )

        assertTrue(retriever.isStoredLogoStillAvailable("https://news.ycombinator.com/y18.svg"))
        assertEquals(expected = 2, actual = requestCount)
    }

    @Test
    fun `reports a stored logo as gone when HEAD is refused and a GET cannot find it`() =
        runTest(testDispatcher) {
            val retriever = createRetriever(
                faviconUrl = null,
                responseStatus = HttpStatusCode.NotFound,
                headStatus = HttpStatusCode.MethodNotAllowed,
            )

            assertFalse(retriever.isStoredLogoStillAvailable("https://news.ycombinator.com/y18.svg"))
        }

    @Test
    fun `re-checks a stored logo and re-resolves a domain after the caches are invalidated`() =
        runTest(testDispatcher) {
            var requestCount = 0
            val retriever = createRetriever(
                faviconUrl = "/favicon.png",
                onRequest = { requestCount++ },
            )
            val logoUrl = "https://www.youtube.com/favicon.png"

            retriever.isStoredLogoStillAvailable(logoUrl)
            retriever.getFeedSourceLogoUrl("https://www.youtube.com/feeds/videos.xml?channel_id=a")
            assertEquals(expected = 2, actual = requestCount)

            retriever.invalidateLookupCaches()
            retriever.isStoredLogoStillAvailable(logoUrl)
            retriever.getFeedSourceLogoUrl("https://www.youtube.com/feeds/videos.xml?channel_id=a")
            assertEquals(expected = 4, actual = requestCount)
        }

    @Test
    fun `reports a stored relative logo as gone so it can be re-resolved`() = runTest(testDispatcher) {
        var requestCount = 0
        val retriever = createRetriever(faviconUrl = null, onRequest = { requestCount++ })

        // Older versions stored the bare "y18.svg" that the page declares.
        assertFalse(retriever.isStoredLogoStillAvailable("y18.svg"))
        assertEquals(expected = 0, actual = requestCount)
    }

    @Test
    fun `keeps a stored data uri logo without hitting the network`() = runTest(testDispatcher) {
        var requestCount = 0
        val retriever = createRetriever(
            faviconUrl = null,
            responseStatus = HttpStatusCode.NotFound,
            onRequest = { requestCount++ },
        )

        assertTrue(retriever.isStoredLogoStillAvailable("data:image/png;base64,iVBORw0KGgo="))
        assertEquals(expected = 0, actual = requestCount)
    }

    @Test
    fun `keeps a stored logo when the check cannot reach the server`() = runTest(testDispatcher) {
        val retriever = createRetriever(faviconUrl = null, failRequests = true)

        assertTrue(retriever.isStoredLogoStillAvailable("https://www.youtube.com/favicon.png"))
    }

    @Test
    fun `checks a shared logo url only once`() = runTest(testDispatcher) {
        var requestCount = 0
        val retriever = createRetriever(
            faviconUrl = null,
            responseStatus = HttpStatusCode.NotFound,
            onRequest = { requestCount++ },
        )
        val logoUrl = "https://www.youtube.com/s/desktop/old/favicon.png"

        assertFalse(retriever.isStoredLogoStillAvailable(logoUrl))
        assertFalse(retriever.isStoredLogoStillAvailable(logoUrl))
        assertEquals(expected = 1, actual = requestCount)
    }

    private fun channelWithImage(link: String?, imageUrl: String): RssChannel =
        RssChannelGenerator.rssChannel(
            title = "Feed",
            link = link,
            image = RssImage(title = null, url = imageUrl, link = null, description = null),
            items = emptyList(),
        )

    private fun createRetriever(
        faviconUrl: String?,
        responseStatus: HttpStatusCode = HttpStatusCode.OK,
        headStatus: HttpStatusCode? = null,
        failRequests: Boolean = false,
        onRequest: () -> Unit = {},
    ): FeedSourceLogoRetrieverImpl =
        FeedSourceLogoRetrieverImpl(
            htmlRetriever = HtmlRetriever(
                logger = testLogger,
                client = HttpClient(MockEngine) {
                    engine {
                        addHandler { request ->
                            onRequest()
                            if (failRequests) {
                                throw IOException("offline")
                            }
                            respond(
                                content = "<html></html>",
                                status = if (request.method == HttpMethod.Head) {
                                    headStatus ?: responseStatus
                                } else {
                                    responseStatus
                                },
                            )
                        }
                    }
                },
                forbiddenFallbackClient = unexpectedRequestHttpClient(),
            ),
            htmlParser = object : HtmlParser {
                override fun getTextFromHTML(html: String): String? = null

                override fun getFaviconUrl(html: String): String? = faviconUrl

                override fun getRssUrl(html: String): String? = null

                override fun parseFeedContent(html: String, baseUrl: String?): ParsedFeedContent =
                    ParsedFeedContent(text = null, commentsUrl = null)
            },
        )
}
