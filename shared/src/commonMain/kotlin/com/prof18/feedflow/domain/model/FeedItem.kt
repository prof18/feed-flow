package com.prof18.feedflow.domain.model

data class FeedItem(
    val id: Int,
    val url: String,
    val title: String,
    val subtitle: String?,
    val content: String?,
    val imageUrl: String?,
    val feedSource: FeedSource,
    val isRead: Boolean,
    val pubDateMillis: Long,
    val dateString: String,
    val commentsUrl: String?,
)
