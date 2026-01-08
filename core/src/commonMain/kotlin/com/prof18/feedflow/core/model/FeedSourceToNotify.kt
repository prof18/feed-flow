package com.prof18.feedflow.core.model

data class FeedSourceToNotify(
    val feedSourceId: String,
    val feedSourceTitle: String,
    val categoryId: String?,
    val categoryTitle: String?,
)
