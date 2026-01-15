package com.prof18.feedflow.database

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.shared.test.DatabaseTestBase
import com.prof18.feedflow.shared.test.buildFeedItemsForSource
import com.prof18.feedflow.shared.test.generators.CategoryGenerator
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import com.prof18.feedflow.shared.test.sampleValue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Clock

class DatabaseHelperFilterTest : DatabaseTestBase() {

    @Test
    fun `should filter read and unread items`() = runTest {
        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(source)

        val now = Clock.System.now().toEpochMilliseconds()
        val items = buildFeedItemsForSource(source, count = 6, startTimestamp = now)
        database.insertFeedItems(items, lastSyncTimestamp = now)

        val readItems = items.take(3).map { FeedItemId(it.id) }
        database.updateReadStatus(readItems, isRead = true)

        val unreadResults = database.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 20L,
            offset = 0L,
            showReadItems = false,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        unreadResults.size shouldBe 3
        unreadResults.all { !it.is_read } shouldBe true

        val readResults = database.getFeedItems(
            feedFilter = FeedFilter.Read,
            pageSize = 20L,
            offset = 0L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        readResults.size shouldBe 3
        readResults.all { it.is_read } shouldBe true
    }

    @Test
    fun `should filter by bookmarked status`() = runTest {
        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(source)

        val now = Clock.System.now().toEpochMilliseconds()
        val items = buildFeedItemsForSource(source, count = 10, startTimestamp = now)
        database.insertFeedItems(items, lastSyncTimestamp = now)

        val bookmarkedItems = items.take(3).map { FeedItemId(it.id) }
        bookmarkedItems.forEach { database.updateBookmarkStatus(it, isBookmarked = true) }

        val results = database.getFeedItems(
            feedFilter = FeedFilter.Bookmarks,
            pageSize = 20L,
            offset = 0L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        results.size shouldBe 3
        results.all { it.is_bookmarked } shouldBe true
    }

    @Test
    fun `should filter by category`() = runTest {
        val category = CategoryGenerator.categoryArb.sampleValue()
        val sourceInCategory = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = category)
        val sourceOutsideCategory = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(sourceInCategory)
        database.insertFeedSourceWithCategory(sourceOutsideCategory)

        val now = Clock.System.now().toEpochMilliseconds()
        val itemsInCategory = buildFeedItemsForSource(sourceInCategory, count = 5, startTimestamp = now)
        val itemsOutside = buildFeedItemsForSource(sourceOutsideCategory, count = 4, startTimestamp = now - 10000L)
        database.insertFeedItems(itemsInCategory + itemsOutside, lastSyncTimestamp = now)

        val results = database.getFeedItems(
            feedFilter = FeedFilter.Category(category),
            pageSize = 20L,
            offset = 0L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        results.size shouldBe 5
        results.all { it.feed_source_id == sourceInCategory.id } shouldBe true
    }

    @Test
    fun `should filter by feed source`() = runTest {
        val source1 = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        val source2 = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(source1)
        database.insertFeedSourceWithCategory(source2)

        val now = Clock.System.now().toEpochMilliseconds()
        val itemsSource1 = buildFeedItemsForSource(source1, count = 3, startTimestamp = now)
        val itemsSource2 = buildFeedItemsForSource(source2, count = 2, startTimestamp = now - 5000L)
        database.insertFeedItems(itemsSource1 + itemsSource2, lastSyncTimestamp = now)

        val results = database.getFeedItems(
            feedFilter = FeedFilter.Source(source1),
            pageSize = 20L,
            offset = 0L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        results.size shouldBe 3
        results.all { it.feed_source_id == source1.id } shouldBe true
    }

    @Test
    fun `should filter uncategorized items`() = runTest {
        val category = CategoryGenerator.categoryArb.sampleValue()
        val categorizedSource = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = category)
        val uncategorizedSource = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(categorizedSource)
        database.insertFeedSourceWithCategory(uncategorizedSource)

        val now = Clock.System.now().toEpochMilliseconds()
        val categorizedItems = buildFeedItemsForSource(categorizedSource, count = 3, startTimestamp = now)
        val uncategorizedItems = buildFeedItemsForSource(uncategorizedSource, count = 4, startTimestamp = now - 10000L)
        database.insertFeedItems(categorizedItems + uncategorizedItems, lastSyncTimestamp = now)

        val results = database.getFeedItems(
            feedFilter = FeedFilter.Uncategorized,
            pageSize = 20L,
            offset = 0L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        results.size shouldBe 4
        results.all { it.feed_source_id == uncategorizedSource.id } shouldBe true
    }
}
