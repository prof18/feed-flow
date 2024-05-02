package com.prof18.feedflow.core.model

import kotlinx.collections.immutable.ImmutableList

sealed class SearchState {
    data class DataFound(
        val items: ImmutableList<FeedItem>,
    ) : SearchState()

    data class NoDataFound(
        val searchQuery: String,
    ) : SearchState()

    data object EmptyState : SearchState()
}
