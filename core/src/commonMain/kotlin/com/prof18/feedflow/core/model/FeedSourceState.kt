package com.prof18.feedflow.core.model

data class FeedSourceState(
    val categoryId: CategoryId?,
    val categoryName: String?,
    val isExpanded: Boolean = false,
    val feedSources: List<FeedSource>,
)

data class FeedSourceListState(
    val feedSourcesWithoutCategory: List<FeedSource> = emptyList(),
    val feedSourcesWithCategory: List<FeedSourceState> = emptyList(),
) {
    fun isEmpty(): Boolean =
        feedSourcesWithoutCategory.isEmpty() && feedSourcesWithCategory.isEmpty()
}
