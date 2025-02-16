package com.prof18.feedflow.core.model

data class FeedSource(
    val id: String,
    val url: String,
    val title: String,
    val category: FeedSourceCategory?,
    val lastSyncTimestamp: Long?,
    val logoUrl: String?,
    val linkOpeningPreference: LinkOpeningPreference,
    val isHiddenFromTimeline: Boolean,
    val isPinned: Boolean,
)
