package com.prof18.feedflow.core.model

data class FeedSourceState(
    val categoryId: CategoryId?,
    val categoryName: String?,
    val isExpanded: Boolean = false,
    val feedSources: List<FeedSource>,
)
