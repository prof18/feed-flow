package com.prof18.feedflow.feedsync.database.model

import com.prof18.feedflow.core.model.CategoryId

data class SyncedFeedSource(
    val id: String,
    val url: String,
    val title: String,
    val categoryId: CategoryId?,
    val logoUrl: String?,
)
