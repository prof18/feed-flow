package com.prof18.feedflow.shared.domain.notification

import com.prof18.feedflow.core.model.FeedSourceToNotify

fun interface Notifier {
    fun showNewArticlesNotification(feedSourcesToNotify: List<FeedSourceToNotify>)
}
