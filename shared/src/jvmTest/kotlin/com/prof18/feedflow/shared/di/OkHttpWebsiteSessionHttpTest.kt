package com.prof18.feedflow.shared.di

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedRecoveryProfile
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpValidators
import com.prof18.feedflow.shared.domain.recovery.RecoveryPolicy
import com.prof18.feedflow.shared.domain.recovery.WebsiteIdentity
import com.prof18.feedflow.shared.domain.recovery.WebsiteIdentityProvider
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionFailure
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionKind
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionRequest
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OkHttpWebsiteSessionHttpTest {

    private val cacheStore = FeedHttpCacheStore(currentTimeMillis = { 0L }, logger = Logger.withTag("SessionTest"))

    @Test
    fun `only the clearance cookie and the approved user agent are sent`() = runBlocking {
        val requests = Collections.synchronizedList(mutableListOf<Request>())
        val http = http(identity { WebsiteIdentity("approved-agent", "cf_clearance=approved; other=private") }) {
            requests += it
            response(it)
        }

        assertIs<WebsiteSessionResponse.Body>(http.fetch(htmlRequest("$ORIGIN/articles/one")))
        assertIs<WebsiteSessionResponse.Body>(http.fetch(htmlRequest("$ORIGIN/articles/two")))

        assertEquals(2, requests.size)
        assertTrue(requests.all { it.header("User-Agent") == "approved-agent" })
        assertTrue(requests.all { it.header("Cookie") == "cf_clearance=approved" })
    }

    @Test
    fun `marked 200 and 403 responses are challenges with retry after`() = runBlocking {
        for (status in listOf(200, 403)) {
            val http = http(identity()) {
                response(it, status, mapOf(RecoveryPolicy.CHALLENGE_HEADER to "challenge", "Retry-After" to "45"))
            }

            assertEquals(WebsiteSessionResponse.Challenge(status, "45"), http.fetch(htmlRequest("$ORIGIN/article")))
        }
    }

    @Test
    fun `a redirect is not followed`() = runBlocking {
        val calls = AtomicInteger()
        val http = http(identity()) {
            calls.incrementAndGet()
            response(it, 302, mapOf("Location" to "https://elsewhere.test/article"))
        }

        assertEquals(
            WebsiteSessionResponse.Failure(WebsiteSessionFailure.Redirect),
            http.fetch(htmlRequest("$ORIGIN/article")),
        )
        assertEquals(1, calls.get())
    }

    @Test
    fun `oversized declared and streamed bodies are too large`() = runBlocking {
        val oversized = ByteArray(RecoveryPolicy.MAX_BODY_BYTES + 1)
        val declared = http(identity()) { response(it, body = oversized) }
        val streamed = http(identity()) {
            val body = oversized.toResponseBody()
            response(it).newBuilder().body(
                object : ResponseBody() {
                    override fun contentType() = body.contentType()
                    override fun contentLength() = -1L
                    override fun source() = body.source()
                },
            ).build()
        }

        val tooLarge = WebsiteSessionResponse.Failure(WebsiteSessionFailure.TooLarge)
        assertEquals(tooLarge, declared.fetch(htmlRequest("$ORIGIN/article")))
        assertEquals(tooLarge, streamed.fetch(htmlRequest("$ORIGIN/article")))
    }

    @Test
    fun `missing identity or another origin sends no request`() = runBlocking {
        val calls = AtomicInteger()
        val noIdentity = http(identity { null }) {
            calls.incrementAndGet()
            response(it)
        }
        val otherOrigin = http(identity()) {
            calls.incrementAndGet()
            response(it)
        }

        val failure = WebsiteSessionResponse.Failure(WebsiteSessionFailure.NoIdentity)
        assertEquals(failure, noIdentity.fetch(htmlRequest("$ORIGIN/article")))
        assertEquals(failure, otherOrigin.fetch(htmlRequest("https://elsewhere.test/article")))
        assertEquals(0, calls.get())
    }

    @Test
    fun `cancellation cancels the call`() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val http = http(identity()) {
            entered.countDown()
            release.await(5, TimeUnit.SECONDS)
            response(it)
        }

        val result = withTimeoutOrNull(500) { http.fetch(htmlRequest("$ORIGIN/article")) }
        release.countDown()

        assertTrue(entered.await(1, TimeUnit.SECONDS))
        assertNull(result)
    }

    @Test
    fun `parallel requests keep their identities isolated`() = runBlocking {
        val seen = Collections.synchronizedList(mutableListOf<Pair<String, String?>>())
        val http = http(identity { WebsiteIdentity("agent-${it.id}", "cf_clearance=${it.id}") }) {
            seen += it.header("User-Agent").orEmpty() to it.header("Cookie")
            response(it)
        }

        coroutineScope {
            listOf("a", "b", "c").map { id ->
                async { http.fetch(htmlRequest("$ORIGIN/$id", PROFILE.copy(id = id))) }
            }.awaitAll()
        }

        assertEquals(
            setOf("agent-a" to "cf_clearance=a", "agent-b" to "cf_clearance=b", "agent-c" to "cf_clearance=c"),
            seen.toSet(),
        )
    }

    @Test
    fun `a feed request sends stored validators and maps 304`() = runBlocking {
        cacheStore.seedValidators(mapOf(FEED_KEY to FeedHttpValidators(etag = "\"v1\"", lastModified = "lm")))
        val requests = mutableListOf<Request>()
        val http = http(identity()) {
            requests += it
            response(it, 304)
        }

        val result = http.fetch(
            WebsiteSessionRequest(PROFILE, "$ORIGIN/feed?verified", WebsiteSessionKind.Feed, cacheKey = FEED_KEY),
        )

        assertEquals(WebsiteSessionResponse.NotModified, result)
        assertEquals("\"v1\"", requests.single().header("If-None-Match"))
        assertEquals("lm", requests.single().header("If-Modified-Since"))
        assertEquals(304, cacheStore.responseFor(FEED_KEY)?.statusCode)
    }

    @Test
    fun `an html request sends no validators`() = runBlocking {
        cacheStore.seedValidators(mapOf("$ORIGIN/article" to FeedHttpValidators(etag = "\"v1\"", lastModified = "lm")))
        val requests = mutableListOf<Request>()
        val http = http(identity()) {
            requests += it
            response(it)
        }

        val body = assertIs<WebsiteSessionResponse.Body>(http.fetch(htmlRequest("$ORIGIN/article")))

        assertNull(requests.single().header("If-None-Match"))
        assertNull(requests.single().header("If-Modified-Since"))
        assertContentEquals(BODY, body.bytes)
    }

    private fun http(provider: WebsiteIdentityProvider, fixture: (Request) -> Response) = OkHttpWebsiteSessionHttp(
        identityProvider = provider,
        cacheStore = cacheStore,
        baseClient = { OkHttpClient.Builder().addInterceptor { chain -> fixture(chain.request()) }.build() },
    )

    @Test
    fun `an identity provider that throws is no identity and sends nothing`() = runBlocking {
        val calls = AtomicInteger()
        val http = http(identity { error("profile store failed") }) {
            calls.incrementAndGet()
            response(it)
        }

        assertEquals(
            WebsiteSessionResponse.Failure(WebsiteSessionFailure.NoIdentity),
            http.fetch(htmlRequest("$ORIGIN/article")),
        )
        assertEquals(0, calls.get())
    }

    private fun identity(
        provide: (FeedRecoveryProfile) -> WebsiteIdentity? = { WebsiteIdentity("approved-agent", "cf_clearance=ok") },
    ) = object : WebsiteIdentityProvider {
        override suspend fun identity(profile: FeedRecoveryProfile, url: String): WebsiteIdentity? = provide(profile)
    }

    private fun htmlRequest(url: String, profile: FeedRecoveryProfile = PROFILE) =
        WebsiteSessionRequest(profile, url, WebsiteSessionKind.Html, cacheKey = null)

    private fun response(
        request: Request,
        status: Int = 200,
        headers: Map<String, String> = emptyMap(),
        body: ByteArray = BODY,
    ): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(status)
        .message("fixture")
        .apply { headers.forEach { (name, value) -> addHeader(name, value) } }
        .body(body.toResponseBody())
        .build()

    private companion object {
        const val ORIGIN = "https://example.test"
        const val FEED_KEY = "https://example.test/feed"
        val BODY = "<article>fixture</article>".encodeToByteArray()
        val PROFILE = FeedRecoveryProfile(
            id = "profile",
            accountEpoch = "epoch",
            origin = "$ORIGIN:443",
            userAgent = "browser-agent",
            engineVersion = "browser-engine",
            readerRetained = false,
            retired = false,
        )
    }
}
