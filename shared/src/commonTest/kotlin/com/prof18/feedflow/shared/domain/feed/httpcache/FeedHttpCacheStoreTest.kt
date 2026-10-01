package com.prof18.feedflow.shared.domain.feed.httpcache

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedFetchTier
import com.prof18.feedflow.core.model.FeedSourceCacheInfo
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

    @Test
    fun `tiers seed and successful fetch overwrites the remembered value`() {
        store.seedTiers(mapOf(feedUrl to FeedFetchTier.LINK_FREE))
        assertEquals(FeedFetchTier.LINK_FREE, store.tierFor(feedUrl))
        store.recordSuccessfulTier(feedUrl, FeedFetchTier.BROWSER)
        assertEquals(FeedFetchTier.BROWSER, store.tierFor(feedUrl))
    }

    @Test
    fun `tier and validator seeds are independent and preserve newly added feeds`() {
        val addedUrl = "https://new.example/feed"
        store.seedValidators(mapOf(feedUrl to FeedHttpValidators("etag", null)))
        store.recordSuccessfulTier(addedUrl, FeedFetchTier.BROWSER)
        store.seedTiers(mapOf(feedUrl to FeedFetchTier.PRIMARY))
        assertEquals("etag", store.validatorsFor(feedUrl)?.etag)
        assertEquals(FeedFetchTier.BROWSER, store.tierFor(addedUrl))
        store.seedValidators(emptyMap())
        store.seedTiers(emptyMap())
        assertEquals(FeedFetchTier.BROWSER, store.tierFor(addedUrl))
        assertEquals(FeedFetchTier.PRIMARY, store.tierFor(feedUrl))
    }

    @Test
    fun `failed fetch preserves previous tier and not modified falls back to it`() {
        val previous = FeedSourceCacheInfo(
            feedSourceId = "id",
            etag = null,
            lastModified = null,
            validatorsTimestamp = null,
            nextFetchTimestamp = null,
            backoffTimestamp = null,
            userAgentTier = FeedFetchTier.LINK_FREE,
        )
        store.recordSuccessfulTier(feedUrl, FeedFetchTier.BROWSER)
        val failed = createCacheInfo(fetchSucceeded = false, previous = previous)
        assertEquals(FeedFetchTier.LINK_FREE, failed.userAgentTier)
        val freshStore = FeedHttpCacheStore({ nowMillis }, Logger.withTag("CacheFactoryTest"))
        val notModified = createCacheInfo(fetchSucceeded = true, previous = previous, cacheStore = freshStore)
        assertEquals(FeedFetchTier.LINK_FREE, notModified.userAgentTier)
        assertEquals(FeedFetchTier.BROWSER, createCacheInfo(true, previous).userAgentTier)
    }

    private fun createCacheInfo(
        fetchSucceeded: Boolean,
        previous: FeedSourceCacheInfo,
        cacheStore: FeedHttpCacheStore = store,
    ) = FeedSourceCacheInfoFactory.create(
        store = cacheStore,
        feedSourceId = "id",
        feedUrl = feedUrl,
        fetchSucceeded = fetchSucceeded,
        refreshValidatorsTimestamp = false,
        previousCacheInfo = previous,
        now = nowMillis,
        logger = Logger.withTag("CacheFactoryTest"),
    )
}
