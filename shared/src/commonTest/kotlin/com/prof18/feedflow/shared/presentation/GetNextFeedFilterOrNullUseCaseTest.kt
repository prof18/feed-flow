package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.CategoryGenerator
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.getValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class GetNextFeedFilterOrNullUseCaseTest : KoinTestBase() {

    private val subject: GetNextFeedFilterOrNullUseCase by inject()
    private val databaseHelper: DatabaseHelper by inject()

    @Test
    fun `should propose next feed source with unread items`() = runTest {
        val unreadSourceId = "unread_source_id"
        val currentSourceId = "current_source_id"

        val currentFilterSource = createFeedSource(
            id = currentSourceId,
            category = CategoryGenerator.category(id = "category_id"),
        )

        val unreadFilterSource = createFeedSource(
            id = unreadSourceId,
            category = CategoryGenerator.category(id = "category_id"),
        )

        databaseHelper.insertFeedSourceWithCategory(unreadFilterSource)
        databaseHelper.insertFeedSourceWithCategory(currentFilterSource)
        val items = listOf(
            generateFeedItem(
                id = "feed_id1",
                feedSource = unreadFilterSource,
                read = false,
            ),
            generateFeedItem(
                id = "feed_id2",
                feedSource = unreadFilterSource,
                read = false,
            ),
            generateFeedItem(
                id = "feed_id3",
                feedSource = currentFilterSource,
                read = false,
            ),
            generateFeedItem(
                id = "feed_id4",
                feedSource = currentFilterSource,
                read = false,
            ),
        )
        databaseHelper.insertFeedItems(items, 0)
        databaseHelper.markAsRead(items.filter { it.isRead }.map { FeedItemId(it.id) })

        val result: FeedFilter? = subject.invoke(FeedFilter.Source(currentFilterSource))

        assertIs<FeedFilter.Source>(result, "Should return a source")
        assertEquals(result.feedSource.id, unreadSourceId, "Should return next unread source")
    }

    @Test
    fun `should return null if next feed has no unread items`() = runTest {
        val readSourceId = "read_source_id"
        val currentSourceId = "current_source_id"

        val currentFilterSource = createFeedSource(
            id = currentSourceId,
            category = CategoryGenerator.category(id = "category_id"),
        )

        val readFilterSource = createFeedSource(
            id = readSourceId,
            category = CategoryGenerator.category(id = "category_id"),
        )

        databaseHelper.insertFeedSourceWithCategory(currentFilterSource)
        databaseHelper.insertFeedSourceWithCategory(readFilterSource)
        val items = listOf(
            generateFeedItem(
                id = "feed_id1",
                feedSource = readFilterSource,
                read = true,
            ),
            generateFeedItem(
                id = "feed_id2",
                feedSource = readFilterSource,
                read = true,
            ),
            generateFeedItem(
                id = "feed_id3",
                feedSource = currentFilterSource,
                read = false,
            ),
            generateFeedItem(
                id = "feed_id4",
                feedSource = currentFilterSource,
                read = false,
            ),
        )

        databaseHelper.insertFeedItems(items, 0)
        databaseHelper.markAsRead(items.filter { it.isRead }.map { FeedItemId(it.id) })

        val result: FeedFilter? = subject.invoke(FeedFilter.Source(currentFilterSource))
        assertNull(result, "Should return no feed")
    }

    @Test
    fun `should propose next category with unread items`() = runTest {
        val unreadSourceId = "unread_source_id"
        val currentSourceId = "current_source_id"

        val currentCategory = CategoryGenerator.category(
            id = "current_category",
            title = "current_category",
        )
        val unreadCategory = CategoryGenerator.category(
            id = "unread_category",
            title = "unread_category",
        )

        val currentFilterSource = createFeedSource(
            id = currentSourceId,
            category = currentCategory,
        )

        val unreadFilterSource = createFeedSource(
            id = unreadSourceId,
            category = unreadCategory,
        )

        databaseHelper.insertFeedSourceWithCategory(unreadFilterSource)
        databaseHelper.insertFeedSourceWithCategory(currentFilterSource)
        val items = listOf(
            generateFeedItem(
                id = "feed_id1",
                feedSource = unreadFilterSource,
                read = false,
            ),
            generateFeedItem(
                id = "feed_id2",
                feedSource = unreadFilterSource,
                read = false,
            ),
            generateFeedItem(
                id = "feed_id3",
                feedSource = currentFilterSource,
                read = false,
            ),
            generateFeedItem(
                id = "feed_id4",
                feedSource = currentFilterSource,
                read = false,
            ),
        )
        databaseHelper.insertFeedItems(items, 0)
        databaseHelper.markAsRead(items.filter { it.isRead }.map { FeedItemId(it.id) })

        val result: FeedFilter? = subject.invoke(FeedFilter.Category(currentCategory))

        assertIs<FeedFilter.Category>(result, "Should return a category")
        assertEquals(result.feedCategory, unreadCategory, "Should return next unread category")
    }

    @Test
    fun `should return null if next category has no unread items`() = runTest {
        val readSourceId = "read_source_id"
        val currentSourceId = "current_source_id"

        val currentCategory = CategoryGenerator.category(
            id = "current_category",
            title = "current_category",
        )
        val readCategory = CategoryGenerator.category(
            id = "unread_category",
            title = "read_category",
        )

        val currentFilterSource = createFeedSource(
            id = currentSourceId,
            category = currentCategory,
        )

        val readFilterSource = createFeedSource(
            id = readSourceId,
            category = readCategory,
        )

        databaseHelper.insertFeedSourceWithCategory(currentFilterSource)
        databaseHelper.insertFeedSourceWithCategory(readFilterSource)
        val items = listOf(
            generateFeedItem(
                id = "feed_id1",
                feedSource = readFilterSource,
                read = true,
            ),
            generateFeedItem(
                id = "feed_id2",
                feedSource = readFilterSource,
                read = true,
            ),
            generateFeedItem(
                id = "feed_id3",
                feedSource = currentFilterSource,
                read = false,
            ),
            generateFeedItem(
                id = "feed_id4",
                feedSource = currentFilterSource,
                read = false,
            ),
        )

        databaseHelper.insertFeedItems(items, 0)
        databaseHelper.markAsRead(items.filter { it.isRead }.map { FeedItemId(it.id) })

        val result: FeedFilter? = subject.invoke(FeedFilter.Category(currentCategory))
        assertNull(result, "Should return no category")
    }

    fun generateFeedItem(id: String, feedSource: FeedSource, read: Boolean) =
        FeedItemGenerator.feedItem(id = id, title = id, feedSource = feedSource, isRead = read)

    private fun createFeedSource(
        id: String,
        category: FeedSourceCategory,
    ): FeedSource = FeedSourceGenerator.feedSource(
        id = id,
        title = id,
        category = category,
    )
}
