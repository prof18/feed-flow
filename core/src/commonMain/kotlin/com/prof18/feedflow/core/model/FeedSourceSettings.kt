package com.prof18.feedflow.core.model

data class FeedSourceSettings(
    val articleOpenMode: ArticleOpenMode = ArticleOpenMode.DEFAULT,
    val isHiddenFromTimeline: Boolean = false,
    val isPinned: Boolean = false,
    val isNotificationEnabled: Boolean = false,
    val isHideImagesEnabled: Boolean = false,
)
