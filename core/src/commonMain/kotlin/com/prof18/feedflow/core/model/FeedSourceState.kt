package com.prof18.feedflow.core.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class FeedSourceState(
    val categoryId: CategoryId?,
    val categoryName: String?,
    val isExpanded: Boolean = false,
    val feedSources: ImmutableList<FeedSource>,
)

data class FeedSourceListState(
    val feedSourcesWithoutCategory: ImmutableList<FeedSource> = persistentListOf(),
    val feedSourcesWithCategory: ImmutableList<FeedSourceState> = persistentListOf(),
) {
    fun isEmpty(): Boolean =
        feedSourcesWithoutCategory.isEmpty() && feedSourcesWithCategory.isEmpty()
}
