package com.prof18.feedflow.shared.domain.feed.httpcache

import co.touchlab.kermit.Logger
import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.withLock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * In-memory bridge between the refresh pipeline (coroutines + database) and the
 * platform HTTP layers (OkHttp interceptor, iOS NSURLProtocol), which need
 * synchronous access to per-feed validators and captured response headers.
 */
class FeedHttpCacheStore(
    private val currentTimeMillis: () -> Long,
    private val logger: Logger,
) {
    private val lock = Lock()
    private val validators = mutableMapOf<String, FeedHttpValidators>()
    private val responses = mutableMapOf<String, FeedHttpResponseInfo>()
    private val lastSuccessTimestamps = mutableMapOf<String, Long>()

    fun seedValidators(urlToValidators: Map<String, FeedHttpValidators>): Unit = lock.withLock {
        validators.clear()
        responses.clear()
        lastSuccessTimestamps.clear()
        validators.putAll(urlToValidators)
    }

    fun validatorsFor(url: String): FeedHttpValidators? = lock.withLock {
        val lastSuccess = lastSuccessTimestamps[url]
        if (lastSuccess != null && (currentTimeMillis() - lastSuccess).milliseconds < RETRY_GUARD) {
            // The parser re-downloads the feed right after a 200 when the XML is malformed;
            // sending validators there would turn the recovery attempt into a useless 304
            logger.d { "Suppressing validators for $url: 200 received moments ago (malformed-XML retry guard)" }
            return@withLock null
        }
        val urlValidators = validators[url]
        if (urlValidators != null) {
            logger.d {
                "Conditional GET for $url: " +
                    "If-None-Match=${urlValidators.etag}, If-Modified-Since=${urlValidators.lastModified}"
            }
        }
        urlValidators
    }

    fun recordResponse(
        url: String,
        statusCode: Int,
        etag: String?,
        lastModified: String?,
        cacheControl: String?,
        expires: String?,
        date: String?,
        retryAfter: String?,
    ): Unit = lock.withLock {
        responses[url] = FeedHttpResponseInfo(
            statusCode = statusCode,
            etag = etag,
            lastModified = lastModified,
            cacheControl = cacheControl,
            expires = expires,
            date = date,
            retryAfter = retryAfter,
        )
        logger.d {
            buildString {
                append("Response for $url: status=$statusCode")
                etag?.let { append(", etag=$it") }
                lastModified?.let { append(", lastModified=$it") }
                cacheControl?.let { append(", cacheControl=$it") }
                expires?.let { append(", expires=$it") }
                retryAfter?.let { append(", retryAfter=$it") }
            }
        }
        if (statusCode in HTTP_SUCCESS_RANGE) {
            lastSuccessTimestamps[url] = currentTimeMillis()
            // Always replace the validators on a successful response, even when the body
            // looks unchanged or the new values are null: reusing stale validators is the
            // classic conditional GET bug
            validators[url] = FeedHttpValidators(etag = etag, lastModified = lastModified)
        }
    }

    fun responseFor(url: String): FeedHttpResponseInfo? = lock.withLock { responses[url] }

    fun storedValidatorsFor(url: String): FeedHttpValidators? = lock.withLock { validators[url] }

    private companion object {
        val RETRY_GUARD = 30.seconds
        val HTTP_SUCCESS_RANGE = 200..299
    }
}
