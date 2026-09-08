package com.prof18.feedflow.feedsync.database.data

import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncedFeedItem
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import com.prof18.feedflow.feedsync.database.di.FEED_SYNC_SCOPE_NAME
import com.prof18.feedflow.feedsync.database.di.SYNC_DB_DRIVER
import com.prof18.feedflow.feedsync.database.model.SyncedFeedSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import kotlin.time.Clock

class SyncedDatabaseHelper(
    private val backgroundDispatcher: CoroutineDispatcher,
    private val koinContext: Koin? = null,
    private val clock: Clock = Clock.System,
) : KoinComponent {

    override fun getKoin(): Koin = koinContext ?: super.getKoin()

    private var database: FeedFlowFeedSyncDB? = null
    private var driver: SqlDriver? = null
    private var scope: Scope? = null
    private var driverClosed = false
    private var closeFailed = false
    private val dbMutex = Mutex()

    private suspend fun <T> withDatabase(block: (FeedFlowFeedSyncDB) -> T): T =
        withContext(backgroundDispatcher) {
            dbMutex.withLock {
                if (closeFailed) closeScopeLocked()
                if (database == null) {
                    val newScope = getKoin().getOrCreateScope(FEED_SYNC_SCOPE_NAME, named(FEED_SYNC_SCOPE_NAME))
                    val newDriver = newScope.get<SqlDriver>(qualifier = named(SYNC_DB_DRIVER))
                    scope = newScope
                    driver = newDriver
                    database = FeedFlowFeedSyncDB(newDriver)
                    driverClosed = false
                }
                block(requireNotNull(database))
            }
        }

    suspend fun closeScope() = withContext(backgroundDispatcher) {
        dbMutex.withLock {
            closeScopeLocked()
        }
    }

    suspend fun <T> withClosedDatabase(block: suspend () -> T): T = withContext(backgroundDispatcher) {
        dbMutex.withLock {
            closeScopeLocked()
            block()
        }
    }

    private fun closeScopeLocked() {
        if (driver == null && database == null && scope == null) {
            closeFailed = false
            return
        }

        try {
            if (!driverClosed) {
                driver?.close()
                driverClosed = true
            }
            scope?.close()
        } catch (e: Exception) {
            closeFailed = true
            throw e
        }

        database = null
        driver = null
        scope = null
        driverClosed = false
        closeFailed = false
    }

    suspend fun insertSyncedFeedSource(sources: List<FeedSource>) {
        withDatabase { database ->
            database.transaction {
                sources.forEach { source ->
                    val category = source.category
                    if (category != null) {
                        database.syncedFeedSourceCategoryQueries.insertOrIgnoreFeedSourceCategory(
                            id = category.id,
                            title = category.title,
                        )
                    }
                    database.syncedFeedSourceQueries.insertOrIgnoreFeedSource(
                        url_hash = source.id,
                        url = source.url,
                        title = source.title,
                        category_id = category?.id,
                        logo_url = source.logoUrl,
                    )
                }
                database.updateMetadata(SyncTable.SYNCED_FEED_SOURCE)
            }
        }
    }

    suspend fun updateFeedSourceName(feedSourceId: String, newName: String) {
        withDatabase { database ->
            database.syncedFeedSourceQueries.updateFeedSourceTitle(
                title = newName,
                urlHash = feedSourceId,
            )
        }
    }

    suspend fun updateFeedSource(feedSource: FeedSource) {
        withDatabase { database ->
            database.syncedFeedSourceQueries.updateFeedSource(
                urlHash = feedSource.id,
                url = feedSource.url,
                title = feedSource.title,
                categoryId = feedSource.category?.id,
            )
        }
    }

    suspend fun getAllFeedSources(): List<SyncedFeedSource> = withDatabase { database ->
        database.syncedFeedSourceQueries
            .getAllSyncedFeedSources()
            .executeAsList()
            .map { source ->
                SyncedFeedSource(
                    id = source.url_hash,
                    url = source.url,
                    title = source.title,
                    categoryId = source.category_id?.let { CategoryId(it) },
                    logoUrl = source.logo_url,
                )
            }
    }

    suspend fun deleteFeedSource(sourceId: String) {
        withDatabase { database ->
            database.transaction {
                database.syncedFeedSourceQueries.delete(sourceId)
                database.updateMetadata(SyncTable.SYNCED_FEED_SOURCE)
            }
        }
    }

    suspend fun insertFeedSourceCategories(categories: List<FeedSourceCategory>) {
        withDatabase { database ->
            database.transaction {
                categories.forEach { category ->
                    database.syncedFeedSourceCategoryQueries.insertOrIgnoreFeedSourceCategory(
                        id = category.id,
                        title = category.title,
                    )
                }
                database.updateMetadata(SyncTable.SYNCED_FEED_SOURCE_CATEGORY)
            }
        }
    }

    suspend fun updateCategoryName(categoryId: String, newName: String) {
        withDatabase { database ->
            database.syncedFeedSourceCategoryQueries.updateCategoryName(
                title = newName,
                id = categoryId,
            )
        }
    }

    suspend fun getAllFeedSourceCategories(): List<FeedSourceCategory> = withDatabase { database ->
        database.syncedFeedSourceCategoryQueries
            .getAllFeedSourceCategories()
            .executeAsList()
            .map { category ->
                FeedSourceCategory(
                    id = category.id,
                    title = category.title,
                )
            }
    }

    suspend fun deleteFeedSourceCategory(categoryId: String) {
        withDatabase { database ->
            database.transaction {
                database.syncedFeedSourceCategoryQueries.delete(categoryId)
                database.updateMetadata(SyncTable.SYNCED_FEED_SOURCE_CATEGORY)
            }
        }
    }

    suspend fun deleteAllFeedSources() = withDatabase { database ->
        database.syncedFeedSourceQueries.deleteAll()
    }

    suspend fun getLastChangeTimestamp(tableName: SyncTable): Long? = withDatabase { database ->
        database.syncedMetadataQueries.selectLastChangeTimestamp(tableName.tableName)
            .executeAsOneOrNull()?.last_change_timestamp
    }

    suspend fun getAllFeedItems(): List<SyncedFeedItem> = withDatabase { database ->
        database.syncedFeedItemQueries
            .selectAllSyncedFeedItems()
            .executeAsList()
            .map { item ->
                SyncedFeedItem(
                    id = item.url_hash,
                    isRead = item.is_read,
                    isBookmarked = item.is_bookmarked,
                )
            }
    }

    suspend fun insertFeedItems(feedItems: List<SyncedFeedItem>) {
        withDatabase { database ->
            database.transaction {
                feedItems.forEach { feedItem ->
                    database.syncedFeedItemQueries.insertOrReplaceSyncedFeedItem(
                        url_hash = feedItem.id,
                        is_read = feedItem.isRead,
                        is_bookmarked = feedItem.isBookmarked,
                    )
                }
                database.updateMetadata(SyncTable.SYNCED_FEED_ITEM)
            }
        }
    }

    suspend fun replaceSnapshot(
        sources: List<FeedSource>,
        categories: List<FeedSourceCategory>,
        items: List<SyncedFeedItem>,
    ) = withDatabase { database ->
        database.transaction {
            database.syncedFeedItemQueries.deleteAll()
            database.syncedFeedSourceQueries.deleteAll()
            database.syncedFeedSourceCategoryQueries.deleteAll()
            categories.forEach { category ->
                database.syncedFeedSourceCategoryQueries.insertOrIgnoreFeedSourceCategory(category.id, category.title)
            }
            sources.forEach { source ->
                database.syncedFeedSourceQueries.insertOrIgnoreFeedSource(
                    source.id,
                    source.url,
                    source.title,
                    source.category?.id,
                    source.logoUrl,
                )
            }
            items.forEach { item ->
                database.syncedFeedItemQueries.insertOrReplaceSyncedFeedItem(item.id, item.isRead, item.isBookmarked)
            }
            SyncTable.entries.forEach { database.updateMetadata(it) }
        }
    }

    suspend fun updateFeedItemsReadStatus(feedItemIds: List<FeedItemId>, isRead: Boolean) {
        withDatabase { database ->
            database.transaction {
                feedItemIds.forEach { feedItemId ->
                    database.syncedFeedItemQueries.insertOrIgnoreSyncedFeedItem(
                        url_hash = feedItemId.id,
                        is_read = false,
                        is_bookmarked = false,
                    )
                    database.syncedFeedItemQueries.updateIsRead(
                        isRead = isRead,
                        urlHash = feedItemId.id,
                    )
                }
                database.updateMetadata(SyncTable.SYNCED_FEED_ITEM)
            }
        }
    }

    suspend fun updateFeedItemBookmarkStatus(feedItemId: FeedItemId, isBookmarked: Boolean) {
        withDatabase { database ->
            database.transaction {
                database.syncedFeedItemQueries.insertOrIgnoreSyncedFeedItem(
                    url_hash = feedItemId.id,
                    is_read = false,
                    is_bookmarked = false,
                )
                database.syncedFeedItemQueries.updateIsBookmarked(
                    isBookmarked = isBookmarked,
                    urlHash = feedItemId.id,
                )
                database.updateMetadata(SyncTable.SYNCED_FEED_ITEM)
            }
        }
    }

    suspend fun applyPendingArticleFlags(readFields: Map<String, Boolean>, bookmarkFields: Map<String, Boolean>) {
        if (readFields.isEmpty() && bookmarkFields.isEmpty()) return
        withDatabase { database ->
            database.transaction {
                (readFields.keys + bookmarkFields.keys).forEach { id ->
                    database.syncedFeedItemQueries.insertOrIgnoreSyncedFeedItem(id, false, false)
                }
                readFields.forEach { (id, value) -> database.syncedFeedItemQueries.updateIsRead(value, id) }
                bookmarkFields.forEach { (id, value) -> database.syncedFeedItemQueries.updateIsBookmarked(value, id) }
                database.updateMetadata(SyncTable.SYNCED_FEED_ITEM)
            }
        }
    }

    suspend fun isDatabaseEmpty(): Boolean = withDatabase { database ->
        database.syncedMetadataQueries.isSyncDatabaseEmpty().executeAsOne() == 0L
    }

    suspend fun deleteFeedItems(feedItemIds: List<FeedItemId>) {
        withDatabase { database ->
            database.transaction {
                feedItemIds.forEach { feedItemId ->
                    database.syncedFeedItemQueries.deleteSyncedFeedItem(feedItemId.id)
                }
                database.updateMetadata(SyncTable.SYNCED_FEED_ITEM)
            }
        }
    }

    suspend fun deleteAllData() {
        withDatabase { database ->
            database.transaction {
                database.syncedFeedItemQueries.deleteAll()
                database.syncedFeedSourceQueries.deleteAll()
                database.syncedFeedSourceCategoryQueries.deleteAll()
                database.syncedMetadataQueries.deleteAll()
            }
        }
    }

    private fun FeedFlowFeedSyncDB.updateMetadata(table: SyncTable) {
        syncedMetadataQueries.insertMetadata(
            table_name = table.tableName,
            last_change_timestamp = clock.now().toEpochMilliseconds(),
        )
    }

    companion object {
        const val SYNC_DATABASE_NAME_PROD = "FeedFlowFeedSyncDB"
        const val SYNC_DATABASE_NAME_DEBUG = "FeedFlowFeedSyncDB-debug"
    }
}
