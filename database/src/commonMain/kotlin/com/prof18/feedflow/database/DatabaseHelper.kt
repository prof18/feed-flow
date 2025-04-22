package com.prof18.feedflow.database

import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransactionWithoutReturn
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrDefault
import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.CategoryWithUnreadCount
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedSourceToNotify
import com.prof18.feedflow.core.model.FeedSourceWithUnreadCount
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.SyncedFeedItem
import com.prof18.feedflow.db.FeedFlowDB
import com.prof18.feedflow.db.Feed_source_preferences
import com.prof18.feedflow.db.GetFeedSourcesWithUnreadCount
import com.prof18.feedflow.db.Search
import com.prof18.feedflow.db.SelectFeedSourceById
import com.prof18.feedflow.db.SelectFeedUrls
import com.prof18.feedflow.db.SelectFeeds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class DatabaseHelper(
    sqlDriver: SqlDriver,
    private val backgroundDispatcher: CoroutineDispatcher,
    private val logger: Logger,
) {
    private val dbRef: FeedFlowDB = FeedFlowDB(
        sqlDriver,
        feed_source_preferencesAdapter = Feed_source_preferences.Adapter(
            link_opening_preferenceAdapter = EnumColumnAdapter(),
        ),
    )

    suspend fun getFeedSources(): List<FeedSource> = withContext(backgroundDispatcher) {
        dbRef.feedSourceQueries
            .selectFeedUrls()
            .executeAsList()
            .map(::transformToFeedSource)
    }

    suspend fun getFeedSource(feedSourceId: String): FeedSource? =
        withContext(backgroundDispatcher) {
            dbRef.feedSourceQueries
                .selectFeedSourceById(feedSourceId)
                .executeAsOneOrNull()
                ?.let { feedSource ->
                    transformToFeedSource(feedSource)
                }
        }

    fun getFeedSourcesFlow(): Flow<List<FeedSource>> =
        dbRef.feedSourceQueries
            .selectFeedUrls()
            .asFlow()
            .catch {
                logger.e(it) { "Something wrong while getting data from Database" }
            }
            .mapToList(backgroundDispatcher)
            .map { feedSources ->
                feedSources.map(::transformToFeedSource)
            }
            .flowOn(backgroundDispatcher)

    fun getFeedSourcesWithUnreadCountFlow(): Flow<List<FeedSourceWithUnreadCount>> =
        dbRef.viewQueries
            .getFeedSourcesWithUnreadCount()
            .asFlow()
            .catch {
                logger.e(it) { "Something wrong while getting data from Database" }
            }
            .mapToList(backgroundDispatcher)
            .map { feedSources ->
                feedSources.map { source ->
                    FeedSourceWithUnreadCount(
                        feedSource = transformToFeedSourceWithCount(source),
                        unreadCount = source.unread_count,
                    )
                }
            }
            .flowOn(backgroundDispatcher)

    suspend fun getFeedItems(
        feedFilter: FeedFilter,
        pageSize: Long,
        offset: Long,
        showReadItems: Boolean,
    ): List<SelectFeeds> = withContext(backgroundDispatcher) {
        dbRef.feedItemQueries
            .selectFeeds(
                feedSourceId = feedFilter.getFeedSourceId(),
                feedSourceCategoryId = feedFilter.getCategoryId(),
                isRead = feedFilter.getIsReadFlag(showReadItems),
                isBookmarked = feedFilter.getBookmarkFlag(),
                isHidden = feedFilter.getIsHiddenFromTimelineFlag(),
                pageSize = pageSize,
                offset = offset,
            )
            .executeAsList()
    }

    fun getFeedWidgetItems(
        pageSize: Long,
    ): Flow<List<SelectFeeds>> =
        dbRef.feedItemQueries
            .selectFeeds(
                feedSourceId = null,
                feedSourceCategoryId = null,
                isRead = false,
                isBookmarked = null,
                isHidden = 0,
                pageSize = pageSize,
                offset = 0,
            )
            .asFlow()
            .mapToList(backgroundDispatcher)

    suspend fun insertCategories(categories: List<FeedSourceCategory>) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            categories.forEach { category ->
                dbRef.feedSourceCategoryQueries.insertFeedSourceCategory(
                    id = category.id,
                    title = category.title,
                )
            }
        }

    suspend fun insertFeedSource(feedSource: List<ParsedFeedSource>) {
        dbRef.transactionWithContext(backgroundDispatcher) {
            feedSource.forEach { feedSource ->
                dbRef.feedSourceQueries.insertFeedSource(
                    url_hash = feedSource.id,
                    url = feedSource.url,
                    title = feedSource.title,
                    category_id = feedSource.category?.id,
                    logo_url = feedSource.logoUrl,
                )
            }
        }
    }

    suspend fun insertFeedItems(feedItems: List<FeedItem>, lastSyncTimestamp: Long) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            for (feedItem in feedItems) {
                with(feedItem) {
                    dbRef.feedItemQueries.insertFeedItem(
                        url_hash = id,
                        url = url,
                        title = title,
                        subtitle = subtitle,
                        content = null,
                        image_url = imageUrl,
                        feed_source_id = feedSource.id,
                        pub_date = pubDateMillis,
                        comments_url = commentsUrl,
                    )

                    dbRef.feedSourceQueries.updateLastSyncTimestamp(
                        lastSyncTimestamp,
                        feedSource.id,
                    )
                }
            }
        }

    suspend fun markAsRead(itemsToUpdates: List<FeedItemId>) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            for (item in itemsToUpdates) {
                dbRef.feedItemQueries.updateReadStatus(urlHash = item.id, isRead = true)
            }
        }

    suspend fun updateReadStatus(feedItemId: FeedItemId, isRead: Boolean) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.feedItemQueries.updateReadStatus(urlHash = feedItemId.id, isRead = isRead)
        }

    suspend fun updateReadStatus(feedItemId: List<FeedItemId>, isRead: Boolean) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.feedItemQueries.updateAllReadStatus(urlHash = feedItemId.map { it.id }, isRead = isRead)
        }

    suspend fun markAllFeedAsRead(feedFilter: FeedFilter) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            when (feedFilter) {
                is FeedFilter.Category -> {
                    dbRef.feedItemQueries.markAllReadByCategory(feedFilter.feedCategory.id)
                }

                is FeedFilter.Source -> {
                    dbRef.feedItemQueries.markAllReadByFeedSource(feedFilter.feedSource.id)
                }

                FeedFilter.Timeline -> {
                    dbRef.feedItemQueries.markAllRead()
                }

                FeedFilter.Read, FeedFilter.Bookmarks -> {
                    // Do nothing
                }
            }
        }

    suspend fun deleteOldFeedItems(timeThreshold: Long) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.feedItemQueries.clearOldItems(timeThreshold)
        }

    suspend fun getOldFeedItem(timeThreshold: Long) = withContext(backgroundDispatcher) {
        dbRef.feedItemQueries.selectOldItems(timeThreshold).executeAsList().map { FeedItemId(it) }
    }

    suspend fun deleteFeedSource(feedSourceId: String) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.feedSourceQueries.deleteFeedSource(feedSourceId)
            dbRef.feedItemQueries.deleteAllWithFeedSource(feedSourceId)
        }

    suspend fun deleteFeedSourceExcept(feedSourceIds: List<String>) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.feedSourceQueries.deleteAllExcept(feedSourceIds)
            dbRef.feedItemQueries.deleteAllExcept(feedSourceIds)
        }

    suspend fun selectAllUrlsForFilter(feedFilter: FeedFilter): List<String> = withContext(backgroundDispatcher) {
        dbRef.feedItemQueries
            .selectFeedUrlsForFilter(
                feedSourceId = feedFilter.getFeedSourceId(),
                feedSourceCategoryId = feedFilter.getCategoryId(),
                // Marking read things already read does not make sense, that's why the hardcoded false
                isRead = feedFilter.getIsReadFlag(false),
                isBookmarked = feedFilter.getBookmarkFlag(),
                isHidden = feedFilter.getIsHiddenFromTimelineFlag(),
            )
            .executeAsList()
    }

    fun observeFeedSourceCategories(): Flow<List<FeedSourceCategory>> =
        dbRef.feedSourceCategoryQueries.selectAll()
            .asFlow()
            .mapToList(backgroundDispatcher)
            .map { categories ->
                categories.map {
                    FeedSourceCategory(
                        id = it.id,
                        title = it.title,
                    )
                }
            }
            .flowOn(backgroundDispatcher)

    fun observeCategoriesWithUnreadCount(): Flow<List<CategoryWithUnreadCount>> =
        dbRef.viewQueries.getCategoriesWithUnreadCount()
            .asFlow()
            .mapToList(backgroundDispatcher)
            .map { categories ->
                categories.map { category ->
                    CategoryWithUnreadCount(
                        category = FeedSourceCategory(
                            id = category.id,
                            title = category.title,
                        ),
                        unreadCount = category.unread_count,
                    )
                }
            }
            .flowOn(backgroundDispatcher)

    suspend fun getFeedSourceCategories(): List<FeedSourceCategory> =
        withContext(backgroundDispatcher) {
            dbRef.feedSourceCategoryQueries.selectAll()
                .executeAsList()
                .map { categories ->
                    FeedSourceCategory(
                        id = categories.id,
                        title = categories.title,
                    )
                }
        }

    fun deleteAllFeeds() =
        dbRef.transaction {
            dbRef.feedItemQueries.deleteAll()
            dbRef.feedSourceQueries.deleteAllLastSync()
        }

    fun getUnreadFeedCountFlow(feedFilter: FeedFilter): Flow<Long> =
        dbRef.feedItemQueries
            .countUnreadFeeds(
                feedSourceId = feedFilter.getFeedSourceId(),
                feedSourceCategoryId = feedFilter.getCategoryId(),
                bookmarked = feedFilter.getBookmarkFlag(),
                isHidden = feedFilter.getIsHiddenFromTimelineFlag(),
            )
            .asFlow()
            .catch {
                logger.e(it) { "Something wrong while getting data from Database" }
            }
            .mapToOneOrDefault(0, backgroundDispatcher)
            .flowOn(backgroundDispatcher)

    suspend fun deleteCategory(id: String) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.feedSourceQueries.resetCategory(categoryId = id)
            dbRef.feedSourceCategoryQueries.delete(id = id)
        }

    suspend fun updateCategoryName(id: String, newName: String) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.feedSourceCategoryQueries.updateCategoryName(title = newName, id = id)
        }

    suspend fun updateCategoryNameAndId(oldId: String, newId: String, newName: String) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.feedSourceCategoryQueries.updateCategoryNameAndId(title = newName, newId = newId, oldId = oldId)
            dbRef.feedSourceQueries.updateCategoryId(newCategoryId = newId, oldCategoryId = oldId)
        }

    suspend fun updateBookmarkStatus(feedItemId: FeedItemId, isBookmarked: Boolean) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.feedItemQueries.updateBookmarkStatus(
                starred = isBookmarked,
                urlHash = feedItemId.id,
            )
        }

    suspend fun updateFeedSourceName(feedSourceId: String, newName: String) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.feedSourceQueries.updateFeedSourceTitle(
                title = newName,
                urlHash = feedSourceId,
            )
        }

    suspend fun updateFeedSource(feedSource: FeedSource) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.feedSourceQueries.updateFeedSource(
                urlHash = feedSource.id,
                url = feedSource.url,
                title = feedSource.title,
                categoryId = feedSource.category?.id,
            )
        }

    fun search(searchQuery: String): Flow<List<Search>> =
        dbRef.feedSearchQueries
            .search(query = searchQuery)
            .asFlow()
            .mapToList(backgroundDispatcher)
            .flowOn(backgroundDispatcher)

    suspend fun getLastChangeTimestamp(tableName: DatabaseTables): Long? = withContext(backgroundDispatcher) {
        dbRef.syncMetadataQueries.selectLastChangeTimestamp(tableName.tableName)
            .executeAsOneOrNull()?.last_change_timestamp
    }

    suspend fun updateSyncMetadata(table: DatabaseTables, timeStamp: Long) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.syncMetadataQueries.insertMetadata(
                table_name = table.tableName,
                last_change_timestamp = timeStamp,
            )
        }

    suspend fun getAllFeedSourceIds(): List<String> = withContext(backgroundDispatcher) {
        dbRef.feedSourceQueries.selectAllUrlHashes().executeAsList()
    }

    suspend fun getAllCategoryIds(): List<String> = withContext(backgroundDispatcher) {
        dbRef.feedSourceCategoryQueries.selectAllIds().executeAsList()
    }

    suspend fun updateFeedItemReadAndBookmarked(
        syncedFeedItems: List<SyncedFeedItem>,
    ) = dbRef.transactionWithContext(backgroundDispatcher) {
        syncedFeedItems.forEach { syncedFeedItem ->
            dbRef.feedItemQueries.updateFeedItemReadAndBookmarked(
                is_read = syncedFeedItem.isRead,
                is_bookmarked = syncedFeedItem.isBookmarked,
                url_hash = syncedFeedItem.id,
            )
        }
    }

    suspend fun getFeedItemsForSync(): List<SyncedFeedItem> = withContext(backgroundDispatcher) {
        dbRef.feedItemQueries.selectForSync()
            .executeAsList()
            .map { queryResult ->
                SyncedFeedItem(
                    id = queryResult.url_hash,
                    isRead = queryResult.is_read,
                    isBookmarked = queryResult.is_bookmarked,
                )
            }
    }

    suspend fun updateFeedItemReadStatus(feedItemId: List<String>) =
        try {
            dbRef.transactionWithContext(backgroundDispatcher) {
                dbRef.feedItemQueries.updateRead(feedItemId)
                dbRef.feedItemQueries.updateUnread(feedItemId)
            }
        } catch (e: Exception) {
            logger.e(e) { "Error while updating read status with FreshRSS" }
        }

    suspend fun updateFeedItemBookmarkStatus(feedItemId: List<String>) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.feedItemQueries.updateBookmarked(feedItemId)
            dbRef.feedItemQueries.updateNotBookmarked(feedItemId)
        }

    suspend fun deleteAll() = dbRef.transactionWithContext(backgroundDispatcher) {
        dbRef.feedItemQueries.deleteAll()
        dbRef.feedSourceCategoryQueries.deleteAll()
        dbRef.feedSourceQueries.deleteAll()
    }

    suspend fun insertFeedSourcePreference(
        feedSourceId: String,
        preference: LinkOpeningPreference,
        isHidden: Boolean,
        isPinned: Boolean,
        isNotificationEnabled: Boolean,
    ) = dbRef.transactionWithContext(backgroundDispatcher) {
        dbRef.feedSourcePreferencesQueries.insertPreference(
            feed_source_id = feedSourceId,
            link_opening_preference = preference,
            is_hidden = isHidden,
            is_pinned = isPinned,
            notifications_enabled = isNotificationEnabled,
        )
    }

    suspend fun deleteCategoriesExcept(categoryIds: List<String>) = dbRef.transactionWithContext(backgroundDispatcher) {
        dbRef.feedSourceCategoryQueries.deleteAllExcept(categoryIds)
    }

    suspend fun getFeedSourcesByCategory(categoryId: String) = withContext(backgroundDispatcher) {
        dbRef.feedSourceQueries
            .selectByCategory(categoryId)
            .executeAsList()
            .map { feedSource ->
                FeedSource(
                    id = feedSource.url_hash,
                    url = feedSource.url,
                    title = feedSource.feed_source_title,
                    category = FeedSourceCategory(
                        id = requireNotNull(feedSource.category_id),
                        title = requireNotNull(feedSource.category_title),
                    ),
                    lastSyncTimestamp = feedSource.last_sync_timestamp,
                    logoUrl = feedSource.feed_source_logo_url,
                    isHiddenFromTimeline = feedSource.is_hidden ?: false,
                    linkOpeningPreference = feedSource.link_opening_preference ?: LinkOpeningPreference.DEFAULT,
                    isPinned = feedSource.is_pinned ?: false,
                    isNotificationEnabled = feedSource.notifications_enabled ?: false,
                )
            }
    }

    suspend fun getFeedItemUrlInfo(feedId: String): FeedItemUrlInfo? = withContext(backgroundDispatcher) {
        dbRef.feedItemQueries
            .getFeedUrl(feedId)
            .executeAsOneOrNull()?.let { url ->
                FeedItemUrlInfo(
                    id = url.url_hash,
                    url = url.url,
                    title = url.title,
                    isBookmarked = url.is_bookmarked,
                    linkOpeningPreference = url.feed_source_link_opening_preference ?: LinkOpeningPreference.DEFAULT,
                )
            }
    }

    suspend fun updateNotificationEnabledStatus(feedSourceId: String, enabled: Boolean) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            val feedSourcePreference = dbRef.feedSourcePreferencesQueries
                .getPreference(feedSourceId)
                .executeAsOneOrNull()
            if (feedSourcePreference == null) {
                dbRef.feedSourcePreferencesQueries.insertNotificationEnabledPreference(
                    feed_source_id = feedSourceId,
                    notifications_enabled = enabled,
                )
            } else {
                dbRef.feedSourcePreferencesQueries.updateNotificationEnabledStatus(
                    feedSourceId = feedSourceId,
                    enabled = enabled,
                )
            }
        }

    suspend fun updateAllNotificationsEnabledStatus(enabled: Boolean) =
        dbRef.transactionWithContext(backgroundDispatcher) {
            val feedSources = dbRef.feedSourceQueries.selectAllUrlHashes().executeAsList()
            feedSources.forEach { feedSourceId ->
                val feedSourcePreference = dbRef.feedSourcePreferencesQueries
                    .getPreference(feedSourceId)
                    .executeAsOneOrNull()
                if (feedSourcePreference == null) {
                    dbRef.feedSourcePreferencesQueries.insertNotificationEnabledPreference(
                        feed_source_id = feedSourceId,
                        notifications_enabled = enabled,
                    )
                } else {
                    dbRef.feedSourcePreferencesQueries.updateNotificationEnabledStatus(
                        feedSourceId = feedSourceId,
                        enabled = enabled,
                    )
                }
            }
        }

    suspend fun getFeedSourceToNotify(): List<FeedSourceToNotify> = withContext(backgroundDispatcher) {
        dbRef.feedItemQueries.selectFeedSourceToNotify()
            .executeAsList()
            .map { item ->
                FeedSourceToNotify(
                    feedSourceId = item.feed_source_id,
                    feedSourceTitle = item.feed_source_title,
                )
            }
    }

    suspend fun markFeedItemsAsNotified() =
        dbRef.transactionWithContext(backgroundDispatcher) {
            dbRef.feedItemQueries.markFeedItemsNotified()
        }

    suspend fun areNotificationsEnabled(): Boolean = withContext(backgroundDispatcher) {
        dbRef.feedSourcePreferencesQueries
            .areNotificationsEnabled()
            .executeAsOne()
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

    private fun FeedFilter.getFeedSourceId(): String? {
        return when (this) {
            is FeedFilter.Source -> feedSource.id

            is FeedFilter.Category,
            FeedFilter.Timeline,
            FeedFilter.Read,
            FeedFilter.Bookmarks,
            -> null
        }
    }

    private fun FeedFilter.getCategoryId(): String? {
        return when (this) {
            is FeedFilter.Category -> feedCategory.id

            is FeedFilter.Source,
            FeedFilter.Timeline,
            FeedFilter.Read,
            FeedFilter.Bookmarks,
            -> null
        }
    }

    private fun FeedFilter.getIsReadFlag(showReadItems: Boolean): Boolean? {
        return when (this) {
            is FeedFilter.Read -> true

            is FeedFilter.Bookmarks -> null

            is FeedFilter.Category,
            is FeedFilter.Source,
            FeedFilter.Timeline,
            -> if (showReadItems) {
                null
            } else {
                false
            }
        }
    }

    private fun FeedFilter.getIsHiddenFromTimelineFlag(): Long? {
        return when (this) {
            is FeedFilter.Timeline -> 0

            is FeedFilter.Bookmarks,
            is FeedFilter.Category,
            is FeedFilter.Source,
            FeedFilter.Read,
            -> null
        }
    }

    private fun FeedFilter.getBookmarkFlag(): Boolean? {
        return when (this) {
            is FeedFilter.Bookmarks -> true

            is FeedFilter.Category,
            is FeedFilter.Source,
            FeedFilter.Timeline,
            FeedFilter.Read,
            -> null
        }
    }

    private fun transformToFeedSource(feedSource: SelectFeedUrls): FeedSource {
        val category = if (feedSource.category_title != null && feedSource.category_id != null) {
            FeedSourceCategory(
                id = requireNotNull(feedSource.category_id),
                title = requireNotNull(feedSource.category_title),
            )
        } else {
            null
        }

        return FeedSource(
            id = feedSource.url_hash,
            url = feedSource.url,
            title = feedSource.feed_source_title,
            category = category,
            lastSyncTimestamp = feedSource.last_sync_timestamp,
            logoUrl = feedSource.feed_source_logo_url,
            linkOpeningPreference = feedSource.link_opening_preference ?: LinkOpeningPreference.DEFAULT,
            isHiddenFromTimeline = feedSource.is_hidden ?: false,
            isPinned = feedSource.is_pinned ?: false,
            isNotificationEnabled = feedSource.notifications_enabled ?: false,
        )
    }

    private fun transformToFeedSource(feedSource: SelectFeedSourceById): FeedSource {
        val category = if (feedSource.category_title != null && feedSource.category_id != null) {
            FeedSourceCategory(
                id = requireNotNull(feedSource.category_id),
                title = requireNotNull(feedSource.category_title),
            )
        } else {
            null
        }

        return FeedSource(
            id = feedSource.url_hash,
            url = feedSource.url,
            title = feedSource.feed_source_title,
            category = category,
            lastSyncTimestamp = feedSource.last_sync_timestamp,
            logoUrl = feedSource.feed_source_logo_url,
            linkOpeningPreference = feedSource.link_opening_preference ?: LinkOpeningPreference.DEFAULT,
            isHiddenFromTimeline = feedSource.is_hidden ?: false,
            isPinned = feedSource.is_pinned ?: false,
            isNotificationEnabled = feedSource.notifications_enabled ?: false,
        )
    }

    private fun transformToFeedSourceWithCount(feedSource: GetFeedSourcesWithUnreadCount): FeedSource {
        val category = if (feedSource.category_title != null && feedSource.category_id != null) {
            FeedSourceCategory(
                id = requireNotNull(feedSource.category_id),
                title = requireNotNull(feedSource.category_title),
            )
        } else {
            null
        }

        return FeedSource(
            id = feedSource.url_hash,
            url = feedSource.url,
            title = feedSource.feed_source_title,
            category = category,
            lastSyncTimestamp = feedSource.last_sync_timestamp,
            logoUrl = feedSource.feed_source_logo_url,
            linkOpeningPreference = feedSource.link_opening_preference ?: LinkOpeningPreference.DEFAULT,
            isHiddenFromTimeline = feedSource.is_hidden ?: false,
            isPinned = feedSource.is_pinned ?: false,
            isNotificationEnabled = feedSource.notifications_enabled ?: false,
        )
    }

    companion object {
        internal const val DB_FILE_NAME_WITH_EXTENSION = "FeedFlow.db"
        const val APP_DATABASE_NAME_PROD = "FeedFlowDB"
        const val APP_DATABASE_NAME_DEBUG = "FeedFlowDB-debug"
    }
}
