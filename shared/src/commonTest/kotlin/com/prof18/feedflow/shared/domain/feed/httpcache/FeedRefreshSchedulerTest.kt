package com.prof18.feedflow.shared.domain.feed.httpcache

import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class FeedRefreshSchedulerTest {

    private val now = 1_750_000_000_000L

    private fun responseInfo(
        statusCode: Int = 200,
        etag: String? = null,
        lastModified: String? = null,
        cacheControl: String? = null,
        expires: String? = null,
        date: String? = null,
        retryAfter: String? = null,
    ) = FeedHttpResponseInfo(
        statusCode = statusCode,
        etag = etag,
        lastModified = lastModified,
        cacheControl = cacheControl,
        expires = expires,
        date = date,
        retryAfter = retryAfter,
    )

    @Test
    fun `max-age longer than the floor is honored`() {
        val nextFetch = FeedRefreshScheduler.computeNextFetchTimestamp(
            now = now,
            responseInfo = responseInfo(cacheControl = "public, max-age=7200"),
        )
        assertEquals(now + 2.hours.inWholeMilliseconds, nextFetch)
    }

    @Test
    fun `short CDN max-age is clamped up to the floor`() {
        val nextFetch = FeedRefreshScheduler.computeNextFetchTimestamp(
            now = now,
            responseInfo = responseInfo(cacheControl = "max-age=300, must-revalidate"),
        )
        assertEquals(now + 1.hours.inWholeMilliseconds, nextFetch)
    }

    @Test
    fun `max-age zero falls back to the floor`() {
        val nextFetch = FeedRefreshScheduler.computeNextFetchTimestamp(
            now = now,
            responseInfo = responseInfo(cacheControl = "no-cache, no-store, max-age=0, must-revalidate"),
        )
        assertEquals(now + 1.hours.inWholeMilliseconds, nextFetch)
    }

    @Test
    fun `huge max-age is clamped to the ceiling`() {
        val nextFetch = FeedRefreshScheduler.computeNextFetchTimestamp(
            now = now,
            responseInfo = responseInfo(cacheControl = "max-age=604800"),
        )
        assertEquals(now + 1.days.inWholeMilliseconds, nextFetch)
    }

    @Test
    fun `expires minus date is used when max-age is missing`() {
        val nextFetch = FeedRefreshScheduler.computeNextFetchTimestamp(
            now = now,
            responseInfo = responseInfo(
                expires = "Mon, 15 Jun 2026 16:00:00 GMT",
                date = "Mon, 15 Jun 2026 14:00:00 GMT",
            ),
        )
        assertEquals(now + 2.hours.inWholeMilliseconds, nextFetch)
    }

    @Test
    fun `heuristic uses ten percent of last modified age`() {
        val lastModifiedMillis = now - 20.hours.inWholeMilliseconds
        val lastModified = httpDate(lastModifiedMillis)
        val nextFetch = FeedRefreshScheduler.computeNextFetchTimestamp(
            now = now,
            responseInfo = responseInfo(lastModified = lastModified),
        )
        assertEquals(now + 2.hours.inWholeMilliseconds, nextFetch)
    }

    @Test
    fun `heuristic from stored last modified is clamped to the ceiling for dormant feeds`() {
        val lastModifiedMillis = now - 400.days.inWholeMilliseconds
        val nextFetch = FeedRefreshScheduler.computeNextFetchTimestamp(
            now = now,
            responseInfo = null,
            fallbackLastModified = httpDate(lastModifiedMillis),
        )
        assertEquals(now + 1.days.inWholeMilliseconds, nextFetch)
    }

    @Test
    fun `no caching information falls back to the floor`() {
        val nextFetch = FeedRefreshScheduler.computeNextFetchTimestamp(
            now = now,
            responseInfo = responseInfo(),
        )
        assertEquals(now + 1.hours.inWholeMilliseconds, nextFetch)
    }

    @Test
    fun `unparsable headers fall back to the floor`() {
        val nextFetch = FeedRefreshScheduler.computeNextFetchTimestamp(
            now = now,
            responseInfo = responseInfo(
                cacheControl = "private; bogus",
                expires = "-1",
                lastModified = "not a date",
            ),
        )
        assertEquals(now + 1.hours.inWholeMilliseconds, nextFetch)
    }

    @Test
    fun `retry after in seconds builds a backoff`() {
        val backoff = FeedRefreshScheduler.computeBackoffTimestamp(now = now, retryAfter = "120")
        assertEquals(now + 120.seconds.inWholeMilliseconds, backoff)
    }

    @Test
    fun `retry after as http date builds a backoff`() {
        val backoff = FeedRefreshScheduler.computeBackoffTimestamp(
            now = now,
            retryAfter = httpDate(now + 2.hours.inWholeMilliseconds),
        )
        assertEquals(now + 2.hours.inWholeMilliseconds, backoff)
    }

    @Test
    fun `retry after is clamped to the ceiling`() {
        val backoff = FeedRefreshScheduler.computeBackoffTimestamp(
            now = now,
            retryAfter = 10.days.inWholeSeconds.toString(),
        )
        assertEquals(now + 1.days.inWholeMilliseconds, backoff)
    }

    @Test
    fun `invalid or past retry after yields no backoff`() {
        assertNull(FeedRefreshScheduler.computeBackoffTimestamp(now = now, retryAfter = null))
        assertNull(FeedRefreshScheduler.computeBackoffTimestamp(now = now, retryAfter = "garbage"))
        assertNull(
            FeedRefreshScheduler.computeBackoffTimestamp(
                now = now,
                retryAfter = httpDate(now - 1.hours.inWholeMilliseconds),
            ),
        )
    }

    private fun httpDate(epochMillis: Long): String =
        Instant.fromEpochMilliseconds(epochMillis)
            .format(DateTimeComponents.Formats.RFC_1123, UtcOffset.ZERO)
}
