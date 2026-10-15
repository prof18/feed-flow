package com.prof18.feedflow.core.model

object ArticleNotificationPolicy {
    const val MAX_INDIVIDUAL = 20
    const val ANDROID_ACTIVE_SOFT_LIMIT = 40
    const val ARTICLE_ID_PREFIX = "article-"
    const val OVERFLOW_ID = "article-overflow"
    const val IMAGE_EDGE_PX = 1024
    const val IMAGE_TIMEOUT_MS = 1_000L
    const val IMAGE_BATCH_BUDGET_MS = 5_000L
    const val ATTACHMENT_MAX_BYTES = 2_097_152
    const val ATTACHMENT_DIRECTORY = "article-notification-images"
    const val ACK_CHUNK_SIZE = 500
}
