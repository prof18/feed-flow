package com.prof18.feedflow.feedsync.feedbin.domain

import arrow.fx.coroutines.parZip
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.DataResult
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.allSuccess
import com.prof18.feedflow.core.model.error
import com.prof18.feedflow.core.model.firstError
import com.prof18.feedflow.core.model.isError
import com.prof18.feedflow.core.model.map
import com.prof18.feedflow.core.model.onSuccessSuspend
import com.prof18.feedflow.core.model.plus
import com.prof18.feedflow.core.model.requireSuccess
import com.prof18.feedflow.core.model.success
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.feedbin.data.FeedbinClient
import com.prof18.feedflow.feedsync.feedbin.data.dto.EntryDTO
import com.prof18.feedflow.feedsync.feedbin.data.dto.SubscriptionDTO
import com.prof18.feedflow.feedsync.feedbin.data.dto.TaggingDTO
import com.prof18.feedflow.feedsync.feedbin.domain.mapping.EntryDTOMapper
import com.prof18.feedflow.feedsync.feedbin.domain.mapping.toFeedSource
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class FeedbinRepository internal constructor(
    private val feedbinClient: FeedbinClient,
    private val logger: Logger,
    private val networkSettings: NetworkSettings,
    private val databaseHelper: DatabaseHelper,
    private val entryDTOMapper: EntryDTOMapper,
    private val dateFormatter: DateFormatter,
    private val dispatcherProvider: DispatcherProvider,
) {
    fun isAccountSet(): Boolean = networkSettings.getSyncPwd().isNotEmpty()

    suspend fun login(
        username: String,
        password: String,
    ): DataResult<Unit> = feedbinClient.login(username, password)
        .map {
            logger.d { "Login successful" }
            networkSettings.setSyncPwd(password)
            networkSettings.setSyncUsername(username)
            databaseHelper.deleteAll()
        }

    suspend fun sync(): DataResult<Unit> = withContext(dispatcherProvider.io) {
        logger.d { "Sync started" }

        val categoriesResult = fetchFeedSourcesAndCategories()
        if (categoriesResult.isError()) {
            return@withContext categoriesResult
        }

        val feedSources = databaseHelper.getFeedSources()

        val entriesResult = fetchEntries(feedSources)
        if (entriesResult.isError()) {
            return@withContext entriesResult
        }

        parZip(
            ctx = dispatcherProvider.io,
            { fetchStarredEntries() },
            { fetchUnreadEntries() },
        ) { starredEntriesResult, unreadEntriesResult ->
            val results = starredEntriesResult + unreadEntriesResult

            if (results.allSuccess()) {
                databaseHelper.updateFeedItemReadStatus(
                    unreadEntriesResult.requireSuccess().map { it.toString() },
                )
                databaseHelper.updateFeedItemBookmarkStatus(
                    starredEntriesResult.requireSuccess().map { it.toString() },
                )
            } else {
                return@parZip results.firstError()?.error()
            }
        }

        networkSettings.setLastSyncDate(Clock.System.now().epochSeconds)
        return@withContext Unit.success()
    }

    fun getLastSyncDate(): Long? = networkSettings.getLastSyncDate()

    suspend fun disconnect() {
        networkSettings.deleteAll()
        databaseHelper.deleteAll()
    }

    suspend fun updateBookmarkStatus(feedItemId: FeedItemId, isBookmarked: Boolean): DataResult<Unit> {
        val entryId = feedItemId.id.toLongOrNull() ?: return Unit.success()

        val result = if (isBookmarked) {
            feedbinClient.starEntries(listOf(entryId))
        } else {
            feedbinClient.unstarEntries(listOf(entryId))
        }

        if (result.isError()) {
            return result
        }

        databaseHelper.updateBookmarkStatus(feedItemId, isBookmarked = isBookmarked)
        return Unit.success()
    }

    suspend fun updateReadStatus(feedItemIds: List<FeedItemId>, isRead: Boolean): DataResult<Unit> {
        val entryIds = feedItemIds.mapNotNull { it.id.toLongOrNull() }

        val result = if (isRead) {
            feedbinClient.markAsRead(entryIds)
        } else {
            feedbinClient.markAsUnread(entryIds)
        }

        if (result.isError()) {
            return result
        }

        databaseHelper.updateReadStatus(feedItemIds, isRead = isRead)
        return Unit.success()
    }

    suspend fun markAllFeedAsRead(filter: FeedFilter): DataResult<Unit> {
        val feedItemIds = databaseHelper.selectAllUrlsForFilter(filter).map { FeedItemId(it) }
        return updateReadStatus(feedItemIds, isRead = true)
    }

    suspend fun deleteCategory(categoryId: String): DataResult<Unit> {
        val taggingsResult = feedbinClient.getTaggings()
        if (taggingsResult.isError()) {
            return taggingsResult
        }

        val taggings = taggingsResult.requireSuccess()
        val taggingsToDelete = taggings.filter { it.name == categoryId }

        for (tagging in taggingsToDelete) {
            val result = feedbinClient.deleteTagging(tagging.id)
            if (result.isError()) {
                return result
            }
        }

        val deleteTagResult = feedbinClient.deleteTag(categoryId)
        if (deleteTagResult.isError()) {
            return deleteTagResult
        }

        databaseHelper.deleteCategory(categoryId)
        return Unit.success()
    }

    suspend fun deleteFeedSource(feedSourceId: String): DataResult<Unit> {
        val subscriptionId = feedSourceId.removePrefix("feed/").toLongOrNull() ?: return Unit.success()

        val result = feedbinClient.deleteSubscription(subscriptionId)
        if (result.isError()) {
            return result
        }

        databaseHelper.deleteFeedSource(feedSourceId)
        return Unit.success()
    }

    suspend fun fetchFeedSourcesAndCategories(): DataResult<Unit> = withContext(dispatcherProvider.io) {
        parZip(
            ctx = dispatcherProvider.io,
            { feedbinClient.getSubscriptions() },
            { feedbinClient.getTaggings() },
        ) { subscriptionsResult, taggingsResult ->
            if (subscriptionsResult.isError()) {
                return@parZip subscriptionsResult
            }
            if (taggingsResult.isError()) {
                return@parZip taggingsResult
            }

            val subscriptions = subscriptionsResult.requireSuccess()
            val taggings = taggingsResult.requireSuccess()

            val feedSources = subscriptions.map { it.toFeedSource(taggings) }
            val categories = feedSources.mapNotNull { it.category }

            databaseHelper.insertCategories(categories)
            databaseHelper.insertFeedSource(feedSources)
            databaseHelper.deleteFeedSourceExcept(feedSources.map { it.id })
            databaseHelper.deleteCategoriesExcept(categories.map { it.id })
            Unit.success()
        }
    }

    suspend fun addFeedSource(
        url: String,
        categoryName: FeedSourceCategory?,
        isNotificationEnabled: Boolean,
    ): DataResult<Unit> {
        val result = feedbinClient.createSubscription(url)
        if (result.isError()) {
            return result
        }

        val subscription = result.requireSuccess()
        val feedSourceId = "feed/${subscription.feedId}"

        databaseHelper.updateNotificationEnabledStatus(feedSourceId, isNotificationEnabled)

        if (categoryName != null) {
            val taggingResult = feedbinClient.createTagging(
                feedId = subscription.feedId,
                name = categoryName.title,
            )
            if (taggingResult.isError()) {
                return taggingResult
            }
        }

        return fetchFeedSourcesAndCategories()
    }

    suspend fun editFeedSource(
        newFeedSource: FeedSource,
        originalFeedSource: FeedSource?,
    ): DataResult<Unit> {
        val feedId = newFeedSource.id.removePrefix("feed/").toLongOrNull() ?: return Unit.success()

        val taggingsResult = feedbinClient.getTaggings()
        if (taggingsResult.isError()) {
            return taggingsResult
        }

        val taggings = taggingsResult.requireSuccess()
        val existingTagging = taggings.firstOrNull { it.feedId == feedId }

        if (originalFeedSource?.category != null && originalFeedSource.category != newFeedSource.category) {
            existingTagging?.let {
                val deleteResult = feedbinClient.deleteTagging(it.id)
                if (deleteResult.isError()) {
                    return deleteResult
                }
            }
        }

        if (newFeedSource.category != null && newFeedSource.category != originalFeedSource?.category) {
            val createResult = feedbinClient.createTagging(
                feedId = feedId,
                name = newFeedSource.category.title,
            )
            if (createResult.isError()) {
                return createResult
            }
        }

        databaseHelper.updateFeedSource(newFeedSource)
        fetchFeedSourcesAndCategories()
        return Unit.success()
    }

    suspend fun editFeedSourceName(
        feedSourceId: String,
        newName: String,
    ): DataResult<Unit> {
        databaseHelper.updateFeedSourceName(feedSourceId, newName)
        return Unit.success()
    }

    suspend fun editCategoryName(categoryId: CategoryId, newName: CategoryName): DataResult<Unit> {
        val result = feedbinClient.renameTag(
            oldName = categoryId.value,
            newName = newName.name,
        )
        if (result.isError()) {
            return result
        }

        databaseHelper.updateCategoryNameAndId(
            oldId = categoryId.value,
            newId = newName.name,
            newName = newName.name,
        )
        return Unit.success()
    }

    private suspend fun fetchStarredEntries(): DataResult<List<Long>> {
        logger.d { "Fetching starred entries" }

        val result = feedbinClient.getStarredEntries()

        logger.d { "Result: $result" }

        if (result.isError()) {
            logger.e { "Failed to fetch starred entries: $result" }
            return result.failure.error()
        }

        return result
    }

    private suspend fun fetchUnreadEntries(): DataResult<List<Long>> {
        logger.d { "Fetching unread entries" }

        val result = feedbinClient.getUnreadEntries()

        if (result.isError()) {
            logger.e { "Failed to fetch unread entries: $result" }
            return result.failure.error()
        }

        return result
    }

    private suspend fun fetchEntries(feedSources: List<FeedSource>): DataResult<Unit> {
        logger.d { "Fetching entries" }

        val lastUpdate = networkSettings.getLastSyncDate()
        val since = if (lastUpdate != null) {
            kotlinx.datetime.Instant.fromEpochSeconds(lastUpdate).toString()
        } else {
            null
        }

        var currentPage = 1
        var hasMore = true
        val allEntries = mutableListOf<EntryDTO>()
        val feedSourcesMap = feedSources.associateBy { it.id.removePrefix("feed/").toLongOrNull() }

        var unreadIds: Set<Long> = emptySet()
        var starredIds: Set<Long> = emptySet()

        parZip(
            ctx = dispatcherProvider.io,
            { feedbinClient.getUnreadEntries() },
            { feedbinClient.getStarredEntries() },
        ) { unreadResult, starredResult ->
            if (unreadResult.isError() || starredResult.isError()) {
                return@parZip
            }
            unreadIds = unreadResult.requireSuccess().toSet()
            starredIds = starredResult.requireSuccess().toSet()
        }

        while (hasMore && currentPage <= MAX_PAGES) {
            val result = feedbinClient.getEntries(
                page = currentPage,
                since = since,
            )

            if (result.isError()) {
                logger.e { "Failed to fetch entries: $result" }
                return result.failure.error()
            }

            result.onSuccessSuspend { entries ->
                logger.d { "Fetched ${entries.size} entries on page $currentPage" }
                allEntries.addAll(entries)
                hasMore = entries.size >= PAGE_SIZE
                currentPage++
            }
        }

        val feedItems = allEntries.mapNotNull { entry ->
            val feedSource = feedSourcesMap[entry.feedId] ?: return@mapNotNull null
            val isRead = !unreadIds.contains(entry.id)
            val isBookmarked = starredIds.contains(entry.id)

            entryDTOMapper.mapToFeedItem(
                entryDTO = entry,
                feedSource = feedSource,
                isRead = isRead,
                isBookmarked = isBookmarked,
            )
        }

        databaseHelper.insertFeedItems(feedItems, dateFormatter.currentTimeMillis())

        return Unit.success()
    }

    companion object {
        private const val PAGE_SIZE = 100
        private const val MAX_PAGES = 10
    }
}
