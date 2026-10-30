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
    val images: Images? = null,
) {
    @Serializable
    data class Enclosure(
        @SerialName("enclosure_url")
        val url: String? = null,
        @SerialName("enclosure_type")
        val type: String? = null,
    )

    @Serializable
    data class Images(
        @SerialName("original_url")
        val originalUrl: String? = null,
        @SerialName("size_1")
        val size1: ResizedImage? = null,
    ) {
        @Serializable
        data class ResizedImage(
            @SerialName("cdn_url")
            val cdnUrl: String? = null,
            val width: Int? = null,
            val height: Int? = null,
        )
    }
}
