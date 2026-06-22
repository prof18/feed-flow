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
        refreshValidatorsTimestamp: Boolean,
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
                validatorsTimestamp = validators.timestamp(
                    refreshValidatorsTimestamp = refreshValidatorsTimestamp,
                    previousCacheInfo = previousCacheInfo,
                    now = now,
                ),
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
                validatorsTimestamp = previousCacheInfo?.validatorsTimestamp,
                nextFetchTimestamp = previousCacheInfo?.nextFetchTimestamp,
                backoffTimestamp = backoffTimestamp,
            )
        }
    }

    private fun FeedHttpValidators?.timestamp(
        refreshValidatorsTimestamp: Boolean,
        previousCacheInfo: FeedSourceCacheInfo?,
        now: Long,
    ): Long? {
        if (this?.etag == null && this?.lastModified == null) {
            return null
        }
        return if (refreshValidatorsTimestamp) {
            now
        } else {
            previousCacheInfo?.validatorsTimestamp
        }
    }
}
