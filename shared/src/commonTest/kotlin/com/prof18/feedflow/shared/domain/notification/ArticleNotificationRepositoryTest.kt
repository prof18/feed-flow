package com.prof18.feedflow.shared.domain.notification

import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.model.ArticleNotificationPolicy
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

class ArticleNotificationRepositoryTest : KoinTestBase() {

    private val databaseHelper: DatabaseHelper by inject()
    private val repository by lazy { ArticleNotificationRepository(databaseHelper) }
    private val sqlDriver: SqlDriver by inject()

    @Test
    fun `query returns eligible article snapshot and exact acknowledgement leaves later rows pending`() =
        runTest(testDispatcher) {
            seed(ids = listOf("item-1", "item-2", "item-3", "item-4"))
            setItem("item-1", title = null, imageUrl = null, pubDate = null)
            setItem("item-2", title = "  Second  ", imageUrl = "https://img.example/2.jpg", pubDate = 20)
            setItem("item-3", title = null, imageUrl = null, pubDate = 20)
            setItem("item-4", title = "Fourth", imageUrl = "https://img.example/4.jpg", pubDate = 20)
            setItem("item-1", blocked = true)
            setItem("item-2", notified = true)
            setRead("item-3")
            seed(source = disabledSource, ids = listOf("disabled-item"))

            val firstSnapshot = databaseHelper.getArticlesToNotify()
            assertEquals(listOf("item-4"), firstSnapshot.map { it.id })
            assertEquals("Fourth", firstSnapshot.first().title)
            assertEquals("Enabled source", firstSnapshot.first().feedSourceTitle)
            assertEquals("https://img.example/4.jpg", firstSnapshot.first().imageUrl)

            seed(ids = listOf("later-item"))
            databaseHelper.markArticleItemsAsNotified(listOf("item-3"))

            assertEquals(listOf("later-item", "item-4"), databaseHelper.getArticlesToNotify().map { it.id })
            assertTrue(repository.pendingArticles().map { it.id }.contains("item-4"))
        }

    @Test
    fun `pending articles cover the empty and single cases`() = runTest(testDispatcher) {
        assertTrue(repository.pendingArticles().isEmpty())

        seed(eligibleCount = 1)
        assertEquals(listOf("item-0"), repository.pendingArticles().map { it.id })
    }

    @Test
    fun `every pending article comes newest first with null dates last`() = runTest(testDispatcher) {
        val count = 25
        seed(eligibleCount = count)
        setItem("item-0", pubDate = null)

        val pending = repository.pendingArticles()

        assertEquals((1 until count).map { "item-$it" } + "item-0", pending.map { it.id })
        repository.acknowledge(pending.take(1).map { it.id })
        assertEquals(count - 1, repository.pendingArticles().size)
        repository.acknowledge(pending.map { it.id })
        assertTrue(repository.pendingArticles().isEmpty())
    }

    @Test
    fun `a large backlog lists every ID exactly once`() = runTest(testDispatcher) {
        val count = ArticleNotificationPolicy.ACK_CHUNK_SIZE + 3
        seed(eligibleCount = count)

        val ids = repository.pendingArticles().map { it.id }

        assertEquals(count, ids.size)
        assertEquals((0 until count).map { "item-$it" }.toSet(), ids.toSet())
    }

    @Test
    fun `empty input and large selective acknowledgement are safe`() = runTest(testDispatcher) {
        databaseHelper.markArticleItemsAsNotified(emptyList())
        seed(eligibleCount = ArticleNotificationPolicy.ACK_CHUNK_SIZE + 2)
        val ids = databaseHelper.getArticlesToNotify().map { it.id }
        databaseHelper.markArticleItemsAsNotified(ids.take(ArticleNotificationPolicy.ACK_CHUNK_SIZE + 1))

        val pending = databaseHelper.getArticlesToNotify().map { it.id }
        assertEquals(1, pending.size)
        assertFalse(pending.any { it in ids.take(ArticleNotificationPolicy.ACK_CHUNK_SIZE + 1) })
    }

    @Test
    fun `enabling notifications for a feed marks its existing articles as notified`() = runTest(testDispatcher) {
        seed(source = disabledSource, ids = listOf("old-1", "old-2"))

        databaseHelper.updateNotificationEnabledStatus(disabledSource.id, enabled = true)
        assertTrue(databaseHelper.getArticlesToNotify().isEmpty())

        insertItems(disabledSource, listOf("new-1"))
        databaseHelper.updateNotificationEnabledStatus(disabledSource.id, enabled = true)
        assertEquals(listOf("new-1"), databaseHelper.getArticlesToNotify().map { it.id })
    }

    @Test
    fun `enabling all notifications or through feed preferences marks existing articles as notified`() =
        runTest(testDispatcher) {
            seed(source = disabledSource, ids = listOf("old-1"))
            databaseHelper.updateAllNotificationsEnabledStatus(enabled = true)
            assertTrue(databaseHelper.getArticlesToNotify().isEmpty())

            databaseHelper.updateAllNotificationsEnabledStatus(enabled = false)
            insertItems(disabledSource, listOf("old-2"))
            databaseHelper.insertFeedSourcePreference(
                feedSourceId = disabledSource.id,
                articleOpenMode = ArticleOpenMode.DEFAULT,
                isHidden = false,
                isPinned = false,
                isNotificationEnabled = true,
                isHideImagesEnabled = false,
            )
            assertTrue(databaseHelper.getArticlesToNotify().isEmpty())
        }

    @Test
    fun `first sync of a feed marks its back catalog as notified but later syncs stay pending`() =
        runTest(testDispatcher) {
            seed(ids = emptyList())
            insertItems(enabledSource, listOf("back-catalog"), markFirstSyncItemsAsNotified = true)
            assertTrue(databaseHelper.getArticlesToNotify().isEmpty())

            insertItems(
                enabledSource.copy(lastSyncTimestamp = 1),
                listOf("fresh"),
                markFirstSyncItemsAsNotified = true,
            )
            assertEquals(listOf("fresh"), databaseHelper.getArticlesToNotify().map { it.id })
        }

    @Test
    fun `articles a sync account reports as read are no longer pending`() = runTest(testDispatcher) {
        seed(ids = listOf("unread", "read-on-server"))

        databaseHelper.updateFeedItemReadStatus(listOf("unread"))

        assertEquals(listOf("unread"), databaseHelper.getArticlesToNotify().map { it.id })
    }

    @Test
    fun `articles read before the next refresh are not notified`() = runTest(testDispatcher) {
        seed(ids = listOf("read", "unread"))
        setRead("read")

        assertEquals(listOf("unread"), databaseHelper.getArticlesToNotify().map { it.id })
        setRead("unread")
        assertTrue(databaseHelper.getArticlesToNotify().isEmpty())
        assertTrue(databaseHelper.getFeedSourceToNotify().isEmpty())
    }

    private suspend fun seed(
        source: FeedSource = enabledSource,
        ids: List<String>? = null,
        eligibleCount: Int? = null,
    ) {
        databaseHelper.insertFeedSourceWithCategory(source)
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = source.id,
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHidden = false,
            isPinned = false,
            isNotificationEnabled = source.id != disabledSource.id,
            isHideImagesEnabled = false,
        )
        insertItems(source, ids ?: (0 until requireNotNull(eligibleCount)).map { "item-$it" })
    }

    private suspend fun insertItems(
        source: FeedSource,
        itemIds: List<String>,
        markFirstSyncItemsAsNotified: Boolean = false,
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        databaseHelper.insertFeedItems(
            feedItems = itemIds.mapIndexed { index, id ->
                FeedItem(
                    id = id,
                    url = "https://example.com/$id",
                    title = id,
                    subtitle = null,
                    content = null,
                    imageUrl = null,
                    feedSource = source,
                    pubDateMillis = now - index,
                    isRead = false,
                    dateString = null,
                    commentsUrl = null,
                    isBookmarked = false,
                )
            },
            lastSyncTimestamp = 0,
            markFirstSyncItemsAsNotified = markFirstSyncItemsAsNotified,
        )
    }

    private fun setItem(
        id: String,
        title: String? = id,
        imageUrl: String? = null,
        pubDate: Long? = 1,
        blocked: Boolean = false,
        notified: Boolean = false,
    ) {
        sqlDriver.execute(
            identifier = null,
            sql = """
                UPDATE feed_item
                SET title = ?, image_url = ?, pub_date = ?, is_blocked = ?, notification_sent = ?
                WHERE url_hash = ?
            """.trimIndent(),
            parameters = 6,
        ) {
            bindString(0, title)
            bindString(1, imageUrl)
            bindLong(2, pubDate)
            bindLong(3, if (blocked) 1 else 0)
            bindLong(4, if (notified) 1 else 0)
            bindString(5, id)
        }.value
    }

    private fun setRead(id: String) {
        sqlDriver.execute(null, "UPDATE feed_item SET is_read = 1 WHERE url_hash = ?", 1) {
            bindString(0, id)
        }.value
    }

    private companion object {
        val enabledSource = FeedSourceGenerator.feedSource(id = "enabled").copy(title = "Enabled source")
        val disabledSource = FeedSourceGenerator.feedSource(id = "disabled")
    }
}
