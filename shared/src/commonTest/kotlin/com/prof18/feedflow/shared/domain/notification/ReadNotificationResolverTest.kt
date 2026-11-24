package com.prof18.feedflow.shared.domain.notification

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.NotificationTag
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReadNotificationResolverTest : KoinTestBase() {

    private val databaseHelper: DatabaseHelper by inject()
    private val resolver by lazy { ReadNotificationResolver(databaseHelper) }

    @Test
    fun `nothing is stale while the articles are unread`() = runTest(testDispatcher) {
        seed(categorized, listOf("c-1"))
        seed(uncategorized, listOf("u-1"))

        assertTrue(resolver.staleTags(everyTag).isEmpty())
    }

    @Test
    fun `an article tag goes stale once that one article is read`() = runTest(testDispatcher) {
        seed(categorized, listOf("c-1", "c-2"))

        databaseHelper.markAsRead(listOf(FeedItemId("c-1")))

        assertEquals(
            listOf(NotificationTag.forArticle("c-1")),
            resolver.staleTags(listOf(NotificationTag.forArticle("c-1"), NotificationTag.forArticle("c-2"))),
        )
    }

    @Test
    fun `reading a feed retires its own tags but not the others`() = runTest(testDispatcher) {
        seed(categorized, listOf("c-1"))
        seed(uncategorized, listOf("u-1"))

        databaseHelper.markAllFeedAsRead(FeedFilter.Source(categorized))

        assertEquals(
            listOf(
                NotificationTag.forArticle("c-1"),
                NotificationTag.forFeedSource(categorized.id),
                NotificationTag.forCategory(CATEGORY_ID),
            ),
            resolver.staleTags(everyTag),
        )
    }

    @Test
    fun `marking everything as read retires every tag`() = runTest(testDispatcher) {
        seed(categorized, listOf("c-1"))
        seed(uncategorized, listOf("u-1"))

        databaseHelper.markAllFeedAsRead(FeedFilter.Timeline)

        assertEquals(everyTag, resolver.staleTags(everyTag))
    }

    @Test
    fun `unread articles of a silent feed keep no notification about other feeds around`() =
        runTest(testDispatcher) {
            seed(categorized, listOf("c-1"))
            seed(silent, listOf("s-1"), notificationsEnabled = false)

            databaseHelper.markAllFeedAsRead(FeedFilter.Source(categorized))

            val covering = listOf(
                NotificationTag.forCategory(CATEGORY_ID),
                NotificationTag.forCategory(null),
                NotificationTag.GROUPED,
            )
            assertEquals(covering, resolver.staleTags(covering))
        }

    @Test
    fun `a feed with notifications turned off keeps its own notifications until they are read`() =
        runTest(testDispatcher) {
            seed(silent, listOf("s-1"), notificationsEnabled = false)
            val ownTags = listOf(NotificationTag.forArticle("s-1"), NotificationTag.forFeedSource(silent.id))

            assertTrue(resolver.staleTags(ownTags).isEmpty())

            databaseHelper.markAsRead(listOf(FeedItemId("s-1")))
            assertEquals(ownTags, resolver.staleTags(ownTags))
        }

    @Test
    fun `a tag that names nothing FeedFlow posted is never stale`() = runTest(testDispatcher) {
        databaseHelper.markAllFeedAsRead(FeedFilter.Timeline)

        // The summary Android adds when it groups an app's notifications by itself.
        assertTrue(resolver.staleTags(listOf("ranker_group")).isEmpty())
    }

    @Test
    fun `repeated tags are answered once and an empty tray asks nothing`() = runTest(testDispatcher) {
        seed(categorized, listOf("c-1"))

        databaseHelper.markAllFeedAsRead(FeedFilter.Timeline)

        val tag = NotificationTag.forFeedSource(categorized.id)
        assertEquals(listOf(tag), resolver.staleTags(listOf(tag, tag, tag)))
        assertTrue(resolver.staleTags(emptyList()).isEmpty())
    }

    private suspend fun seed(source: FeedSource, ids: List<String>, notificationsEnabled: Boolean = true) {
        databaseHelper.insertFeedSourceWithCategory(source)
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = source.id,
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHidden = false,
            isPinned = false,
            isNotificationEnabled = notificationsEnabled,
            isHideImagesEnabled = false,
        )
        databaseHelper.insertFeedItems(
            feedItems = ids.mapIndexed { index, id ->
                FeedItem(
                    id = id,
                    url = "https://example.com/$id",
                    title = id,
                    subtitle = null,
                    content = null,
                    imageUrl = null,
                    feedSource = source,
                    pubDateMillis = 1_000L - index,
                    isRead = false,
                    dateString = null,
                    commentsUrl = null,
                    isBookmarked = false,
                )
            },
            lastSyncTimestamp = 0,
        )
    }

    private companion object {
        const val CATEGORY_ID = "category-id"
        val categorized = FeedSourceGenerator.feedSourceWithCategory(categoryName = "News", id = "categorized")
        val uncategorized = FeedSourceGenerator.feedSource(id = "uncategorized")
        val silent = FeedSourceGenerator.feedSource(id = "silent")

        val everyTag = listOf(
            NotificationTag.forArticle("c-1"),
            NotificationTag.forArticle("u-1"),
            NotificationTag.forFeedSource(categorized.id),
            NotificationTag.forCategory(CATEGORY_ID),
            NotificationTag.forCategory(null),
            NotificationTag.GROUPED,
        )
    }
}
