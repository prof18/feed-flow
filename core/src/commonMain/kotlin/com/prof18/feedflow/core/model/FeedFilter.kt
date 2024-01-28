package com.prof18.feedflow.core.model

sealed class FeedFilter {

    data object Timeline : FeedFilter()

    data object Read : FeedFilter()

    data class Category(
        val feedCategory: FeedSourceCategory,
    ) : FeedFilter()

    data class Source(
        val feedSource: FeedSource,
    ) : FeedFilter()
}
