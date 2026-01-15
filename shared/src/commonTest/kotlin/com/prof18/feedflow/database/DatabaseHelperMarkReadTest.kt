package com.prof18.feedflow.database

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.shared.test.DatabaseTestBase
import com.prof18.feedflow.shared.test.buildFeedItemsForSource
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import com.prof18.feedflow.shared.test.sampleValue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Clock

class DatabaseHelperMarkReadTest : DatabaseTestBase() {

    @Test
    fun `should update read status for a single item`() = runTest {
        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(source)

        val now = Clock.System.now().toEpochMilliseconds()
        val item = buildFeedItemsForSource(source, count = 1, startTimestamp = now).first()
        database.insertFeedItems(listOf(item), lastSyncTimestamp = now)

        database.updateReadStatus(FeedItemId(item.id), isRead = true)

        val results = database.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10L,
            offset = 0L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        results.single { it.url_hash == item.id }.is_read shouldBe true
    }

    @Test
    fun `should mark multiple items as read`() = runTest {
        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(source)

        val now = Clock.System.now().toEpochMilliseconds()
        val items = buildFeedItemsForSource(source, count = 3, startTimestamp = now)
        database.insertFeedItems(items, lastSyncTimestamp = now)

        database.markAsRead(items.map { FeedItemId(it.id) })

        val results = database.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10L,
            offset = 0L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        results.all { it.is_read } shouldBe true
    }

    @Test
    fun `should mark all items above as read`() = runTest {
        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(source)

        val now = Clock.System.now().toEpochMilliseconds()
        val items = buildFeedItemsForSource(source, count = 5, startTimestamp = now)
        database.insertFeedItems(items, lastSyncTimestamp = now)

        val targetItem = items[2]
        database.markAllAboveAsRead(targetItem.id, FeedFilter.Timeline)

        val results = database.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10L,
            offset = 0L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        ).associate { it.url_hash to it.is_read }

        results[items[0].id] shouldBe true
        results[items[1].id] shouldBe true
        results[items[2].id] shouldBe true
        results[items[3].id] shouldBe false
        results[items[4].id] shouldBe false
    }

    @Test
    fun `should mark all items below as read`() = runTest {
        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(source)

        val now = Clock.System.now().toEpochMilliseconds()
        val items = buildFeedItemsForSource(source, count = 5, startTimestamp = now)
        database.insertFeedItems(items, lastSyncTimestamp = now)

        val targetItem = items[2]
        database.markAllBelowAsRead(targetItem.id, FeedFilter.Timeline)

        val results = database.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10L,
            offset = 0L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        ).associate { it.url_hash to it.is_read }

        results[items[0].id] shouldBe false
        results[items[1].id] shouldBe false
        results[items[2].id] shouldBe true
        results[items[3].id] shouldBe true
        results[items[4].id] shouldBe true
    }
}
