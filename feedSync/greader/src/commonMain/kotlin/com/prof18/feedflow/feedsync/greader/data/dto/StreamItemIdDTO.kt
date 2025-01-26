package com.prof18.feedflow.feedsync.greader.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class StreamItemIdDTO(
    val itemRefs: List<ItemDTO>,
    val continuation: String?,
)

@Serializable
internal data class ItemDTO(
    val id: String,
) {
    @Suppress("MagicNumber")
    fun getHexID(): String = id.toLong().toString(16).padStart(16, '0')
}
