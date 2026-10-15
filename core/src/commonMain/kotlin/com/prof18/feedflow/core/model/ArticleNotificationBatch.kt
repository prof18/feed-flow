package com.prof18.feedflow.core.model

data class ArticleNotificationBatch(
    val articles: List<ArticleToNotify>,
    val remainderIds: List<String>,
)
