package com.prof18.feedflow.feedsync.feedbin.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CreateTaggingRequest(
    @SerialName("feed_id")
    val feedId: Long,
    val name: String,
)
