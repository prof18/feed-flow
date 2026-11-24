package com.prof18.feedflow.core.model

/**
 * The tag a new-article notification carries, naming whose read state retires it: one article,
 * one feed, one category, or everything. Android stores it as the notification tag; iOS stores it
 * in the user info under [USER_INFO_KEY], because its identifier has to stay unique per post.
 */
object NotificationTag {
    const val USER_INFO_KEY = "tag"

    const val FEED_SOURCE_PREFIX = "feed-source-"
    const val CATEGORY_PREFIX = "category-"
    const val UNCATEGORIZED = "uncategorized"
    const val GROUPED = "grouped"

    fun forArticle(articleId: String): String = ArticleNotificationPolicy.ARTICLE_ID_PREFIX + articleId

    fun forFeedSource(feedSourceId: String): String = FEED_SOURCE_PREFIX + feedSourceId

    fun forCategory(categoryId: String?): String =
        if (categoryId != null) CATEGORY_PREFIX + categoryId else UNCATEGORIZED
}
