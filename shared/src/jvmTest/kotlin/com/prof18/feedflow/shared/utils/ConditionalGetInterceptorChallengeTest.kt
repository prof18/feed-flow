package com.prof18.feedflow.shared.utils

import co.touchlab.kermit.Logger
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ConditionalGetInterceptorChallengeTest {

    private val store = FeedHttpCacheStore(
        currentTimeMillis = { 0L },
        logger = Logger.withTag("ConditionalGetInterceptorChallengeTest"),
    )

    @Test
    fun `a challenged response is marked in the store`() {
        execute(challengeHeader = "challenge")

        assertNotNull(store.challengeFor(FEED_URL))
    }

    @Test
    fun `a plain forbidden response is not marked`() {
        execute(challengeHeader = null)

        assertNull(store.challengeFor(FEED_URL))
    }

    private fun execute(challengeHeader: String?) {
        val cannedResponse = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(403)
                .message("Forbidden")
                .apply { challengeHeader?.let { header("cf-mitigated", it) } }
                .body("".toResponseBody())
                .build()
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(ConditionalGetInterceptor(store))
            .addInterceptor(cannedResponse)
            .build()
        client.newCall(Request.Builder().url(FEED_URL).build()).execute().close()
    }

    private companion object {
        const val FEED_URL = "https://e.test/feed"
    }
}
