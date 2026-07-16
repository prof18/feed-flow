package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.CategoryWithUnreadCount
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.DrawerItem.DrawerCategory
import com.prof18.feedflow.core.model.DrawerItem.DrawerFeedSource
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedOperation
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedSourceWithUnreadCount
import com.prof18.feedflow.core.model.FeedUpdateStatus
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.core.model.VisibleFeedItem
import com.prof18.feedflow.core.model.canonical
import com.prof18.feedflow.core.model.canonicalCategoryName
import com.prof18.feedflow.core.model.trimmed
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feed.FeedFontSizeRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import com.prof18.feedflow.shared.presentation.model.DeleteFeedSourceError
import com.prof18.feedflow.shared.presentation.model.HomeViewMenuState
import com.prof18.feedflow.shared.presentation.model.NextFeedPreviewState
import com.prof18.feedflow.shared.presentation.model.SyncError
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class HomeViewModel internal constructor(
    private val feedActionsRepository: FeedActionsRepository,
    private val feedSourcesRepository: FeedSourcesRepository,
    private val settingsRepository: SettingsRepository,
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository,
    private val feedSyncRepository: FeedSyncRepository,
    private val feedFontSizeRepository: FeedFontSizeRepository,
    private val feedCategoryRepository: FeedCategoryRepository,
    private val feedStateRepository: FeedStateRepository,
    private val feedFetcherRepository: FeedFetcherRepository,
    private val getNextFeedFilterOrNullUseCase: GetNextFeedFilterOrNullUseCase,
) : ViewModel() {

    // Loading
    val loadingState: StateFlow<FeedUpdateStatus> = feedStateRepository.updateState

    // Feeds
    val feedState: StateFlow<ImmutableList<FeedItem>> = feedStateRepository.feedState

    @OptIn(ExperimentalCoroutinesApi::class)
    val unreadCountFlow: Flow<Long> = feedAppearanceSettingsRepository.hideUnreadCount.flatMapLatest { hide ->
        if (hide) flowOf(0L) else feedStateRepository.getUnreadFeedCountFlow()
    }

    val isUnreadCountHidden: StateFlow<Boolean> = feedAppearanceSettingsRepository.hideUnreadCount

    // Error
    private val mutableUIErrorState: MutableSharedFlow<UIErrorState> = MutableSharedFlow()
    val errorState: SharedFlow<UIErrorState> = mutableUIErrorState.asSharedFlow()

    // Drawer State
    private val drawerMutableState = MutableStateFlow(NavDrawerState())
    val navDrawerState = drawerMutableState.asStateFlow()

    private val feedOperationMutableState = MutableStateFlow<FeedOperation>(FeedOperation.None)
    val feedOperationState: StateFlow<FeedOperation> = feedOperationMutableState.asStateFlow()
    private val refreshTriggerMutableState = MutableStateFlow(0)
    val refreshTriggerState: StateFlow<Int> = refreshTriggerMutableState.asStateFlow()

    private val nextFeedPreviewMutableState: MutableStateFlow<NextFeedPreviewState> = MutableStateFlow(
        NextFeedPreviewState.NextFeedPreviewDisabledState,
    )
    val nextFeedPreviewState: StateFlow<NextFeedPreviewState> = nextFeedPreviewMutableState.asStateFlow()

    private val scrollReadTracker = ScrollReadTracker()
    private val pendingScrollReadIds = mutableSetOf<FeedItemId>()
    private var scrollReadDebounceJob: Job? = null
    private var hasTriggeredAppLaunch = false

    val currentFeedFilter = feedStateRepository.currentFeedFilter
    val isSyncUploadRequired: StateFlow<Boolean> = settingsRepository.isSyncUploadRequired
    val swipeActions: StateFlow<SwipeActions> = feedAppearanceSettingsRepository.swipeActions
    val feedLayout: StateFlow<FeedLayout> = feedAppearanceSettingsRepository.feedLayout
    val isGridLayoutEnabled: StateFlow<Boolean> = feedAppearanceSettingsRepository.gridLayoutEnabled
    val feedItemDisplaySettings: StateFlow<FeedItemDisplaySettings> = combine(
        feedAppearanceSettingsRepository.hideUnreadDot,
        feedAppearanceSettingsRepository.hideFeedSource,
        feedAppearanceSettingsRepository.descriptionLineLimit,
    ) { hideUnreadDot, hideFeedSource, descriptionLineLimit ->
        FeedItemDisplaySettings(
            isHideUnreadDotEnabled = hideUnreadDot,
            isHideFeedSourceEnabled = hideFeedSource,
            descriptionLineLimit = descriptionLineLimit,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FeedItemDisplaySettings())

    val feedFontSizeState: StateFlow<FeedFontSizes> = feedFontSizeRepository.feedFontSizeState

    val viewMenuState: StateFlow<HomeViewMenuState> = combine(
        feedAppearanceSettingsRepository.feedOrder,
        settingsRepository.showReadArticlesTimelineFlow,
    ) { order, showRead -> HomeViewMenuState(order, showRead) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            HomeViewMenuState(
                feedOrder = feedAppearanceSettingsRepository.getFeedOrder(),
                showReadArticlesTimeline = settingsRepository.getShowReadArticlesTimeline(),
            ),
        )

    init {
        observeErrorState()
        viewModelScope.launch {
            feedStateRepository.updateFeedFilter(FeedFilter.Timeline)
            initDrawerData()
        }
    }

    fun onAppLaunch() {
        if (hasTriggeredAppLaunch) {
            return
        }
        hasTriggeredAppLaunch = true
        viewModelScope.launch {
            feedStateRepository.getFeeds()
        }
        getNewFeeds(isFirstLaunch = true)
    }

    private suspend fun initDrawerData() {
        combine(
            combine(
                feedSourcesRepository.observeFeedSourcesByCategoryWithUnreadCount(),
                feedCategoryRepository.observeCategoriesWithUnreadCount(),
                feedStateRepository.getUnreadTimelineCountFlow(),
                feedStateRepository.getUnreadBookmarksCountFlow(),
                feedAppearanceSettingsRepository.hideUnreadCount,
            ) { feedSourceByCategoryWithCount, categoriesWithCount, timelineCount, bookmarksCount, hideUnreadCount ->
                DrawerDataSnapshot(
                    feedSourceByCategoryWithCount = feedSourceByCategoryWithCount,
                    categoriesWithCount = categoriesWithCount,
                    timelineCount = timelineCount,
                    bookmarksCount = bookmarksCount,
                    hideUnreadCount = hideUnreadCount,
                )
            },
            settingsRepository.uncategorizedPositionFlow,
        ) { snapshot, uncategorizedPosition ->
            val feedSourceByCategoryWithCount = snapshot.feedSourceByCategoryWithCount
            val categoriesWithCount = snapshot.categoriesWithCount
            val timelineCount = snapshot.timelineCount
            val bookmarksCount = snapshot.bookmarksCount
            val hideUnreadCount = snapshot.hideUnreadCount
            val containsOnlyNullKey = feedSourceByCategoryWithCount.keys.all { it == null }

            val pinnedFeedSources = feedSourceByCategoryWithCount.values
                .flatten()
                .filter { it.feedSource.isPinned }
                .sortedWith(
                    compareBy(
                        { it.feedSource.pinnedPosition },
                        { it.feedSource.title },
                    ),
                )

            fun displayCount(actual: Long): Long = if (hideUnreadCount) 0L else actual

            val feedSourcesWithoutCategory = feedSourceByCategoryWithCount[null]
                ?.map { feedSourceWithCount ->
                    DrawerFeedSource(
                        feedSource = feedSourceWithCount.feedSource,
                        unreadCount = displayCount(feedSourceWithCount.unreadCount),
                    )
                }?.toImmutableList() ?: persistentListOf()

            NavDrawerState(
                timeline = persistentListOf(DrawerItem.Timeline(unreadCount = displayCount(timelineCount))),
                read = persistentListOf(DrawerItem.Read),
                bookmarks = persistentListOf(DrawerItem.Bookmarks(unreadCount = displayCount(bookmarksCount))),
                categories = categoriesWithCount.map { categoryWithCount ->
                    DrawerCategory(
                        category = categoryWithCount.category,
                        unreadCount = displayCount(categoryWithCount.unreadCount),
                    )
                }.toImmutableList(),
                pinnedFeedSources = pinnedFeedSources.map { feedSourceWithCount ->
                    DrawerFeedSource(
                        feedSource = feedSourceWithCount.feedSource,
                        unreadCount = displayCount(feedSourceWithCount.unreadCount),
                    )
                }.toImmutableList(),
                feedSourcesWithoutCategory = if (containsOnlyNullKey) {
                    feedSourcesWithoutCategory
                } else {
                    persistentListOf()
                },
                feedSourcesByCategory = if (containsOnlyNullKey) {
                    persistentMapOf()
                } else {
                    feedSourceByCategoryWithCount.map { (category, feedSources) ->
                        val categoryWrapper = DrawerFeedSource.FeedSourceCategoryWrapper(
                            feedSourceCategory = category,
                        )
                        categoryWrapper to feedSources.map { feedSourceWithCount ->
                            DrawerFeedSource(
                                feedSource = feedSourceWithCount.feedSource,
                                unreadCount = displayCount(feedSourceWithCount.unreadCount),
                            )
                        }
                    }.toMap().toPersistentMap()
                },
                uncategorizedPosition = uncategorizedPosition,
            )
        }.collect { navDrawerState ->
            drawerMutableState.update { navDrawerState }
        }
    }

    private data class DrawerDataSnapshot(
        val feedSourceByCategoryWithCount: Map<FeedSourceCategory?, List<FeedSourceWithUnreadCount>>,
        val categoriesWithCount: List<CategoryWithUnreadCount>,
        val timelineCount: Long,
        val bookmarksCount: Long,
        val hideUnreadCount: Boolean,
    )

    private fun observeErrorState() {
        viewModelScope.launch {
            feedStateRepository.errorState
                .collect { error ->
                    when (error) {
                        is DatabaseError -> {
                            mutableUIErrorState.emit(
                                UIErrorState.DatabaseError(
                                    errorCode = error.errorCode,
                                ),
                            )
                        }

                        is SyncError -> {
                            mutableUIErrorState.emit(
                                UIErrorState.SyncError(
                                    errorCode = error.errorCode,
                                ),
                            )
                        }

                        is DeleteFeedSourceError -> {
                            mutableUIErrorState.emit(UIErrorState.DeleteFeedSourceError)
                        }
                    }
                }
        }
    }

    fun getNewFeeds(isFirstLaunch: Boolean = false, forceRefresh: Boolean = false) {
        if (isFirstLaunch && !settingsRepository.getRefreshFeedsOnLaunch()) {
            return
        }
        launchAfterFlushingScrollReadState {
            retryPendingReadStatusActionsNow()
            feedFetcherRepository.fetchFeeds(isFirstLaunch = isFirstLaunch, forceRefresh = forceRefresh)
        }
    }

    fun onVisibleFeedItemsChanged(visibleItems: List<VisibleFeedItem>) {
        if (!settingsRepository.markFeedAsReadWhenScrollingFlow.value) {
            flushScrollReadStateInBackground()
            return
        }

        val currentFeedListVersion = feedStateRepository.feedListVersion.value
        val idsToMark = scrollReadTracker.onVisibleItemsChanged(
            visibleItems = visibleItems,
            feedItems = feedState.value,
            feedListVersion = currentFeedListVersion,
            listShapeKey = ScrollReadListShapeKey(
                showReadArticlesTimeline = settingsRepository.showReadArticlesTimelineFlow.value,
                hideReadItems = settingsRepository.hideReadItemsFlow.value,
                feedLayout = feedAppearanceSettingsRepository.feedLayout.value,
                isGridLayoutEnabled = feedAppearanceSettingsRepository.gridLayoutEnabled.value,
            ),
        )
        if (idsToMark.isEmpty()) {
            return
        }

        enqueueScrollReadIds(idsToMark)
    }

    fun requestNewFeedsPage() {
        viewModelScope.launch {
            feedStateRepository.loadMoreFeeds()
        }
    }

    fun markAllRead() {
        launchAfterFlushingScrollReadState {
            feedOperationMutableState.update { FeedOperation.MarkingAllRead }
            feedActionsRepository.markAllCurrentFeedAsRead()
            feedOperationMutableState.update { FeedOperation.None }
            feedStateRepository.getFeeds()
        }
    }

    fun markAllReadForFeedSource(feedSource: FeedSource) {
        launchAfterFlushingScrollReadState {
            feedOperationMutableState.update { FeedOperation.MarkingAllRead }
            feedActionsRepository.markAllFeedAsRead(FeedFilter.Source(feedSource))
            feedOperationMutableState.update { FeedOperation.None }
            feedStateRepository.getFeeds()
        }
    }

    fun markAllReadForCategory(category: FeedSourceCategory) {
        launchAfterFlushingScrollReadState {
            feedOperationMutableState.update { FeedOperation.MarkingAllRead }
            feedActionsRepository.markAllFeedAsRead(FeedFilter.Category(category))
            feedOperationMutableState.update { FeedOperation.None }
            feedStateRepository.getFeeds()
        }
    }

    fun markAsRead(feedItemId: String) {
        val idsToMark = takePendingScrollReadIds()
        idsToMark.add(FeedItemId(feedItemId))
        scrollReadTracker.reset()
        viewModelScope.launch {
            feedActionsRepository.markAsRead(idsToMark)
        }
    }

    fun markAllAboveAsRead(feedItemId: String) {
        launchAfterFlushingScrollReadState {
            feedActionsRepository.markAllAboveAsRead(feedItemId)
        }
    }

    fun markAllBelowAsRead(feedItemId: String) {
        launchAfterFlushingScrollReadState {
            feedActionsRepository.markAllBelowAsRead(feedItemId)
        }
    }

    fun deleteOldFeedItems() {
        launchAfterFlushingScrollReadState {
            feedOperationMutableState.update { FeedOperation.Deleting }
            feedActionsRepository.deleteOldFeeds()
            feedOperationMutableState.update { FeedOperation.None }
        }
    }

    fun refreshFeeds() {
        refreshTriggerMutableState.update { it + 1 }
        getNewFeeds(forceRefresh = true)
    }

    fun deleteAllFeeds() {
        launchAfterFlushingScrollReadState {
            feedSourcesRepository.deleteAllFeeds()
        }
    }

    fun updateFeedSourceFilter(feedSourceId: String) {
        launchAfterFlushingScrollReadState {
            feedStateRepository.updateFeedSourceFilter(feedSourceId)
        }
    }

    fun updateCategoryFilter(categoryId: String) {
        launchAfterFlushingScrollReadState {
            feedStateRepository.updateCategoryFilter(categoryId)
        }
    }

    fun onFeedFilterSelected(selectedFeedFilter: FeedFilter) {
        launchAfterFlushingScrollReadState {
            feedStateRepository.updateFeedFilter(selectedFeedFilter)
        }

        updateNextFeedPreview(selectedFeedFilter)
    }

    fun updateNextFeedPreview(currentFeedFilter: FeedFilter) {
        viewModelScope.launch {
            nextFeedPreviewMutableState.update {
                val nextFeed = getNextFeedFilterOrNullUseCase(currentFeedFilter)

                when (nextFeed) {
                    is FeedFilter.Category -> NextFeedPreviewState.NextFeedPreviewEnabledState(
                        feedFilter = nextFeed,
                        title = nextFeed.feedCategory.title,
                    )
                    is FeedFilter.Source -> NextFeedPreviewState.NextFeedPreviewEnabledState(
                        feedFilter = nextFeed,
                        title = nextFeed.feedSource.title,
                    )
                    else -> NextFeedPreviewState.NextFeedPreviewDisabledState
                }
            }
        }
    }

    fun updateReadStatus(feedItemId: FeedItemId, read: Boolean) {
        if (read) {
            val idsToMark = takePendingScrollReadIds().apply {
                add(feedItemId)
            }
            scrollReadTracker.reset()
            viewModelScope.launch {
                feedActionsRepository.markAsRead(idsToMark)
            }
        } else {
            launchAfterFlushingScrollReadState {
                feedActionsRepository.updateReadStatus(feedItemId, read)
            }
        }
    }

    fun updateBookmarkStatus(feedItemId: FeedItemId, bookmarked: Boolean) {
        launchAfterFlushingScrollReadState {
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
                isNotificationEnabled = feedSource.isNotificationEnabled,
            )
        }
    }

    fun reorderPinnedFeedSources(feedSources: List<FeedSource>) {
        drawerMutableState.update { oldState ->
            oldState.withReorderedPinnedFeedSources(feedSources)
        }
        viewModelScope.launch {
            feedSourcesRepository.reorderPinnedFeedSources(feedSources.map { it.id })
        }
    }

    fun reorderCategories(categoryWrappers: List<DrawerFeedSource.FeedSourceCategoryWrapper>) {
        drawerMutableState.update { oldState ->
            oldState.withReorderedCategoryWrappers(categoryWrappers)
        }
        viewModelScope.launch {
            feedCategoryRepository.reorderCategories(categoryWrappers.map { it.feedSourceCategory?.id })
        }
    }

    fun reorderFeedSources(feedSources: List<FeedSource>) {
        drawerMutableState.update { oldState ->
            oldState.withReorderedFeedSources(feedSources)
        }
        viewModelScope.launch {
            feedSourcesRepository.reorderFeedSources(feedSources.map { it.id })
        }
    }

    private fun NavDrawerState.withReorderedPinnedFeedSources(feedSources: List<FeedSource>): NavDrawerState =
        copy(
            pinnedFeedSources = pinnedFeedSources.reorderedFeedSources(feedSources)?.toImmutableList()
                ?: pinnedFeedSources,
        )

    private fun NavDrawerState.withReorderedCategoryWrappers(
        categoryWrappers: List<DrawerFeedSource.FeedSourceCategoryWrapper>,
    ): NavDrawerState {
        val reorderedKeys = categoryWrappers.toSet()
        if (feedSourcesByCategory.keys.toSet() != reorderedKeys) {
            return this
        }

        return copy(
            feedSourcesByCategory = categoryWrappers
                .associateWith { categoryWrapper -> feedSourcesByCategory.getValue(categoryWrapper) }
                .toPersistentMap(),
        )
    }

    private fun NavDrawerState.withReorderedFeedSources(feedSources: List<FeedSource>): NavDrawerState {
        val updatedFeedSourcesWithoutCategory = feedSourcesWithoutCategory.reorderedFeedSources(feedSources)
        if (updatedFeedSourcesWithoutCategory != null) {
            return copy(feedSourcesWithoutCategory = updatedFeedSourcesWithoutCategory.toImmutableList())
        }

        return copy(
            feedSourcesByCategory = feedSourcesByCategory.mapValues { (_, drawerItems) ->
                drawerItems.reorderedFeedSources(feedSources) ?: drawerItems
            }.toPersistentMap(),
        )
    }

    private fun List<DrawerItem>.reorderedFeedSources(feedSources: List<FeedSource>): List<DrawerFeedSource>? {
        val feedSourceIds = feedSources.map { it.id }.toSet()
        val drawerFeedSources = filterIsInstance<DrawerFeedSource>()
        if (drawerFeedSources.map { it.feedSource.id }.toSet() != feedSourceIds) {
            return null
        }

        val drawerFeedSourceById = drawerFeedSources.associateBy { it.feedSource.id }
        return feedSources.mapNotNull { feedSource -> drawerFeedSourceById[feedSource.id] }
    }

    // Used on iOS
    fun enqueueBackup() {
        launchAfterFlushingScrollReadState {
            retryPendingReadStatusActionsNow()
            feedSyncRepository.enqueueBackup()
        }
    }

    fun deleteFeedSource(feedSource: FeedSource) {
        launchAfterFlushingScrollReadState {
            feedOperationMutableState.update { FeedOperation.Deleting }
            feedSourcesRepository.deleteFeed(feedSource)
            feedStateRepository.getFeeds()
            feedOperationMutableState.update { FeedOperation.None }
        }
    }

    fun deleteAllFeedsInCategory(feedSources: List<FeedSource>) {
        launchAfterFlushingScrollReadState {
            feedOperationMutableState.update { FeedOperation.Deleting }
            for (feedSource in feedSources) {
                feedSourcesRepository.deleteFeed(feedSource)
            }
            feedStateRepository.getFeeds()
            feedOperationMutableState.update { FeedOperation.None }
        }
    }

    fun deleteAllFeedsInCategory(categoryId: CategoryId) {
        launchAfterFlushingScrollReadState {
            feedOperationMutableState.update { FeedOperation.Deleting }
            val feedSources = feedSourcesRepository.getFeedSources()
                .first()
                .filter { it.category?.id == categoryId.value }
            for (feedSource in feedSources) {
                feedSourcesRepository.deleteFeed(feedSource)
            }
            feedStateRepository.getFeeds()
            feedOperationMutableState.update { FeedOperation.None }
        }
    }

    fun updateCategoryName(categoryId: CategoryId, newName: CategoryName) {
        viewModelScope.launch {
            feedCategoryRepository.updateCategoryName(categoryId, newName)
        }
    }

    fun validateCategoryName(categoryId: CategoryId?, newName: CategoryName): CategoryNameValidationResult {
        if (newName.trimmed().name.isBlank()) {
            return CategoryNameValidationResult.BLANK
        }

        // The drawer can be used without initializing FeedCategoryRepository.categoriesState,
        // so validation here uses the already loaded drawer state.
        val isDuplicate = navDrawerState.value.categories
            .filterIsInstance<DrawerCategory>()
            .any { category ->
                category.category.id != categoryId?.value &&
                    category.category.title.canonicalCategoryName() == newName.canonical()
            }

        return if (isDuplicate) {
            CategoryNameValidationResult.DUPLICATE
        } else {
            CategoryNameValidationResult.VALID
        }
    }

    fun deleteCategory(categoryId: CategoryId) {
        launchAfterFlushingScrollReadState {
            feedOperationMutableState.update { FeedOperation.Deleting }
            feedCategoryRepository.deleteCategory(categoryId.value)
            feedStateRepository.getFeeds()
            feedOperationMutableState.update { FeedOperation.None }
        }
    }

    fun onNavigateToNextFeed() {
        when (val nextFeed = nextFeedPreviewState.value) {
            is NextFeedPreviewState.NextFeedPreviewEnabledState -> {
                onFeedFilterSelected(selectedFeedFilter = nextFeed.feedFilter)
            }
            is NextFeedPreviewState.NextFeedPreviewDisabledState -> {}
        }
    }

    fun getCurrentThemeMode() = settingsRepository.getThemeMode()

    fun updateFeedOrder(order: com.prof18.feedflow.core.model.FeedOrder) {
        launchAfterFlushingScrollReadState {
            feedAppearanceSettingsRepository.setFeedOrder(order)
            feedStateRepository.getFeeds()
        }
    }

    fun updateShowReadArticlesTimeline(value: Boolean) {
        launchAfterFlushingScrollReadState {
            settingsRepository.setShowReadArticlesTimeline(value)
            feedStateRepository.getFeeds()
        }
    }

    private fun enqueueScrollReadIds(idsToMark: Set<FeedItemId>) {
        if (!settingsRepository.hideReadItemsFlow.value) {
            feedStateRepository.markAsRead(idsToMark.toHashSet())
        }
        pendingScrollReadIds.addAll(idsToMark)
        scrollReadDebounceJob?.cancel()
        scrollReadDebounceJob = viewModelScope.launch {
            delay(SCROLL_READ_DEBOUNCE)
            val ids = takePendingScrollReadIds(cancelDebounceJob = false)
            if (ids.isNotEmpty()) {
                feedActionsRepository.markAsRead(ids)
            }
        }
    }

    private fun launchAfterFlushingScrollReadState(block: suspend () -> Unit) {
        val idsToFlush = takePendingScrollReadIds()
        scrollReadTracker.reset()
        viewModelScope.launch {
            persistScrollReadIds(idsToFlush)
            block()
        }
    }

    private fun flushScrollReadStateInBackground() {
        val idsToFlush = takePendingScrollReadIds()
        scrollReadTracker.reset()
        if (idsToFlush.isNotEmpty()) {
            viewModelScope.launch {
                persistScrollReadIds(idsToFlush)
            }
        }
    }

    private suspend fun persistScrollReadIds(idsToFlush: HashSet<FeedItemId>) {
        if (idsToFlush.isNotEmpty()) {
            feedActionsRepository.markAsRead(idsToFlush)
        }
    }

    private fun takePendingScrollReadIds(cancelDebounceJob: Boolean = true): HashSet<FeedItemId> {
        val ids = pendingScrollReadIds.toHashSet()
        clearPendingScrollReadIds(cancelDebounceJob)
        return ids
    }

    private fun clearPendingScrollReadIds(cancelDebounceJob: Boolean = true) {
        pendingScrollReadIds.clear()
        if (cancelDebounceJob) {
            scrollReadDebounceJob?.cancel()
        }
        scrollReadDebounceJob = null
    }

    private suspend fun retryPendingReadStatusActionsNow() {
        feedActionsRepository.retryPendingReadStatusActions()
    }

    private companion object {
        val SCROLL_READ_DEBOUNCE = 2.seconds
    }
}
