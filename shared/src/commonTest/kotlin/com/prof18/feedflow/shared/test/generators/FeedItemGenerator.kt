package com.prof18.feedflow.shared.test.generators

import com.prof18.feedflow.core.model.FeedItem

object FeedItemGenerator {
    fun feedItem(
        id: String = "feed-item-id",
        url: String = "https://example.com/article",
        title: String? = "Feed item title",
        subtitle: String? = "Feed item subtitle",
        content: String? = "Feed item content",
        imageUrl: String? = "https://example.com/image.jpg",
        feedSource: com.prof18.feedflow.core.model.FeedSource = FeedSourceGenerator.feedSource(),
        pubDateMillis: Long? = 1_704_067_200_000L,
        isRead: Boolean = false,
        dateString: String? = "2024-01-01",
        commentsUrl: String? = "https://example.com/comments",
        isBookmarked: Boolean = false,
    ): FeedItem = FeedItem(
        id = id,
        url = url,
        title = title,
        subtitle = subtitle,
        content = content,
        imageUrl = imageUrl,
        feedSource = feedSource,
        pubDateMillis = pubDateMillis,
        isRead = isRead,
        dateString = dateString,
        commentsUrl = commentsUrl,
        isBookmarked = isBookmarked,
    )

    fun unreadFeedItem(
        id: String = "feed-item-id",
        url: String = "https://example.com/article",
        title: String? = "Feed item title",
        subtitle: String? = "Feed item subtitle",
        content: String? = "Feed item content",
        imageUrl: String? = "https://example.com/image.jpg",
        feedSource: com.prof18.feedflow.core.model.FeedSource = FeedSourceGenerator.feedSource(),
        pubDateMillis: Long? = 1_704_067_200_000L,
        dateString: String? = "2024-01-01",
        commentsUrl: String? = "https://example.com/comments",
        isBookmarked: Boolean = false,
    ): FeedItem = feedItem(
        id = id,
        url = url,
        title = title,
        subtitle = subtitle,
        content = content,
        imageUrl = imageUrl,
        feedSource = feedSource,
        pubDateMillis = pubDateMillis,
        isRead = false,
        dateString = dateString,
        commentsUrl = commentsUrl,
        isBookmarked = isBookmarked,
    )

    fun bookmarkedFeedItem(
        id: String = "feed-item-id",
        url: String = "https://example.com/article",
        title: String? = "Feed item title",
        subtitle: String? = "Feed item subtitle",
        content: String? = "Feed item content",
        imageUrl: String? = "https://example.com/image.jpg",
        feedSource: com.prof18.feedflow.core.model.FeedSource = FeedSourceGenerator.feedSource(),
        pubDateMillis: Long? = 1_704_067_200_000L,
        isRead: Boolean = false,
        dateString: String? = "2024-01-01",
        commentsUrl: String? = "https://example.com/comments",
    ): FeedItem = feedItem(
        id = id,
        url = url,
        title = title,
        subtitle = subtitle,
        content = content,
        imageUrl = imageUrl,
        feedSource = feedSource,
        pubDateMillis = pubDateMillis,
        isRead = isRead,
        dateString = dateString,
        commentsUrl = commentsUrl,
        isBookmarked = true,
    )

    fun feedItemsForSource(
        feedSource: com.prof18.feedflow.core.model.FeedSource,
        count: Int = 10,
    ): List<FeedItem> = List(count) { index ->
        feedItem(
            id = "feed-item-id-$index",
            url = "https://example.com/article-$index",
            feedSource = feedSource,
        )
    }
}
