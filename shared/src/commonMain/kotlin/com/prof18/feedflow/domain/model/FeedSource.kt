package com.prof18.feedflow.domain.model

data class FeedSource(
    val id: Int,
    val url: String,
    val title: String,
    val lastSyncTimestamp: Long?,
)
