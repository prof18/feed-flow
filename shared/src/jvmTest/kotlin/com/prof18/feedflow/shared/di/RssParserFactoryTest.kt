package com.prof18.feedflow.shared.di

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.FEEDFLOW_FALLBACK_USER_AGENT
import com.prof18.feedflow.core.utils.FEEDFLOW_USER_AGENT
import com.prof18.feedflow.shared.domain.feed.RssParserWrapperImpl
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import java.net.InetSocketAddress
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals

class RssParserFactoryTest {

    @Test
    fun `forbidden response retries with configured fallback user agent`() = runTest {
        val receivedUserAgents = Collections.synchronizedList(mutableListOf<String>())
        val server = HttpServer.create(InetSocketAddress(LOOPBACK_ADDRESS, 0), 0).apply {
            createContext(FEED_PATH) { exchange ->
                val userAgent = exchange.requestHeaders.getFirst("User-Agent")
                receivedUserAgents.add(userAgent)

                when (userAgent) {
                    FEEDFLOW_USER_AGENT -> exchange.sendResponseHeaders(HTTP_FORBIDDEN, NO_RESPONSE_BODY)
                    FEEDFLOW_FALLBACK_USER_AGENT -> {
                        val responseBody = RSS_FEED.toByteArray()
                        exchange.sendResponseHeaders(HTTP_OK, responseBody.size.toLong())
                        exchange.responseBody.use { it.write(responseBody) }
                    }
                    else -> exchange.sendResponseHeaders(HTTP_INTERNAL_SERVER_ERROR, NO_RESPONSE_BODY)
                }
                exchange.close()
            }
            start()
        }

        try {
            val cacheStore = FeedHttpCacheStore(
                currentTimeMillis = { 0L },
                logger = Logger.withTag("RssParserFactoryTest"),
            )
            val wrapper = RssParserWrapperImpl(
                primaryParser = createRssParser(
                    userAgent = FEEDFLOW_USER_AGENT,
                    feedHttpCacheStore = cacheStore,
                )::getRssChannel,
                forbiddenFallbackParser = createRssParser(
                    userAgent = FEEDFLOW_FALLBACK_USER_AGENT,
                    feedHttpCacheStore = cacheStore,
                )::getRssChannel,
            )

            val channel = wrapper.getRssChannel("http://$LOOPBACK_ADDRESS:${server.address.port}$FEED_PATH")

            assertEquals("Fallback Feed", channel.title)
            assertEquals(
                listOf(FEEDFLOW_USER_AGENT, FEEDFLOW_FALLBACK_USER_AGENT),
                receivedUserAgents.toList(),
            )
        } finally {
            server.stop(0)
        }
    }

    private companion object {
        const val LOOPBACK_ADDRESS = "127.0.0.1"
        const val FEED_PATH = "/feed"
        const val HTTP_OK = 200
        const val HTTP_FORBIDDEN = 403
        const val HTTP_INTERNAL_SERVER_ERROR = 500
        const val NO_RESPONSE_BODY = -1L
        val RSS_FEED = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <title>Fallback Feed</title>
                    <link>https://example.com</link>
                    <description>Fallback test feed</description>
                </channel>
            </rss>
        """.trimIndent()
    }
}
