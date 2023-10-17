package com.prof18.feedflow.core.model

sealed interface FeedFilter {

    data object Timeline : FeedFilter

    data class Category(
        val feedCategory: FeedSourceCategory,
    ) : FeedFilter

    data class Source(
        val feedSource: FeedSource,
    ) : FeedFilter
}
