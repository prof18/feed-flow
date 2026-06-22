package com.prof18.feedflow.shared.domain.feed.httpcache

import kotlinx.datetime.format.DateTimeComponents
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Computes when a feed should be fetched again, based on the caching headers of the
 * last HTTP response. Ladder, first match wins:
 * 1. Cache-Control max-age
 * 2. Expires - Date
 * 3. RFC 9111 heuristic: 10% of the time elapsed since Last-Modified
 * 4. Default interval
 * The result is always clamped between [MIN_INTERVAL] and [MAX_INTERVAL],
 * so servers can only ever stretch the polling window, never tighten it.
 */
internal object FeedRefreshScheduler {

    fun computeNextFetchTimestamp(
        now: Long,
        responseInfo: FeedHttpResponseInfo?,
        fallbackLastModified: String? = null,
    ): Long {
        val explicitFreshness = responseInfo?.let { explicitFreshness(info = it, now = now) }
        val interval = explicitFreshness
            ?: heuristicFreshness(
                lastModified = responseInfo?.lastModified ?: fallbackLastModified,
                now = now,
            )
            ?: MIN_INTERVAL
        return now + interval.coerceIn(MIN_INTERVAL, MAX_INTERVAL).inWholeMilliseconds
    }

    fun computeBackoffTimestamp(now: Long, retryAfter: String?): Long? {
        if (retryAfter.isNullOrBlank()) {
            return null
        }
        val retryAfterSeconds = retryAfter.trim().toLongOrNull()
        val until = if (retryAfterSeconds != null) {
            retryAfterSeconds.seconds
        } else {
            val untilMillis = parseHttpDateMillis(retryAfter) ?: return null
            (untilMillis - now).milliseconds
        }
        if (until <= Duration.ZERO) {
            return null
        }
        return now + until.coerceAtMost(MAX_INTERVAL).inWholeMilliseconds
    }

    fun shouldUseValidators(now: Long, validatorsTimestamp: Long?): Boolean {
        val timestamp = validatorsTimestamp ?: return false
        return (now - timestamp).milliseconds <= VALIDATOR_MAX_AGE
    }

    private fun explicitFreshness(info: FeedHttpResponseInfo, now: Long): Duration? {
        val maxAgeSeconds = parseMaxAgeSeconds(info.cacheControl)
        if (maxAgeSeconds != null) {
            return maxAgeSeconds.seconds
        }
        val expiresMillis = parseHttpDateMillis(info.expires) ?: return null
        val baseMillis = parseHttpDateMillis(info.date) ?: now
        return (expiresMillis - baseMillis).milliseconds
    }

    private fun parseMaxAgeSeconds(cacheControl: String?): Long? {
        if (cacheControl == null) {
            return null
        }
        return MAX_AGE_REGEX.find(cacheControl.lowercase())
            ?.groupValues
            ?.get(1)
            ?.toLongOrNull()
    }

    private fun heuristicFreshness(lastModified: String?, now: Long): Duration? {
        val lastModifiedMillis = parseHttpDateMillis(lastModified) ?: return null
        val age = now - lastModifiedMillis
        if (age <= 0) {
            return null
        }
        return age.milliseconds / HEURISTIC_FRESHNESS_DIVIDER
    }

    private fun parseHttpDateMillis(value: String?): Long? {
        if (value.isNullOrBlank()) {
            return null
        }
        return runCatching {
            DateTimeComponents.Formats.RFC_1123.parse(value.trim())
                .toInstantUsingOffset()
                .toEpochMilliseconds()
        }.getOrNull()
    }

    val MIN_INTERVAL = 1.hours
    val VALIDATOR_MAX_AGE = 8.days
    private val MAX_INTERVAL = 1.days
    private const val HEURISTIC_FRESHNESS_DIVIDER = 10
    private val MAX_AGE_REGEX = Regex("""(?:^|[,\s])max-age\s*=\s*"?(\d+)""")
}
