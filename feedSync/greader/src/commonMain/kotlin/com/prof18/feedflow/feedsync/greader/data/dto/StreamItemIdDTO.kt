package com.prof18.feedflow.feedsync.greader.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class StreamItemIdDTO(
    val itemRefs: List<ItemDTO>,
    val continuation: String? = null,
)

@Serializable
internal data class ItemDTO(
    val id: String,
) {
    @Suppress("MagicNumber")
    fun getHexID(): String {
        val raw = id.substringAfterLast("/")
        val bits = raw.toLongOrNull()?.toULong() ?: raw.toULongOrNull()
        return bits?.toString(16)?.padStart(16, '0') ?: raw
    }
}
