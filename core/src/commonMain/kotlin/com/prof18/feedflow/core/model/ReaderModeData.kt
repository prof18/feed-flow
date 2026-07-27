package com.prof18.feedflow.core.model

data class ReaderModeData(
    val id: FeedItemId,
    val title: String?,
    val content: String,
    val url: String,
    val baseUrl: String,
    val fontSize: Int,
    val lineHeight: Int,
    val isBookmarked: Boolean,
    val commentsUrl: String? = null,
    val imageUrl: String? = null,
    val shownContentSource: ShownContentSource = ShownContentSource.WEB,
    val canToggleContentSource: Boolean = false,
    val siteName: String? = null,
)
