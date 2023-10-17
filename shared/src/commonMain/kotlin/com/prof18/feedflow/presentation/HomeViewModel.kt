package com.prof18.feedflow.presentation

import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.domain.model.FeedItemId
import com.prof18.feedflow.domain.model.FeedUpdateStatus
import com.prof18.feedflow.presentation.model.DatabaseError
import com.prof18.feedflow.presentation.model.FeedErrorState
import com.prof18.feedflow.presentation.model.UIErrorState
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel internal constructor(
    private val feedRetrieverRepository: FeedRetrieverRepository,
    private val feedManagerRepository: FeedManagerRepository,
) : BaseViewModel() {

    // Loading
    @NativeCoroutinesState
    val loadingState: StateFlow<FeedUpdateStatus> = feedRetrieverRepository.updateState

    // Feeds
    private val mutableFeedState: MutableStateFlow<List<FeedItem>> = MutableStateFlow(emptyList())

    @NativeCoroutinesState
    val feedState = mutableFeedState.asStateFlow()

    @NativeCoroutines
    val countState = feedState.map { it.count { item -> !item.isRead } }

    // Error
    private val mutableUIErrorState: MutableSharedFlow<UIErrorState?> = MutableSharedFlow()

    @NativeCoroutines
    val errorState = mutableUIErrorState.asSharedFlow()

    // Drawer State
    private val drawerMutableState = MutableStateFlow<List<DrawerItem>>(
        listOf(DrawerItem.Timeline),
    )

    @NativeCoroutinesState
    val drawerItems = drawerMutableState.asStateFlow()

    private var lastUpdateIndex = 0

    private val currentFeedFilterMutableState: MutableStateFlow<FeedFilter> = MutableStateFlow(FeedFilter.Timeline)

    @NativeCoroutinesState
    val currentFeedFilter = currentFeedFilterMutableState.asStateFlow()

    init {
        currentFeedFilterMutableState.value = FeedFilter.Timeline
        initDrawerData()
        observeErrorState()
        observeFeed()
        getNewFeeds(isFirstLaunch = true)
    }

    private fun observeFeed() {
        scope.launch {
            feedRetrieverRepository.feedState
                .combine(currentFeedFilter) { items, filter ->
                    lastUpdateIndex = 0
                    when (filter) {
                        is FeedFilter.Category -> {
                            items.filter { feedItem ->
                                feedItem.feedSource.category?.id == filter.feedCategory.id
                            }
                        }

                        is FeedFilter.Source -> {
                            items.filter { feedItem ->
                                feedItem.feedSource.id == filter.feedSource.id
                            }
                        }

                        FeedFilter.Timeline -> {
                            items
                        }
                    }
                }.collect { feedItems ->
                    mutableFeedState.value = feedItems
                }
        }
    }

    private fun initDrawerData() {
        scope.launch {
            val feedSourceByCategory = feedManagerRepository.getFeedSourcesByCategory()
            val categories = feedManagerRepository.getCategories()

            val drawerItems = mutableListOf<DrawerItem>(
                DrawerItem.Timeline,
            )

            if (categories.isNotEmpty()) {
                drawerItems.add(DrawerItem.CategorySectionTitle)
            }

            drawerItems.addAll(
                categories.map { category ->
                    DrawerItem.DrawerCategory(
                        category = category,
                    )
                },
            )

            if (feedSourceByCategory.isNotEmpty()) {
                drawerItems.add(DrawerItem.CategorySourcesTitle)
            }

            drawerItems.addAll(
                feedSourceByCategory.map { (category, sources) ->
                    DrawerItem.DrawerCategoryWrapper(
                        category = category,
                        feedSources = sources.map { feedSource ->
                            DrawerItem.DrawerCategoryWrapper.FeedSourceWrapper(
                                feedSource = feedSource,
                            )
                        },
                        isExpanded = false,
                        onExpandClick = { drawerCategoryWrapper ->
                            drawerMutableState.update { oldDrawerItems ->
                                oldDrawerItems.map { drawerItem ->
                                    if (drawerItem is DrawerItem.DrawerCategoryWrapper &&
                                        drawerItem == drawerCategoryWrapper
                                    ) {
                                        drawerItem.copy(isExpanded = !drawerItem.isExpanded)
                                    } else {
                                        drawerItem
                                    }
                                }
                            }
                        },
                    )
                },
            )

            drawerMutableState.update { drawerItems }
        }
    }

    private fun observeErrorState() {
        feedRetrieverRepository.getFeeds()

        scope.launch {
            feedRetrieverRepository.errorState
                .collect { error ->
                    when (error) {
                        is FeedErrorState -> {
                            mutableUIErrorState.emit(
                                UIErrorState(
                                    message = StringDesc.ResourceFormatted(
                                        stringRes = MR.strings.feed_error_message,
                                        error.failingSourceName,
                                    ),
                                ),
                            )
                        }

                        is DatabaseError -> {
                            mutableUIErrorState.emit(
                                UIErrorState(
                                    message = StringDesc.Resource(
                                        stringRes = MR.strings.database_error,
                                    ),
                                ),
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

    fun updateReadStatus(lastVisibleIndex: Int) {
        // To avoid issues
        if (loadingState.value.isLoading()) {
            return
        }

        scope.launch {
            feedRetrieverRepository.updateReadStatus(
                lastUpdateIndex = lastUpdateIndex,
                lastVisibleIndex = lastVisibleIndex,
            )
            lastUpdateIndex = lastVisibleIndex
        }
    }

    fun markAllRead() {
        scope.launch {
            feedRetrieverRepository.markAllFeedAsRead()
            feedRetrieverRepository.fetchFeeds()
        }
    }

    fun markAsRead(feedItemId: Int) {
        scope.launch {
            feedRetrieverRepository.updateReadStatus(
                listOf(
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
        currentFeedFilterMutableState.update { selectedFeedFilter }
    }
}
