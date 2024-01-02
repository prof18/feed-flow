package com.prof18.feedflow.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class FeedItem(
    val id: Int,
    val url: String,
    val title: String?,
    val subtitle: String?,
    val content: String?,
    val imageUrl: String?,
    val feedSource: FeedSource,
    val pubDateMillis: Long?,
    val isRead: Boolean,
    val dateString: String?,
    val commentsUrl: String?,
)
