package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedSourceListState
import com.prof18.feedflow.core.model.FeedSourceState
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import com.prof18.feedflow.shared.presentation.model.DeleteFeedSourceError
import com.prof18.feedflow.shared.presentation.model.FeedErrorState
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedSourceListViewModel internal constructor(
    private val feedSourcesRepository: FeedSourcesRepository,
    private val feedStateRepository: FeedStateRepository,
    private val feedFetcherRepository: FeedFetcherRepository,
) : ViewModel() {

    private val feedsMutableState: MutableStateFlow<FeedSourceListState> = MutableStateFlow(FeedSourceListState())
    val feedSourcesState: StateFlow<FeedSourceListState> = feedsMutableState.asStateFlow()

    private val mutableUIErrorState: MutableSharedFlow<UIErrorState> = MutableSharedFlow()
    val errorState: SharedFlow<UIErrorState> = mutableUIErrorState.asSharedFlow()

    private var expandedCategories: MutableList<CategoryId> = mutableListOf()

    init {
        observeErrorState()
        viewModelScope.launch {
            feedSourcesRepository.getFeedSources().collect { feeds ->
                val groupedSources = feeds.groupBy {
                    val id = it.category?.id
                    val name = it.category?.title

                    if (id != null && name != null) {
                        FeedSourceCategory(id, name)
                    } else {
                        null
                    }
                }

                val containsOnlyNullKey = groupedSources.keys.all { it == null }

                val feedSourceStates = mutableListOf<FeedSourceState>()

                if (containsOnlyNullKey) {
                    feedsMutableState.update {
                        FeedSourceListState(
                            feedSourcesWithoutCategory = feeds.sortedBy { it.title }.toImmutableList(),
                            feedSourcesWithCategory = persistentListOf(),
                        )
                    }
                } else {
                    groupedSources.forEach { (category: FeedSourceCategory?, feedSources: List<FeedSource>) ->
                        feedSourceStates.add(
                            FeedSourceState(
                                categoryId = category?.id?.let { CategoryId(it) },
                                categoryName = category?.title,
                                isExpanded = category?.id?.let { CategoryId(it) } in expandedCategories,
                                feedSources = feedSources.toImmutableList(),
                            ),
                        )
                    }

                    feedSourceStates.sortedBy { it.categoryName }

                    feedsMutableState.update {
                        FeedSourceListState(
                            feedSourcesWithoutCategory = persistentListOf(),
                            feedSourcesWithCategory = feedSourceStates.toImmutableList(),
                        )
                    }
                    expandedCategories.clear()
                }
            }
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
            setExpandedCategory()
            feedSourcesRepository.deleteFeed(feedSource)
            feedFetcherRepository.fetchFeeds()
        }
    }

    fun deleteAllFeedsInCategory(feedSources: List<FeedSource>) {
        viewModelScope.launch {
            setExpandedCategory()
            for (feedSource in feedSources) {
                feedSourcesRepository.deleteFeed(feedSource)
            }
            feedFetcherRepository.fetchFeeds()
        }
    }

    fun expandCategory(categoryId: CategoryId?) {
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
            setExpandedCategory()
            feedSourcesRepository.updateFeedSourceName(feedSource.id, newName)
            feedStateRepository.getFeeds()
        }
    }

    private fun setExpandedCategory() {
        expandedCategories = feedSourcesState.value.feedSourcesWithCategory.filter {
            it.isExpanded
        }.mapNotNull {
            it.categoryId
        }.toMutableList()
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
}
