package com.prof18.feedflow.feedsync.feedbin.mapper

import com.prof18.feedflow.core.model.FeedSource // Assuming FeedSource is the domain model
import com.prof18.feedflow.core.model.FeedSourceCategory // Assuming this is for category info
import com.prof18.feedflow.feedsync.feedbin.remote.model.FeedbinSubscriptionDTO

internal fun FeedbinSubscriptionDTO.toDomainModel(
    // No explicit feedAccountId needed if FeedSource doesn't require it directly for Feedbin scenario
    // May need category mapping if Feedbin provides category info with subscriptions,
    // or if categories are handled separately. For now, assuming simple mapping.
): FeedSource {
    return FeedSource(
        id = this.id.toString(), // Use Feedbin's subscription ID as the unique ID for the feed source
        url = this.feedUrl,
        title = this.title,
        category = null, // Feedbin's subscription DTO doesn't directly provide category. This might be enriched later.
        logoUrl = null, // Feedbin API for subscriptions doesn't provide icons here.
        isUserGenerated = false, // Defaulting, adjust if Feedbin provides this info
        isFavourite = false, // Defaulting
        lastSyncTimestamp = null, // To be updated by sync logic
        isNotificationEnabled = false, // Defaulting
        isDefault = false, // Defaulting
        position = 0, // Defaulting
        websiteUrl = this.siteUrl,
        isLocal = false, // This is a remote feed
    )
}
