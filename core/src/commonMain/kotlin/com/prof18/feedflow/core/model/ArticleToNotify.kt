package com.prof18.feedflow.core.model

data class ArticleToNotify(
    val id: String,
    val title: String?,
    val feedSourceTitle: String,
    val imageUrl: String?,
)
