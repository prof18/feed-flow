package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedSourceListState
import com.prof18.feedflow.core.model.FeedSourceState
import com.prof18.feedflow.shared.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.shared.domain.feed.retriever.FeedRetrieverRepository
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedSourceListViewModel internal constructor(
    private val feedManagerRepository: FeedManagerRepository,
    private val feedRetrieverRepository: FeedRetrieverRepository,
) : BaseViewModel() {

    private val feedsMutableState: MutableStateFlow<FeedSourceListState> = MutableStateFlow(FeedSourceListState())

    @NativeCoroutinesState
    val feedSourcesState: StateFlow<FeedSourceListState> = feedsMutableState.asStateFlow()

    private var expandedCategories: MutableList<CategoryId> = mutableListOf()

    init {
        scope.launch {
            feedManagerRepository.getFeedSources().collect { feeds ->
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

    fun deleteFeedSource(feedSource: FeedSource) {
        scope.launch {
            setExpandedCategory()
            feedManagerRepository.deleteFeed(feedSource)
            feedRetrieverRepository.fetchFeeds()
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
        scope.launch {
            setExpandedCategory()
            feedManagerRepository.updateFeedSourceName(feedSource.id, newName)
            feedRetrieverRepository.fetchFeeds()
        }
    }

    private fun setExpandedCategory() {
        expandedCategories = feedSourcesState.value.feedSourcesWithCategory.filter {
            it.isExpanded
        }.mapNotNull {
            it.categoryId
        }.toMutableList()
    }
}
