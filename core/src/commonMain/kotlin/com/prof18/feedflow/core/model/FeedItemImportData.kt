package com.prof18.feedflow.core.model

data class FeedItemImportData(
    val urlHash: String,
    val url: String,
    val title: String?,
    val subtitle: String?,
    val imageUrl: String?,
    val feedSourceId: String,
    val isRead: Boolean,
    val isBookmarked: Boolean,
    val pubDateMillis: Long?,
    val commentsUrl: String?,
    val notificationSent: Boolean,
    val isBlocked: Boolean,
    val contentFetched: Boolean,
)
