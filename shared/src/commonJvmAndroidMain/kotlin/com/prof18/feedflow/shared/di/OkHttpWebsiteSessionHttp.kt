package com.prof18.feedflow.shared.di

import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import com.prof18.feedflow.shared.domain.recovery.RecoveryOrigin
import com.prof18.feedflow.shared.domain.recovery.RecoveryPolicy
import com.prof18.feedflow.shared.domain.recovery.WebsiteIdentity
import com.prof18.feedflow.shared.domain.recovery.WebsiteIdentityProvider
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionFailure
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionHttp
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionKind
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionRequest
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Sends one request with a website's saved identity. Feeds and reader articles share it. */
internal class OkHttpWebsiteSessionHttp(
    private val identityProvider: WebsiteIdentityProvider,
    private val cacheStore: FeedHttpCacheStore,
    baseClient: () -> OkHttpClient = ::OkHttpClient,
) : WebsiteSessionHttp {
    // Built on first request: Koin resolves this adapter at startup to compute availability.
    private val client by lazy {
        baseClient().newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .cookieJar(CookieJar.NO_COOKIES)
            .cache(null)
            .callTimeout(RecoveryPolicy.SESSION_REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .build()
    }

    override suspend fun fetch(request: WebsiteSessionRequest): WebsiteSessionResponse {
        if (RecoveryOrigin.of(request.url)?.value != request.profile.origin) {
            return WebsiteSessionResponse.Failure(WebsiteSessionFailure.NoIdentity)
        }
        val identity = try {
            identityProvider.identity(request.profile, request.url)?.validated()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return WebsiteSessionResponse.Failure(WebsiteSessionFailure.NoIdentity)
        val call = client.newCall(httpRequest(request, identity))
        return try {
            call.await().use { response -> withContext(Dispatchers.IO) { map(request, response) } }
        } catch (e: InterruptedIOException) {
            val reason = if (e.message == "timeout") WebsiteSessionFailure.Timeout else WebsiteSessionFailure.Transport
            WebsiteSessionResponse.Failure(reason)
        } catch (_: IOException) {
            WebsiteSessionResponse.Failure(WebsiteSessionFailure.Transport)
        }
    }

    private fun httpRequest(request: WebsiteSessionRequest, identity: WebsiteIdentity): Request {
        val validators = request.cacheKey
            ?.takeIf { request.kind == WebsiteSessionKind.Feed }
            ?.let(cacheStore::validatorsFor)
        return Request.Builder()
            .url(request.url)
            .header("User-Agent", identity.userAgent)
            .header("Accept", if (request.kind == WebsiteSessionKind.Feed) FEED_ACCEPT else HTML_ACCEPT)
            .apply {
                identity.clearanceCookie?.let { header("Cookie", it) }
                validators?.etag?.let { header("If-None-Match", it) }
                validators?.lastModified?.let { header("If-Modified-Since", it) }
            }
            .build()
    }

    private fun map(request: WebsiteSessionRequest, response: Response): WebsiteSessionResponse {
        val feedCacheKey = request.cacheKey?.takeIf { request.kind == WebsiteSessionKind.Feed }
        feedCacheKey?.let { key ->
            cacheStore.recordResponse(
                url = key,
                statusCode = response.code,
                etag = response.header("ETag"),
                lastModified = response.header("Last-Modified"),
                cacheControl = response.header("Cache-Control"),
                expires = response.header("Expires"),
                date = response.header("Date"),
                retryAfter = response.header("Retry-After"),
            )
        }
        return when {
            RecoveryPolicy.isChallenge(response.header(RecoveryPolicy.CHALLENGE_HEADER)) -> {
                feedCacheKey?.let { cacheStore.markChallenge(it, request.url) }
                WebsiteSessionResponse.Challenge(response.code, response.header("Retry-After"))
            }
            response.code == HTTP_NOT_MODIFIED -> WebsiteSessionResponse.NotModified
            response.code in HTTP_REDIRECTS -> WebsiteSessionResponse.Failure(WebsiteSessionFailure.Redirect)
            !response.isSuccessful -> WebsiteSessionResponse.Failure(WebsiteSessionFailure.Http)
            response.body.contentLength() > RecoveryPolicy.MAX_BODY_BYTES ->
                WebsiteSessionResponse.Failure(WebsiteSessionFailure.TooLarge)
            else -> readBody(response)?.let { bytes ->
                WebsiteSessionResponse.Body(bytes, response.header("Content-Type"), request.url)
            } ?: WebsiteSessionResponse.Failure(WebsiteSessionFailure.TooLarge)
        }
    }

    /** Returns null when the body grows past [RecoveryPolicy.MAX_BODY_BYTES]. */
    private fun readBody(response: Response): ByteArray? {
        val source = response.body.source()
        val buffer = Buffer()
        while (true) {
            val read = source.read(buffer, RecoveryPolicy.TRANSFER_CHUNK_BYTES.toLong())
            if (read == -1L) break
            if (buffer.size > RecoveryPolicy.MAX_BODY_BYTES) return null
        }
        return buffer.readByteArray()
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response) { _, value, _ -> value.close() }
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            },
        )
    }

    private companion object {
        const val HTTP_NOT_MODIFIED = 304
        val HTTP_REDIRECTS = 300..399
        const val FEED_ACCEPT =
            "application/rss+xml, application/atom+xml, application/xml;q=0.9, text/xml;q=0.8, */*;q=0.5"
        const val HTML_ACCEPT = "text/html, application/xhtml+xml"
        const val ASCII_SPACE = 32
        const val ASCII_LAST = 126

        /** Keeps only a printable user agent and the single clearance cookie; null when invalid. */
        fun WebsiteIdentity.validated(): WebsiteIdentity? {
            if (userAgent.isBlank() || !userAgent.isPrintableAscii()) return null
            val cookies = clearanceCookie ?: return WebsiteIdentity(userAgent, null)
            if (!cookies.isPrintableAscii()) return null
            val clearance = cookies.split(';').map(String::trim).filter {
                it.substringBefore('=') == RecoveryPolicy.REPLAY_COOKIE
            }
            if (clearance.size > 1) return null
            val selected = clearance.singleOrNull()
                ?.takeIf { '=' in it && it.substringAfter('=').isNotBlank() }
            if (clearance.isNotEmpty() && selected == null) return null
            return WebsiteIdentity(userAgent, selected)
        }

        private fun String.isPrintableAscii(): Boolean = all { it.code in ASCII_SPACE..ASCII_LAST }
    }
}
