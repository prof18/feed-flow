package com.prof18.feedflow.feedsync.database.data

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransactionWithoutReturn
import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import com.prof18.feedflow.feedsync.database.di.FEED_SYNC_SCOPE_NAME
import com.prof18.feedflow.feedsync.database.di.FeedSyncScope
import com.prof18.feedflow.feedsync.database.model.SyncedFeedSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import kotlin.coroutines.CoroutineContext

class SyncedDatabaseHelper(
    private val backgroundDispatcher: CoroutineDispatcher,
) : KoinComponent {

    private fun getDbRef(): FeedFlowFeedSyncDB {
        val scope = getKoin().getOrCreateScope<FeedSyncScope>(FEED_SYNC_SCOPE_NAME)
        val driver = scope.get<SqlDriver>()
        return FeedFlowFeedSyncDB(driver)
    }

    fun closeScope() {
        val scope = getKoin().getOrCreateScope<FeedSyncScope>(FEED_SYNC_SCOPE_NAME)
        val driver = scope.get<SqlDriver>()
        driver.close()
        scope.close()
    }

    suspend fun insertSyncedFeedSource(sources: List<FeedSource>) =
        getDbRef().transactionWithContext(backgroundDispatcher) {
            sources.forEach { source ->
                val category = source.category
                if (category != null) {
                    getDbRef().syncedFeedSourceCategoryQueries.insertOrIgnoreFeedSourceCategory(
                        id = category.id,
                        title = category.title,
                    )
                }
                getDbRef().syncedFeedSourceQueries.insertOrIgnoreFeedSource(
                    url_hash = source.id,
                    url = source.url,
                    title = source.title,
                    category_id = category?.id,
                    logo_url = source.logoUrl,
                )
            }
            updateMetadata(SyncTable.SYNCED_FEED_SOURCE)
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

    internal companion object {
        const val DB_FILE_NAME_WITH_EXTENSION = "FeedFlowFeedSyncDB.db"
        const val DATABASE_NAME_PROD = "FeedFlowFeedSyncDB"
        const val DATABASE_NAME_DEBUG = "FeedFlowFeedSyncDB-debug"
    }
}

private enum class SyncTable(val tableName: String) {
    SYNCED_FEED_SOURCE("synced_feed_source"),
    SYNCED_FEED_SOURCE_CATEGORY("synced_feed_source_category"),
    SYNCED_FEED_ITEM("synced_feed_item"),
}
