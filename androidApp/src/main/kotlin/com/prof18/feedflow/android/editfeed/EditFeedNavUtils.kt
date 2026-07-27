package com.prof18.feedflow.android.editfeed

import com.prof18.feedflow.android.EditFeed
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory

internal fun EditFeed.toFeedSource(): FeedSource {
    return FeedSource(
        id = id,
        url = url,
        title = title,
        category = if (categoryId != null && categoryTitle != null) {
            FeedSourceCategory(
                id = categoryId,
                title = categoryTitle,
            )
        } else {
            null
        },
        lastSyncTimestamp = lastSyncTimestamp,
        logoUrl = logoUrl,
        websiteUrl = websiteUrl,
        articleOpenMode = ArticleOpenMode.valueOf(articleOpenMode),
        isHiddenFromTimeline = isHidden,
        isPinned = isPinned,
        isNotificationEnabled = isNotificationEnabled,
        isHideImagesEnabled = isHideImagesEnabled,
        fetchFailed = fetchFailed,
    )
}

internal fun FeedSource.toEditFeed(): EditFeed {
    return EditFeed(
        id = id,
        url = url,
        title = title,
        categoryId = category?.id,
        categoryTitle = category?.title,
        lastSyncTimestamp = lastSyncTimestamp,
        logoUrl = logoUrl,
        websiteUrl = websiteUrl,
        articleOpenMode = articleOpenMode.name,
        isHidden = isHiddenFromTimeline,
        isPinned = isPinned,
        isNotificationEnabled = isNotificationEnabled,
        isHideImagesEnabled = isHideImagesEnabled,
        fetchFailed = fetchFailed,
    )
}
