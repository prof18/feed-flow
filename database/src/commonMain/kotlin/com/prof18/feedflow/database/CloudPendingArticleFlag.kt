package com.prof18.feedflow.database

data class CloudPendingArticleFlag(
    val itemId: String,
    val field: CloudArticleFlag,
    val value: Boolean,
    val revision: Long,
)
