package com.prof18.feedflow.feedsync.greader.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionQuickAddResult(
    val numResults: Int?,
    val query: String? = null,
    val streamId: String? = null,
    val streamName: String? = null,
)
