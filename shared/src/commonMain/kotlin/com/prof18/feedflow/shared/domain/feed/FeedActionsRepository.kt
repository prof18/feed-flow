package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.DataResult
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSyncError
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.isSuccess
import com.prof18.feedflow.core.model.onErrorSuspend
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.db.Search
import com.prof18.feedflow.feedsync.feedbin.domain.FeedbinRepository
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.presentation.model.SyncError
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

internal class FeedActionsRepository(
    private val databaseHelper: DatabaseHelper,
    private val feedSyncRepository: FeedSyncRepository,
    private val gReaderRepository: GReaderRepository,
    private val feedbinRepository: FeedbinRepository,
    private val accountsRepository: AccountsRepository,
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository,
    private val feedStateRepository: FeedStateRepository,
    private val feedItemParserWorker: FeedItemParserWorker,
) {
    suspend fun markAsRead(itemsToUpdates: HashSet<FeedItemId>) {
        feedStateRepository.markAsRead(itemsToUpdates)
        updateReadStatus(
            feedItemIds = itemsToUpdates.toList(),
            isRead = true,
        )
    }

    suspend fun retryPendingReadStatusActions() {
        val currentAccount = accountsRepository.getCurrentSyncAccount()
        if (!currentAccount.isRemoteReadStatusAccount()) {
            return
        }

        databaseHelper.getReadStatusPendingActions(currentAccount.name)
            .groupBy { it.is_read }
            .forEach { (isRead, pendingActions) ->
                val feedItemIds = pendingActions.map { FeedItemId(it.feed_item_id) }
                sendRemoteReadStatus(
                    account = currentAccount,
                    feedItemIds = feedItemIds,
                    isRead = isRead,
                ).handleRemoteReadStatusResult(
                    account = currentAccount,
                    feedItemIds = feedItemIds,
                    isRead = isRead,
                )
            }
    }

    private suspend fun updateReadStatus(
        feedItemIds: List<FeedItemId>,
        isRead: Boolean,
    ) {
        if (feedItemIds.isEmpty()) {
            return
        }

        when (val currentAccount = accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX, SyncAccounts.BAZQUX -> {
                persistPendingRemoteReadStatus(feedItemIds, isRead, currentAccount)
                sendRemoteReadStatus(currentAccount, feedItemIds, isRead)
                    .handleRemoteReadStatusResult(currentAccount, feedItemIds, isRead)
            }

            SyncAccounts.FEEDBIN -> {
                persistPendingRemoteReadStatus(feedItemIds, isRead, currentAccount)
                sendRemoteReadStatus(currentAccount, feedItemIds, isRead)
                    .handleRemoteReadStatusResult(currentAccount, feedItemIds, isRead)
            }

            SyncAccounts.LOCAL,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.ICLOUD,
            -> {
                databaseHelper.updateReadStatus(feedItemIds, isRead)
                feedSyncRepository.setIsSyncUploadRequired()
            }
        }
    }

    private suspend fun persistPendingRemoteReadStatus(
        feedItemIds: List<FeedItemId>,
        isRead: Boolean,
        currentAccount: SyncAccounts,
    ) {
        databaseHelper.updateReadStatus(feedItemIds, isRead)
        databaseHelper.upsertReadStatusPendingActions(
            feedItemIds = feedItemIds,
            isRead = isRead,
            syncAccount = currentAccount.name,
        )
    }

    private suspend fun sendRemoteReadStatus(
        account: SyncAccounts,
        feedItemIds: List<FeedItemId>,
        isRead: Boolean,
    ): DataResult<Unit> =
        when (account) {
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX, SyncAccounts.BAZQUX ->
                gReaderRepository.updateReadStatus(feedItemIds, isRead)
            SyncAccounts.FEEDBIN ->
                feedbinRepository.updateReadStatus(feedItemIds, isRead)
            SyncAccounts.LOCAL,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.ICLOUD,
            -> DataResult.Success(Unit)
        }

    private suspend fun DataResult<Unit>.handleRemoteReadStatusResult(
        account: SyncAccounts,
        feedItemIds: List<FeedItemId>,
        isRead: Boolean,
    ) {
        if (isSuccess()) {
            databaseHelper.deleteReadStatusPendingActions(
                feedItemIds = feedItemIds,
                isRead = isRead,
                syncAccount = account.name,
            )
        }
    }

    suspend fun markAllAboveAsRead(targetItemId: String) {
        val currentFilter = feedStateRepository.getCurrentFeedFilter()
        val feedOrder = feedAppearanceSettingsRepository.getFeedOrder()

        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX, SyncAccounts.BAZQUX -> {
                val itemIds = when (feedOrder) {
                    FeedOrder.NEWEST_FIRST -> databaseHelper.getNewerItems(targetItemId, currentFilter)
                    FeedOrder.OLDEST_FIRST -> databaseHelper.getOlderItems(targetItemId, currentFilter)
                }
                if (itemIds.isNotEmpty()) {
                    val feedItemIds = itemIds.map { FeedItemId(it) }
                    updateReadStatus(feedItemIds, isRead = true)
                }
            }

            SyncAccounts.FEEDBIN -> {
                val itemIds = when (feedOrder) {
                    FeedOrder.NEWEST_FIRST -> databaseHelper.getNewerItems(targetItemId, currentFilter)
                    FeedOrder.OLDEST_FIRST -> databaseHelper.getOlderItems(targetItemId, currentFilter)
                }
                if (itemIds.isNotEmpty()) {
                    val feedItemIds = itemIds.map { FeedItemId(it) }
                    updateReadStatus(feedItemIds, isRead = true)
                }
            }

            SyncAccounts.LOCAL,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.ICLOUD,
            -> {
                when (feedOrder) {
                    FeedOrder.NEWEST_FIRST -> databaseHelper.markAllNewerAsRead(targetItemId, currentFilter)
                    FeedOrder.OLDEST_FIRST -> databaseHelper.markAllOlderAsRead(targetItemId, currentFilter)
                }
                feedSyncRepository.setIsSyncUploadRequired()
            }
        }
        // Update the in-memory state without reloading everything
        feedStateRepository.markItemsAboveAsRead(targetItemId)
    }

    suspend fun markAllBelowAsRead(targetItemId: String) {
        val currentFilter = feedStateRepository.getCurrentFeedFilter()
        val feedOrder = feedAppearanceSettingsRepository.getFeedOrder()

        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX, SyncAccounts.BAZQUX -> {
                val itemIds = when (feedOrder) {
                    FeedOrder.NEWEST_FIRST -> databaseHelper.getOlderItems(targetItemId, currentFilter)
                    FeedOrder.OLDEST_FIRST -> databaseHelper.getNewerItems(targetItemId, currentFilter)
                }
                if (itemIds.isNotEmpty()) {
                    val feedItemIds = itemIds.map { FeedItemId(it) }
                    updateReadStatus(feedItemIds, isRead = true)
                }
            }

            SyncAccounts.FEEDBIN -> {
                val itemIds = when (feedOrder) {
                    FeedOrder.NEWEST_FIRST -> databaseHelper.getOlderItems(targetItemId, currentFilter)
                    FeedOrder.OLDEST_FIRST -> databaseHelper.getNewerItems(targetItemId, currentFilter)
                }
                if (itemIds.isNotEmpty()) {
                    val feedItemIds = itemIds.map { FeedItemId(it) }
                    updateReadStatus(feedItemIds, isRead = true)
                }
            }

            SyncAccounts.LOCAL,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.ICLOUD,
            -> {
                when (feedOrder) {
                    FeedOrder.NEWEST_FIRST -> databaseHelper.markAllOlderAsRead(targetItemId, currentFilter)
                    FeedOrder.OLDEST_FIRST -> databaseHelper.markAllNewerAsRead(targetItemId, currentFilter)
                }
                feedSyncRepository.setIsSyncUploadRequired()
            }
        }
        // Update the in-memory state without reloading everything
        feedStateRepository.markItemsBelowAsRead(targetItemId)
    }

    suspend fun markAllCurrentFeedAsRead() {
        markAllFeedAsRead(feedStateRepository.getCurrentFeedFilter())
    }

    suspend fun markAllFeedAsRead(feedFilter: FeedFilter) {
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX, SyncAccounts.BAZQUX -> {
                gReaderRepository.markAllFeedAsRead(feedFilter)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError(FeedSyncError.MarkAllFeedsAsReadFailed))
                    }
            }

            SyncAccounts.FEEDBIN -> {
                feedbinRepository.markAllFeedAsRead(feedFilter)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError(FeedSyncError.MarkAllFeedsAsReadFailed))
                    }
            }

            SyncAccounts.LOCAL,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.ICLOUD,
            -> {
                databaseHelper.markAllFeedAsRead(feedFilter)
                feedSyncRepository.setIsSyncUploadRequired()
            }
        }
        feedStateRepository.getFeeds()
    }

    suspend fun deleteOldFeeds() {
        // One week
        val threshold = Clock.System.now().minus(7.days).toEpochMilliseconds()
        val currentFilter = feedStateRepository.getCurrentFeedFilter()
        val oldFeedIds = databaseHelper.getOldFeedItem(threshold, currentFilter)
        databaseHelper.deleteOldFeedItems(threshold, currentFilter)
        feedSyncRepository.deleteFeedItems(oldFeedIds)
        feedStateRepository.getFeeds()
    }

    suspend fun updateBookmarkStatus(feedItemId: FeedItemId, isBookmarked: Boolean) {
        feedStateRepository.updateBookmarkStatus(feedItemId, isBookmarked)

        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX, SyncAccounts.BAZQUX -> {
                gReaderRepository.updateBookmarkStatus(feedItemId, isBookmarked)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError(FeedSyncError.UpdateBookmarkStatusFailed))
                    }
            }

            SyncAccounts.FEEDBIN -> {
                feedbinRepository.updateBookmarkStatus(feedItemId, isBookmarked)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError(FeedSyncError.UpdateBookmarkStatusFailed))
                    }
            }

            SyncAccounts.LOCAL,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.ICLOUD,
            -> {
                databaseHelper.updateBookmarkStatus(feedItemId, isBookmarked)
                feedSyncRepository.setIsSyncUploadRequired()
            }
        }

        if (isBookmarked) {
            val urlInfo = databaseHelper.getFeedItemUrlInfo(feedItemId.id)
            if (urlInfo != null) {
                feedItemParserWorker.parse(urlInfo.id, urlInfo.url, urlInfo.imageUrl)
            }
        }
    }

    suspend fun updateReadStatus(feedItemId: FeedItemId, isRead: Boolean) {
        feedStateRepository.updateReadStatus(feedItemId, isRead)
        updateReadStatus(
            feedItemIds = listOf(feedItemId),
            isRead = isRead,
        )
    }

    private fun SyncAccounts.isRemoteReadStatusAccount(): Boolean =
        this == SyncAccounts.FRESH_RSS ||
            this == SyncAccounts.MINIFLUX ||
            this == SyncAccounts.BAZQUX ||
            this == SyncAccounts.FEEDBIN

    fun search(
        query: String,
        feedFilter: FeedFilter? = null,
    ): Flow<List<Search>> =
        databaseHelper.search(
            searchQuery = query,
            feedFilter = feedFilter,
        )
}
