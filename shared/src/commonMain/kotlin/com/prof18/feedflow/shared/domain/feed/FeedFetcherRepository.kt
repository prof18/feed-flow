package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.domain.FeedSourceLogoRetriever
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.Failure
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCacheInfo
import com.prof18.feedflow.core.model.FeedSourceToNotify
import com.prof18.feedflow.core.model.FeedSyncError
import com.prof18.feedflow.core.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.core.model.InProgressFeedUpdateStatus
import com.prof18.feedflow.core.model.NetworkFailure
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.core.model.StartedFeedUpdateStatus
import com.prof18.feedflow.core.model.onErrorSuspend
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.feedbin.domain.FeedbinRepository
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.contentprefetch.ContentPrefetchRepository
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpValidators
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedRefreshScheduler
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedSourceCacheInfoFactory
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.mappers.RssChannelMapper
import com.prof18.feedflow.shared.presentation.model.FeedErrorState
import com.prof18.feedflow.shared.presentation.model.SyncError
import com.prof18.feedflow.shared.utils.getNumberOfConcurrentParsingRequests
import com.prof18.rssparser.exception.HttpException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class FeedFetcherRepository internal constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val feedStateRepository: FeedStateRepository,
    private val gReaderRepository: GReaderRepository,
    private val feedbinRepository: FeedbinRepository,
    private val databaseHelper: DatabaseHelper,
    private val feedSyncRepository: FeedSyncRepository,
    private val settingsRepository: SettingsRepository,
    private val contentPrefetchRepository: ContentPrefetchRepository,
    private val logger: Logger,
    private val rssParserWrapper: RssParserWrapper,
    private val rssChannelMapper: RssChannelMapper,
    private val dateFormatter: DateFormatter,
    private val feedSourceLogoRetriever: FeedSourceLogoRetriever,
    private val feedHttpCacheStore: FeedHttpCacheStore,
) {
    private val feedToUpdate = hashSetOf<String>()
    private var isFeedSyncDone = true

    suspend fun fetchFeeds(isFirstLaunch: Boolean = false) {
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
                    fetchFeedsWithRssParser(isFirstLaunch)
                }
            }
        }
    }

    @Suppress("unused") // Used on iOS
    suspend fun getFeedSourceToNotify(): List<FeedSourceToNotify> =
        databaseHelper.getFeedSourceToNotify()

    @Suppress("unused") // Used on iOS
    suspend fun markItemsAsNotified() =
        databaseHelper.markFeedItemsAsNotified()

    private suspend fun fetchFeedsWithGReader() {
        val feedSourceUrls = databaseHelper.getFeedSources()
        if (feedSourceUrls.isEmpty()) {
            feedStateRepository.emitUpdateStatus(NoFeedSourcesStatus)
        } else {
            gReaderRepository.sync()
                .onErrorSuspend { failure ->
                    feedStateRepository.emitErrorState(SyncError(failure.toGReaderSyncErrorCode()))
                }
        }
        // If the sync is skipped quickly, sometimes the loading spinner stays out
        delay(50.milliseconds)
        contentPrefetchRepository.prefetchContent()
        isFeedSyncDone = true
        updateRefreshCount()
        // After fetching new feeds, delete old ones based on user settings
        cleanOldFeeds()
        feedStateRepository.getFeeds()
    }

    private suspend fun fetchFeedsWithFeedbin() {
        val feedSourceUrls = databaseHelper.getFeedSources()
        if (feedSourceUrls.isEmpty()) {
            feedStateRepository.emitUpdateStatus(NoFeedSourcesStatus)
        } else {
            feedbinRepository.sync()
                .onErrorSuspend {
                    feedStateRepository.emitErrorState(SyncError(FeedSyncError.SyncFeedsFailed))
                }
        }
        // If the sync is skipped quickly, sometimes the loading spinner stays out
        delay(50.milliseconds)
        isFeedSyncDone = true
        updateRefreshCount()
        // After fetching new feeds, delete old ones based on user settings
        cleanOldFeeds()
        feedStateRepository.getFeeds()
    }

    private suspend fun fetchFeedsWithRssParser(isFirstLaunch: Boolean = false) {
        feedSyncRepository.syncFeedSources()

        val feedSourceUrls = databaseHelper.getFeedSources()
        feedToUpdate.clear()
        feedToUpdate.addAll(feedSourceUrls.map { it.url })
        if (feedSourceUrls.isEmpty()) {
            feedStateRepository.emitUpdateStatus(NoFeedSourcesStatus)
        } else {
            if (!isFirstLaunch) {
                feedStateRepository.getFeeds()
            }
            feedStateRepository.emitUpdateStatus(
                InProgressFeedUpdateStatus(
                    refreshedFeedCount = 0,
                    totalFeedCount = feedSourceUrls.size,
                ),
            )

            isFeedSyncDone = false
            parseFeeds(
                feedSourceUrls = feedSourceUrls,
            )

            feedSyncRepository.syncFeedItems()
            // If the sync is skipped quickly, sometimes the loading spinner stays out
            delay(50.milliseconds)
            contentPrefetchRepository.prefetchContent()
            isFeedSyncDone = true
            // After fetching new feeds, delete old ones based on user settings
            cleanOldFeeds()
            updateRefreshCount()
            feedStateRepository.getFeeds()
        }
    }

    private fun updateRefreshCount() {
        val oldUpdate = feedStateRepository.updateState.value
        val refreshedFeedCount = oldUpdate.refreshedFeedCount + 1
        val totalFeedCount = oldUpdate.totalFeedCount

        if (feedToUpdate.isEmpty() && isFeedSyncDone) {
            feedStateRepository.emitUpdateStatus(
                FinishedFeedUpdateStatus,
            )
        } else {
            feedStateRepository.emitUpdateStatus(
                InProgressFeedUpdateStatus(
                    refreshedFeedCount = refreshedFeedCount.coerceAtMost(totalFeedCount),
                    totalFeedCount = totalFeedCount,
                ),
            )
        }
    }

    private suspend fun cleanOldFeeds() {
        val deletePeriod = settingsRepository.getAutoDeletePeriod()
        if (deletePeriod == AutoDeletePeriod.DISABLED) {
            return
        }

        val threshold = when (deletePeriod) {
            AutoDeletePeriod.DISABLED -> return
            AutoDeletePeriod.ONE_DAY -> Clock.System.now().minus(1.days).toEpochMilliseconds()
            AutoDeletePeriod.ONE_WEEK -> Clock.System.now().minus(7.days).toEpochMilliseconds()
            AutoDeletePeriod.TWO_WEEKS -> Clock.System.now().minus(14.days).toEpochMilliseconds()
            AutoDeletePeriod.ONE_MONTH -> Clock.System.now().minus(30.days).toEpochMilliseconds()
        }

        val currentFilter = feedStateRepository.currentFeedFilter.value
        val oldFeedIds = databaseHelper.getOldFeedItem(timeThreshold = threshold, feedFilter = currentFilter)
        databaseHelper.deleteOldFeedItems(timeThreshold = threshold, feedFilter = currentFilter)
        feedSyncRepository.deleteFeedItems(oldFeedIds)
        databaseHelper.cleanupOldDeletedItems()
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

    private sealed interface FeedFetchResult {
        val feedSource: FeedSource

        data class Success(
            override val feedSource: FeedSource,
            val feedItems: List<FeedItem>,
        ) : FeedFetchResult

        data class NotModified(override val feedSource: FeedSource) : FeedFetchResult
        data class Failure(override val feedSource: FeedSource) : FeedFetchResult
    }

    @Suppress("LongMethod")
    private suspend fun parseFeeds(
        feedSourceUrls: List<FeedSource>,
    ) {
        val allFeedItems = mutableListOf<FeedItem>()
        val notModifiedFeedSourceIds = mutableListOf<String>()
        val updatedCacheInfo = mutableListOf<FeedSourceCacheInfo>()
        val shouldShowParsingErrors = settingsRepository.getShowRssParsingErrors()

        val currentTime = dateFormatter.currentTimeMillis()
        val cacheInfoById = databaseHelper.getFeedSourcesCacheInfo().associateBy { it.feedSourceId }
        feedHttpCacheStore.seedValidators(
            feedSourceUrls.mapNotNull { feedSource ->
                val cacheInfo = cacheInfoById[feedSource.id] ?: return@mapNotNull null
                if (cacheInfo.etag == null && cacheInfo.lastModified == null) {
                    null
                } else if (!FeedRefreshScheduler.shouldUseValidators(
                        now = currentTime,
                        validatorsTimestamp = cacheInfo.validatorsTimestamp,
                    )
                ) {
                    logger.d { "Dropping stale conditional GET validators for ${feedSource.url}" }
                    null
                } else {
                    feedSource.url to FeedHttpValidators(
                        etag = cacheInfo.etag,
                        lastModified = cacheInfo.lastModified,
                    )
                }
            }.toMap(),
        )

        feedSourceUrls
            .mapNotNull { feedSource ->
                val shouldRefresh = shouldRefreshFeed(
                    feedSource = feedSource,
                    cacheInfo = cacheInfoById[feedSource.id],
                    currentTime = currentTime,
                )
                if (shouldRefresh) {
                    feedSource
                } else {
                    logger.d { "Refresh window not expired, skipping: ${feedSource.url}" }
                    feedToUpdate.remove(feedSource.url)
                    updateRefreshCount()
                    null
                }
            }
            .asFlow()
            .flatMapMerge(concurrency = getNumberOfConcurrentParsingRequests()) { feedSource ->
                suspend {
                    logger.d { "-> Getting ${feedSource.url}" }
                    try {
                        val rssChannel = withTimeout(1.minutes) {
                            rssParserWrapper.getRssChannel(feedSource.url)
                        }
                        logger.d { "<- Got back ${rssChannel.title}" }
                        feedToUpdate.remove(feedSource.url)
                        updateRefreshCount()
                        if (feedSource.logoUrl == null) {
                            val logoUrl = feedSourceLogoRetriever.getFeedSourceLogoUrl(rssChannel)
                            databaseHelper.updateFeedSourceLogoUrl(feedSourceId = feedSource.id, logoUrl = logoUrl)
                        }
                        if (feedSource.websiteUrl == null) {
                            databaseHelper.updateFeedSourceWebsiteUrl(
                                feedSourceId = feedSource.id,
                                websiteUrl = rssChannel.link,
                            )
                        }
                        FeedFetchResult.Success(
                            feedSource = feedSource,
                            feedItems = rssChannelMapper.getFeedItems(
                                rssChannel = rssChannel,
                                feedSource = feedSource,
                            ),
                        )
                    } catch (e: Throwable) {
                        feedToUpdate.remove(feedSource.url)
                        updateRefreshCount()
                        handleFetchError(
                            feedSource = feedSource,
                            error = e,
                            shouldShowParsingErrors = shouldShowParsingErrors,
                        )
                    }
                }.asFlow()
            }
            .collect { result ->
                when (result) {
                    is FeedFetchResult.Success -> {
                        logger.d { "Collected ${result.feedItems.size} items" }
                        allFeedItems.addAll(result.feedItems)
                    }

                    is FeedFetchResult.NotModified -> {
                        notModifiedFeedSourceIds.add(result.feedSource.id)
                    }

                    is FeedFetchResult.Failure -> {
                        // Failure flag already persisted above
                    }
                }
                updatedCacheInfo.add(
                    buildUpdatedCacheInfo(
                        result = result,
                        previousCacheInfo = cacheInfoById[result.feedSource.id],
                    ),
                )
            }

        if (allFeedItems.isNotEmpty()) {
            logger.d { "Inserting ${allFeedItems.size} items into database" }
            try {
                databaseHelper.insertFeedItems(
                    feedItems = allFeedItems,
                    lastSyncTimestamp = dateFormatter.currentTimeMillis(),
                )
            } catch (e: Throwable) {
                logger.d(e) { "Failed to insert feed items into database" }
            }
        }

        if (notModifiedFeedSourceIds.isNotEmpty()) {
            databaseHelper.updateLastSyncTimestamps(
                feedSourceIds = notModifiedFeedSourceIds,
                lastSyncTimestamp = dateFormatter.currentTimeMillis(),
            )
        }

        if (updatedCacheInfo.isNotEmpty()) {
            databaseHelper.updateFeedSourcesCacheInfo(updatedCacheInfo)
        }
    }

    private suspend fun handleFetchError(
        feedSource: FeedSource,
        error: Throwable,
        shouldShowParsingErrors: Boolean,
    ): FeedFetchResult {
        if ((error as? HttpException)?.code == HTTP_NOT_MODIFIED) {
            logger.d { "Feed not modified, skipping parsing: ${feedSource.url}" }
            return FeedFetchResult.NotModified(feedSource)
        }
        logger.d { "Error, skip: ${feedSource.url}}. Error: $error" }
        // Mark failure flag on error
        databaseHelper.setFeedFetchFailed(feedSource.id, true)
        if (shouldShowParsingErrors) {
            feedStateRepository.emitErrorState(
                FeedErrorState(
                    failingSourceName = feedSource.title,
                ),
            )
        }
        return FeedFetchResult.Failure(feedSource)
    }

    private fun buildUpdatedCacheInfo(
        result: FeedFetchResult,
        previousCacheInfo: FeedSourceCacheInfo?,
    ): FeedSourceCacheInfo = FeedSourceCacheInfoFactory.create(
        store = feedHttpCacheStore,
        feedSourceId = result.feedSource.id,
        feedUrl = result.feedSource.url,
        fetchSucceeded = result !is FeedFetchResult.Failure,
        refreshValidatorsTimestamp = result is FeedFetchResult.Success,
        previousCacheInfo = previousCacheInfo,
        now = dateFormatter.currentTimeMillis(),
        logger = logger,
    )

    private companion object {
        const val HTTP_NOT_MODIFIED = 304
    }
}

private fun Failure.toGReaderSyncErrorCode(): FeedSyncError =
    when (this) {
        NetworkFailure.BadToken -> FeedSyncError.GReaderBadToken
        else -> FeedSyncError.SyncFeedsFailed
    }
