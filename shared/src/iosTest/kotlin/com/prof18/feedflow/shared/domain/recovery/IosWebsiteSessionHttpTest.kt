package com.prof18.feedflow.shared.domain.recovery

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedRecoveryProfile
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSData
import platform.Foundation.create
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class IosWebsiteSessionHttpTest {

    private val cacheStore = FeedHttpCacheStore(currentTimeMillis = { 0L }, logger = Logger.withTag("IosSessionTest"))

    @Test
    fun `a challenge result is a challenge`() = runTest {
        val bridge = FakeBridge { result(status = 403, challenge = true, retryAfter = "30") }
        val http = IosWebsiteSessionHttp(bridge, cacheStore)

        assertEquals(WebsiteSessionResponse.Challenge(403, "30"), http.fetch(htmlRequest()))
    }

    @Test
    fun `a redirect failure maps to redirect`() = runTest {
        val http = IosWebsiteSessionHttp(FakeBridge { result(status = 302, failure = "redirect") }, cacheStore)

        assertEquals(WebsiteSessionResponse.Failure(WebsiteSessionFailure.Redirect), http.fetch(htmlRequest()))
    }

    @Test
    fun `a feed 304 is not modified and recorded under the cache key`() = runTest {
        val http = IosWebsiteSessionHttp(FakeBridge { result(status = 304) }, cacheStore)

        val response = http.fetch(WebsiteSessionRequest(PROFILE, "$ORIGIN/feed", WebsiteSessionKind.Feed, FEED_KEY))

        assertEquals(WebsiteSessionResponse.NotModified, response)
        assertEquals(304, cacheStore.responseFor(FEED_KEY)?.statusCode)
    }

    @Test
    fun `a 200 is a body with identical bytes`() = runTest {
        val bytes = "<article>fixture</article>".encodeToByteArray()
        val http = IosWebsiteSessionHttp(FakeBridge { result(status = 200, data = bytes) }, cacheStore)

        val body = assertIs<WebsiteSessionResponse.Body>(http.fetch(htmlRequest()))

        assertContentEquals(bytes, body.bytes)
        assertEquals("text/html", body.contentType)
    }

    @Test
    fun `a bridge that never answers times out`() = runTest {
        val http = IosWebsiteSessionHttp(FakeBridge { awaitCancellation() }, cacheStore)

        assertEquals(WebsiteSessionResponse.Failure(WebsiteSessionFailure.Timeout), http.fetch(htmlRequest()))
    }

    @Test
    fun `a timeout cancels the bridge request with the fetched id`() = runTest {
        val bridge = FakeBridge { awaitCancellation() }
        IosWebsiteSessionHttp(bridge, cacheStore).fetch(htmlRequest())

        assertEquals(1, bridge.fetchedIds.size)
        assertEquals(bridge.fetchedIds, bridge.cancelledIds)
    }

    @Test
    fun `a normal completion cancels the bridge request once`() = runTest {
        val bridge = FakeBridge { result(status = 200) }
        IosWebsiteSessionHttp(bridge, cacheStore).fetch(htmlRequest())

        assertEquals(1, bridge.fetchedIds.size)
        assertEquals(bridge.fetchedIds, bridge.cancelledIds)
    }

    @Test
    fun `another origin never calls the bridge`() = runTest {
        var calls = 0
        val http = IosWebsiteSessionHttp(
            FakeBridge {
                calls++
                result(status = 200)
            },
            cacheStore,
        )

        val response = http.fetch(htmlRequest(url = "https://elsewhere.test/article"))

        assertEquals(WebsiteSessionResponse.Failure(WebsiteSessionFailure.NoIdentity), response)
        assertEquals(0, calls)
    }

    private fun htmlRequest(url: String = "$ORIGIN/article") =
        WebsiteSessionRequest(PROFILE, url, WebsiteSessionKind.Html, cacheKey = null)

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun result(
        status: Int,
        data: ByteArray = ByteArray(0),
        challenge: Boolean = false,
        retryAfter: String? = null,
        failure: String? = null,
    ) = IosWebsiteSessionResult(
        data = if (data.isEmpty()) {
            NSData()
        } else {
            data.usePinned { NSData.create(bytes = it.addressOf(0), length = data.size.toULong()) }
        },
        statusCode = status,
        contentType = "text/html",
        etag = null,
        lastModified = null,
        cacheControl = null,
        expires = null,
        date = null,
        retryAfter = retryAfter,
        challenge = challenge,
        failure = failure,
    )

    private class FakeBridge(private val answer: suspend () -> IosWebsiteSessionResult) : IosWebsiteSessionBridge {
        val fetchedIds = mutableListOf<String>()
        val cancelledIds = mutableListOf<String>()

        override fun cancel(requestId: String) {
            cancelledIds += requestId
        }

        override suspend fun fetch(
            requestId: String,
            url: String,
            profileId: String,
            userAgent: String,
            accept: String,
            etag: String?,
            lastModified: String?,
        ): IosWebsiteSessionResult {
            fetchedIds += requestId
            return answer()
        }
    }

    private companion object {
        const val ORIGIN = "https://example.test"
        const val FEED_KEY = "https://example.test/feed"
        val PROFILE = FeedRecoveryProfile(
            id = "feedflow-recovery-1",
            accountEpoch = "epoch",
            origin = "$ORIGIN:443",
            userAgent = "agent",
            engineVersion = "engine",
            readerRetained = false,
            retired = false,
        )
    }
}
