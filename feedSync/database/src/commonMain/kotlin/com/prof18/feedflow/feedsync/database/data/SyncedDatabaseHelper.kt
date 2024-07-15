package com.prof18.feedflow.feedsync.database.data

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransactionWithoutReturn
import app.cash.sqldelight.db.SqlDriver
import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.concurrency.value
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
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named
import kotlin.coroutines.CoroutineContext

class SyncedDatabaseHelper(
    private val backgroundDispatcher: CoroutineDispatcher,
) : KoinComponent {

    private var dbRef: AtomicReference<FeedFlowFeedSyncDB?> = AtomicReference(null)

    private fun getDbRef(): FeedFlowFeedSyncDB {
        if (dbRef.value == null) {
            val scope = getKoin().getOrCreateScope(FEED_SYNC_SCOPE_NAME, named(FEED_SYNC_SCOPE_NAME))

            val driver = scope.get<SqlDriver>(qualifier = named(SYNC_DB_DRIVER))
            dbRef.set(FeedFlowFeedSyncDB(driver))
        }
        return requireNotNull(dbRef.get())
    }

    fun closeScope() {
        val scope = getKoin().getOrCreateScope(FEED_SYNC_SCOPE_NAME, named(FEED_SYNC_SCOPE_NAME))
        val driver = scope.get<SqlDriver>(qualifier = named(SYNC_DB_DRIVER))
        driver.close()
        scope.close()
        dbRef.set(null)
    }

    suspend fun insertSyncedFeedSource(sources: List<FeedSource>) {
        val dbRef = getDbRef()
        dbRef.transactionWithContext(backgroundDispatcher) {
            sources.forEach { source ->
                val category = source.category
                if (category != null) {
                    dbRef.syncedFeedSourceCategoryQueries.insertOrIgnoreFeedSourceCategory(
                        id = category.id,
                        title = category.title,
                    )
                }
                dbRef.syncedFeedSourceQueries.insertOrIgnoreFeedSource(
                    url_hash = source.id,
                    url = source.url,
                    title = source.title,
                    category_id = category?.id,
                    logo_url = source.logoUrl,
                )
            }
            updateMetadata(SyncTable.SYNCED_FEED_SOURCE)
        }
    }

    suspend fun updateFeedSourceName(feedSourceId: String, newName: String) =
        getDbRef().transactionWithContext(backgroundDispatcher) {
            getDbRef().syncedFeedSourceQueries.updateFeedSourceTitle(
                title = newName,
                urlHash = feedSourceId,
            )
        }

    suspend fun getAllFeedSources(): List<SyncedFeedSource> = withContext(backgroundDispatcher) {
        getDbRef().syncedFeedSourceQueries
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

    suspend fun deleteFeedSource(sourceId: String) =
        getDbRef().transactionWithContext(backgroundDispatcher) {
            getDbRef().syncedFeedSourceQueries.delete(sourceId)
            updateMetadata(SyncTable.SYNCED_FEED_SOURCE)
        }

    suspend fun insertFeedSourceCategories(categories: List<FeedSourceCategory>) =
        getDbRef().transactionWithContext(backgroundDispatcher) {
            categories.forEach { category ->
                getDbRef().syncedFeedSourceCategoryQueries.insertOrIgnoreFeedSourceCategory(
                    id = category.id,
                    title = category.title,
                )
            }
            updateMetadata(SyncTable.SYNCED_FEED_SOURCE_CATEGORY)
        }

    suspend fun getAllFeedSourceCategories(): List<FeedSourceCategory> = withContext(backgroundDispatcher) {
        getDbRef().syncedFeedSourceCategoryQueries
            .getAllFeedSourceCategories()
            .executeAsList()
            .map { category ->
                FeedSourceCategory(
                    id = category.id,
                    title = category.title,
                )
            }
    }

    suspend fun deleteFeedSourceCategory(categoryId: String) =
        getDbRef().transactionWithContext(backgroundDispatcher) {
            getDbRef().syncedFeedSourceCategoryQueries.delete(categoryId)
            updateMetadata(SyncTable.SYNCED_FEED_SOURCE_CATEGORY)
        }

    fun deleteAllFeedSources() =
        getDbRef().syncedFeedSourceQueries.deleteAll()

    suspend fun getLastChangeTimestamp(tableName: SyncTable): Long? = withContext(backgroundDispatcher) {
        getDbRef().syncedMetadataQueries.selectLastChangeTimestamp(tableName.tableName)
            .executeAsOneOrNull()?.last_change_timestamp
    }

    suspend fun getAllFeedItems(): List<SyncedFeedItem> = withContext(backgroundDispatcher) {
        getDbRef().syncedFeedItemQueries
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

    suspend fun insertFeedItems(feedItems: List<SyncedFeedItem>) =
        getDbRef().transactionWithContext(backgroundDispatcher) {
            feedItems.forEach { feedItem ->
                getDbRef().syncedFeedItemQueries.insertOrReplaceSyncedFeedItem(
                    url_hash = feedItem.id,
                    is_read = feedItem.isRead,
                    is_bookmarked = feedItem.isBookmarked,
                )
            }
            updateMetadata(SyncTable.SYNCED_FEED_ITEM)
        }

    suspend fun isDatabaseEmpty(): Boolean = withContext(backgroundDispatcher) {
        getDbRef().syncedMetadataQueries.isSyncDatabaseEmpty().executeAsOne() == 0L
    }

    suspend fun deleteFeedItems(feedItemIds: List<FeedItemId>) =
        getDbRef().transactionWithContext(backgroundDispatcher) {
            feedItemIds.forEach { feedItemId ->
                getDbRef().syncedFeedItemQueries.deleteSyncedFeedItem(feedItemId.id)
            }
            updateMetadata(SyncTable.SYNCED_FEED_ITEM)
        }

    private fun updateMetadata(table: SyncTable) {
        getDbRef().syncedMetadataQueries.insertMetadata(
            table_name = table.tableName,
            last_change_timestamp = Clock.System.now().toEpochMilliseconds(),
        )
    }

    private suspend fun Transacter.transactionWithContext(
        coroutineContext: CoroutineContext,
        noEnclosing: Boolean = false,
        body: TransactionWithoutReturn.() -> Unit,
    ) {
        withContext(coroutineContext) {
            this@transactionWithContext.transaction(noEnclosing) {
                body()
            }
        }
    }

    companion object {
        const val SYNC_DATABASE_NAME_PROD = "FeedFlowFeedSyncDB"
        const val SYNC_DATABASE_NAME_DEBUG = "FeedFlowFeedSyncDB-debug"
    }
}
