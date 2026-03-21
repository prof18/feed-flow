package com.prof18.feedflow.core.model

data class FeedItemDisplaySettings(
    val isHideUnreadDotEnabled: Boolean = false,
    val isHideFeedSourceEnabled: Boolean = false,
    val descriptionLineLimit: DescriptionLineLimit = DescriptionLineLimit.THREE,
)
