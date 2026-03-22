package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.testLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun `keeps absolute favicon urls unchanged`() = runTest(testDispatcher) {
        val retriever = createRetriever(faviconUrl = "https://cdn.example.com/favicon.png")

        val result = retriever.getFeedSourceLogoUrl("https://example.com/feed.xml")

        assertEquals(
            expected = "https://cdn.example.com/favicon.png",
            actual = result,
        )
    }

    private fun createRetriever(faviconUrl: String?): FeedSourceLogoRetrieverImpl =
        FeedSourceLogoRetrieverImpl(
            htmlRetriever = HtmlRetriever(
                logger = testLogger,
                client = HttpClient(MockEngine) {
                    engine {
                        addHandler {
                            respond(
                                content = "<html></html>",
                                status = HttpStatusCode.OK,
                            )
                        }
                    }
                },
            ),
            htmlParser = object : HtmlParser {
                override fun getTextFromHTML(html: String): String? = null

                override fun getFaviconUrl(html: String): String? = faviconUrl

                override fun getRssUrl(html: String): String? = null
            },
        )
}
