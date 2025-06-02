package com.prof18.feedflow.feedsync.feedbin.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class FeedbinEntryDTO(
    @SerialName("id") val id: Long,
    @SerialName("feed_id") val feedId: Long,
    @SerialName("title") val title: String?,
    @SerialName("author") val author: String?,
    @SerialName("summary") val summary: String?, // Often HTML
    @SerialName("content") val content: String?, // Often HTML, typically more complete than summary
    @SerialName("url") val url: String,
    @SerialName("published") val published: String, // ISO 8601 date string
    @SerialName("created_at") val createdAt: String, // ISO 8601 date string
    @SerialName("original") val original: JsonElement? = null,
    @SerialName("images") val images: JsonElement? = null,
    @SerialName("enclosure") val enclosure: JsonElement? = null,
    @SerialName("twitter_id") val twitterId: String? = null,
    @SerialName("twitter_thread_ids") val twitterThreadIds: List<Long>? = null,
    @SerialName("extracted_articles") val extractedArticles: JsonElement? = null,
    @SerialName("json_feed") val jsonFeed: JsonElement? = null,
    @SerialName("extracted_content_url") val extractedContentUrl: String? = null
)
