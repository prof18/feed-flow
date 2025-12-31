package com.prof18.feedflow.feedsync.feedbin.domain

import arrow.fx.coroutines.parZip
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.DataNotFound
import com.prof18.feedflow.core.model.DataResult
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.error
import com.prof18.feedflow.core.model.isError
import com.prof18.feedflow.core.model.isSuccess
import com.prof18.feedflow.core.model.map
import com.prof18.feedflow.core.model.onSuccessSuspend
import com.prof18.feedflow.core.model.requireError
import com.prof18.feedflow.core.model.requireSuccess
import com.prof18.feedflow.core.model.success
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.feedbin.data.FeedbinClient
import com.prof18.feedflow.feedsync.feedbin.data.dto.EntryDTO
import com.prof18.feedflow.feedsync.feedbin.domain.mapping.EntryDTOMapper
import com.prof18.feedflow.feedsync.feedbin.domain.mapping.toFeedSource
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class FeedbinRepository internal constructor(
    private val feedbinClient: FeedbinClient,
    private val logger: Logger,
    private val networkSettings: NetworkSettings,
    private val databaseHelper: DatabaseHelper,
    private val entryDTOMapper: EntryDTOMapper,
    private val dateFormatter: DateFormatter,
    private val dispatcherProvider: DispatcherProvider,
) {
    fun isAccountSet(): Boolean =
        networkSettings.getSyncAccountType() == SyncAccounts.FEEDBIN &&
            networkSettings.getSyncPwd().isNotEmpty()

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

    /**
     * This is done after a login, to fetch synchronously just the unread and starred entries.
     * A later background sync will fetch the rest of the entries with a window of 2 months:
     * the [FeedbinHistorySyncScheduler] will call [syncHistoryFromBackground].
     *
     */
    suspend fun syncUnreadAndStarredAfterLogin(): DataResult<Unit> = withContext(dispatcherProvider.io) {
        logger.d { "Quick sync started" }

        val categoriesResult = fetchFeedSourcesAndCategories()
        if (categoriesResult.isError()) {
            return@withContext categoriesResult
        }

        val feedSources = databaseHelper.getFeedSources()
        val statusIdsResult = fetchUnreadAndStarredIds()
        if (statusIdsResult.isError()) {
            return@withContext statusIdsResult.requireError().error()
        }
        val statusIds = statusIdsResult.requireSuccess()
        val entryIds = (statusIds.unreadIds + statusIds.starredIds).toSet()

        if (entryIds.isNotEmpty()) {
            val entriesResult = fetchEntriesByIds(
                feedSources = feedSources,
                statusIds = statusIds,
                entryIds = entryIds,
            )
            if (entriesResult.isError()) {
                return@withContext entriesResult
            }
        }
        updateStatuses(statusIds)
        networkSettings.setLastSyncDate(Clock.System.now().epochSeconds)
        return@withContext Unit.success()
    }

    // This is called after the initial sync to get history entries in the background
    suspend fun syncHistoryFromBackground(): DataResult<Unit> = withContext(dispatcherProvider.io) {
        logger.d { "History sync started" }

        val feedSources = databaseHelper.getFeedSources()
        val statusIdsResult = fetchUnreadAndStarredIds()
        if (statusIdsResult.isError()) {
            return@withContext statusIdsResult.requireError().error()
        }
        val statusIds = statusIdsResult.requireSuccess()
        val since = buildHistorySince()

        val entriesResult = fetchEntriesByPages(
            feedSources = feedSources,
            statusIds = statusIds,
            since = since,
        )
        if (entriesResult.isError()) {
            return@withContext entriesResult
        }

        updateStatuses(statusIds)
        networkSettings.setLastSyncDate(Clock.System.now().epochSeconds)
        return@withContext Unit.success()
    }

    // This is called during the regular sync
    suspend fun sync(): DataResult<Unit> = withContext(dispatcherProvider.io) {
        logger.d { "Sync started" }

        val categoriesResult = fetchFeedSourcesAndCategories()
        if (categoriesResult.isError()) {
            return@withContext categoriesResult
        }

        val feedSources = databaseHelper.getFeedSources()
        val statusIdsResult = fetchUnreadAndStarredIds()
        if (statusIdsResult.isError()) {
            return@withContext statusIdsResult.requireError().error()
        }
        val statusIds = statusIdsResult.requireSuccess()
        val since = buildSinceForRegularSync()

        val entriesResult = fetchEntriesByPages(
            feedSources = feedSources,
            statusIds = statusIds,
            since = since,
        )
        if (entriesResult.isError()) {
            return@withContext entriesResult
        }

        val missingStarredResult = fetchMissingStarredEntries(
            feedSources = feedSources,
            statusIds = statusIds,
        )
        updateStatuses(statusIds)
        if (missingStarredResult.isError()) {
            return@withContext missingStarredResult
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
        val entryId = feedItemId.id.toLongOrNull() ?: return DataNotFound.error()

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

        if (entryIds.isEmpty()) {
            return Unit.success()
        }

        val batches = entryIds.chunked(MAX_MARK_BATCH_SIZE)
        for (batch in batches) {
            if (isRead) {
                val result = feedbinClient.markAsRead(batch)
                if (result.isError()) {
                    return result
                }
            } else {
                val result = feedbinClient.markAsUnread(batch)
                if (result.isError()) {
                    return result.failure.error()
                }
            }
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
        val feedbinIds = parseFeedbinIds(feedSourceId) ?: return DataNotFound.error()
        val subscriptionIdResult = resolveSubscriptionId(feedbinIds)
        if (subscriptionIdResult.isError()) {
            return subscriptionIdResult.failure.error()
        }

        val result = feedbinClient.deleteSubscription(subscriptionIdResult.requireSuccess())
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
            { feedbinClient.getIcons() },
        ) { subscriptionsResult, taggingsResult, iconsResult ->
            if (subscriptionsResult.isError()) {
                return@parZip subscriptionsResult
            }
            if (taggingsResult.isError()) {
                return@parZip taggingsResult
            }

            val subscriptions = subscriptionsResult.requireSuccess()
            val taggings = taggingsResult.requireSuccess()
            val icons = if (iconsResult.isSuccess()) {
                iconsResult.requireSuccess()
            } else {
                emptyList()
            }

            val feedSources = subscriptions.map { it.toFeedSource(taggings, icons) }
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
        val feedSourceId = feedbinFeedSourceId(
            subscriptionId = subscription.id,
            feedId = subscription.feedId,
        )

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
        val feedbinIds = parseFeedbinIds(newFeedSource.id) ?: return DataNotFound.error()
        val feedId = feedbinIds.feedId

        val titleResult = updateFeedSourceTitleIfNeeded(feedbinIds, newFeedSource, originalFeedSource)
        if (titleResult.isError()) {
            return titleResult
        }

        if (originalFeedSource?.category != newFeedSource.category) {
            val taggingsResult = feedbinClient.getTaggings()
            if (taggingsResult.isError()) {
                return taggingsResult
            }

            val existingTaggingId = taggingsResult.requireSuccess()
                .firstOrNull { it.feedId == feedId }
                ?.id

            val categoryResult = updateFeedSourceCategoryIfNeeded(
                feedId = feedId,
                newFeedSource = newFeedSource,
                originalFeedSource = originalFeedSource,
                existingTaggingId = existingTaggingId,
            )
            if (categoryResult.isError()) {
                return categoryResult
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
        val feedbinIds = parseFeedbinIds(feedSourceId) ?: return DataNotFound.error()
        val subscriptionIdResult = resolveSubscriptionId(feedbinIds)
        if (subscriptionIdResult.isError()) {
            return subscriptionIdResult.failure.error()
        }

        val updateResult = feedbinClient.updateSubscription(
            subscriptionId = subscriptionIdResult.requireSuccess(),
            title = newName,
        )
        if (updateResult.isError()) {
            return updateResult.failure.error()
        }
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

    private suspend fun updateFeedSourceTitleIfNeeded(
        feedbinIds: FeedbinIds,
        newFeedSource: FeedSource,
        originalFeedSource: FeedSource?,
    ): DataResult<Unit> {
        if (originalFeedSource?.title != null && originalFeedSource.title != newFeedSource.title) {
            val subscriptionIdResult = resolveSubscriptionId(feedbinIds)
            if (subscriptionIdResult.isError()) {
                return subscriptionIdResult.failure.error()
            }

            val updateResult = feedbinClient.updateSubscription(
                subscriptionId = subscriptionIdResult.requireSuccess(),
                title = newFeedSource.title,
            )
            if (updateResult.isError()) {
                return updateResult.failure.error()
            }
        }

        return Unit.success()
    }

    private suspend fun updateFeedSourceCategoryIfNeeded(
        feedId: Long,
        newFeedSource: FeedSource,
        originalFeedSource: FeedSource?,
        existingTaggingId: Long?,
    ): DataResult<Unit> {
        if (originalFeedSource?.category != null && originalFeedSource.category != newFeedSource.category) {
            existingTaggingId?.let {
                val deleteResult = feedbinClient.deleteTagging(it)
                if (deleteResult.isError()) {
                    return deleteResult
                }
            }
        }

        val newCategory = newFeedSource.category
        if (newCategory != null && newCategory != originalFeedSource?.category) {
            val createResult = feedbinClient.createTagging(
                feedId = feedId,
                name = newCategory.title,
            )
            if (createResult.isError()) {
                return createResult
            }
        }

        return Unit.success()
    }

    private suspend fun fetchEntriesByIds(
        feedSources: List<FeedSource>,
        statusIds: FeedbinEntryStatusIds,
        entryIds: Set<Long>,
    ): DataResult<Unit> {
        if (entryIds.isEmpty()) {
            return Unit.success()
        }

        logger.d { "Fetching entries by id" }

        val feedSourcesMap = feedSources.mapNotNull { feedSource ->
            val feedId = parseFeedbinIds(feedSource.id)?.feedId ?: return@mapNotNull null
            feedId to feedSource
        }.toMap()

        val allEntries = mutableListOf<EntryDTO>()
        for (chunk in entryIds.chunked(ENTRY_IDS_CHUNK_SIZE)) {
            val result = feedbinClient.getEntries(
                ids = chunk,
                mode = ENTRY_MODE_EXTENDED,
            )
            if (result.isError()) {
                logger.e { "Failed to fetch entries: $result" }
                return result.failure.error()
            }
            result.onSuccessSuspend { entries ->
                allEntries.addAll(entries)
            }
        }

        val feedItems = allEntries.mapNotNull { entry ->
            val feedSource = feedSourcesMap[entry.feedId] ?: return@mapNotNull null
            val isRead = !statusIds.unreadIds.contains(entry.id)
            val isBookmarked = statusIds.starredIds.contains(entry.id)

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

    private suspend fun fetchEntriesByPages(
        feedSources: List<FeedSource>,
        statusIds: FeedbinEntryStatusIds,
        since: String?,
    ): DataResult<Unit> {
        logger.d { "Fetching entries by page" }

        val feedSourcesMap = feedSources.mapNotNull { feedSource ->
            val feedId = parseFeedbinIds(feedSource.id)?.feedId ?: return@mapNotNull null
            feedId to feedSource
        }.toMap()

        val allEntries = mutableListOf<EntryDTO>()
        var nextPage: Int? = 1
        while (nextPage != null) {
            val result = feedbinClient.getEntriesPage(
                page = nextPage,
                since = since,
                perPage = PAGE_SIZE,
                mode = ENTRY_MODE_EXTENDED,
            )

            if (result.isError()) {
                logger.e { "Failed to fetch entries: $result" }
                return result.failure.error()
            }

            result.onSuccessSuspend { entriesPage ->
                val entries = entriesPage.entries
                logger.d { "Fetched ${entries.size} entries on page $nextPage" }
                allEntries.addAll(entries)
                nextPage = entriesPage.nextPage
            }
        }

        val feedItems = allEntries.mapNotNull { entry ->
            val feedSource = feedSourcesMap[entry.feedId] ?: return@mapNotNull null
            val isRead = !statusIds.unreadIds.contains(entry.id)
            val isBookmarked = statusIds.starredIds.contains(entry.id)

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

    private suspend fun fetchMissingStarredEntries(
        feedSources: List<FeedSource>,
        statusIds: FeedbinEntryStatusIds,
    ): DataResult<Unit> {
        if (statusIds.starredIds.isEmpty()) {
            return Unit.success()
        }

        val missingStarredIds = databaseHelper
            .getMissingFeedItemIds(statusIds.starredIds.map { it.toString() })
            .mapNotNull { it.toLongOrNull() }
            .toSet()
        if (missingStarredIds.isEmpty()) {
            return Unit.success()
        }

        return fetchEntriesByIds(
            feedSources = feedSources,
            statusIds = statusIds,
            entryIds = missingStarredIds,
        )
    }

    private suspend fun fetchUnreadAndStarredIds(): DataResult<FeedbinEntryStatusIds> {
        logger.d { "Fetching starred entries" }
        val starredResult = feedbinClient.getStarredEntries()
        if (starredResult.isError()) {
            logger.d { "Failed to fetch starred entries: $starredResult" }
            return starredResult.failure.error()
        }

        logger.d { "Fetching unread entries" }
        val unreadResult = feedbinClient.getUnreadEntries()
        if (unreadResult.isError()) {
            logger.d { "Failed to fetch unread entries: $unreadResult" }
            return unreadResult.failure.error()
        }

        return FeedbinEntryStatusIds(
            unreadIds = unreadResult.requireSuccess().toSet(),
            starredIds = starredResult.requireSuccess().toSet(),
        ).success()
    }

    private suspend fun updateStatuses(statusIds: FeedbinEntryStatusIds) {
        databaseHelper.updateFeedItemReadStatus(
            statusIds.unreadIds.map { it.toString() },
        )
        databaseHelper.updateFeedItemBookmarkStatus(
            statusIds.starredIds.map { it.toString() },
        )
    }

    private suspend fun buildSinceForRegularSync(): String? {
        val lastUpdate = networkSettings.getLastSyncDate()
        return if (lastUpdate != null) {
            kotlinx.datetime.Instant.fromEpochSeconds(lastUpdate).toString()
        } else if (!databaseHelper.hasFeedItems()) {
            buildHistorySince()
        } else {
            null
        }
    }

    private fun buildHistorySince(): String {
        val cutoffSeconds = Clock.System.now().epochSeconds - FIRST_SYNC_CUTOFF.inWholeSeconds
        return kotlinx.datetime.Instant.fromEpochSeconds(cutoffSeconds).toString()
    }

    private data class FeedbinEntryStatusIds(
        val unreadIds: Set<Long>,
        val starredIds: Set<Long>,
    )

    private suspend fun resolveSubscriptionId(feedbinIds: FeedbinIds): DataResult<Long> {
        feedbinIds.subscriptionId?.let { return it.success() }

        val subscriptionsResult = feedbinClient.getSubscriptions()
        if (subscriptionsResult.isError()) {
            return subscriptionsResult.requireError().error()
        }

        val subscriptionId = subscriptionsResult.requireSuccess()
            .firstOrNull { it.feedId == feedbinIds.feedId }
            ?.id

        return subscriptionId?.success() ?: DataNotFound.error()
    }

    fun buildCategoryId(categoryName: CategoryName): String {
        return categoryName.name
    }

    companion object {
        private const val PAGE_SIZE = 100
        private const val ENTRY_MODE_EXTENDED = "extended"
        private const val MAX_MARK_BATCH_SIZE = 1000
        private const val ENTRY_IDS_CHUNK_SIZE = 100
        private val FIRST_SYNC_CUTOFF = 60.days
    }
}
