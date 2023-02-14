package com.prof18.feedflow

import co.touchlab.kermit.Logger
import com.prof18.feedflow.db.FeedFlowDB
import com.prof18.feedflow.db.SelectFeeds
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.TransactionWithoutReturn
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class DatabaseHelper(
    sqlDriver: SqlDriver,
    private val backgroundDispatcher: CoroutineDispatcher
) {
    private val dbRef: FeedFlowDB = FeedFlowDB(sqlDriver)

    fun getFeedSourceUrls(): List<FeedSource> =
        dbRef.feedSourceQueries
            .selectFeedUrls()
            .executeAsList()
            .map { feedSource ->
                FeedSource(
                    id = feedSource.url_hash,
                    url = feedSource.url,
                    title = feedSource.title,
                )
            }


    fun getFeedItems(): Flow<List<SelectFeeds>> =
        dbRef.feedItemQueries
            .selectFeeds()
            .asFlow()
            .mapToList()
            .flowOn(backgroundDispatcher)


    suspend fun insertCategories(categories: List<String>) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            categories.forEach { category ->
                dbRef.feedSourceCategoryQueries.insertFeedSourceCategory(category)
            }
        }

    suspend fun insertFeedSource(feedSource: List<ParsedFeedSource>) {
        dbRef.transactionWithContext(backgroundDispatcher) {
            feedSource.forEach { feedSource ->
                dbRef.feedSourceQueries.insertFeedSource(
                    url_hash = feedSource.hashCode(),
                    url = feedSource.url,
                    title = feedSource.title,
                    title_ = feedSource.category ?: "", // todo: does it make sense?

                )
            }
        }
    }

    suspend fun insertFeedItems(feedItems: List<FeedItem>) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            for (feedItem in feedItems) {
                with(feedItem) {
                    dbRef.feedItemQueries.insertFeedItem(
                        url_hash = id,
                        url = url,
                        title = title,
                        subtitle = subtitle,
                        content = content,
                        image_url = imageUrl,
                        feed_source_id = feedSource.id,
                        pub_date = pubDateMillis,
                    )
                }
            }
        }

    suspend fun updateReadStatus(itemsToUpdates: List<FeedItemId>) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            for (item in itemsToUpdates) {
                Logger.d {
                    "Updating: ${item.id}"
                }
                dbRef.feedItemQueries.updateReadStatus(item.id)
            }
        }

    suspend fun updateNewStatus() =
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.feedItemQueries.updateNewStatus()
            Logger.d { "Update new status" }
        }

    private suspend fun Transacter.transactionWithContext(
        coroutineContext: CoroutineContext,
        noEnclosing: Boolean = false,
        body: TransactionWithoutReturn.() -> Unit
    ) {
        withContext(coroutineContext) {
            this@transactionWithContext.transaction(noEnclosing) {
                body()
            }
        }
    }
}