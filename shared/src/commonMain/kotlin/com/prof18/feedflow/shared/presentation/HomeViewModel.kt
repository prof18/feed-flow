package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.DrawerItem.DrawerCategory
import com.prof18.feedflow.core.model.DrawerItem.DrawerFeedSource
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feed.FeedFontSizeRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import com.prof18.feedflow.shared.presentation.model.FeedErrorState
import com.prof18.feedflow.shared.presentation.model.SyncError
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel internal constructor(
    private val feedActionsRepository: FeedActionsRepository,
    private val feedSourcesRepository: FeedSourcesRepository,
    private val settingsRepository: SettingsRepository,
    private val feedSyncRepository: FeedSyncRepository,
    private val feedFontSizeRepository: FeedFontSizeRepository,
    private val feedCategoryRepository: FeedCategoryRepository,
    private val feedStateRepository: FeedStateRepository,
    private val feedFetcherRepository: FeedFetcherRepository,
    private val dateFormatter: DateFormatter,
    private val logger: Logger,
) : ViewModel() {

    // Loading
    val loadingState: StateFlow<FeedUpdateStatus> = feedStateRepository.updateState

    // Feeds
    val feedState: StateFlow<ImmutableList<FeedItem>> = feedStateRepository.feedState

    val unreadCountFlow: Flow<Long> = feedStateRepository.getUnreadFeedCountFlow()

    // Error
    private val mutableUIErrorState: MutableSharedFlow<UIErrorState> = MutableSharedFlow()
    val errorState: SharedFlow<UIErrorState> = mutableUIErrorState.asSharedFlow()

    // Drawer State
    private val drawerMutableState = MutableStateFlow(NavDrawerState())
    val navDrawerState = drawerMutableState.asStateFlow()

    private val isDeletingMutableState = MutableStateFlow(false)
    val isDeletingState: StateFlow<Boolean> = isDeletingMutableState.asStateFlow()

    private var lastUpdateIndex = 0

    val currentFeedFilter = feedStateRepository.currentFeedFilter
    val isSyncUploadRequired: StateFlow<Boolean> = settingsRepository.isSyncUploadRequired

    val feedFontSizeState: StateFlow<FeedFontSizes> = feedFontSizeRepository.feedFontSizeState

    init {
        observeErrorState()
        viewModelScope.launch {
            feedStateRepository.updateFeedFilter(FeedFilter.Timeline)
            initDrawerData()
            feedStateRepository.getFeeds()
        }
    }

    private suspend fun initDrawerData() {
        combine(
            feedSourcesRepository.observeFeedSourcesByCategoryWithUnreadCount(),
            feedCategoryRepository.observeCategoriesWithUnreadCount(),
            feedStateRepository.getUnreadTimelineCountFlow(),
            feedStateRepository.getUnreadBookmarksCountFlow(),
        ) { feedSourceByCategoryWithCount, categoriesWithCount, timelineCount, bookmarksCount ->
            val containsOnlyNullKey = feedSourceByCategoryWithCount.keys.all { it == null }

            val pinnedFeedSources = feedSourceByCategoryWithCount.values.flatten().filter { it.feedSource.isPinned }

            val feedSourcesWithoutCategory = feedSourceByCategoryWithCount[null]
                ?.map { feedSourceWithCount ->
                    DrawerFeedSource(
                        feedSource = feedSourceWithCount.feedSource,
                        unreadCount = feedSourceWithCount.unreadCount,
                    )
                } ?: listOf()

            NavDrawerState(
                timeline = listOf(DrawerItem.Timeline(unreadCount = timelineCount)),
                read = listOf(DrawerItem.Read),
                bookmarks = listOf(DrawerItem.Bookmarks(unreadCount = bookmarksCount)),
                categories = categoriesWithCount.map { categoryWithCount ->
                    DrawerCategory(
                        category = categoryWithCount.category,
                        unreadCount = categoryWithCount.unreadCount,
                    )
                },
                pinnedFeedSources = pinnedFeedSources.map { feedSourceWithCount ->
                    DrawerFeedSource(
                        feedSource = feedSourceWithCount.feedSource,
                        unreadCount = feedSourceWithCount.unreadCount,
                    )
                },
                feedSourcesWithoutCategory = if (containsOnlyNullKey) {
                    feedSourcesWithoutCategory
                } else {
                    listOf()
                },
                feedSourcesByCategory = if (containsOnlyNullKey) {
                    mapOf()
                } else {
                    feedSourceByCategoryWithCount.map { (category, feedSources) ->
                        val categoryWrapper = DrawerFeedSource.FeedSourceCategoryWrapper(
                            feedSourceCategory = category,
                        )
                        categoryWrapper to feedSources.map { feedSourceWithCount ->
                            DrawerFeedSource(
                                feedSource = feedSourceWithCount.feedSource,
                                unreadCount = feedSourceWithCount.unreadCount,
                            )
                        }
                    }.toMap()
                },
            )
        }.collect { navDrawerState ->
            drawerMutableState.update { navDrawerState }
        }
    }

    private fun observeErrorState() {
        viewModelScope.launch {
            feedStateRepository.errorState
                .collect { error ->
                    when (error) {
                        is FeedErrorState -> {
                            mutableUIErrorState.emit(
                                UIErrorState.FeedErrorState(
                                    feedName = error.failingSourceName,
                                ),
                            )
                        }

                        is DatabaseError -> {
                            mutableUIErrorState.emit(
                                UIErrorState.DatabaseError,
                            )
                        }

                        is SyncError -> {
                            mutableUIErrorState.emit(
                                UIErrorState.SyncError,
                            )
                        }
                    }
                }
        }
    }

    fun getNewFeeds() {
        lastUpdateIndex = 0
        viewModelScope.launch {
            feedFetcherRepository.fetchFeeds()
        }
    }

    fun markAsReadOnScroll(lastVisibleIndex: Int) {
        if (settingsRepository.getMarkFeedAsReadWhenScrolling()) {
            // To avoid issues
            if (loadingState.value.isLoading()) {
                return
            }

            viewModelScope.launch {
                val urlToUpdates = hashSetOf<FeedItemId>()
                val items = feedState.value.toMutableList()
                if (lastVisibleIndex <= lastUpdateIndex) {
                    return@launch
                }
                for (index in lastUpdateIndex..lastVisibleIndex) {
                    items.getOrNull(index)?.let { item ->
                        urlToUpdates.add(
                            FeedItemId(
                                id = item.id,
                            ),
                        )
                    }
                }
                feedActionsRepository.markAsRead(urlToUpdates)
                lastUpdateIndex = lastVisibleIndex
            }
        }
    }

    fun requestNewFeedsPage() {
        viewModelScope.launch {
            feedStateRepository.loadMoreFeeds()
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            feedActionsRepository.markAllCurrentFeedAsRead()
            feedFetcherRepository.fetchFeeds()
        }
    }

    fun markAsRead(feedItemId: String) {
        viewModelScope.launch {
            feedActionsRepository.markAsRead(
                hashSetOf(
                    FeedItemId(feedItemId),
                ),
            )
        }
    }

    fun deleteOldFeedItems() {
        viewModelScope.launch {
            isDeletingMutableState.update { true }
            feedActionsRepository.deleteOldFeeds()
            isDeletingMutableState.update { false }
        }
    }

    fun forceFeedRefresh() {
        lastUpdateIndex = 0
        viewModelScope.launch {
            feedFetcherRepository.fetchFeeds(forceRefresh = true)
        }
    }

    fun deleteAllFeeds() {
        viewModelScope.launch {
            feedSourcesRepository.deleteAllFeeds()
        }
    }

    fun onFeedFilterSelected(selectedFeedFilter: FeedFilter) {
        viewModelScope.launch {
            feedStateRepository.getFeeds()
            feedStateRepository.updateFeedFilter(selectedFeedFilter)
            lastUpdateIndex = 0
        }
    }

    fun updateReadStatus(feedItemId: FeedItemId, read: Boolean) {
        viewModelScope.launch {
            feedActionsRepository.updateReadStatus(feedItemId, read)
        }
    }

    fun updateBookmarkStatus(feedItemId: FeedItemId, bookmarked: Boolean) {
        viewModelScope.launch {
            feedActionsRepository.updateBookmarkStatus(feedItemId, bookmarked)
        }
    }

    fun toggleFeedPin(feedSource: FeedSource) {
        viewModelScope.launch {
            feedSourcesRepository.insertFeedSourcePreference(
                feedSourceId = feedSource.id,
                preference = feedSource.linkOpeningPreference,
                isHidden = feedSource.isHiddenFromTimeline,
                isPinned = !feedSource.isPinned,
            )
        }
    }

    // Used on iOS
    fun enqueueBackup() {
        viewModelScope.launch {
            feedSyncRepository.enqueueBackup()
        }
    }

    fun deleteFeedSource(feedSource: FeedSource) {
        viewModelScope.launch {
            feedSourcesRepository.deleteFeed(feedSource)
            feedFetcherRepository.fetchFeeds()
        }
    }

    fun updateCategoryName(categoryId: CategoryId, newName: CategoryName) {
        viewModelScope.launch {
            feedCategoryRepository.updateCategoryName(categoryId, newName)
        }
    }

    fun deleteCategory(categoryId: CategoryId) {
        viewModelScope.launch {
            feedCategoryRepository.deleteCategory(categoryId.value)
            feedStateRepository.getFeeds()
        }
    }

    @Suppress("MagicNumber")
    fun loadFeeds() {
        val lastForegroundTimestamp = settingsRepository.getLastFeedSyncTimestamp()
        val oneHourInMillis = 60 * 60 * 1000L
        val currentTimestamp = dateFormatter.currentTimeMillis()
        if ((currentTimestamp - lastForegroundTimestamp) >= oneHourInMillis) {
            getNewFeeds()
        } else {
            logger.d { "An hour is not passed, skipping automatic refresh" }
        }
    }
}
