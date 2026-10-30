package com.prof18.feedflow.shared.domain.recovery

import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.NSData
import platform.Foundation.NSUUID
import platform.posix.memcpy

/** Sends one request with a website's saved identity through the Swift bridge. */
internal class IosWebsiteSessionHttp(
    private val bridge: IosWebsiteSessionBridge,
    private val cacheStore: FeedHttpCacheStore,
) : WebsiteSessionHttp {

    override suspend fun fetch(request: WebsiteSessionRequest): WebsiteSessionResponse {
        if (RecoveryOrigin.of(request.url)?.value != request.profile.origin) {
            return WebsiteSessionResponse.Failure(WebsiteSessionFailure.NoIdentity)
        }
        val feedCacheKey = request.cacheKey?.takeIf { request.kind == WebsiteSessionKind.Feed }
        val validators = feedCacheKey?.let(cacheStore::validatorsFor)
        val requestId = NSUUID().UUIDString
        val result = try {
            withTimeoutOrNull(RecoveryPolicy.SESSION_REQUEST_TIMEOUT_MILLIS) {
                bridge.fetch(
                    requestId = requestId,
                    url = request.url,
                    profileId = request.profile.id,
                    userAgent = request.profile.userAgent,
                    accept = if (request.kind == WebsiteSessionKind.Feed) FEED_ACCEPT else HTML_ACCEPT,
                    etag = validators?.etag,
                    lastModified = validators?.lastModified,
                )
            } ?: return WebsiteSessionResponse.Failure(WebsiteSessionFailure.Timeout)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return WebsiteSessionResponse.Failure(WebsiteSessionFailure.Transport)
        } finally {
            bridge.cancel(requestId)
        }
        result.failure?.let { return WebsiteSessionResponse.Failure(failureOf(it)) }

        feedCacheKey?.let { key ->
            cacheStore.recordResponse(
                url = key,
                statusCode = result.statusCode,
                etag = result.etag,
                lastModified = result.lastModified,
                cacheControl = result.cacheControl,
                expires = result.expires,
                date = result.date,
                retryAfter = result.retryAfter,
            )
        }
        return when {
            result.challenge -> {
                feedCacheKey?.let { cacheStore.markChallenge(it, request.url) }
                WebsiteSessionResponse.Challenge(result.statusCode, result.retryAfter)
            }
            result.statusCode == HTTP_NOT_MODIFIED -> WebsiteSessionResponse.NotModified
            result.statusCode in HTTP_REDIRECTS -> WebsiteSessionResponse.Failure(WebsiteSessionFailure.Redirect)
            result.statusCode !in HTTP_SUCCESS -> WebsiteSessionResponse.Failure(WebsiteSessionFailure.Http)
            result.data.length > RecoveryPolicy.MAX_BODY_BYTES.toULong() ->
                WebsiteSessionResponse.Failure(WebsiteSessionFailure.TooLarge)
            else -> WebsiteSessionResponse.Body(result.data.toRecoveryBytes(), result.contentType, request.url)
        }
    }

    private fun failureOf(reason: String): WebsiteSessionFailure = when (reason) {
        "no-identity" -> WebsiteSessionFailure.NoIdentity
        "redirect" -> WebsiteSessionFailure.Redirect
        "too-large" -> WebsiteSessionFailure.TooLarge
        "timeout" -> WebsiteSessionFailure.Timeout
        else -> WebsiteSessionFailure.Transport
    }

    private companion object {
        const val HTTP_NOT_MODIFIED = 304
        val HTTP_REDIRECTS = 300..399
        val HTTP_SUCCESS = 200..299
        const val FEED_ACCEPT =
            "application/rss+xml, application/atom+xml, application/xml;q=0.9, text/xml;q=0.8, */*;q=0.5"
        const val HTML_ACCEPT = "text/html, application/xhtml+xml"
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toRecoveryBytes(): ByteArray {
    require(length <= RecoveryPolicy.MAX_BODY_BYTES.toULong())
    return ByteArray(length.toInt()).also { body ->
        if (body.isNotEmpty()) body.usePinned { memcpy(it.addressOf(0), bytes, length) }
    }
}
