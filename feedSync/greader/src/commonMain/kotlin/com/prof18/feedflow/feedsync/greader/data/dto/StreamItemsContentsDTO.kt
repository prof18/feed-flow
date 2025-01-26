package com.prof18.feedflow.feedsync.greader.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class StreamItemsContentsDTO(
    val items: List<ItemContentDTO>,
)
