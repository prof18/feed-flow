package com.prof18.feedflow.feedsync.feedbin.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class IconDTO(
    val host: String,
    val url: String,
)
