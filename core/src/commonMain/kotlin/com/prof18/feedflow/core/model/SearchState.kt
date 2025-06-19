package com.prof18.feedflow.core.model

import kotlinx.collections.immutable.ImmutableList

sealed class SearchState {
    data class DataFound(
        val items: ImmutableList<FeedItem>,
        val feedLayout: FeedLayout,
    ) : SearchState()

    data class NoDataFound(
        val searchQuery: String,
    ) : SearchState()

    data object EmptyState : SearchState()
}
