package com.prof18.feedflow.feedsync.feedbin.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CreateSubscriptionRequest(
    @SerialName("feed_url")
    val feedUrl: String,
)
