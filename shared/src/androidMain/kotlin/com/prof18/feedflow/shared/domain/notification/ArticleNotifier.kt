package com.prof18.feedflow.shared.domain.notification

import com.prof18.feedflow.core.model.ArticleNotificationBatch

interface ArticleNotifier {
    suspend fun showArticleNotifications(
        prepareBatch: suspend () -> ArticleNotificationBatch,
        acknowledge: suspend (List<String>) -> Unit,
    )
}
