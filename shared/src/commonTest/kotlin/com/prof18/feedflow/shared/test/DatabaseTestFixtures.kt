package com.prof18.feedflow.shared.test

import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.database.DatabaseHelper

fun FeedSource.toParsedFeedSource(): ParsedFeedSource = ParsedFeedSource(
    id = id,
    url = url,
    title = title,
    category = category,
    logoUrl = logoUrl,
    websiteUrl = websiteUrl,
)

suspend fun DatabaseHelper.insertFeedSourceWithCategory(feedSource: FeedSource) {
    feedSource.category?.let { insertCategories(listOf(it)) }
    insertFeedSource(listOf(feedSource.toParsedFeedSource()))
}

fun buildFeedItem(
    id: String,
    title: String,
    pubDateMillis: Long,
    source: FeedSource,
    subtitle: String? = null,
): FeedItem = FeedItem(
    id = id,
    url = "https://example.com/$id",
    title = title,
    subtitle = subtitle,
    content = null,
    imageUrl = null,
    feedSource = source,
    pubDateMillis = pubDateMillis,
    isRead = false,
    dateString = null,
    commentsUrl = null,
    isBookmarked = false,
)

fun buildFeedItemsForSource(
    source: FeedSource,
    count: Int,
    startTimestamp: Long,
    stepMillis: Long = 1000L,
    titlePrefix: String = "Item",
): List<FeedItem> = List(count) { index ->
    buildFeedItem(
        id = "${source.id}-$index",
        title = "$titlePrefix $index",
        pubDateMillis = startTimestamp - (index * stepMillis),
        source = source,
    )
}
