package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import co.touchlab.skie.configuration.annotations.SuspendInterop
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedRecoveryRecord
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCacheInfo
import com.prof18.feedflow.core.model.FeedSourceToNotify
import com.prof18.feedflow.core.model.StartedFeedUpdateStatus
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.core.utils.withSuspensionGuard
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.feedbin.domain.FeedbinRepository
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpValidators
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedRefreshScheduler
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedSourceCacheInfoFactory
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.mappers.RssChannelMapper
import com.prof18.feedflow.shared.domain.recovery.FeedAttempt
import com.prof18.feedflow.shared.domain.recovery.FeedOutcome
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.RecoveryTicket
import com.prof18.feedflow.shared.domain.recovery.RecoveryUpdate
import kotlinx.coroutines.CancellationException
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
    private val rssChannelMapper: RssChannelMapper,
    private val dateFormatter: DateFormatter,
    private val feedHttpCacheStore: FeedHttpCacheStore,
    private val feedAttempt: FeedAttempt,
    private val recoveryRepository: FeedRecoveryRepository,
) {
    @Suppress("unused") // Used on iOS
    suspend fun getFeedSourceToNotify(): List<FeedSourceToNotify> =
        databaseHelper.getFeedSourceToNotify()

    @Suppress("unused") // Used on iOS
    suspend fun markItemsAsNotified() =
        databaseHelper.markFeedItemsAsNotified()

    @SuspendInterop.Enabled
    suspend fun fetchFeeds(forceRefresh: Boolean = false) {
        return withContext(dispatcherProvider.io) {
            withSuspensionGuard("FeedFlow serial feed fetch") {
                feedStateRepository.emitUpdateStatus(StartedFeedUpdateStatus)
                when {
                    gReaderRepository.isAccountSet() -> {
                        fetchFeedsWithGReader()
                    }
                    feedbinRepository.isAccountSet() -> {
                        fetchFeedsWithFeedbin()
                    }
                    else -> {
                        fetchFeedsWithRssParser(forceRefresh = forceRefresh)
                    }
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

    private suspend fun fetchFeedsWithRssParser(forceRefresh: Boolean) {
        feedSyncRepository.syncFeedSources()

        val feedSourceUrls = databaseHelper.getFeedSources()
        parseFeedsSerially(feedSourceUrls = feedSourceUrls, forceRefresh = forceRefresh)
        feedSyncRepository.syncFeedItems()
        feedStateRepository.getFeeds()
    }

    private fun shouldRefreshFeed(
        feedSource: FeedSource,
        cacheInfo: FeedSourceCacheInfo?,
        currentTime: Long,
        forceRefresh: Boolean,
    ): Boolean {
        val backoffTimestamp = cacheInfo?.backoffTimestamp
        if (backoffTimestamp != null && currentTime < backoffTimestamp) {
            logger.d {
                val minutes = (backoffTimestamp - currentTime).milliseconds.inWholeMinutes
                "Skipping ${feedSource.url}: Retry-After backoff active for another $minutes min"
            }
            return false
        }

        if (forceRefresh) {
            return true
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
        forceRefresh: Boolean,
    ) {
        val ticket = recoveryRepository.issueTicket()
        val recoveryRecords = recoveryRepository.records(ticket.accountEpoch)

        val currentTime = dateFormatter.currentTimeMillis()
        val cacheInfoById = databaseHelper.getFeedSourcesCacheInfo().associateBy { it.feedSourceId }
        feedHttpCacheStore.seedTiers(
            feedSourceUrls.mapNotNull { source ->
                cacheInfoById[source.id]?.userAgentTier?.let { source.url to it }
            }.toMap(),
        )
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

        val feedsToProcess = feedSourceUrls.mapNotNull { feedSource ->
            val shouldRefresh = shouldRefreshFeed(
                feedSource = feedSource,
                cacheInfo = cacheInfoById[feedSource.id],
                currentTime = currentTime,
                forceRefresh = forceRefresh,
            )
            if (shouldRefresh) {
                feedSource
            } else {
                null
            }
        }

        // Process feeds one at a time
        for (feedSource in feedsToProcess) {
            logger.d { "-> Getting ${feedSource.url}" }
            val record = recoveryRecords[feedSource.id]?.takeIf { it.subscriptionUrl == feedSource.url }
            var fetchSucceeded = false
            var receivedFullResponse = false
            var recoveryUpdate: RecoveryUpdate? = null
            try {
                when (val outcome = feedAttempt.run(feedSource, record)) {
                    is FeedOutcome.Fetched -> {
                        fetchSucceeded = true
                        receivedFullResponse = true
                        recoveryUpdate = clearedRecord(feedSource, record, outcome.viaSession)
                        val feedItems = rssChannelMapper.getFeedItems(
                            rssChannel = outcome.channel,
                            feedSource = feedSource,
                        )

                        databaseHelper.insertFeedItems(
                            feedItems = feedItems,
                            lastSyncTimestamp = dateFormatter.currentTimeMillis(),
                        )
                    }

                    is FeedOutcome.NotModified -> {
                        logger.d { "Feed not modified, skipping parsing: ${feedSource.url}" }
                        fetchSucceeded = true
                        recoveryUpdate = clearedRecord(feedSource, record, outcome.viaSession)
                        databaseHelper.updateLastSyncTimestamps(
                            feedSourceIds = listOf(feedSource.id),
                            lastSyncTimestamp = dateFormatter.currentTimeMillis(),
                        )
                    }

                    is FeedOutcome.Challenged -> {
                        logger.d { "Error, skip: ${feedSource.url}. Error: website verification required" }
                        recoveryUpdate = RecoveryUpdate.RecordChallenge(feedSource, outcome.challenge, record)
                    }

                    is FeedOutcome.Failed -> logger.d { "Error, skip: ${feedSource.url}. Error: ${outcome.error}" }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                logger.d { "Error, skip: ${feedSource.url}. Error: $e" }
            }
            databaseHelper.updateFeedSourcesCacheInfo(
                listOf(
                    FeedSourceCacheInfoFactory.create(
                        store = feedHttpCacheStore,
                        feedSourceId = feedSource.id,
                        feedUrl = feedSource.url,
                        fetchSucceeded = fetchSucceeded,
                        refreshValidatorsTimestamp = receivedFullResponse,
                        previousCacheInfo = cacheInfoById[feedSource.id],
                        now = dateFormatter.currentTimeMillis(),
                        logger = logger,
                    ),
                ),
            )
            recoveryUpdate?.let { applyRecoveryUpdate(it, ticket) }
        }
    }

    private fun clearedRecord(
        feedSource: FeedSource,
        record: FeedRecoveryRecord?,
        viaSession: Boolean,
    ): RecoveryUpdate? =
        if (record != null && !viaSession) RecoveryUpdate.ClearRecord(feedSource, record) else null

    private suspend fun applyRecoveryUpdate(update: RecoveryUpdate, ticket: RecoveryTicket) {
        try {
            recoveryRepository.apply(update, ticket)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logger.d(e) { "Recovery update skipped" }
        }
    }
}
