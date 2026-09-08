package com.prof18.feedflow.shared.domain.feedsync

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.shared.data.SettingsRepository
import kotlin.time.Clock

class FeedSyncRepository internal constructor(
    private val syncedDatabaseHelper: SyncedDatabaseHelper,
    private val feedSyncWorker: FeedSyncWorker,
    private val feedSyncAccountRepository: AccountsRepository,
    private val feedSyncMessageQueue: FeedSyncMessageQueue,
    private val dropboxSettings: DropboxSettings,
    private val logger: Logger,
    private val settingsRepository: SettingsRepository,
    private val pendingCloudChanges: PendingCloudChangesManager,
) {
    private var canApplyDownloadedItems = true
    val isUploadRequired = pendingCloudChanges.isUploadRequired

    internal suspend fun cloudSessionForEdit(): String? = pendingCloudChanges.sessionForEdit()

    suspend fun enqueueBackup(forceBackup: Boolean = false) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            if (forceBackup || settingsRepository.getIsSyncUploadRequired() ||
                pendingCloudChanges.hasPendingChanges()
            ) {
                feedSyncWorker.upload()
            }
        }
    }

    suspend fun performBackup(forceBackup: Boolean = false) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            if (forceBackup || settingsRepository.getIsSyncUploadRequired() ||
                pendingCloudChanges.hasPendingChanges()
            ) {
                feedSyncWorker.uploadImmediate()
            }
        }
    }

    // Used only on iOS when the system performs a background upload
    fun onDropboxUploadSuccessAfterResume() {
        if (feedSyncAccountRepository.getCurrentSyncAccount() != SyncAccounts.DROPBOX) return
        dropboxSettings.setLastUploadTimestamp(Clock.System.now().toEpochMilliseconds())
        logger.d { "Upload to dropbox successfully from restarted session" }
        // A resumed request has no captured edit generation; it cannot acknowledge newer work.
    }

    internal suspend fun firstSync() {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            logger.d { "run first sync" }
            val result = feedSyncWorker.download(isFirstSync = true)
            if (result is SyncResult.BackupNotFound) {
                feedSyncWorker.uploadImmediate()
            } else if (result.isError()) {
                feedSyncMessageQueue.emitResult(result)
            }
        }
    }

    internal suspend fun addSourceAndCategories(sources: List<FeedSource>, categories: List<FeedSourceCategory>) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            withErrorHandling {
                syncedDatabaseHelper.insertSyncedFeedSource(sources)
                syncedDatabaseHelper.insertFeedSourceCategories(categories)
                settingsRepository.setIsSyncUploadRequired(true)
            }
        }
    }

    internal suspend fun insertSyncedFeedSource(sources: List<FeedSource>) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            withErrorHandling {
                syncedDatabaseHelper.insertSyncedFeedSource(sources)
                settingsRepository.setIsSyncUploadRequired(true)
            }
        }
    }

    internal suspend fun insertFeedSourceCategories(categories: List<FeedSourceCategory>) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            withErrorHandling {
                syncedDatabaseHelper.insertFeedSourceCategories(categories)
                settingsRepository.setIsSyncUploadRequired(true)
            }
        }
    }

    internal suspend fun updateCategory(category: FeedSourceCategory) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            withErrorHandling {
                syncedDatabaseHelper.updateCategoryName(
                    categoryId = category.id,
                    newName = category.title,
                )
                settingsRepository.setIsSyncUploadRequired(true)
            }
        }
    }

    internal suspend fun deleteFeedSource(feedSource: FeedSource) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            withErrorHandling {
                syncedDatabaseHelper.deleteFeedSource(feedSource.id)
                settingsRepository.setIsSyncUploadRequired(true)
            }
        }
    }

    internal suspend fun deleteFeedSourceCategory(categoryId: String) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            withErrorHandling {
                syncedDatabaseHelper.deleteFeedSourceCategory(categoryId)
                settingsRepository.setIsSyncUploadRequired(true)
            }
        }
    }

    internal suspend fun deleteAllFeedSources() {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            withErrorHandling {
                syncedDatabaseHelper.deleteAllFeedSources()
                settingsRepository.setIsSyncUploadRequired(true)
            }
        }
    }

    internal suspend fun updateFeedSourceName(feedSourceId: String, newName: String) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            withErrorHandling {
                syncedDatabaseHelper.updateFeedSourceName(feedSourceId, newName)
                settingsRepository.setIsSyncUploadRequired(true)
            }
        }
    }

    internal suspend fun updateFeedSource(feedSource: FeedSource) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            withErrorHandling {
                syncedDatabaseHelper.updateFeedSource(feedSource)
                settingsRepository.setIsSyncUploadRequired(true)
            }
        }
    }

    internal suspend fun deleteFeedItems(feedIds: List<FeedItemId>) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            withErrorHandling {
                syncedDatabaseHelper.deleteFeedItems(feedIds)
                settingsRepository.setIsSyncUploadRequired(true)
            }
        }
    }

    internal suspend fun updateFeedItemsReadStatus(feedItemIds: List<FeedItemId>, isRead: Boolean) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            withErrorHandling {
                syncedDatabaseHelper.updateFeedItemsReadStatus(feedItemIds, isRead)
                settingsRepository.setIsSyncUploadRequired(true)
            }
        }
    }

    internal suspend fun updateFeedItemBookmarkStatus(feedItemId: FeedItemId, isBookmarked: Boolean) {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            withErrorHandling {
                syncedDatabaseHelper.updateFeedItemBookmarkStatus(feedItemId, isBookmarked)
                settingsRepository.setIsSyncUploadRequired(true)
            }
        }
    }

    internal fun setIsSyncUploadRequired() {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            settingsRepository.setIsSyncUploadRequired(true)
        }
    }

    internal suspend fun syncFeedSources() {
        if (feedSyncAccountRepository.isSyncEnabled()) {
            pendingCloudChanges.sessionForEdit()
            canApplyDownloadedItems = false
            val result = feedSyncWorker.download()
            if (result is SyncResult.BackupNotFound) {
                feedSyncWorker.uploadImmediate()
                return
            }
            if (result.isError()) {
                Logger.d { "Error on download" }
                feedSyncMessageQueue.emitResult(result)
                return
            }

            val feedSourcesResult = feedSyncWorker.syncFeedSources()
            if (feedSourcesResult.isError()) {
                feedSyncMessageQueue.emitResult(feedSourcesResult)
                return
            }
            canApplyDownloadedItems = true
        }
    }

    internal suspend fun syncFeedItems() {
        if (feedSyncAccountRepository.isSyncEnabled() && canApplyDownloadedItems) {
            val feedItemResult = feedSyncWorker.syncFeedItems()
            feedSyncMessageQueue.emitResult(feedItemResult)
        }
    }

    internal suspend fun deleteAll() {
        syncedDatabaseHelper.deleteAllData()
    }

    private suspend fun withErrorHandling(
        body: suspend () -> Unit,
    ) {
        try {
            pendingCloudChanges.sessionForEdit()
            body()
        } catch (e: Exception) {
            logger.d(e) { "Error during feed sync" }
        }
    }
}
