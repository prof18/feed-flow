package com.prof18.feedflow.core.model

data class CategoryWithUnreadCount(
    val category: FeedSourceCategory,
    val unreadCount: Long,
)
