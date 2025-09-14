package com.prof18.feedflow.android.editfeed

import com.prof18.feedflow.android.EditFeed
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference

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
        linkOpeningPreference = LinkOpeningPreference.valueOf(linkOpeningPreference),
        isHiddenFromTimeline = isHidden,
        isPinned = isPinned,
        isNotificationEnabled = isNotificationEnabled,
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
        linkOpeningPreference = linkOpeningPreference.name,
        isHidden = isHiddenFromTimeline,
        isPinned = isPinned,
        isNotificationEnabled = isNotificationEnabled,
        fetchFailed = fetchFailed,
    )
}
