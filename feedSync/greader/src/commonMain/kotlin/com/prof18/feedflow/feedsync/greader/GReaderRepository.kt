package com.prof18.feedflow.feedsync.greader

import arrow.fx.coroutines.parZip
import co.touchlab.kermit.Logger
import co.touchlab.stately.concurrency.AtomicReference
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.DataNotFound
import com.prof18.feedflow.core.model.DataResult
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.NetworkFailure
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.model.Unknown
import com.prof18.feedflow.core.model.anySuccess
import com.prof18.feedflow.core.model.error
import com.prof18.feedflow.core.model.firstError
import com.prof18.feedflow.core.model.fold
import com.prof18.feedflow.core.model.isError
import com.prof18.feedflow.core.model.map
import com.prof18.feedflow.core.model.onSuccess
import com.prof18.feedflow.core.model.plus
import com.prof18.feedflow.core.model.requireSuccess
import com.prof18.feedflow.core.model.success
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.greader.data.dto.ItemDTO
import com.prof18.feedflow.feedsync.greader.domain.Stream
import com.prof18.feedflow.feedsync.greader.domain.SubscriptionEditAction
import com.prof18.feedflow.feedsync.greader.domain.mapping.ItemContentDTOMapper
import com.prof18.feedflow.feedsync.greader.domain.mapping.toFeedSource
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

class GReaderRepository internal constructor(
    private val gReaderClient: GReaderClient,
    private val logger: Logger,
    private val networkSettings: NetworkSettings,
    private val databaseHelper: DatabaseHelper,
    private val itemContentDTOMapper: ItemContentDTOMapper,
    private val dateFormatter: DateFormatter,
    private val dispatcherProvider: DispatcherProvider,
    private val feedSyncMessageQueue: FeedSyncMessageQueue,
) {
    private var syncItemHolder: AtomicReference<SyncItemHolder?> = AtomicReference(null)

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
        syncItemHolder.set(SyncItemHolder())

        val categoriesResult = fetchFeedSourcesAndCategories()
        if (categoriesResult.isError()) {
            syncItemHolder.set(null)
            return@withContext categoriesResult
        }

        val defaultCutOffDate = Clock.System.now().minus(90.days).epochSeconds
        val lastUpdate = networkSettings.getLastSyncDate() ?: defaultCutOffDate

        return@withContext parZip(
            ctx = dispatcherProvider.io,
            { fetchStarredItems() },
            { fetchReadingList(since = lastUpdate) },
            { fetchUnreadItems() },
        ) { starredItemsResult, readingListItemsResult, unreadItemsResult ->
            starredItemsResult to readingListItemsResult to unreadItemsResult

            val results = starredItemsResult + readingListItemsResult + unreadItemsResult

            if (results.anySuccess()) {
                getItemsContent()

                databaseHelper.updateFeedItemReadStatus(
                    syncItemHolder.get()?.unreadItems?.map { it.getHexID() }.orEmpty(),
                )
                databaseHelper.updateFeedItemBookmarkStatus(
                    syncItemHolder.get()?.starredItems?.map { it.getHexID() }.orEmpty(),
                )

                networkSettings.setLastSyncDate(Clock.System.now().epochSeconds)

                syncItemHolder.set(null)
                logger.d { "Sync finished" }
                return@parZip Unit.success()
            } else {
                return@parZip results.firstError()?.error()
            }
        } ?: Unknown.error()
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

    private suspend fun fetchStarredItems(): DataResult<Unit> {
        logger.d { "Fetching starred items" }
        var currentContinuation: String? = null

        do {
            val result = gReaderClient.getStreamItemsIDs(
                stream = Stream.Starred(),
                continuation = currentContinuation,
            )

            logger.d { "Result: $result" }

            if (result.isError()) {
                logger.e { "Failed to fetch starred items: $result" }
                return result.failure.error()
            }

            result.onSuccess { items ->
                logger.d { "Starred items: ${items.itemRefs.size}" }
                syncItemHolder.get()?.starredItems?.addAll(items.itemRefs)
                currentContinuation = items.continuation
                if (currentContinuation != null) {
                    logger.d { "Will fetch more starred items" }
                }
            }
        } while (currentContinuation != null)

        return Unit.success()
    }

    private suspend fun fetchReadingList(since: Long): DataResult<Unit> {
        logger.d { "Fetching reading list items" }
        var currentContinuation: String? = null

        do {
            val result = gReaderClient.getStreamItemsIDs(
                stream = Stream.ReadingList(),
                continuation = currentContinuation,
                since = since,
            )

            logger.d { "Result: $result" }

            if (result.isError()) {
                logger.e { "Failed to fetch reading list items: $result" }
                return result.failure.error()
            }

            result.onSuccess { items ->
                logger.d { "Reading list items: ${items.itemRefs.size}" }
                syncItemHolder.get()?.readingListItems?.addAll(items.itemRefs)
                currentContinuation = items.continuation
                if (currentContinuation != null) {
                    logger.d { "Will fetch more reading list items" }
                }
            }
        } while (currentContinuation != null)

        return Unit.success()
    }

    private suspend fun fetchUnreadItems(): DataResult<Unit> {
        logger.d { "Fetching unread items" }
        var currentContinuation: String? = null

        do {
            val result = gReaderClient.getStreamItemsIDs(
                stream = Stream.ReadingList(),
                continuation = currentContinuation,
                excludedStream = Stream.Read(),
            )

            if (result.isError()) {
                logger.e { "Failed to fetch unread items: $result" }
                return result.failure.error()
            }

            result.onSuccess { items ->
                logger.d { "Unread items: ${items.itemRefs.size}" }
                syncItemHolder.get()?.unreadItems?.addAll(items.itemRefs)
                currentContinuation = items.continuation
                if (currentContinuation != null) {
                    logger.d { "Will fetch more unread items" }
                }
            }
        } while (currentContinuation != null)

        return Unit.success()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getItemsContent() {
        val idsFromDb = databaseHelper.getAllFeedItemUrlHashes().toSet()
        val items: Set<String> = with(syncItemHolder.get()) {
            this ?: return@with emptySet()
            (starredItems + unreadItems + readingListItems)
                .distinctBy { it.id }
                .map { it.getHexID() }
                .subtract(idsFromDb)
                .toSet()
        }

        val feedSources = databaseHelper.getFeedSources().toSet()

        items
            .chunked(PAGE_SIZE)
            .asFlow()
            .flatMapMerge { itemIds ->
                suspend {
                    gReaderClient.getStreamItemsContents(itemIds)
                        .fold(
                            onSuccess = { itemsResponse ->
                                val feedItems = itemsResponse.items.mapNotNull { item ->
                                    val feedSource = feedSources.firstOrNull {
                                        it.id == item.origin.streamId
                                    } ?: return@mapNotNull null
                                    itemContentDTOMapper.mapToFeedItem(
                                        itemContentDTO = item,
                                        feedSource = feedSource,
                                    )
                                }
                                databaseHelper.insertFeedItems(feedItems, dateFormatter.currentTimeMillis())
                            },
                            onFailure = {
                                feedSyncMessageQueue.emitResult(SyncResult.Error)
                            },
                        )
                }.asFlow()
            }
            .collect()
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
        private const val PAGE_SIZE = 100
    }
}

internal class SyncItemHolder {
    val starredItems = mutableListOf<ItemDTO>()
    val unreadItems = mutableListOf<ItemDTO>()
    val readingListItems = mutableListOf<ItemDTO>()
}
