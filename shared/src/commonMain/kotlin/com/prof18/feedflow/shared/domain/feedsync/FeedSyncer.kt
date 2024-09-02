package com.prof18.feedflow.shared.domain.feedsync

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.database.DatabaseTables
import com.prof18.feedflow.feedsync.database.data.SyncTable
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import kotlinx.datetime.Clock

internal class FeedSyncer(
    private val syncedDatabaseHelper: SyncedDatabaseHelper,
    private val appDatabaseHelper: DatabaseHelper,
    private val logger: Logger,
) {

    suspend fun populateSyncDbIfEmpty() {
        if (syncedDatabaseHelper.isDatabaseEmpty()) {
            val allFeedSources = appDatabaseHelper.getFeedSources()
            syncedDatabaseHelper.insertSyncedFeedSource(allFeedSources)

            val allFeeCategories = appDatabaseHelper.getFeedSourceCategories()
            syncedDatabaseHelper.insertFeedSourceCategories(allFeeCategories)

            val allFeedItems = appDatabaseHelper.getFeedItemsForSync()
            syncedDatabaseHelper.insertFeedItems(allFeedItems)
        }
    }

    suspend fun updateFeedItemsToSyncDatabase() {
        val allFeedItems = appDatabaseHelper.getFeedItemsForSync()
        syncedDatabaseHelper.insertFeedItems(allFeedItems)
    }

    suspend fun syncFeedSource() {
        val lastSyncTimestamp = appDatabaseHelper.getLastChangeTimestamp(DatabaseTables.FEED_SOURCE)
        val remoteSyncTimestamp = syncedDatabaseHelper.getLastChangeTimestamp(SyncTable.SYNCED_FEED_SOURCE)

        // If the local database is up-to-date, return
        val skipSync = lastSyncTimestamp != null &&
            remoteSyncTimestamp != null &&
            lastSyncTimestamp == remoteSyncTimestamp

        if (skipSync) {
            return
        }

        val appUrlHashes = appDatabaseHelper.getAllFeedSourceIds().toSet()
        val syncFeedSources = syncedDatabaseHelper.getAllFeedSources()

        if (syncFeedSources.isNotEmpty()) {
            val syncedUrlHashes = syncFeedSources.map { it.id }.toSet()
            appDatabaseHelper.insertFeedSource(
                syncFeedSources.map { syncedFeedSource ->
                    ParsedFeedSource(
                        id = syncedFeedSource.id,
                        url = syncedFeedSource.url,
                        title = syncedFeedSource.title,
                        category = syncedFeedSource.categoryId?.let {
                            FeedSourceCategory(
                                id = it.value,
                                title = "", // not necessary here, the caller doesn't use the name
                            )
                        },
                        logoUrl = syncedFeedSource.logoUrl,
                    )
                },
            )

            val urlHashesToDelete = appUrlHashes - syncedUrlHashes
            logger.d { "appUrlHash: $appUrlHashes" }
            logger.d { "syncedUrlHashes: $syncedUrlHashes" }
            logger.d { "urlHashedToDelete: $urlHashesToDelete" }
            urlHashesToDelete.forEach { urlHash ->
                appDatabaseHelper.deleteFeedSource(urlHash)
            }
        }

        val currentTimestamp = Clock.System.now().toEpochMilliseconds()
        appDatabaseHelper.updateSyncMetadata(DatabaseTables.FEED_SOURCE, currentTimestamp)

        logger.d { "Feed Source sync completed" }
    }

    suspend fun syncFeedSourceCategory() {
        val lastSyncTimestamp = appDatabaseHelper.getLastChangeTimestamp(DatabaseTables.FEED_SOURCE_CATEGORY)
        val remoteSyncTimestamp = syncedDatabaseHelper.getLastChangeTimestamp(SyncTable.SYNCED_FEED_SOURCE_CATEGORY)

        // If the local database is up-to-date, return
        val skipSync = lastSyncTimestamp != null &&
            remoteSyncTimestamp != null &&
            lastSyncTimestamp == remoteSyncTimestamp

        if (skipSync) {
            return
        }

        val appIds = appDatabaseHelper.getAllCategoryIds().toSet()

        val syncFeedSourceCategories = syncedDatabaseHelper.getAllFeedSourceCategories()

        if (syncFeedSourceCategories.isNotEmpty()) {
            val syncedIds = syncFeedSourceCategories.map { it.id }.toSet()
            appDatabaseHelper.insertCategories(
                syncFeedSourceCategories.map { syncedFeedSourceCategory ->
                    FeedSourceCategory(
                        id = syncedFeedSourceCategory.id,
                        title = syncedFeedSourceCategory.title,
                    )
                },
            )

            // Sync deletions
            val idsToDelete = appIds - syncedIds
            idsToDelete.forEach { id ->
                appDatabaseHelper.deleteCategory(id)
            }
        }

        val currentTimestamp = Clock.System.now().toEpochMilliseconds()
        appDatabaseHelper.updateSyncMetadata(DatabaseTables.FEED_SOURCE_CATEGORY, currentTimestamp)

        logger.d { "Feed Source category sync completed" }
    }

    suspend fun syncFeedItem() {
        val lastSyncTimestamp = appDatabaseHelper.getLastChangeTimestamp(DatabaseTables.FEED_ITEM)
        val remoteSyncTimestamp = syncedDatabaseHelper.getLastChangeTimestamp(SyncTable.SYNCED_FEED_ITEM)

        // If the local database is up-to-date, return
        val skipSync = lastSyncTimestamp != null &&
            remoteSyncTimestamp != null &&
            lastSyncTimestamp == remoteSyncTimestamp

        if (skipSync) {
            return
        }

        // Sync updates
        val syncFeedItems = syncedDatabaseHelper.getAllFeedItems()

        if (syncFeedItems.isNotEmpty()) {
            appDatabaseHelper.updateFeedItemReadAndBookmarked(
                syncedFeedItems = syncFeedItems,
            )
        }

        val currentTimestamp = Clock.System.now().toEpochMilliseconds()
        appDatabaseHelper.updateSyncMetadata(DatabaseTables.FEED_ITEM, currentTimestamp)

        logger.d { "Feed Item sync completed" }
    }

    fun closeDB() {
        syncedDatabaseHelper.closeScope()
    }
}
