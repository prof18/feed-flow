package com.prof18.feedflow.feedsync.feedbin.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EntryDTO(
    val id: Long,
    @SerialName("feed_id")
    val feedId: Long,
    val title: String?,
    val author: String?,
    val content: String?,
    val summary: String?,
    val url: String,
    @SerialName("extracted_content_url")
    val extractedContentUrl: String?,
    val published: String,
    @SerialName("created_at")
    val createdAt: String,
    val enclosure: Enclosure? = null,
) {
    @Serializable
    data class Enclosure(
        @SerialName("enclosure_url")
        val url: String? = null,
        @SerialName("enclosure_type")
        val type: String? = null,
    )
}
