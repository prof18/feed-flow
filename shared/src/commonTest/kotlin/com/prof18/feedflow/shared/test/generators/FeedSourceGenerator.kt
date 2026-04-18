package com.prof18.feedflow.shared.test.generators

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference

object FeedSourceGenerator {
    fun feedSource(
        id: String = "feed-source-id",
        title: String = "Feed source title",
        url: String = "https://example.com/feed.xml",
        category: FeedSourceCategory? = null,
        lastSyncTimestamp: Long? = null,
        logoUrl: String? = "https://example.com/logo.png",
        websiteUrl: String? = "https://example.com",
        fetchFailed: Boolean = false,
        linkOpeningPreference: LinkOpeningPreference = LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline: Boolean = false,
        isPinned: Boolean = false,
        isNotificationEnabled: Boolean = false,
    ): FeedSource = FeedSource(
        id = id,
        title = title,
        url = url,
        category = category,
        lastSyncTimestamp = lastSyncTimestamp,
        logoUrl = logoUrl,
        websiteUrl = websiteUrl,
        fetchFailed = fetchFailed,
        linkOpeningPreference = linkOpeningPreference,
        isHiddenFromTimeline = isHiddenFromTimeline,
        isPinned = isPinned,
        isNotificationEnabled = isNotificationEnabled,
    )

    fun feedSourceWithCategory(
        categoryName: String,
        id: String = "feed-source-id",
        title: String = "Feed source title",
        url: String = "https://example.com/feed.xml",
        categoryId: String = "category-id",
        lastSyncTimestamp: Long? = null,
        logoUrl: String? = "https://example.com/logo.png",
        websiteUrl: String? = "https://example.com",
        fetchFailed: Boolean = false,
        linkOpeningPreference: LinkOpeningPreference = LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline: Boolean = false,
        isPinned: Boolean = false,
        isNotificationEnabled: Boolean = false,
    ): FeedSource = feedSource(
        id = id,
        title = title,
        url = url,
        category = CategoryGenerator.category(
            id = categoryId,
            title = categoryName,
        ),
        lastSyncTimestamp = lastSyncTimestamp,
        logoUrl = logoUrl,
        websiteUrl = websiteUrl,
        fetchFailed = fetchFailed,
        linkOpeningPreference = linkOpeningPreference,
        isHiddenFromTimeline = isHiddenFromTimeline,
        isPinned = isPinned,
        isNotificationEnabled = isNotificationEnabled,
    )
}
