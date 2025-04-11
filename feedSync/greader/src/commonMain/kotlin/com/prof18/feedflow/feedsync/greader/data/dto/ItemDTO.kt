package com.prof18.feedflow.feedsync.greader.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ItemContentDTO(
    val id: String,
    val published: Long,
    val title: String,
    val canonical: List<Link>,
    val summary: Summary,
    val origin: Origin,
    val content: Content? = null,
    val author: String? = null,
    val enclosure: List<Enclosure>? = null,
    val categories: List<String>? = null,
) {

    val read: Boolean
        get() = isRead(categories)

    val starred: Boolean
        get() = isStarred(categories)

    val hexID = id.split("/").last()

    @Serializable
    data class Origin(
        val streamId: String,
        val htmlUrl: String?,
        val title: String,
    )

    @Serializable
    data class Summary(
        val content: String,
    )

    @Serializable
    data class Content(
        val content: String,
    )

    @Serializable
    data class Enclosure(
        val href: String,
        val type: String,
    )

    @Serializable
    data class Link(
        val href: String,
    )

    val image: Enclosure?
        get() = enclosure?.find { it.type.startsWith("image") }
}

/** open for testing */
internal fun isRead(categories: List<String>?): Boolean {
    return categories?.any { it.endsWith("state/com.google/read") } ?: false
}

/** open for testing */
internal fun isStarred(categories: List<String>?): Boolean {
    return categories?.any { it.endsWith("state/com.google/starred") } ?: false
}
