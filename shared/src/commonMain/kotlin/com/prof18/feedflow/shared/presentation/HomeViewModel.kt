package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.DrawerItem.DrawerCategory
import com.prof18.feedflow.core.model.DrawerItem.DrawerFeedSource
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.shared.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.shared.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.domain.settings.SettingsRepository
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import com.prof18.feedflow.shared.presentation.model.FeedErrorState
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
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
    private val feedRetrieverRepository: FeedRetrieverRepository,
    private val feedManagerRepository: FeedManagerRepository,
    private val settingsRepository: SettingsRepository,
    private val feedSyncRepository: FeedSyncRepository,
) : BaseViewModel() {

    // Loading
    @NativeCoroutinesState
    val loadingState: StateFlow<FeedUpdateStatus> = feedRetrieverRepository.updateState

    // Feeds
    @NativeCoroutinesState
    val feedState: StateFlow<ImmutableList<FeedItem>> = feedRetrieverRepository.feedState

    @NativeCoroutines
    val unreadCountFlow: Flow<Long> = feedRetrieverRepository.getUnreadFeedCountFlow()

    // Error
    private val mutableUIErrorState: MutableSharedFlow<UIErrorState?> = MutableSharedFlow()

    @NativeCoroutines
    val errorState: SharedFlow<UIErrorState?> = mutableUIErrorState.asSharedFlow()

    // Drawer State
    private val drawerMutableState = MutableStateFlow(NavDrawerState())

    @NativeCoroutinesState
    val navDrawerState = drawerMutableState.asStateFlow()

    private var lastUpdateIndex = 0

    @NativeCoroutinesState
    val currentFeedFilter = feedRetrieverRepository.currentFeedFilter

    @NativeCoroutinesState
    val isSyncUploadRequired: StateFlow<Boolean> = settingsRepository.isSyncUploadRequired

    init {
        scope.launch {
            feedRetrieverRepository.updateFeedFilter(FeedFilter.Timeline)
            initDrawerData()
            observeErrorState()
            feedRetrieverRepository.getFeeds()
            getNewFeeds(isFirstLaunch = true)
        }
    }

    private fun initDrawerData() {
        scope.launch {
            feedManagerRepository.observeFeedSourcesByCategory()
                .combine(feedManagerRepository.observeCategories()) { feedSourceByCategory, categories ->

                    val containsOnlyNullKey = feedSourceByCategory.keys.all { it == null }

                    val feedSourcesWithoutCategory = feedSourceByCategory[null]
                        ?.map { feedSource ->
                            DrawerFeedSource(
                                feedSource = feedSource,
                            )
                        } ?: listOf()

                    NavDrawerState(
                        timeline = listOf(DrawerItem.Timeline),
                        read = listOf(DrawerItem.Read),
                        bookmarks = listOf(DrawerItem.Bookmarks),
                        categories = categories.map { category ->
                            DrawerCategory(category = category)
                        },
                        feedSourcesWithoutCategory = if (containsOnlyNullKey) {
                            feedSourcesWithoutCategory
                        } else {
                            listOf()
                        },
                        feedSourcesByCategory = if (containsOnlyNullKey) {
                            mapOf()
                        } else {
                            feedSourceByCategory.map { (category, feedSources) ->
                                val categoryWrapper = DrawerFeedSource.FeedSourceCategoryWrapper(
                                    feedSourceCategory = category,
                                )
                                categoryWrapper to feedSources.map { feedSource ->
                                    DrawerFeedSource(
                                        feedSource = feedSource,
                                    )
                                }
                            }.toMap()
                        },
                    )
                }.collect { navDrawerState ->
                    drawerMutableState.update { navDrawerState }
                }
        }
    }

    private fun observeErrorState() {
        scope.launch {
            feedRetrieverRepository.errorState
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

                        null -> {
                            // Do nothing
                        }
                    }
                }
        }
    }

    fun getNewFeeds(isFirstLaunch: Boolean = false) {
        lastUpdateIndex = 0
        scope.launch {
            feedRetrieverRepository.fetchFeeds(isFirstLaunch = isFirstLaunch)
        }
    }

    fun markAsReadOnScroll(lastVisibleIndex: Int) {
        if (settingsRepository.isMarkFeedAsReadWhenScrollingEnabled()) {
            // To avoid issues
            if (loadingState.value.isLoading()) {
                return
            }

            scope.launch {
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
                feedRetrieverRepository.markAsRead(urlToUpdates)
                lastUpdateIndex = lastVisibleIndex
            }
        }
    }

    fun requestNewFeedsPage() {
        scope.launch {
            feedRetrieverRepository.loadMoreFeeds()
        }
    }

    fun markAllRead() {
        scope.launch {
            feedRetrieverRepository.markAllFeedAsRead()
            feedRetrieverRepository.fetchFeeds()
        }
    }

    fun markAsRead(feedItemId: String) {
        scope.launch {
            feedRetrieverRepository.markAsRead(
                hashSetOf(
                    FeedItemId(feedItemId),
                ),
            )
        }
    }

    fun deleteOldFeedItems() {
        scope.launch {
            feedRetrieverRepository.deleteOldFeeds()
        }
    }

    fun forceFeedRefresh() {
        lastUpdateIndex = 0
        scope.launch {
            feedRetrieverRepository.fetchFeeds(forceRefresh = true)
        }
    }

    fun deleteAllFeeds() {
        feedManagerRepository.deleteAllFeeds()
    }

    fun onFeedFilterSelected(selectedFeedFilter: FeedFilter) {
        scope.launch {
            feedRetrieverRepository.clearReadFeeds()
            feedRetrieverRepository.updateFeedFilter(selectedFeedFilter)
            lastUpdateIndex = 0
        }
    }

    fun updateReadStatus(feedItemId: FeedItemId, read: Boolean) {
        scope.launch {
            feedRetrieverRepository.updateReadStatus(feedItemId, read)
        }
    }

    fun updateBookmarkStatus(feedItemId: FeedItemId, bookmarked: Boolean) {
        scope.launch {
            feedRetrieverRepository.updateBookmarkStatus(feedItemId, bookmarked)
        }
    }

    // Used on iOS
    fun enqueueBackup() {
        scope.launch {
            feedSyncRepository.enqueueBackup()
        }
    }
}
