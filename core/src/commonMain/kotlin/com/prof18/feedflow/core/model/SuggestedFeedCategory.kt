package com.prof18.feedflow.core.model

data class SuggestedFeedCategory(
    val id: String,
    val name: String,
    val icon: String,
    val feeds: List<SuggestedFeed>,
)

data class SuggestedFeed(
    val name: String,
    val url: String,
    val description: String,
    val logoUrl: String? = null,
)
