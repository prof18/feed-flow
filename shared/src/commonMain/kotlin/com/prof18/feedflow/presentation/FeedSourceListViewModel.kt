package com.prof18.feedflow.presentation

import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedSourceState
import com.prof18.feedflow.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.domain.feed.retriever.FeedRetrieverRepository
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedSourceListViewModel internal constructor(
    private val feedManagerRepository: FeedManagerRepository,
    private val feedRetrieverRepository: FeedRetrieverRepository,
) : BaseViewModel() {

    private val feedsMutableState: MutableStateFlow<List<FeedSourceState>> = MutableStateFlow(listOf())

    @NativeCoroutinesState
    val feedSourcesState: StateFlow<List<FeedSourceState>> = feedsMutableState.asStateFlow()

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
                val feedSourceStates = mutableListOf<FeedSourceState>()

                groupedSources.forEach { (category: FeedSourceCategory?, feedSources: List<FeedSource>) ->
                    feedSourceStates.add(
                        FeedSourceState(
                            categoryId = category?.id?.let { CategoryId(it) },
                            categoryName = category?.title,
                            isExpanded = false,
                            feedSources = feedSources,
                        ),
                    )
                }

                feedSourceStates.sortBy { it.categoryName }

                feedsMutableState.update { feedSourceStates }
            }
        }
    }

    fun deleteFeedSource(feedSource: FeedSource) {
        scope.launch {
            feedManagerRepository.deleteFeed(feedSource)
            feedRetrieverRepository.fetchFeeds(updateLoadingInfo = false)
        }
    }

    fun expandCategory(categoryId: CategoryId?) {
        feedsMutableState.update { oldState ->
            oldState.map { feedSourceState ->
                if (categoryId == feedSourceState.categoryId) {
                    feedSourceState.copy(isExpanded = feedSourceState.isExpanded.not())
                } else {
                    feedSourceState
                }
            }
        }
    }
}
