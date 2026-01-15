package com.prof18.feedflow.database

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.shared.test.DatabaseTestBase
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import com.prof18.feedflow.shared.test.sampleValue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class DatabaseHelperCleanupTest : DatabaseTestBase() {

    @Test
    fun `should delete old feed items respecting threshold`() = runTest {
        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(source)

        val now = Clock.System.now().toEpochMilliseconds()
        val threshold = now - 30.days.inWholeMilliseconds
        val oldItems = List(5) { index ->
            buildFeedItem(
                id = "old-$index",
                title = "Old $index",
                pubDateMillis = threshold - (index + 1) * 1000L,
                source = source,
            )
        }
        val recentItems = List(5) { index ->
            buildFeedItem(
                id = "recent-$index",
                title = "Recent $index",
                pubDateMillis = now - 5.days.inWholeMilliseconds - (index * 1000L),
                source = source,
            )
        }
        database.insertFeedItems(oldItems + recentItems, lastSyncTimestamp = now)

        database.deleteOldFeedItems(threshold, FeedFilter.Timeline)

        val remaining = database.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 20L,
            offset = 0L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        remaining.shouldHaveSize(5)
        remaining.all { it.url_hash.startsWith("recent-") } shouldBe true
    }

    @Test
    fun `should keep deleted markers until cleanup`() = runTest {
        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(source)

        val now = Clock.System.now().toEpochMilliseconds()
        val threshold = now - 30.days.inWholeMilliseconds
        val oldItems = List(3) { index ->
            buildFeedItem(
                id = "old-reinsert-$index",
                title = "Old Reinsert $index",
                pubDateMillis = threshold - (index + 1) * 1000L,
                source = source,
            )
        }
        val recentItems = List(2) { index ->
            buildFeedItem(
                id = "recent-reinsert-$index",
                title = "Recent Reinsert $index",
                pubDateMillis = now - (index * 1000L),
                source = source,
            )
        }
        database.insertFeedItems(oldItems + recentItems, lastSyncTimestamp = now)

        database.deleteOldFeedItems(threshold, FeedFilter.Timeline)
        database.insertFeedItems(oldItems, lastSyncTimestamp = now)

        val afterReinsert = database.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 20L,
            offset = 0L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        afterReinsert.shouldHaveSize(2)

        database.cleanupOldDeletedItems(monthsToKeep = 0)
        database.insertFeedItems(oldItems, lastSyncTimestamp = now)

        val afterCleanup = database.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 20L,
            offset = 0L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        afterCleanup.shouldHaveSize(5)
    }

    @Test
    fun `should not delete bookmarked items`() = runTest {
        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(source)

        val now = Clock.System.now().toEpochMilliseconds()
        val threshold = now - 30.days.inWholeMilliseconds
        val bookmarkedItem = buildFeedItem(
            id = "old-bookmarked",
            title = "Old Bookmarked",
            pubDateMillis = threshold - 1000L,
            source = source,
        )
        val normalItem = buildFeedItem(
            id = "old-normal",
            title = "Old Normal",
            pubDateMillis = threshold - 2000L,
            source = source,
        )
        database.insertFeedItems(listOf(bookmarkedItem, normalItem), lastSyncTimestamp = now)
        database.updateBookmarkStatus(FeedItemId(bookmarkedItem.id), isBookmarked = true)

        database.deleteOldFeedItems(threshold, FeedFilter.Timeline)

        val remaining = database.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 20L,
            offset = 0L,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        remaining.shouldHaveSize(1)
        remaining.single().url_hash shouldBe "old-bookmarked"
    }
}
