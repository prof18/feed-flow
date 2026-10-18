package com.prof18.feedflow.shared.domain.recovery

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.domain.FeedSourceLogoRetriever
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedRecoveryProfile
import com.prof18.feedflow.core.model.FeedRecoveryRecord
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.RecoveredFeedOperation
import com.prof18.feedflow.core.model.RecoveryMode
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedRefreshScheduler
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.mappers.RssChannelMapper
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.CancellationException

internal class FeedRecoveryService(
    private val coordinator: FeedRecoveryCoordinator,
    private val recoveryRepository: FeedRecoveryRepository,
    private val capturedFeedParser: CapturedFeedParser?,
    private val rssChannelMapper: RssChannelMapper,
    private val feedSourceLogoRetriever: FeedSourceLogoRetriever,
    private val databaseHelper: DatabaseHelper,
    private val feedSyncRepository: FeedSyncRepository,
    private val feedStateRepository: FeedStateRepository,
    private val accountsRepository: AccountsRepository,
    private val dateFormatter: DateFormatter,
    private val logger: Logger,
) {
    @Suppress("ThrowsCount")
    suspend fun recover(request: FeedRecoveryRequest, ticket: RecoveryTicket): FeedSource {
        if (accountsRepository.getCurrentSyncAccount() !in LOCAL_ACCOUNTS) {
            throw WebsiteVerificationFailure(VerificationFailure.Unsupported)
        }
        val parser = capturedFeedParser ?: throw WebsiteVerificationFailure(VerificationFailure.Unsupported)
        if (isBackingOff(request)) throw WebsiteVerificationFailure(VerificationFailure.AccessDenied)

        val source = ticket.guard {
            coordinator.verify(request.verificationUrl, VerificationPurpose.Feed, ticket) { capture, profile ->
                if (!recoveryRepository.isCurrent(profile)) {
                    throw WebsiteVerificationFailure(VerificationFailure.BrowserStopped)
                }
                val channel = try {
                    parser.parse(capture.bytes, capture.contentType)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    throw WebsiteVerificationFailure(VerificationFailure.InvalidContent)
                }
                commit(request, channel, capture, profile, ticket)
            }
        }
        try {
            feedSyncRepository.enqueueBackup()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.d(e) { "Recovered feed committed; backup deferred" }
        }
        feedStateRepository.getFeeds()
        return source
    }

    private suspend fun commit(
        request: FeedRecoveryRequest,
        channel: RssChannel,
        capture: BrowserCapture,
        profile: FeedRecoveryProfile,
        ticket: RecoveryTicket,
    ): FeedSource {
        val source = sourceFor(request, channel)
        val items = rssChannelMapper.getFeedItems(channel, source)
        val cloudSession = feedSyncRepository.cloudSessionForEdit()
        val record = FeedRecoveryRecord(
            accountEpoch = ticket.accountEpoch,
            sourceId = source.id,
            subscriptionUrl = source.url,
            effectiveUrl = capture.effectiveUrl,
            origin = profile.origin,
            mode = RecoveryMode.SessionApproved,
            profileId = profile.id,
            statusCode = null,
            retryAfter = null,
        )
        try {
            databaseHelper.commitRecoveredFeed(
                source = source,
                items = items,
                operation = request.operation,
                expectedUrl = request.expectedUrl,
                record = record,
                cloudSessionId = cloudSession,
                commit = ticket::commit,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.d(e) { "Unable to save the verified feed" }
            throw WebsiteVerificationFailure(VerificationFailure.SaveFailed)
        }
        feedSyncRepository.localEditCommitted(cloudSession)
        return source
    }

    private suspend fun sourceFor(request: FeedRecoveryRequest, channel: RssChannel): FeedSource {
        val now = dateFormatter.currentTimeMillis()
        return when (request) {
            is FeedRecoveryRequest.Add -> FeedSource(
                id = request.subscriptionUrl.hashCode().toString(),
                url = request.subscriptionUrl,
                title = rssChannelMapper.getFeedTitle(channel)
                    ?: throw WebsiteVerificationFailure(VerificationFailure.InvalidContent),
                category = request.category,
                lastSyncTimestamp = now,
                logoUrl = feedSourceLogoRetriever.getFeedSourceLogoUrl(channel),
                websiteUrl = channel.link,
                fetchFailed = false,
                articleOpenMode = ArticleOpenMode.DEFAULT,
                isHiddenFromTimeline = false,
                isPinned = false,
                isNotificationEnabled = request.notificationsEnabled,
                isHideImagesEnabled = false,
            )

            is FeedRecoveryRequest.Edit -> request.replacement.copy(
                lastSyncTimestamp = now,
                websiteUrl = channel.link ?: request.replacement.websiteUrl,
                fetchFailed = false,
            )

            is FeedRecoveryRequest.Refresh -> request.source.copy(
                lastSyncTimestamp = now,
                logoUrl = request.source.logoUrl ?: feedSourceLogoRetriever.getFeedSourceLogoUrl(channel),
                websiteUrl = request.source.websiteUrl ?: channel.link,
                fetchFailed = false,
            )
        }
    }

    private suspend fun isBackingOff(request: FeedRecoveryRequest): Boolean {
        val now = dateFormatter.currentTimeMillis()
        val requested = FeedRefreshScheduler.computeBackoffTimestamp(now = now, retryAfter = request.retryAfter)
        val existingId = when (request) {
            is FeedRecoveryRequest.Add -> null
            is FeedRecoveryRequest.Edit -> request.original.id
            is FeedRecoveryRequest.Refresh -> request.source.id
        }
        val cached = existingId?.let { id ->
            databaseHelper.getFeedSourcesCacheInfo().firstOrNull { it.feedSourceId == id }?.backoffTimestamp
        }
        return listOfNotNull(requested, cached).any { it > now }
    }

    private val FeedRecoveryRequest.operation: RecoveredFeedOperation
        get() = when (this) {
            is FeedRecoveryRequest.Add -> RecoveredFeedOperation.Add
            is FeedRecoveryRequest.Edit -> RecoveredFeedOperation.Edit
            is FeedRecoveryRequest.Refresh -> RecoveredFeedOperation.Refresh
        }

    private val FeedRecoveryRequest.expectedUrl: String?
        get() = when (this) {
            is FeedRecoveryRequest.Add -> null
            is FeedRecoveryRequest.Edit -> original.url
            is FeedRecoveryRequest.Refresh -> source.url
        }

    private companion object {
        val LOCAL_ACCOUNTS = setOf(
            SyncAccounts.LOCAL,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.ICLOUD,
        )
    }
}
