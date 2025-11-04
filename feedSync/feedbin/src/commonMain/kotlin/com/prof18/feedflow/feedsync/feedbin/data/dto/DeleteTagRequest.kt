package com.prof18.feedflow.feedsync.feedbin.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class DeleteTagRequest(
    val name: String,
)
