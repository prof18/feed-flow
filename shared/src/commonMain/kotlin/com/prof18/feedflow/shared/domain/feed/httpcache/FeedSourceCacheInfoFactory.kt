package com.prof18.feedflow.shared.domain.feed.httpcache

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedSourceCacheInfo
import kotlin.time.Duration.Companion.milliseconds

internal object FeedSourceCacheInfoFactory {

    @Suppress("LongParameterList")
    fun create(
        store: FeedHttpCacheStore,
        feedSourceId: String,
        feedUrl: String,
        fetchSucceeded: Boolean,
        previousCacheInfo: FeedSourceCacheInfo?,
        now: Long,
        logger: Logger,
    ): FeedSourceCacheInfo {
        val responseInfo = store.responseFor(feedUrl)
        return if (fetchSucceeded) {
            val validators = store.storedValidatorsFor(feedUrl)
            val nextFetchTimestamp = FeedRefreshScheduler.computeNextFetchTimestamp(
                now = now,
                responseInfo = responseInfo,
                fallbackLastModified = validators?.lastModified ?: previousCacheInfo?.lastModified,
            )
            logger.d {
                val minutes = (nextFetchTimestamp - now).milliseconds.inWholeMinutes
                "Next fetch for $feedUrl in $minutes min " +
                    "(cacheControl=${responseInfo?.cacheControl}, expires=${responseInfo?.expires}, " +
                    "lastModified=${validators?.lastModified ?: previousCacheInfo?.lastModified})"
            }
            FeedSourceCacheInfo(
                feedSourceId = feedSourceId,
                etag = validators?.etag,
                lastModified = validators?.lastModified,
                nextFetchTimestamp = nextFetchTimestamp,
                backoffTimestamp = null,
            )
        } else {
            val backoffTimestamp = FeedRefreshScheduler.computeBackoffTimestamp(
                now = now,
                retryAfter = responseInfo?.retryAfter,
            )
            if (backoffTimestamp != null) {
                logger.d {
                    val minutes = (backoffTimestamp - now).milliseconds.inWholeMinutes
                    "Backing off $feedUrl for $minutes min " +
                        "(status=${responseInfo?.statusCode}, retryAfter=${responseInfo?.retryAfter})"
                }
            }
            FeedSourceCacheInfo(
                feedSourceId = feedSourceId,
                etag = previousCacheInfo?.etag,
                lastModified = previousCacheInfo?.lastModified,
                nextFetchTimestamp = previousCacheInfo?.nextFetchTimestamp,
                backoffTimestamp = backoffTimestamp,
            )
        }
    }
}
