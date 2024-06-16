package com.prof18.feedflow.core.model

data class SyncedFeedItem(
    val id: String,
    val isRead: Boolean,
    val isBookmarked: Boolean,
)
