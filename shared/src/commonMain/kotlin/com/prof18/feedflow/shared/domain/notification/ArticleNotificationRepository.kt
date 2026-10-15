package com.prof18.feedflow.shared.domain.notification

import co.touchlab.skie.configuration.annotations.SuspendInterop
import com.prof18.feedflow.core.model.ArticleNotificationBatch
import com.prof18.feedflow.core.model.ArticleNotificationPolicy
import com.prof18.feedflow.database.DatabaseHelper

class ArticleNotificationRepository internal constructor(
    private val databaseHelper: DatabaseHelper,
) {
    @SuspendInterop.Enabled
    @Throws(Exception::class)
    suspend fun prepareBatch(): ArticleNotificationBatch {
        val pendingArticles = databaseHelper.getArticlesToNotify()
        val selectedArticles = pendingArticles.take(ArticleNotificationPolicy.MAX_INDIVIDUAL)
        return ArticleNotificationBatch(
            articles = selectedArticles,
            remainderIds = pendingArticles.drop(selectedArticles.size).map { it.id },
        )
    }

    @SuspendInterop.Enabled
    @Throws(Exception::class)
    suspend fun acknowledge(ids: List<String>) {
        databaseHelper.markArticleItemsAsNotified(ids)
    }
}
