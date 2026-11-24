package com.prof18.feedflow.shared.domain.notification

import com.prof18.feedflow.core.model.ArticleNotificationPolicy
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceWithUnreadCount
import com.prof18.feedflow.core.model.NotificationTag
import com.prof18.feedflow.core.utils.withSuspensionGuard
import com.prof18.feedflow.database.DatabaseHelper
import kotlinx.coroutines.flow.Flow

/**
 * Tells which posted new-article notifications have nothing unread left behind them. Each platform
 * lists its own tray, asks here, and removes the notifications carrying the tags that come back.
 */
class ReadNotificationResolver internal constructor(
    private val databaseHelper: DatabaseHelper,
) {
    /** Emits on subscription and after every write to the articles. */
    fun changes(): Flow<Unit> = databaseHelper.getFeedItemChangesFlow()

    @Throws(Exception::class)
    suspend fun staleTags(tags: List<String>): List<String> {
        if (tags.isEmpty()) return emptyList()
        val distinctTags = tags.distinct()
        // On iOS a read still open on the app-group database when the app is suspended gets the
        // app killed (0xdead10cc), so the read asks for the time to finish.
        return withSuspensionGuard("FeedFlow notification read check") {
            val articleIds = distinctTags.mapNotNull { it.articleId() }
            val unreadArticleIds = if (articleIds.isEmpty()) emptySet() else databaseHelper.getUnreadItemIds(articleIds)
            val unread = UnreadFeeds(databaseHelper.getFeedSourcesWithUnreadCount())
            distinctTags.filter { unread.isStale(it, unreadArticleIds) }
        }
    }
}

private fun String.articleId(): String? =
    if (startsWith(ArticleNotificationPolicy.ARTICLE_ID_PREFIX)) {
        removePrefix(ArticleNotificationPolicy.ARTICLE_ID_PREFIX)
    } else {
        null
    }

private class UnreadFeeds(feeds: List<FeedSourceWithUnreadCount>) {
    private val unread: List<FeedSource> = feeds.filter { it.unreadCount > 0 }.map { it.feedSource }
    private val unreadFeedIds = unread.mapTo(mutableSetOf()) { it.id }

    // A feed's own notification goes by its own articles, whether or not the feed still sends any.
    // A notification covering several feeds counts only the feeds that send notifications, so an
    // old unread article in a silent feed can't keep it around.
    private val notifying = unread.filter { it.isNotificationEnabled }
    private val notifyingCategoryIds = notifying.mapNotNullTo(mutableSetOf()) { it.category?.id }

    fun isStale(tag: String, unreadArticleIds: Set<String>): Boolean {
        val articleId = tag.articleId()
        return when {
            articleId != null -> articleId !in unreadArticleIds
            tag.startsWith(NotificationTag.FEED_SOURCE_PREFIX) ->
                tag.removePrefix(NotificationTag.FEED_SOURCE_PREFIX) !in unreadFeedIds
            tag.startsWith(NotificationTag.CATEGORY_PREFIX) ->
                tag.removePrefix(NotificationTag.CATEGORY_PREFIX) !in notifyingCategoryIds
            tag == NotificationTag.UNCATEGORIZED -> notifying.none { it.category == null }
            tag == NotificationTag.GROUPED -> notifying.isEmpty()
            else -> false
        }
    }
}
