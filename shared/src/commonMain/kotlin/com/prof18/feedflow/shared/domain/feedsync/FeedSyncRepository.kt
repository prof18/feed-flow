package com.prof18.feedflow.shared.domain.feedsync

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.shared.data.SettingsHelper
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines

class FeedSyncRepository internal constructor(
    private val syncedDatabaseHelper: SyncedDatabaseHelper,
    private val feedSyncWorker: FeedSyncWorker,
    private val settingsHelper: SettingsHelper,
    private val feedSyncAccountRepository: FeedSyncAccountsRepository,
    private val feedSyncMessageQueue: FeedSyncMessageQueue,
) {
    fun enqueueBackup(forceBackup: Boolean = false) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            if (forceBackup || settingsHelper.getIsSyncUploadRequired()) {
                feedSyncWorker.upload()
            }
        }
    }

    @NativeCoroutines
    suspend fun performBackup(forceBackup: Boolean = false) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            if (forceBackup || settingsHelper.getIsSyncUploadRequired()) {
                feedSyncWorker.uploadImmediate()
            }
        }
    }

    internal suspend fun addSourceAndCategories(sources: List<FeedSource>, categories: List<FeedSourceCategory>) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            syncedDatabaseHelper.insertSyncedFeedSource(sources)
            syncedDatabaseHelper.insertFeedSourceCategories(categories)
            settingsHelper.setIsSyncUploadRequired(true)
        }
    }

    internal suspend fun insertSyncedFeedSource(sources: List<FeedSource>) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            syncedDatabaseHelper.insertSyncedFeedSource(sources)
            settingsHelper.setIsSyncUploadRequired(true)
        }
    }

    internal suspend fun insertFeedSourceCategories(categories: List<FeedSourceCategory>) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            syncedDatabaseHelper.insertFeedSourceCategories(categories)
            settingsHelper.setIsSyncUploadRequired(true)
        }
    }

    internal suspend fun deleteFeedSource(feedSource: FeedSource) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            syncedDatabaseHelper.deleteFeedSource(feedSource.id)
            settingsHelper.setIsSyncUploadRequired(true)
        }
    }

    internal suspend fun deleteFeedSourceCategory(categoryId: String) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            syncedDatabaseHelper.deleteFeedSourceCategory(categoryId)
            settingsHelper.setIsSyncUploadRequired(true)
        }
    }

    internal fun deleteAllFeedSources() {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            syncedDatabaseHelper.deleteAllFeedSources()
            settingsHelper.setIsSyncUploadRequired(true)
        }
    }

    internal suspend fun updateFeedSourceName(feedSourceId: String, newName: String) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            syncedDatabaseHelper.updateFeedSourceName(feedSourceId, newName)
            settingsHelper.setIsSyncUploadRequired(true)
        }
    }

    internal suspend fun deleteFeedItems(feedIds: List<FeedItemId>) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            syncedDatabaseHelper.deleteFeedItems(feedIds)
            settingsHelper.setIsSyncUploadRequired(true)
        }
    }

    internal suspend fun updateFeedItemReadStatus(feedItemId: FeedItemId, isRead: Boolean) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            syncedDatabaseHelper.setFeedItemAsRead(feedItemId, isRead)
            settingsHelper.setIsSyncUploadRequired(true)
        }
    }

    internal suspend fun updateFeedItemBookmarkStatus(feedItemId: FeedItemId, isBookmarked: Boolean) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            syncedDatabaseHelper.setFeedItemAsBookmarked(feedItemId, isBookmarked)
            settingsHelper.setIsSyncUploadRequired(true)
        }
    }

    internal suspend fun markAllFeedAsRead(allFeedItemIds: List<FeedItemId>) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            for (feedItemId in allFeedItemIds) {
                syncedDatabaseHelper.setFeedItemAsRead(feedItemId, true)
            }
            settingsHelper.setIsSyncUploadRequired(true)
        }
    }

    internal suspend fun syncFeedSources() {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            val result = feedSyncWorker.download()
            if (result.isError()) {
                Logger.d { "Error on download" }
                feedSyncMessageQueue.emitResult(result)
            }

            val feedSourcesResult = feedSyncWorker.syncFeedSources()
            if (feedSourcesResult.isError()) {
                feedSyncMessageQueue.emitResult(feedSourcesResult)
            }
        }
    }

    internal suspend fun syncFeedItems() {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            val feedItemResult = feedSyncWorker.syncFeedItems()
            feedSyncMessageQueue.emitResult(feedItemResult)
        }
    }
}
