package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedUpdateStatus
import com.prof18.feedflow.core.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.mappers.toFeedItem
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import com.prof18.feedflow.shared.presentation.model.ErrorState
import com.prof18.feedflow.shared.utils.executeWithRetry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import com.prof18.feedflow.core.model.DatabaseError as DatabaseErrorCode

@OptIn(ExperimentalCoroutinesApi::class)
internal class FeedStateRepository(
    private val databaseHelper: DatabaseHelper,
    private val logger: Logger,
    private val settingsRepository: SettingsRepository,
    private val dateFormatter: DateFormatter,
) {
    private val errorMutableState: MutableSharedFlow<ErrorState> = MutableSharedFlow()
    val errorState = errorMutableState.asSharedFlow()

    private val updateMutableState: MutableStateFlow<FeedUpdateStatus> = MutableStateFlow(
        FinishedFeedUpdateStatus,
    )
    val updateState = updateMutableState.asStateFlow()

    private val mutableFeedState: MutableStateFlow<ImmutableList<FeedItem>> = MutableStateFlow(persistentListOf())
    val feedState = mutableFeedState.asStateFlow()

    private val currentFeedFilterMutableState: MutableStateFlow<FeedFilter> = MutableStateFlow(FeedFilter.Timeline)
    val currentFeedFilter: StateFlow<FeedFilter> = currentFeedFilterMutableState.asStateFlow()

    private var currentPage: Int = 0

    suspend fun getFeeds() {
        try {
            val feedOrder = settingsRepository.getFeedOrder()

            val feeds = executeWithRetry {
                databaseHelper.getFeedItems(
                    feedFilter = currentFeedFilterMutableState.value,
                    pageSize = FEED_DB_PAGE_SIZE,
                    offset = 0,
                    showReadItems = settingsRepository.getShowReadArticlesTimeline(),
                    sortOrder = feedOrder,
                )
            }
            currentPage = 1
            val removeTitleFromDesc = settingsRepository.getRemoveTitleFromDescription()
            val hideDesc = settingsRepository.getHideDescription()
            val hideImages = settingsRepository.getHideImages()
            val hideDate = settingsRepository.getHideDate()
            val dateFormat = settingsRepository.getDateFormat()
            val timeFormat = settingsRepository.getTimeFormat()

            if (feeds.isNotEmpty()) {
                updateMutableState.update { FinishedFeedUpdateStatus }
            }
            mutableFeedState.update {
                feeds.map {
                    it.toFeedItem(
                        dateFormatter = dateFormatter,
                        removeTitleFromDesc = removeTitleFromDesc,
                        hideDescription = hideDesc,
                        hideImages = hideImages,
                        hideDate = hideDate,
                        dateFormat = dateFormat,
                        timeFormat = timeFormat,
                    )
                }.toImmutableList()
            }
        } catch (e: Throwable) {
            logger.e(e) { "Something wrong while getting data from Database" }
            errorMutableState.emit(DatabaseError(DatabaseErrorCode.FeedRetrievalFailed))
        }
    }

    suspend fun loadMoreFeeds() {
        // Stop loading if there are no more items
        if (mutableFeedState.value.size % FEED_DB_PAGE_SIZE != 0L) {
            return
        }
        try {
            val feedOrder = settingsRepository.getFeedOrder()
            val feeds = executeWithRetry {
                databaseHelper.getFeedItems(
                    feedFilter = currentFeedFilterMutableState.value,
                    pageSize = FEED_DB_PAGE_SIZE,
                    offset = currentPage * FEED_DB_PAGE_SIZE,
                    showReadItems = settingsRepository.getShowReadArticlesTimeline(),
                    sortOrder = feedOrder,
                )
            }
            currentPage += 1
            val removeTitleFromDesc = settingsRepository.getRemoveTitleFromDescription()
            val hideDesc = settingsRepository.getHideDescription()
            val hideImages = settingsRepository.getHideImages()
            val hideDate = settingsRepository.getHideDate()
            val dateFormat = settingsRepository.getDateFormat()
            val timeFormat = settingsRepository.getTimeFormat()
            mutableFeedState.update { currentItems ->
                val newList = feeds.map {
                    it.toFeedItem(
                        dateFormatter = dateFormatter,
                        removeTitleFromDesc = removeTitleFromDesc,
                        hideDescription = hideDesc,
                        hideImages = hideImages,
                        hideDate = hideDate,
                        dateFormat = dateFormat,
                        timeFormat = timeFormat,
                    )
                }.toImmutableList()
                (currentItems + newList).toImmutableList()
            }
        } catch (e: Throwable) {
            logger.e(e) { "Something wrong while getting data from Database" }
            errorMutableState.emit(DatabaseError(DatabaseErrorCode.PaginationFailed))
        }
    }

    suspend fun updateFeedFilter(feedFilter: FeedFilter) {
        currentFeedFilterMutableState.update {
            feedFilter
        }
        getFeeds()
    }

    suspend fun updateFeedSourceFilter(feedSourceId: String) {
        val feedSource = databaseHelper.getFeedSource(feedSourceId)
        if (feedSource == null) {
            getFeeds()
            return
        }
        val newFeedFilter = FeedFilter.Source(
            feedSource = feedSource,
        )
        currentFeedFilterMutableState.update {
            newFeedFilter
        }
        getFeeds()
    }

    fun getUnreadFeedCountFlow(): Flow<Long> =
        currentFeedFilter.flatMapLatest { feedFilter ->
            databaseHelper.getUnreadFeedCountFlow(
                feedFilter = feedFilter,
            )
        }

    fun getUnreadBookmarksCountFlow(): Flow<Long> =
        databaseHelper.getUnreadFeedCountFlow(
            feedFilter = FeedFilter.Bookmarks,
        )

    fun getUnreadTimelineCountFlow(): Flow<Long> =
        databaseHelper.getUnreadFeedCountFlow(
            feedFilter = FeedFilter.Timeline,
        )

    fun updateCurrentFilterName(newName: String) {
        val currentFilter = currentFeedFilter.value
        if (currentFilter is FeedFilter.Source) {
            currentFeedFilterMutableState.update {
                FeedFilter.Source(
                    feedSource = currentFilter.feedSource.copy(
                        title = newName,
                    ),
                )
            }
        }
    }

    fun markAsRead(itemsToUpdates: HashSet<FeedItemId>) {
        mutableFeedState.update { currentItems ->
            currentItems.map { feedItem ->
                if (FeedItemId(feedItem.id) in itemsToUpdates) {
                    feedItem.copy(isRead = true)
                } else {
                    feedItem
                }
            }.toImmutableList()
        }
    }

    fun getCurrentFeedFilter(): FeedFilter =
        currentFeedFilter.value

    fun updateBookmarkStatus(feedItemId: FeedItemId, isBookmarked: Boolean) {
        mutableFeedState.update { currentItems ->
            currentItems.mapNotNull { feedItem ->
                if (feedItem.id == feedItemId.id) {
                    if (currentFeedFilter.value == FeedFilter.Bookmarks && !isBookmarked) {
                        null
                    } else {
                        feedItem.copy(isBookmarked = isBookmarked)
                    }
                } else {
                    feedItem
                }
            }.toImmutableList()
        }
    }

    fun updateReadStatus(feedItemId: FeedItemId, isRead: Boolean) {
        mutableFeedState.update { currentItems ->
            currentItems.mapNotNull { feedItem ->
                if (feedItem.id == feedItemId.id) {
                    if (currentFeedFilter.value == FeedFilter.Read && !isRead) {
                        null
                    } else {
                        feedItem.copy(isRead = isRead)
                    }
                } else {
                    feedItem
                }
            }.toImmutableList()
        }
    }

    fun emitUpdateStatus(status: FeedUpdateStatus) {
        updateMutableState.update {
            status
        }
    }

    suspend fun emitErrorState(errorState: ErrorState) {
        errorMutableState.emit(errorState)
    }

    suspend fun getNextArticle(currentArticleId: String): FeedItem? {
        val currentList = mutableFeedState.value
        val currentIndex = currentList.indexOfFirst { it.id == currentArticleId }
        if (currentIndex == -1) return null

        val nextIndex = currentIndex + 1
        if (nextIndex < currentList.size) {
            val shouldLoadMore = nextIndex >= currentList.size - PAGINATION_THRESHOLD
            if (shouldLoadMore) {
                loadMoreFeeds()
            }
            return currentList.getOrNull(nextIndex)
        }
        return null
    }

    fun getPreviousArticle(currentArticleId: String): FeedItem? {
        val currentList = mutableFeedState.value
        val currentIndex = currentList.indexOfFirst { it.id == currentArticleId }
        if (currentIndex == -1 || currentIndex == 0) return null
        return currentList.getOrNull(currentIndex - 1)
    }

    fun getArticlePosition(currentArticleId: String): ArticlePosition? {
        val currentList = mutableFeedState.value
        val currentIndex = currentList.indexOfFirst { it.id == currentArticleId }
        if (currentIndex == -1) return null
        return ArticlePosition(
            currentPosition = currentIndex + 1,
            totalArticles = currentList.size,
        )
    }

    data class ArticlePosition(
        val currentPosition: Int,
        val totalArticles: Int,
    )

    companion object {
        internal const val FEED_DB_PAGE_SIZE = 40L
        private const val PAGINATION_THRESHOLD = 5
    }
}
