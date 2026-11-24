package com.prof18.feedflow.shared.domain.notification

import com.prof18.feedflow.core.model.ArticleToNotify

interface ArticleNotifier {
    suspend fun showArticleNotifications(
        pendingArticles: suspend () -> List<ArticleToNotify>,
        acknowledge: suspend (List<String>) -> Unit,
    )
}
