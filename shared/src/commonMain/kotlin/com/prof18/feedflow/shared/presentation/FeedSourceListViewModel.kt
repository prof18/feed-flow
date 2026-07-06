package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.FeedOperation
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedSourceListState
import com.prof18.feedflow.core.model.FeedSourceState
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryRepository
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import com.prof18.feedflow.shared.presentation.model.DeleteFeedSourceError
import com.prof18.feedflow.shared.presentation.model.SyncError
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedSourceListViewModel internal constructor(
    private val feedSourcesRepository: FeedSourcesRepository,
    private val feedCategoryRepository: FeedCategoryRepository,
    private val feedStateRepository: FeedStateRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val feedsMutableState: MutableStateFlow<FeedSourceListState> = MutableStateFlow(FeedSourceListState())
    val feedSourcesState: StateFlow<FeedSourceListState> = feedsMutableState.asStateFlow()

    private val mutableUIErrorState: MutableSharedFlow<UIErrorState> = MutableSharedFlow()
    val errorState: SharedFlow<UIErrorState> = mutableUIErrorState.asSharedFlow()

    private val feedOperationMutableState = MutableStateFlow<FeedOperation>(FeedOperation.None)
    val feedOperationState: StateFlow<FeedOperation> = feedOperationMutableState.asStateFlow()

    private val expandedCategories = mutableSetOf<CategoryId?>()

    init {
        observeErrorState()
        viewModelScope.launch {
            combine(
                feedSourcesRepository.getFeedSources(),
                settingsRepository.uncategorizedPositionFlow,
            ) { feeds, uncategorizedPosition ->
                feeds to uncategorizedPosition
            }.collect { (feeds, uncategorizedPosition) ->
                val groupedSources = feeds.groupBy { it.category }

                val containsOnlyNullKey = groupedSources.keys.all { it == null }

                val feedSourceStates = mutableListOf<FeedSourceState>()

                if (containsOnlyNullKey) {
                    feedsMutableState.update {
                        FeedSourceListState(
                            feedSourcesWithoutCategory = feeds
                                .sortedWith(
                                    compareBy(
                                        { it.position },
                                        { it.title },
                                    ),
                                )
                                .toImmutableList(),
                            feedSourcesWithCategory = persistentListOf(),
                        )
                    }
                } else {
                    groupedSources.forEach { (category: FeedSourceCategory?, feedSources: List<FeedSource>) ->
                        feedSourceStates.add(
                            FeedSourceState(
                                categoryId = category?.id?.let { CategoryId(it) },
                                categoryName = category?.title,
                                categoryPosition = category?.position ?: uncategorizedPosition,
                                isExpanded = category?.id?.let { CategoryId(it) } in expandedCategories,
                                feedSources = feedSources
                                    .sortedWith(
                                        compareBy(
                                            { it.position },
                                            { it.title },
                                        ),
                                    )
                                    .toImmutableList(),
                            ),
                        )
                    }

                    feedsMutableState.update {
                        FeedSourceListState(
                            feedSourcesWithoutCategory = persistentListOf(),
                            feedSourcesWithCategory = feedSourceStates
                                .sortedWith(
                                    compareBy(
                                        { it.categoryPosition },
                                        { it.categoryName },
                                    ),
                                )
                                .toImmutableList(),
                        )
                    }
                }
            }
        }
    }

    private fun observeErrorState() {
        viewModelScope.launch {
            feedStateRepository.errorState
                .collect { error ->
                    when (error) {
                        is DatabaseError -> {
                            mutableUIErrorState.emit(
                                UIErrorState.DatabaseError(errorCode = error.errorCode),

                            )
                        }

                        is SyncError -> {
                            mutableUIErrorState.emit(
                                UIErrorState.SyncError(errorCode = error.errorCode),
                            )
                        }

                        is DeleteFeedSourceError -> {
                            mutableUIErrorState.emit(UIErrorState.DeleteFeedSourceError)
                        }
                    }
                }
        }
    }

    fun deleteFeedSource(feedSource: FeedSource) {
        viewModelScope.launch {
            feedOperationMutableState.update { FeedOperation.Deleting }
            feedSourcesRepository.deleteFeed(feedSource)
            feedStateRepository.getFeeds()
            feedOperationMutableState.update { FeedOperation.None }
        }
    }

    fun deleteAllFeedsInCategory(feedSources: List<FeedSource>) {
        viewModelScope.launch {
            feedOperationMutableState.update { FeedOperation.Deleting }
            for (feedSource in feedSources) {
                feedSourcesRepository.deleteFeed(feedSource)
            }
            feedStateRepository.getFeeds()
            feedOperationMutableState.update { FeedOperation.None }
        }
    }

    fun expandCategory(categoryId: CategoryId?) {
        if (!expandedCategories.add(categoryId)) {
            expandedCategories.remove(categoryId)
        }

        feedsMutableState.update { oldState ->
            val newFeedSourceStates = oldState.feedSourcesWithCategory.map { feedSourceState ->
                if (categoryId == feedSourceState.categoryId) {
                    feedSourceState.copy(isExpanded = feedSourceState.isExpanded.not())
                } else {
                    feedSourceState
                }
            }
            FeedSourceListState(
                feedSourcesWithoutCategory = oldState.feedSourcesWithoutCategory,
                feedSourcesWithCategory = newFeedSourceStates.toImmutableList(),
            )
        }
    }

    fun updateFeedName(feedSource: FeedSource, newName: String) {
        viewModelScope.launch {
            feedSourcesRepository.updateFeedSourceName(feedSource.id, newName)
            feedStateRepository.getFeeds()
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

    fun reorderCategories(categories: List<FeedSourceState>) {
        feedsMutableState.update { oldState ->
            oldState.copy(feedSourcesWithCategory = categories.toImmutableList())
        }
        viewModelScope.launch {
            feedCategoryRepository.reorderCategories(categories.map { it.categoryId?.value })
        }
    }

    fun reorderFeedSources(feedSources: List<FeedSource>) {
        feedsMutableState.update { oldState ->
            oldState.withReorderedFeedSources(feedSources)
        }
        viewModelScope.launch {
            feedSourcesRepository.reorderFeedSources(feedSources.map { it.id })
        }
    }

    private fun FeedSourceListState.withReorderedFeedSources(feedSources: List<FeedSource>): FeedSourceListState {
        val reorderedIds = feedSources.map { it.id }.toSet()

        if (feedSourcesWithoutCategory.map { it.id }.toSet() == reorderedIds) {
            return copy(feedSourcesWithoutCategory = feedSources.toImmutableList())
        }

        return copy(
            feedSourcesWithCategory = feedSourcesWithCategory.map { feedSourceState ->
                if (feedSourceState.feedSources.map { it.id }.toSet() == reorderedIds) {
                    feedSourceState.copy(feedSources = feedSources.toImmutableList())
                } else {
                    feedSourceState
                }
            }.toImmutableList(),
        )
    }
}
