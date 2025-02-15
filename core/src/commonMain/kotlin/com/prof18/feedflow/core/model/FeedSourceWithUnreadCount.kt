package com.prof18.feedflow.core.model

data class FeedSourceWithUnreadCount(
    val feedSource: FeedSource,
    val unreadCount: Long,
)
