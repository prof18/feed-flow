package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import com.prof18.feedflow.shared.domain.feed.FeedUrlRetriever
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.unexpectedRequestHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FeedUrlRetrieverTest : KoinTestBase() {
    private val channelId = "UCsBjURrPoezykLs9EqgamOA"
    private val feedUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
    private val requestedUrls = mutableListOf<String>()

    @Test
    fun `channel id resolves without fetching the page`() = runTest(testDispatcher) {
        val retriever = retriever("")
        assertEquals(feedUrl, retriever.getFeedUrl("https://www.youtube.com/channel/$channelId/videos?si=shared"))
        assertEquals(emptyList(), requestedUrls)
    }

    @Test
    fun `all channel aliases discover advertised RSS and strip tab and tracking`() = runTest(testDispatcher) {
        val retriever = retriever("""<link rel="alternate" type="application/rss+xml" href="$feedUrl">""")
        for (path in listOf("@Fireship", "c/Fireship", "user/Fireship")) {
            assertEquals(feedUrl, retriever.getFeedUrl("https://m.youtube.com/$path/videos?si=shared"))
        }
        assertEquals(
            listOf("@Fireship", "c/Fireship", "user/Fireship").map { "https://www.youtube.com/$it" },
            requestedUrls,
        )
    }

    @Test
    fun `canonical channel URL recovers missing RSS metadata`() = runTest(testDispatcher) {
        val retriever = retriever("""<link href="https://www.youtube.com/channel/$channelId" rel="canonical">""")
        assertEquals(feedUrl, retriever.getFeedUrl("https://www.youtube.com/@Fireship"))
    }

    @Test
    fun `advertised RSS takes priority over canonical fallback`() = runTest(testDispatcher) {
        val retriever = retriever(
            """
                <link rel="alternate" type="application/rss+xml" href="$feedUrl">
                <link rel="canonical" href="https://www.youtube.com/channel/UCBJycsmduvYEL83R_U4JriQ">
            """.trimIndent(),
        )
        assertEquals(feedUrl, retriever.getFeedUrl("https://www.youtube.com/@Fireship"))
    }

    @Test
    fun `unrelated feed metadata falls back to canonical channel`() = runTest(testDispatcher) {
        val retriever = retriever(
            """
                <link rel="alternate" type="application/rss+xml" href="https://example.com/feed">
                <link rel="canonical" href="https://www.youtube.com/channel/$channelId">
            """.trimIndent(),
        )
        assertEquals(feedUrl, retriever.getFeedUrl("https://www.youtube.com/@Fireship"))
    }

    @Test
    fun `missing or unrelated canonical metadata cannot resolve a channel`() = runTest(testDispatcher) {
        for (html in listOf(
            "<html>Consent required</html>",
            """<link rel="canonical" href="https://example.com/channel/$channelId">""",
            """<link rel="canonical" href="https://www.youtube.com/@Fireship">""",
            """<link rel="canonical" href="https://www.youtube.com/channel/invalid">""",
        )) {
            assertNull(retriever(html).getFeedUrl("https://www.youtube.com/@Fireship"))
        }
    }

    @Test
    fun `unavailable channel does not resolve`() = runTest(testDispatcher) {
        assertNull(retriever("", HttpStatusCode.NotFound).getFeedUrl("https://www.youtube.com/@missing"))
    }

    @Test
    fun `ordinary websites retain generic RSS discovery`() = runTest(testDispatcher) {
        val retriever = retriever(
            """<link rel="alternate" type="application/atom+xml" href="https://example.com/atom.xml">""",
        )
        assertEquals("https://example.com/atom.xml", retriever.getFeedUrl("https://example.com/blog"))
        assertEquals(listOf("https://example.com/blog"), requestedUrls)
    }

    private fun retriever(html: String, status: HttpStatusCode = HttpStatusCode.OK): FeedUrlRetriever {
        val logger = Logger.withTag("FeedUrlRetrieverTest")
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    requestedUrls.add(request.url.toString())
                    respond(html, status, headersOf(HttpHeaders.ContentType, "text/html"))
                }
            }
        }
        return FeedUrlRetriever(
            htmlParser = JvmHtmlParser(logger),
            htmlRetriever = HtmlRetriever(logger, client, unexpectedRequestHttpClient()),
        )
    }
}
