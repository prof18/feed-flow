package com.prof18.feedflow.core.model

data class FeedSourceWithPreferences(
    val feedSource: FeedSource,
    val linkOpeningPreference: LinkOpeningPreference = LinkOpeningPreference.DEFAULT,
)