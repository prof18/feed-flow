package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSyncError
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.onErrorSuspend
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.db.Search
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
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
    private val accountsRepository: AccountsRepository,
    private val feedStateRepository: FeedStateRepository,
    private val feedItemParserWorker: FeedItemParserWorker,
) {
    suspend fun markAsRead(itemsToUpdates: HashSet<FeedItemId>) {
        feedStateRepository.markAsRead(itemsToUpdates)
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.updateReadStatus(itemsToUpdates.toList(), isRead = true)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError(FeedSyncError.MarkItemsAsReadFailed))
                    }
            }

            else -> {
                databaseHelper.markAsRead(itemsToUpdates.toList())
                feedSyncRepository.setIsSyncUploadRequired()
            }
        }
    }

    suspend fun markAllAboveAsRead(targetItemId: String) {
        val currentFilter = feedStateRepository.getCurrentFeedFilter()
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                val itemIds = databaseHelper.getItemsAbove(targetItemId, currentFilter)
                if (itemIds.isNotEmpty()) {
                    val feedItemIds = itemIds.map { FeedItemId(it) }
                    gReaderRepository.updateReadStatus(feedItemIds, isRead = true)
                        .onErrorSuspend {
                            feedStateRepository.emitErrorState(SyncError(FeedSyncError.MarkItemsAsReadFailed))
                        }
                }
            }

            else -> {
                databaseHelper.markAllAboveAsRead(targetItemId, currentFilter)
                feedSyncRepository.setIsSyncUploadRequired()
            }
        }
        // Update the in-memory state without reloading everything
        feedStateRepository.markItemsAboveAsRead(targetItemId)
    }

    suspend fun markAllBelowAsRead(targetItemId: String) {
        val currentFilter = feedStateRepository.getCurrentFeedFilter()
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                val itemIds = databaseHelper.getItemsBelow(targetItemId, currentFilter)
                if (itemIds.isNotEmpty()) {
                    val feedItemIds = itemIds.map { FeedItemId(it) }
                    gReaderRepository.updateReadStatus(feedItemIds, isRead = true)
                        .onErrorSuspend {
                            feedStateRepository.emitErrorState(SyncError(FeedSyncError.MarkItemsAsReadFailed))
                        }
                }
            }

            else -> {
                databaseHelper.markAllBelowAsRead(targetItemId, currentFilter)
                feedSyncRepository.setIsSyncUploadRequired()
            }
        }
        // Update the in-memory state without reloading everything
        feedStateRepository.markItemsBelowAsRead(targetItemId)
    }

    suspend fun markAllCurrentFeedAsRead() {
        val currentFilter = feedStateRepository.getCurrentFeedFilter()
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.markAllFeedAsRead(currentFilter)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError(FeedSyncError.MarkAllFeedsAsReadFailed))
                    }
            }

            else -> {
                databaseHelper.markAllFeedAsRead(currentFilter)
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
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.updateBookmarkStatus(feedItemId, isBookmarked)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError(FeedSyncError.UpdateBookmarkStatusFailed))
                    }
            }

            else -> {
                databaseHelper.updateBookmarkStatus(feedItemId, isBookmarked)
                feedSyncRepository.setIsSyncUploadRequired()
            }
        }

        if (isBookmarked) {
            val urlInfo = databaseHelper.getFeedItemUrlInfo(feedItemId.id)
            if (urlInfo != null) {
                feedItemParserWorker.parse(urlInfo.id, urlInfo.url)
            }
        }
    }

    suspend fun updateReadStatus(feedItemId: FeedItemId, isRead: Boolean) {
        feedStateRepository.updateReadStatus(feedItemId, isRead)

        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.updateReadStatus(listOf(feedItemId), isRead)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError(FeedSyncError.UpdateReadStatusFailed))
                    }
            }

            else -> {
                databaseHelper.updateReadStatus(feedItemId, isRead)
                feedSyncRepository.setIsSyncUploadRequired()
            }
        }
    }

    fun search(
        query: String,
        feedFilter: FeedFilter? = null,
        showReadItems: Boolean = true,
    ): Flow<List<Search>> =
        databaseHelper.search(
            searchQuery = query,
            feedFilter = feedFilter ?: feedStateRepository.getCurrentFeedFilter(),
            showReadItems = showReadItems,
        )
}
