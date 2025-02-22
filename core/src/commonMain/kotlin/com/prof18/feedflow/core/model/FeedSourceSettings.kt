package com.prof18.feedflow.core.model

data class FeedSourceSettings(
    val linkOpeningPreference: LinkOpeningPreference = LinkOpeningPreference.DEFAULT,
    val isHiddenFromTimeline: Boolean = false,
    val isPinned: Boolean = false,
)
