package com.prof18.feedflow.database

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.shared.test.DatabaseTestBase
import com.prof18.feedflow.shared.test.buildFeedItemsForSource
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import com.prof18.feedflow.shared.test.sampleValue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Clock

class DatabaseHelperPaginationTest : DatabaseTestBase() {

    @Test
    fun `should paginate feed items with 40-item pages`() = runTest {
        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(source)

        val now = Clock.System.now().toEpochMilliseconds()
        val items = buildFeedItemsForSource(source, count = 100, startTimestamp = now)
        database.insertFeedItems(items, lastSyncTimestamp = now)

        val page1 = database.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 40L,
            offset = 0L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        page1.size shouldBe 40

        val page2 = database.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 40L,
            offset = 40L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        page2.size shouldBe 40

        val page3 = database.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 40L,
            offset = 80L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        page3.size shouldBe 20

        val allIds = (page1 + page2 + page3).map { it.url_hash }
        allIds.distinct().size shouldBe 100
    }

    @Test
    fun `should handle empty results`() = runTest {
        val results = database.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 40L,
            offset = 0L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        results.shouldBeEmpty()
    }

    @Test
    fun `should handle offset beyond total items`() = runTest {
        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(source)

        val now = Clock.System.now().toEpochMilliseconds()
        val items = buildFeedItemsForSource(source, count = 10, startTimestamp = now)
        database.insertFeedItems(items, lastSyncTimestamp = now)

        val results = database.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 40L,
            offset = 100L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        results.shouldBeEmpty()
    }
}
