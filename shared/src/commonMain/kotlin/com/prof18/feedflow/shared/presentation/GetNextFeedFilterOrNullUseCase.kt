package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import kotlinx.coroutines.flow.first

class GetNextFeedFilterOrNullUseCase internal constructor(
    private val feedSourcesRepository: FeedSourcesRepository,
) {

    suspend operator fun invoke(
        currentFeedFilter: FeedFilter,
    ): FeedFilter? {
        when (currentFeedFilter) {
            is FeedFilter.Category -> {
                val unreadCategoryList = feedSourcesRepository
                    .observeFeedSourcesByCategoryWithUnreadCount()
                    .first()
                    .mapValues { source -> source.value.sumOf { it.unreadCount } }
                    .filter { it.value > 0 }
                    .keys
                    .filterNotNull()

                val currentCategoryIndex = unreadCategoryList
                    .indexOfFirst { it.id == currentFeedFilter.feedCategory.id }

                return FeedFilter.Category(
                    feedCategory = unreadCategoryList
                        .drop(currentCategoryIndex + 1)
                        .firstOrNull()
                        ?: return null,
                )
            }
            is FeedFilter.Source -> {
                val feedSources = feedSourcesRepository.getFeedSourcesWithUnreadCount().first()
                    .filter {
                        it.feedSource.category?.id == currentFeedFilter.feedSource.category?.id
                    }

                val currentSourceIndex = feedSources.indexOfFirst {
                    it.feedSource.id == currentFeedFilter.feedSource.id
                }

                if (currentSourceIndex == -1) return null

                val nextUnreadSource = feedSources
                    .drop(currentSourceIndex + 1)
                    .firstOrNull { it.unreadCount > 0 }

                return FeedFilter.Source(
                    feedSource = nextUnreadSource?.feedSource ?: return null,
                )
            }
            else -> return null
        }
    }
}
