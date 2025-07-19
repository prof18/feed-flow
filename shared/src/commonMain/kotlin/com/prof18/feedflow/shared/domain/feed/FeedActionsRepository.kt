package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.onErrorSuspend
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.db.Search
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
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
) {
    suspend fun markAsRead(itemsToUpdates: HashSet<FeedItemId>) {
        feedStateRepository.markAsRead(itemsToUpdates)
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.updateReadStatus(itemsToUpdates.toList(), isRead = true)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError)
                    }
            }

            else -> {
                databaseHelper.markAsRead(itemsToUpdates.toList())
                feedSyncRepository.setIsSyncUploadRequired()
            }
        }
    }

    suspend fun markAllCurrentFeedAsRead() {
        val currentFilter = feedStateRepository.getCurrentFeedFilter()
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.markAllFeedAsRead(currentFilter)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError)
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
        val oldFeedIds = databaseHelper.getOldFeedItem(threshold)
        databaseHelper.deleteOldFeedItems(threshold)
        feedSyncRepository.deleteFeedItems(oldFeedIds)
        feedStateRepository.getFeeds()
    }

    suspend fun updateBookmarkStatus(feedItemId: FeedItemId, isBookmarked: Boolean) {
        feedStateRepository.updateBookmarkStatus(feedItemId, isBookmarked)

        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.updateBookmarkStatus(feedItemId, isBookmarked)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError)
                    }
            }

            else -> {
                databaseHelper.updateBookmarkStatus(feedItemId, isBookmarked)
                feedSyncRepository.setIsSyncUploadRequired()
            }
        }
    }

    suspend fun updateReadStatus(feedItemId: FeedItemId, isRead: Boolean) {
        feedStateRepository.updateReadStatus(feedItemId, isRead)

        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.updateReadStatus(listOf(feedItemId), isRead)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError)
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
