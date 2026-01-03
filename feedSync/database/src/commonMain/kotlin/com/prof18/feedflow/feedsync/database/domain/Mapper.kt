package com.prof18.feedflow.feedsync.database.domain

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.ParsedFeedSource

fun ParsedFeedSource.toFeedSource() = FeedSource(
    id = this.id,
    url = this.url,
    title = this.title,
    category = this.category,
    lastSyncTimestamp = null,
    logoUrl = this.logoUrl,
    websiteUrl = this.websiteUrl,
    fetchFailed = false,
    linkOpeningPreference = LinkOpeningPreference.DEFAULT,
    isHiddenFromTimeline = false,
    isPinned = false,
    isNotificationEnabled = false,
)
