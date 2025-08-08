package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.StartedFeedUpdateStatus
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.mappers.RssChannelMapper
import com.prof18.feedflow.shared.domain.notification.Notifier
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.exception.HttpException
import kotlinx.coroutines.withContext

/**
 * Serial version of FeedFetcherRepository that processes feeds one at a time
 * to reduce resource usage during background processing on iOS.
 */
class SerialFeedFetcherRepository internal constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val feedStateRepository: FeedStateRepository,
    private val gReaderRepository: GReaderRepository,
    private val databaseHelper: DatabaseHelper,
    private val feedSyncRepository: FeedSyncRepository,
    private val logger: Logger,
    private val rssParser: RssParser,
    private val rssChannelMapper: RssChannelMapper,
    private val dateFormatter: DateFormatter,
    private val notifier: Notifier,
) {
    suspend fun fetchFeeds() {
        return withContext(dispatcherProvider.io) {
            feedStateRepository.emitUpdateStatus(StartedFeedUpdateStatus)
            when {
                gReaderRepository.isAccountSet() -> {
                    fetchFeedsWithGReader()
                    databaseHelper.markFeedItemsAsNotified()
                }

                else -> {
                    fetchFeedsWithRssParser()
                    databaseHelper.markFeedItemsAsNotified()
                }
            }
            val feedSourceToNotify = databaseHelper.getFeedSourceToNotify()
            notifier.showNewArticlesNotification(feedSourceToNotify)
            databaseHelper.markFeedItemsAsNotified()
        }
    }

    private suspend fun fetchFeedsWithGReader() {
        gReaderRepository.sync()
        feedStateRepository.getFeeds()
    }

    private suspend fun fetchFeedsWithRssParser() {
        feedSyncRepository.syncFeedSources()

        val feedSourceUrls = databaseHelper.getFeedSources()
        parseFeedsSerially(feedSourceUrls = feedSourceUrls)
        feedSyncRepository.syncFeedItems()
        feedStateRepository.getFeeds()
    }

    @Suppress("MagicNumber")
    private fun shouldRefreshFeed(
        feedSource: FeedSource,
    ): Boolean {
        val isOpenRssFeed = feedSource.url.contains("openrss.org", ignoreCase = true)

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

    private suspend fun parseFeedsSerially(
        feedSourceUrls: List<FeedSource>,
    ) {
        val feedsToProcess = feedSourceUrls.mapNotNull { feedSource ->
            val shouldRefresh = shouldRefreshFeed(feedSource)
            if (shouldRefresh) {
                feedSource
            } else {
                null
            }
        }

        // Process feeds one at a time
        for (feedSource in feedsToProcess) {
            logger.d { "-> Getting ${feedSource.url}" }
            try {
                val rssChannel = rssParser.getRssChannel(feedSource.url)
                val feedItems = rssChannelMapper.getFeedItems(
                    rssChannel = rssChannel,
                    feedSource = feedSource,
                )

                databaseHelper.insertFeedItems(
                    feedItems = feedItems,
                    lastSyncTimestamp = dateFormatter.currentTimeMillis(),
                )
            } catch (e: HttpException) {
                // Ignore HTTP errors
                logger.d { "Error, skip: ${feedSource.url}. Error: $e" }
            } catch (e: Throwable) {
                logger.e { "Error, skip: ${feedSource.url}. Error: $e" }
            }
        }
    }
}
