package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceToNotify
import com.prof18.feedflow.core.model.onErrorSuspend
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.mappers.RssChannelMapper
import com.prof18.feedflow.shared.domain.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.shared.domain.model.InProgressFeedUpdateStatus
import com.prof18.feedflow.shared.domain.model.NoFeedSourcesStatus
import com.prof18.feedflow.shared.domain.model.StartedFeedUpdateStatus
import com.prof18.feedflow.shared.presentation.model.FeedErrorState
import com.prof18.feedflow.shared.presentation.model.SyncError
import com.prof18.feedflow.shared.utils.getNumberOfConcurrentParsingRequests
import com.prof18.rssparser.RssParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock // Already present, but good to confirm
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

    @Suppress("MagicNumber")
    private suspend fun fetchFeedsWithGReader() {
        val feedSourceUrls = databaseHelper.getFeedSources()
        if (feedSourceUrls.isEmpty()) {
            feedStateRepository.emitUpdateStatus(NoFeedSourcesStatus)
        } else {
            gReaderRepository.sync()
                .onErrorSuspend {
                    feedStateRepository.emitErrorState(SyncError)
                }
        }
        // If the sync is skipped quickly, sometimes the loading spinner stays out
        delay(50)
        isFeedSyncDone = true
        updateRefreshCount()
        // After fetching new feeds, delete old ones based on user settings
        cleanOldFeeds()
        feedStateRepository.getFeeds()
    }

    @Suppress("MagicNumber")
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
            delay(50)
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

        val oldFeedIds = databaseHelper.getOldFeedItem(threshold)
        databaseHelper.deleteOldFeedItems(threshold)
        feedSyncRepository.deleteFeedItems(oldFeedIds)
        feedStateRepository.getFeeds()
    }

    @Suppress("MagicNumber")
    private fun shouldRefreshFeed(
        feedSource: FeedSource,
        forceRefresh: Boolean,
    ): Boolean {
        val isOpenRssFeed = feedSource.url.contains("openrss.org", ignoreCase = true)
        val feedTitle = feedSource.title // For logging

        // 1. Handle forceRefresh (maintains existing openrss.org exclusion from forceRefresh)
        if (forceRefresh && !isOpenRssFeed) {
            logger.d { "Fetching '$feedTitle' due to forceRefresh." }
            return true
        }

        val currentTimeMillis = Clock.System.now().toEpochMilliseconds()

        // 2. Check next_fetch_timestamp (if available)
        // Assuming feedSource.next_fetch_timestamp is Long? and accessible here
        // This field would have been added to the core model if mappings were updated
        // after the DB change.
        val nextFetchTimestamp = feedSource.next_fetch_timestamp

        if (nextFetchTimestamp != null) {
            return if (currentTimeMillis >= nextFetchTimestamp) {
                logger.d { "Fetching '$feedTitle' as current time ($currentTimeMillis) is past next_fetch_timestamp ($nextFetchTimestamp)." }
                true
            } else {
                logger.d { "Skipping '$feedTitle' as current time ($currentTimeMillis) is before next_fetch_timestamp ($nextFetchTimestamp). Needs to wait until $nextFetchTimestamp." }
                false
            }
        }

        // 3. If next_fetch_timestamp is null, fall back to old logic (lastSyncTimestamp based)
        logger.d { "Next_fetch_timestamp for '$feedTitle' is null. Falling back to lastSyncTimestamp logic." }
        val lastSyncTimestamp = feedSource.lastSyncTimestamp
        if (lastSyncTimestamp == null) { // If never synced before
            logger.d { "Fetching '$feedTitle' as it was never synced (lastSyncTimestamp is null)." }
            return true
        }

        val timeDifference = currentTimeMillis - lastSyncTimestamp

        val refreshThresholdInMillis = if (isOpenRssFeed) {
            (6 * 60 * 60) * 1000L // 6 hours for openrss.org feeds
        } else {
            (60 * 60) * 1000L // 1 hour for other feeds
        }

        val shouldFetchByOldLogic = timeDifference >= refreshThresholdInMillis
        if (shouldFetchByOldLogic) {
            logger.d { "Fetching '$feedTitle' based on fallback logic (time difference: $timeDifference >= threshold: $refreshThresholdInMillis)." }
        } else {
            logger.d { "Skipping '$feedTitle' based on fallback logic (time difference: $timeDifference < threshold: $refreshThresholdInMillis)." }
        }
        return shouldFetchByOldLogic
    }

    private suspend fun parseFeeds(
        feedSourceUrls: List<FeedSource>,
        forceRefresh: Boolean,
    ) {
        val allFeedItems = mutableListOf<FeedItem>()
        val dateFormat = settingsRepository.getDateFormat()

        feedSourceUrls
            .mapNotNull { feedSource ->
                // The shouldRefreshFeed function now incorporates the next_fetch_timestamp logic
                if (shouldRefreshFeed(feedSource, forceRefresh)) {
                    feedSource
                } else {
                    // Logger message is now inside shouldRefreshFeed, so no need for another one here
                    // logger.d { "Skipping feed: ${feedSource.url} based on refresh logic." }
                    feedToUpdate.remove(feedSource.url) // Ensure this is safe if called multiple times
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
                        rssChannelMapper.getFeedItems(
                            rssChannel = rssChannel,
                            feedSource = feedSource,
                            dateFormat = dateFormat,
                        )
                    } catch (e: Throwable) {
                        logger.e { "Error, skip: ${feedSource.url}}. Error: $e" }
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
