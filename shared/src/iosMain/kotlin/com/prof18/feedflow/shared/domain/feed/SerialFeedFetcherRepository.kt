package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCacheInfo
import com.prof18.feedflow.core.model.FeedSourceToNotify
import com.prof18.feedflow.core.model.StartedFeedUpdateStatus
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.feedbin.domain.FeedbinRepository
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpValidators
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedRefreshScheduler
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedSourceCacheInfoFactory
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.mappers.RssChannelMapper
import com.prof18.rssparser.exception.HttpException
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Serial version of FeedFetcherRepository that processes feeds one at a time
 * to reduce resource usage during background processing on iOS.
 */
class SerialFeedFetcherRepository internal constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val feedStateRepository: FeedStateRepository,
    private val gReaderRepository: GReaderRepository,
    private val feedbinRepository: FeedbinRepository,
    private val databaseHelper: DatabaseHelper,
    private val feedSyncRepository: FeedSyncRepository,
    private val logger: Logger,
    private val rssParserWrapper: RssParserWrapper,
    private val rssChannelMapper: RssChannelMapper,
    private val dateFormatter: DateFormatter,
    private val feedHttpCacheStore: FeedHttpCacheStore,
) {
    @Suppress("unused") // Used on iOS
    suspend fun getFeedSourceToNotify(): List<FeedSourceToNotify> =
        databaseHelper.getFeedSourceToNotify()

    @Suppress("unused") // Used on iOS
    suspend fun markItemsAsNotified() =
        databaseHelper.markFeedItemsAsNotified()

    suspend fun fetchFeeds() {
        return withContext(dispatcherProvider.io) {
            feedStateRepository.emitUpdateStatus(StartedFeedUpdateStatus)
            when {
                gReaderRepository.isAccountSet() -> {
                    fetchFeedsWithGReader()
                }
                feedbinRepository.isAccountSet() -> {
                    fetchFeedsWithFeedbin()
                }
                else -> {
                    fetchFeedsWithRssParser()
                }
            }
        }
    }

    private suspend fun fetchFeedsWithGReader() {
        gReaderRepository.sync()
        feedStateRepository.getFeeds()
    }

    private suspend fun fetchFeedsWithFeedbin() {
        feedbinRepository.sync()
        feedStateRepository.getFeeds()
    }

    private suspend fun fetchFeedsWithRssParser() {
        feedSyncRepository.syncFeedSources()

        val feedSourceUrls = databaseHelper.getFeedSources()
        parseFeedsSerially(feedSourceUrls = feedSourceUrls)
        feedSyncRepository.syncFeedItems()
        feedStateRepository.getFeeds()
    }

    private fun shouldRefreshFeed(
        feedSource: FeedSource,
        cacheInfo: FeedSourceCacheInfo?,
        currentTime: Long,
    ): Boolean {
        val backoffTimestamp = cacheInfo?.backoffTimestamp
        if (backoffTimestamp != null && currentTime < backoffTimestamp) {
            logger.d {
                val minutes = (backoffTimestamp - currentTime).milliseconds.inWholeMinutes
                "Skipping ${feedSource.url}: Retry-After backoff active for another $minutes min"
            }
            return false
        }

        val nextFetchTimestamp = cacheInfo?.nextFetchTimestamp
        if (nextFetchTimestamp != null) {
            val shouldRefresh = currentTime >= nextFetchTimestamp
            if (!shouldRefresh) {
                logger.d {
                    val minutes = (nextFetchTimestamp - currentTime).milliseconds.inWholeMinutes
                    "Skipping ${feedSource.url}: refresh window expires in $minutes min"
                }
            }
            return shouldRefresh
        }

        val lastSyncTimestamp = feedSource.lastSyncTimestamp ?: return true
        return (currentTime - lastSyncTimestamp).milliseconds >= FeedRefreshScheduler.MIN_INTERVAL
    }

    private suspend fun parseFeedsSerially(
        feedSourceUrls: List<FeedSource>,
    ) {
        val currentTime = dateFormatter.currentTimeMillis()
        val cacheInfoById = databaseHelper.getFeedSourcesCacheInfo().associateBy { it.feedSourceId }
        feedHttpCacheStore.seedValidators(
            feedSourceUrls.mapNotNull { feedSource ->
                val cacheInfo = cacheInfoById[feedSource.id] ?: return@mapNotNull null
                if (cacheInfo.etag == null && cacheInfo.lastModified == null) {
                    null
                } else {
                    feedSource.url to FeedHttpValidators(
                        etag = cacheInfo.etag,
                        lastModified = cacheInfo.lastModified,
                    )
                }
            }.toMap(),
        )

        val feedsToProcess = feedSourceUrls.mapNotNull { feedSource ->
            val shouldRefresh = shouldRefreshFeed(
                feedSource = feedSource,
                cacheInfo = cacheInfoById[feedSource.id],
                currentTime = currentTime,
            )
            if (shouldRefresh) {
                feedSource
            } else {
                null
            }
        }

        val updatedCacheInfo = mutableListOf<FeedSourceCacheInfo>()

        // Process feeds one at a time
        for (feedSource in feedsToProcess) {
            logger.d { "-> Getting ${feedSource.url}" }
            var fetchSucceeded = false
            try {
                val rssChannel = rssParserWrapper.getRssChannel(feedSource.url)
                fetchSucceeded = true
                val feedItems = rssChannelMapper.getFeedItems(
                    rssChannel = rssChannel,
                    feedSource = feedSource,
                )

                databaseHelper.insertFeedItems(
                    feedItems = feedItems,
                    lastSyncTimestamp = dateFormatter.currentTimeMillis(),
                )
            } catch (e: HttpException) {
                if (e.code == HTTP_NOT_MODIFIED) {
                    logger.d { "Feed not modified, skipping parsing: ${feedSource.url}" }
                    fetchSucceeded = true
                    databaseHelper.updateLastSyncTimestamps(
                        feedSourceIds = listOf(feedSource.id),
                        lastSyncTimestamp = dateFormatter.currentTimeMillis(),
                    )
                } else {
                    logger.d { "Error, skip: ${feedSource.url}. Error: $e" }
                }
            } catch (e: Throwable) {
                logger.d { "Error, skip: ${feedSource.url}. Error: $e" }
            }
            updatedCacheInfo.add(
                FeedSourceCacheInfoFactory.create(
                    store = feedHttpCacheStore,
                    feedSourceId = feedSource.id,
                    feedUrl = feedSource.url,
                    fetchSucceeded = fetchSucceeded,
                    previousCacheInfo = cacheInfoById[feedSource.id],
                    now = dateFormatter.currentTimeMillis(),
                    logger = logger,
                ),
            )
        }

        if (updatedCacheInfo.isNotEmpty()) {
            databaseHelper.updateFeedSourcesCacheInfo(updatedCacheInfo)
        }
    }

    private companion object {
        const val HTTP_NOT_MODIFIED = 304
    }
}
