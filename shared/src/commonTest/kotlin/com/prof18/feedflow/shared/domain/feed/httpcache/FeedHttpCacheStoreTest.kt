package com.prof18.feedflow.shared.domain.feed.httpcache

import co.touchlab.kermit.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FeedHttpCacheStoreTest {

    private var nowMillis = 1_750_000_000_000L
    private val store = FeedHttpCacheStore(
        currentTimeMillis = { nowMillis },
        logger = Logger.withTag("FeedHttpCacheStoreTest"),
    )

    private val feedUrl = "https://example.com/feed.xml"

    @Test
    fun `seeded validators are returned for the url`() {
        store.seedValidators(
            mapOf(feedUrl to FeedHttpValidators(etag = "\"abc\"", lastModified = "lm-value")),
        )

        val validators = store.validatorsFor(feedUrl)

        assertEquals("\"abc\"", validators?.etag)
        assertEquals("lm-value", validators?.lastModified)
        assertNull(store.validatorsFor("https://other.com/feed.xml"))
    }

    @Test
    fun `successful response replaces validators`() {
        store.seedValidators(
            mapOf(feedUrl to FeedHttpValidators(etag = "\"old\"", lastModified = "old-lm")),
        )

        store.recordResponse(
            url = feedUrl,
            statusCode = 200,
            etag = "\"new\"",
            lastModified = null,
            cacheControl = null,
            expires = null,
            date = null,
            retryAfter = null,
        )

        nowMillis += 1.minutes.inWholeMilliseconds
        val validators = store.validatorsFor(feedUrl)
        assertEquals("\"new\"", validators?.etag)
        assertNull(validators?.lastModified)
    }

    @Test
    fun `not modified response keeps stored validators`() {
        store.seedValidators(
            mapOf(feedUrl to FeedHttpValidators(etag = "\"old\"", lastModified = "old-lm")),
        )

        store.recordResponse(
            url = feedUrl,
            statusCode = 304,
            etag = null,
            lastModified = null,
            cacheControl = "max-age=1800",
            expires = null,
            date = null,
            retryAfter = null,
        )

        val validators = store.validatorsFor(feedUrl)
        assertEquals("\"old\"", validators?.etag)
        assertEquals("old-lm", validators?.lastModified)
        assertEquals("max-age=1800", store.responseFor(feedUrl)?.cacheControl)
    }

    @Test
    fun `validators are suppressed right after a successful response`() {
        store.recordResponse(
            url = feedUrl,
            statusCode = 200,
            etag = "\"fresh\"",
            lastModified = null,
            cacheControl = null,
            expires = null,
            date = null,
            retryAfter = null,
        )

        // Within the guard window: the parser is likely re-downloading for the
        // malformed XML recovery, so no validators should be attached
        assertNull(store.validatorsFor(feedUrl))
        assertEquals("\"fresh\"", store.storedValidatorsFor(feedUrl)?.etag)

        nowMillis += 31.seconds.inWholeMilliseconds
        assertEquals("\"fresh\"", store.validatorsFor(feedUrl)?.etag)
    }

    @Test
    fun `seeding clears previous responses and validators`() {
        store.recordResponse(
            url = feedUrl,
            statusCode = 200,
            etag = "\"a\"",
            lastModified = null,
            cacheControl = "max-age=600",
            expires = null,
            date = null,
            retryAfter = null,
        )

        store.seedValidators(emptyMap())

        assertNull(store.responseFor(feedUrl))
        assertNull(store.validatorsFor(feedUrl))
    }

    @Test
    fun `recorded response is exposed`() {
        store.recordResponse(
            url = feedUrl,
            statusCode = 429,
            etag = null,
            lastModified = null,
            cacheControl = null,
            expires = null,
            date = null,
            retryAfter = "3600",
        )

        val response = store.responseFor(feedUrl)
        assertEquals(429, response?.statusCode)
        assertEquals("3600", response?.retryAfter)
    }
}
