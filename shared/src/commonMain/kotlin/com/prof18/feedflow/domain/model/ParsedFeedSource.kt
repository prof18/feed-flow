package com.prof18.feedflow.domain.model

data class ParsedFeedSource(
    val url: String,
    val title: String,
    val category: String?,
)
