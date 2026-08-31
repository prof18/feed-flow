package com.prof18.feedflow.core.model

data class ReadLaterMarker(
    val id: String,
    val feedItemId: String,
    val scrollPosition: Int,
    val createdAt: Long,
)

data class ReadLaterMarkerWithDetails(
    val id: String,
    val feedItemId: String,
    val scrollPosition: Int,
    val createdAt: Long,
    val title: String?,
    val url: String,
    val feedSourceTitle: String,
    val imageUrl: String?,
    val pubDateMillis: Long?,
)
