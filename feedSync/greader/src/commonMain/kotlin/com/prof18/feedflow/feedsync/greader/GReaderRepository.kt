package com.prof18.feedflow.feedsync.greader

import arrow.fx.coroutines.parZip
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.DataNotFound
import com.prof18.feedflow.core.model.DataResult
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.NetworkFailure
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
import com.prof18.feedflow.feedsync.greader.data.dto.ItemContentDTO
import com.prof18.feedflow.feedsync.greader.data.dto.ItemDTO
import com.prof18.feedflow.feedsync.greader.data.dto.StreamItemsContentsDTO
import com.prof18.feedflow.feedsync.greader.domain.Stream
import com.prof18.feedflow.feedsync.greader.domain.SubscriptionEditAction
import com.prof18.feedflow.feedsync.greader.domain.mapping.ItemContentDTOMapper
import com.prof18.feedflow.feedsync.greader.domain.mapping.toFeedSource
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class GReaderRepository internal constructor(
    private val gReaderClient: GReaderClient,
    private val logger: Logger,
    private val networkSettings: NetworkSettings,
    private val databaseHelper: DatabaseHelper,
    private val itemContentDTOMapper: ItemContentDTOMapper,
    private val dateFormatter: DateFormatter,
    private val dispatcherProvider: DispatcherProvider,
) {
    fun isAccountSet(): Boolean = networkSettings.getSyncPwd().isNotEmpty()

    suspend fun login(
        username: String,
        password: String,
        baseURL: String,
    ): DataResult<Unit> = gReaderClient.login(username, password, baseURL)
        .map { response ->
            logger.d { "Login successful: $response" }
            val authToken = getAuthToken(response) ?: return NetworkFailure.Unauthorised.error()

            if (authToken == UNAUTHORIZED_MESSAGE) {
                logger.d { "Unauthorized Login" }
                return NetworkFailure.Unauthorised.error()
            }
            networkSettings.setSyncPwd(authToken)
            networkSettings.setSyncUsername(username)
            networkSettings.setSyncUrl(baseURL)
            databaseHelper.deleteAll()
        }

    suspend fun sync(): DataResult<Unit> = withContext(dispatcherProvider.io) {
        logger.d { "Sync started" }

        val categoriesResult = fetchFeedSourcesAndCategories()
        if (categoriesResult.isError()) {
            return@withContext categoriesResult
        }

        val lastUpdate = networkSettings.getLastSyncDate()
        val feedSources = databaseHelper.getFeedSources()

        val readingListResult = fetchContent(
            block = { continuation ->
                gReaderClient.getItems(
                    continuation = continuation,
                    lastModified = lastUpdate,
                    excludeTargets = listOf(Stream.Read().id, Stream.Starred().id),
                    max = PAGE_SIZE,
                )
            },
            feedSources = feedSources,
        )
        if (readingListResult.isError()) {
            return@withContext readingListResult
        }

        val starredResult = fetchContent(
            block = { continuation ->
                gReaderClient.getStarredItemsContent(
                    since = lastUpdate,
                    maxNumber = PAGE_SIZE,
                    continuation = continuation,
                )
            },
            feedSources = feedSources,
        )
        if (starredResult.isError()) {
            return@withContext starredResult
        }

        parZip(
            ctx = dispatcherProvider.io,
            { fetchStarredItems() },
            { fetchUnreadItems() },
        ) { starredItemsResult, unreadItemsResult ->
            val results = starredItemsResult + unreadItemsResult

            if (results.allSuccess()) {
                databaseHelper.updateFeedItemReadStatus(
                    unreadItemsResult.requireSuccess().map { it.getHexID() },
                )
                databaseHelper.updateFeedItemBookmarkStatus(
                    starredItemsResult.requireSuccess().map { it.getHexID() },
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
        val addTag = if (isBookmarked) Stream.Starred() else null
        val removeTag = if (isBookmarked) null else Stream.Starred()

        val result = gReaderClient.editTag(
            itemIds = listOf(feedItemId.id),
            addTag = addTag,
            removeTag = removeTag,
        )
        if (result.isError()) {
            return result
        }

        databaseHelper.updateBookmarkStatus(feedItemId, isBookmarked = isBookmarked)
        return Unit.success()
    }

    suspend fun updateReadStatus(feedItemIds: List<FeedItemId>, isRead: Boolean): DataResult<Unit> {
        val addTag = if (isRead) Stream.Read() else null
        val removeTag = if (isRead) null else Stream.Read()

        val result = gReaderClient.editTag(
            itemIds = feedItemIds.map { it.id },
            addTag = addTag,
            removeTag = removeTag,
        )
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
        val feedSources = databaseHelper.getFeedSourcesByCategory(categoryId)

        val result = gReaderClient.disableTag(categoryId)
        if (result.isError()) {
            return result
        }

        for (feedSource in feedSources) {
            gReaderClient.editSubscription(
                feedSourceId = feedSource.id,
                editAction = SubscriptionEditAction.EDIT,
                removeCategoryId = categoryId,
            )
            if (result.isError()) {
                return result
            }
        }

        databaseHelper.deleteCategory(categoryId)
        return Unit.success()
    }

    suspend fun deleteFeedSource(feedSourceId: String): DataResult<Unit> {
        val result = gReaderClient.editSubscription(
            feedSourceId = feedSourceId,
            editAction = SubscriptionEditAction.UNSUBSCRIBE,
        )
        if (result.isError()) {
            return result
        }

        databaseHelper.deleteFeedSource(feedSourceId)
        return Unit.success()
    }

    suspend fun fetchFeedSourcesAndCategories(): DataResult<Unit> =
        gReaderClient.getFeedSourcesAndCategories()
            .map { response ->
                val feedSources = response.subscriptions.map { it.toFeedSource() }
                val categories = feedSources.mapNotNull { it.category }

                databaseHelper.insertCategories(categories)
                databaseHelper.insertFeedSource(feedSources)
                databaseHelper.deleteFeedSourceExcept(feedSources.map { it.id })
                databaseHelper.deleteCategoriesExcept(categories.map { it.id })
                Unit.success()
            }

    suspend fun addFeedSource(url: String, categoryName: FeedSourceCategory?): DataResult<Unit> {
        val result = gReaderClient.addSubscription(url)
        if (result.isError()) {
            return result
        }

        val feedSourceDTO = result.requireSuccess()
        feedSourceDTO.streamId ?: return DataNotFound.error()
        feedSourceDTO.query ?: return DataNotFound.error()

        val categoryResult = gReaderClient.editSubscription(
            feedSourceId = feedSourceDTO.streamId,
            editAction = SubscriptionEditAction.EDIT,
            addCategoryId = categoryName?.id,
        )
        if (categoryResult.isError()) {
            return categoryResult
        }

        return fetchFeedSourcesAndCategories()
    }

    suspend fun editFeedSource(
        newFeedSource: FeedSource,
        originalFeedSource: FeedSource?,
    ): DataResult<Unit> {
        val result = gReaderClient.editSubscription(
            feedSourceId = newFeedSource.id,
            editAction = SubscriptionEditAction.EDIT,
            title = newFeedSource.title,
            addCategoryId = newFeedSource.category?.id,
            removeCategoryId = originalFeedSource?.category?.id,
        )
        if (result.isError()) {
            return result
        }

        databaseHelper.updateFeedSource(newFeedSource)
        fetchFeedSourcesAndCategories()
        return Unit.success()
    }

    suspend fun editFeedSourceName(
        feedSourceId: String,
        newName: String,
    ): DataResult<Unit> {
        val result = gReaderClient.editSubscription(
            feedSourceId = feedSourceId,
            editAction = SubscriptionEditAction.EDIT,
            title = newName,
        )
        if (result.isError()) {
            return result
        }

        databaseHelper.updateFeedSourceName(feedSourceId, newName)
        return Unit.success()
    }

    private suspend fun fetchStarredItems(): DataResult<List<ItemDTO>> {
        logger.d { "Fetching starred items" }

        val result = gReaderClient.getStreamItemsIDs(
            count = PAGE_SIZE * MAX_PAGES,
            stream = Stream.Starred(),
        )

        logger.d { "Result: $result" }

        if (result.isError()) {
            logger.e { "Failed to fetch starred items: $result" }
            return result.failure.error()
        }

        return result.map { it.itemRefs }
    }

    private suspend fun fetchUnreadItems(): DataResult<List<ItemDTO>> {
        logger.d { "Fetching unread items" }

        val result = gReaderClient.getStreamItemsIDs(
            stream = Stream.ReadingList(),
            excludedStream = Stream.Read(),
            count = PAGE_SIZE * MAX_PAGES,
        )

        if (result.isError()) {
            logger.e { "Failed to fetch unread items: $result" }
            return result.failure.error()
        }

        return result.map { it.itemRefs }
    }

    private suspend fun fetchContent(
        block: suspend (continuation: String?) -> DataResult<StreamItemsContentsDTO>,
        feedSources: List<FeedSource>,
    ): DataResult<Unit> {
        logger.d { "Fetching reading list items" }
        var currentContinuation: String? = null
        var count = 0
        val itemsToSave = mutableListOf<ItemContentDTO>()

        do {
            val result = block(currentContinuation)

            if (result.isError()) {
                logger.e { "Failed to fetch reading list items: $result" }
                return result.failure.error()
            }

            result.onSuccessSuspend { items ->
                logger.d { "Reading list items: ${items.items.size}" }
                itemsToSave.addAll(items.items)
                currentContinuation = items.continuation
                count++
                if (currentContinuation != null) {
                    logger.d { "Will fetch more reading list items" }
                }
            }
        } while (currentContinuation != null && count < MAX_PAGES)

        val feedItems = itemsToSave.mapNotNull { item ->
            val feedSource = feedSources.firstOrNull {
                it.id == item.origin.streamId
            } ?: return@mapNotNull null
            itemContentDTOMapper.mapToFeedItem(
                itemContentDTO = item,
                feedSource = feedSource,
            )
        }
        databaseHelper.insertFeedItems(feedItems, dateFormatter.currentTimeMillis())

        return Unit.success()
    }

    private fun getAuthToken(responseBody: String): String? =
        responseBody
            .split("\n")
            .map { it.split("=") }
            .associate {
                it.first() to it.last()
            }
            .getOrElse("Auth") { null }

    companion object {
        private const val UNAUTHORIZED_MESSAGE = "Unauthorized"
        private const val PAGE_SIZE = 500
        private const val MAX_PAGES = 2
    }
}
