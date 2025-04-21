package com.prof18.feedflow.core.model

data class ReaderModeData(
    val id: FeedItemId,
    val title: String?,
    val content: String,
    val url: String,
    val fontSize: Int,
    val isBookmarked: Boolean,
    val heroImageUrl: String?,
)
