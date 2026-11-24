package com.prof18.feedflow.shared.domain.notification

import co.touchlab.skie.configuration.annotations.SuspendInterop
import com.prof18.feedflow.core.model.ArticleToNotify
import com.prof18.feedflow.database.DatabaseHelper

class ArticleNotificationRepository internal constructor(
    private val databaseHelper: DatabaseHelper,
) {
    /** Every article waiting to be announced, newest first. */
    @SuspendInterop.Enabled
    @Throws(Exception::class)
    suspend fun pendingArticles(): List<ArticleToNotify> = databaseHelper.getArticlesToNotify()

    @SuspendInterop.Enabled
    @Throws(Exception::class)
    suspend fun acknowledge(ids: List<String>) {
        databaseHelper.markArticleItemsAsNotified(ids)
    }
}
