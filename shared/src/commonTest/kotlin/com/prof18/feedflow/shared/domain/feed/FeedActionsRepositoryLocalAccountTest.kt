package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FeedActionsRepositoryLocalAccountTest : KoinTestBase() {

    private val feedActionsRepository: FeedActionsRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository by inject()

    @Test
    fun `markAllAboveAsRead with NEWEST_FIRST should mark newer items as read`() = runTest(testDispatcher) {
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)

        // Items ordered by pub_date DESC (newest first):
        // Article 1 (10000) - above target
        // Article 2 (9000)  - above target
        // Article 3 (8000)  - TARGET
        // Article 4 (7000)  - below target
        // Article 5 (6000)  - below target
        val feedItems = listOf(
            buildFeedItem("item1", "Article 1", 10000L, feedSource),
            buildFeedItem("item2", "Article 2", 9000L, feedSource),
            buildFeedItem("item3", "Article 3", 8000L, feedSource),
            buildFeedItem("item4", "Article 4", 7000L, feedSource),
            buildFeedItem("item5", "Article 5", 6000L, feedSource),
        )
        databaseHelper.insertFeedItems(feedItems, lastSyncTimestamp = 0)

        feedAppearanceSettingsRepository.setFeedOrder(FeedOrder.NEWEST_FIRST)
        feedStateRepository.updateFeedFilter(FeedFilter.Timeline)
        feedStateRepository.getFeeds()
        advanceUntilIdle()

        val targetItem = feedItems[2] // Article 3
        feedActionsRepository.markAllAboveAsRead(targetItem.id)
        advanceUntilIdle()

        val updatedItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )

        // With NEWEST_FIRST, "above" means newer items (pub_date >= target)
        // Articles 1, 2, 3 should be read (index 0, 1, 2)
        // Articles 4, 5 should be unread (index 3, 4)
        val item1 = updatedItems.find { it.url_hash == "item1" }
        val item2 = updatedItems.find { it.url_hash == "item2" }
        val item3 = updatedItems.find { it.url_hash == "item3" }
        val item4 = updatedItems.find { it.url_hash == "item4" }
        val item5 = updatedItems.find { it.url_hash == "item5" }

        assertNotNull(item1)
        assertNotNull(item2)
        assertNotNull(item3)
        assertNotNull(item4)
        assertNotNull(item5)

        assertTrue(item1.is_read, "Article 1 (above target) should be read")
        assertTrue(item2.is_read, "Article 2 (above target) should be read")
        assertTrue(item3.is_read, "Article 3 (target) should be read")
        assertFalse(item4.is_read, "Article 4 (below target) should be unread")
        assertFalse(item5.is_read, "Article 5 (below target) should be unread")
    }

    @Test
    fun `markAllAboveAsRead with OLDEST_FIRST should mark older items as read`() = runTest(testDispatcher) {
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)

        // Items ordered by pub_date ASC (oldest first):
        // Article 5 (6000)  - above target (visually, but OLDER)
        // Article 4 (7000)  - above target (visually, but OLDER)
        // Article 3 (8000)  - TARGET
        // Article 2 (9000)  - below target (visually, but NEWER)
        // Article 1 (10000) - below target (visually, but NEWER)
        val feedItems = listOf(
            buildFeedItem("item1", "Article 1", 10000L, feedSource),
            buildFeedItem("item2", "Article 2", 9000L, feedSource),
            buildFeedItem("item3", "Article 3", 8000L, feedSource),
            buildFeedItem("item4", "Article 4", 7000L, feedSource),
            buildFeedItem("item5", "Article 5", 6000L, feedSource),
        )
        databaseHelper.insertFeedItems(feedItems, lastSyncTimestamp = 0)

        feedAppearanceSettingsRepository.setFeedOrder(FeedOrder.OLDEST_FIRST)
        feedStateRepository.updateFeedFilter(FeedFilter.Timeline)
        feedStateRepository.getFeeds()
        advanceUntilIdle()

        val targetItem = feedItems[2] // Article 3
        feedActionsRepository.markAllAboveAsRead(targetItem.id)
        advanceUntilIdle()

        val updatedItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.OLDEST_FIRST,
        )

        // With OLDEST_FIRST, "above" (visually) means OLDER items (pub_date <= target)
        // Articles 5, 4, 3 should be read (older or same as target)
        // Articles 2, 1 should be unread (newer than target)
        val item1 = updatedItems.find { it.url_hash == "item1" }
        val item2 = updatedItems.find { it.url_hash == "item2" }
        val item3 = updatedItems.find { it.url_hash == "item3" }
        val item4 = updatedItems.find { it.url_hash == "item4" }
        val item5 = updatedItems.find { it.url_hash == "item5" }

        assertNotNull(item1)
        assertNotNull(item2)
        assertNotNull(item3)
        assertNotNull(item4)
        assertNotNull(item5)

        assertFalse(item1.is_read, "Article 1 (below target in OLDEST_FIRST) should be unread")
        assertFalse(item2.is_read, "Article 2 (below target in OLDEST_FIRST) should be unread")
        assertTrue(item3.is_read, "Article 3 (target) should be read")
        assertTrue(item4.is_read, "Article 4 (above target in OLDEST_FIRST) should be read")
        assertTrue(item5.is_read, "Article 5 (above target in OLDEST_FIRST) should be read")
    }

    @Test
    fun `markAllBelowAsRead with NEWEST_FIRST should mark older items as read`() = runTest(testDispatcher) {
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)

        val feedItems = listOf(
            buildFeedItem("item1", "Article 1", 10000L, feedSource),
            buildFeedItem("item2", "Article 2", 9000L, feedSource),
            buildFeedItem("item3", "Article 3", 8000L, feedSource),
            buildFeedItem("item4", "Article 4", 7000L, feedSource),
            buildFeedItem("item5", "Article 5", 6000L, feedSource),
        )
        databaseHelper.insertFeedItems(feedItems, lastSyncTimestamp = 0)

        feedAppearanceSettingsRepository.setFeedOrder(FeedOrder.NEWEST_FIRST)
        feedStateRepository.updateFeedFilter(FeedFilter.Timeline)
        feedStateRepository.getFeeds()
        advanceUntilIdle()

        val targetItem = feedItems[2] // Article 3
        feedActionsRepository.markAllBelowAsRead(targetItem.id)
        advanceUntilIdle()

        val updatedItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )

        // With NEWEST_FIRST, "below" means older items (pub_date <= target)
        val item1 = updatedItems.find { it.url_hash == "item1" }
        val item2 = updatedItems.find { it.url_hash == "item2" }
        val item3 = updatedItems.find { it.url_hash == "item3" }
        val item4 = updatedItems.find { it.url_hash == "item4" }
        val item5 = updatedItems.find { it.url_hash == "item5" }

        assertNotNull(item1)
        assertNotNull(item2)
        assertNotNull(item3)
        assertNotNull(item4)
        assertNotNull(item5)

        assertFalse(item1.is_read, "Article 1 (above target) should be unread")
        assertFalse(item2.is_read, "Article 2 (above target) should be unread")
        assertTrue(item3.is_read, "Article 3 (target) should be read")
        assertTrue(item4.is_read, "Article 4 (below target) should be read")
        assertTrue(item5.is_read, "Article 5 (below target) should be read")
    }

    @Test
    fun `markAllBelowAsRead with OLDEST_FIRST should mark newer items as read`() = runTest(testDispatcher) {
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)

        val feedItems = listOf(
            buildFeedItem("item1", "Article 1", 10000L, feedSource),
            buildFeedItem("item2", "Article 2", 9000L, feedSource),
            buildFeedItem("item3", "Article 3", 8000L, feedSource),
            buildFeedItem("item4", "Article 4", 7000L, feedSource),
            buildFeedItem("item5", "Article 5", 6000L, feedSource),
        )
        databaseHelper.insertFeedItems(feedItems, lastSyncTimestamp = 0)

        feedAppearanceSettingsRepository.setFeedOrder(FeedOrder.OLDEST_FIRST)
        feedStateRepository.updateFeedFilter(FeedFilter.Timeline)
        feedStateRepository.getFeeds()
        advanceUntilIdle()

        val targetItem = feedItems[2] // Article 3
        feedActionsRepository.markAllBelowAsRead(targetItem.id)
        advanceUntilIdle()

        val updatedItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.OLDEST_FIRST,
        )

        // With OLDEST_FIRST, "below" (visually) means NEWER items (pub_date >= target)
        val item1 = updatedItems.find { it.url_hash == "item1" }
        val item2 = updatedItems.find { it.url_hash == "item2" }
        val item3 = updatedItems.find { it.url_hash == "item3" }
        val item4 = updatedItems.find { it.url_hash == "item4" }
        val item5 = updatedItems.find { it.url_hash == "item5" }

        assertNotNull(item1)
        assertNotNull(item2)
        assertNotNull(item3)
        assertNotNull(item4)
        assertNotNull(item5)

        assertTrue(item1.is_read, "Article 1 (below target in OLDEST_FIRST) should be read")
        assertTrue(item2.is_read, "Article 2 (below target in OLDEST_FIRST) should be read")
        assertTrue(item3.is_read, "Article 3 (target) should be read")
        assertFalse(item4.is_read, "Article 4 (above target in OLDEST_FIRST) should be unread")
        assertFalse(item5.is_read, "Article 5 (above target in OLDEST_FIRST) should be unread")
    }

    private fun createFeedSource(
        id: String,
        title: String,
        category: FeedSourceCategory? = null,
    ): FeedSource = FeedSource(
        id = id,
        url = "https://example.com/$id/feed.xml",
        title = title,
        category = category,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = "https://example.com/$id",
        fetchFailed = false,
        linkOpeningPreference = LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
    )
}
