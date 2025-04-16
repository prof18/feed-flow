package com.prof18.feedflow.shared.domain.notification

import com.prof18.feedflow.core.model.FeedSourceToNotify

interface Notifier {
    fun showNewArticlesNotification(feedSourcesToNotify: List<FeedSourceToNotify>)
}
