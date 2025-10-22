package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceToNotify
import com.prof18.feedflow.core.model.FeedSyncError
import com.prof18.feedflow.core.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.core.model.InProgressFeedUpdateStatus
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.core.model.StartedFeedUpdateStatus
import com.prof18.feedflow.core.model.onErrorSuspend
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.mappers.RssChannelMapper
import com.prof18.feedflow.shared.presentation.model.FeedErrorState
import com.prof18.feedflow.shared.presentation.model.SyncError
import com.prof18.feedflow.shared.utils.getNumberOfConcurrentParsingRequests
import com.prof18.feedflow.shared.utils.skipLogging
import com.prof18.rssparser.RssParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalCoroutinesApi::class)
class FeedFetcherRepository internal constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val feedStateRepository: FeedStateRepository,
    private val gReaderRepository: GReaderRepository,
    private val databaseHelper: DatabaseHelper,
    private val feedSyncRepository: FeedSyncRepository,
    private val settingsRepository: SettingsRepository,
    private val logger: Logger,
    private val rssParser: RssParser,
    private val rssChannelMapper: RssChannelMapper,
    private val dateFormatter: DateFormatter,
    private val feedSourceLogoRetriever: FeedSourceLogoRetriever,
) {
    private val feedToUpdate = hashSetOf<String>()
    private var isFeedSyncDone = true

    @Suppress("MagicNumber")
    suspend fun fetchFeeds(
        forceRefresh: Boolean = false,
        isFirstLaunch: Boolean = false,
        isFetchingFromBackground: Boolean = false,
    ) {
        return withContext(dispatcherProvider.io) {
            feedStateRepository.emitUpdateStatus(StartedFeedUpdateStatus)
            when {
                gReaderRepository.isAccountSet() -> {
                    fetchFeedsWithGReader()
                    if (!isFetchingFromBackground) {
                        databaseHelper.markFeedItemsAsNotified()
                    }
                }
                else -> {
                    fetchFeedsWithRssParser(forceRefresh, isFirstLaunch)
                    if (!isFetchingFromBackground) {
                        databaseHelper.markFeedItemsAsNotified()
                    }
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
                .onErrorSuspend {
                    feedStateRepository.emitErrorState(SyncError(FeedSyncError.SyncFeedsFailed))
                }
        }
        // If the sync is skipped quickly, sometimes the loading spinner stays out
        delay(timeMillis = 50)
        isFeedSyncDone = true
        updateRefreshCount()
        // After fetching new feeds, delete old ones based on user settings
        cleanOldFeeds()
        feedStateRepository.getFeeds()
    }

    private suspend fun fetchFeedsWithRssParser(
        forceRefresh: Boolean = false,
        isFirstLaunch: Boolean = false,
    ) {
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
                forceRefresh = forceRefresh,
            )

            feedSyncRepository.syncFeedItems()
            // If the sync is skipped quickly, sometimes the loading spinner stays out
            delay(timeMillis = 50)
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
            AutoDeletePeriod.ONE_WEEK -> Clock.System.now().minus(7.days).toEpochMilliseconds()
            AutoDeletePeriod.TWO_WEEKS -> Clock.System.now().minus(14.days).toEpochMilliseconds()
            AutoDeletePeriod.ONE_MONTH -> Clock.System.now().minus(30.days).toEpochMilliseconds()
        }

        val currentFilter = feedStateRepository.currentFeedFilter.value
        val oldFeedIds = databaseHelper.getOldFeedItem(timeThreshold = threshold, feedFilter = currentFilter)
        databaseHelper.deleteOldFeedItems(timeThreshold = threshold, feedFilter = currentFilter)
        feedSyncRepository.deleteFeedItems(oldFeedIds)
        feedStateRepository.getFeeds()
    }

    @Suppress("MagicNumber")
    private fun shouldRefreshFeed(
        feedSource: FeedSource,
        forceRefresh: Boolean,
    ): Boolean {
        val isOpenRssFeed = feedSource.url.contains("openrss.org", ignoreCase = true)

        if (forceRefresh && !isOpenRssFeed) {
            return true
        }

        val lastSyncTimestamp = feedSource.lastSyncTimestamp ?: return true

        val currentTime = dateFormatter.currentTimeMillis()
        val timeDifference = currentTime - lastSyncTimestamp

        val refreshThresholdInMillis = if (isOpenRssFeed) {
            // 6 hours for openrss.org feeds
            (6 * 60 * 60) * 1000L
        } else {
            // 1 hour for other feeds
            (60 * 60) * 1000L
        }

        return timeDifference >= refreshThresholdInMillis
    }

    private suspend fun parseFeeds(
        feedSourceUrls: List<FeedSource>,
        forceRefresh: Boolean,
    ) {
        val allFeedItems = mutableListOf<FeedItem>()

        feedSourceUrls
            .mapNotNull { feedSource ->
                val shouldRefresh = shouldRefreshFeed(feedSource, forceRefresh)
                if (shouldRefresh) {
                    feedSource
                } else {
                    logger.d { "One hour is not passed, skipping: ${feedSource.url}}" }
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
                        val rssChannel = rssParser.getRssChannel(feedSource.url)
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
                        rssChannelMapper.getFeedItems(
                            rssChannel = rssChannel,
                            feedSource = feedSource,
                        )
                    } catch (e: Throwable) {
                        if (!e.skipLogging()) {
                            logger.e { "Error, skip: ${feedSource.url}}. Error: $e" }
                        }
                        // Mark failure flag on error
                        databaseHelper.setFeedFetchFailed(feedSource.id, true)
                        feedStateRepository.emitErrorState(
                            FeedErrorState(
                                failingSourceName = feedSource.title,
                            ),
                        )
                        feedToUpdate.remove(feedSource.url)
                        updateRefreshCount()
                        null
                    }
                }.asFlow()
            }
            .collect { items ->
                logger.d { "Collected ${items?.size} items" }
                items?.let { feedItems -> allFeedItems.addAll(feedItems) }
            }

        if (allFeedItems.isNotEmpty()) {
            logger.d { "Inserting ${allFeedItems.size} items into database" }
            databaseHelper.insertFeedItems(
                feedItems = allFeedItems,
                lastSyncTimestamp = dateFormatter.currentTimeMillis(),
            )
        }
    }
}
