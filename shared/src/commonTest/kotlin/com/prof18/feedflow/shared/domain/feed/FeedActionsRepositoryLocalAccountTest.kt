package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.SyncAccounts
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FeedActionsRepositoryLocalAccountTest : KoinTestBase() {

    private val feedActionsRepository: FeedActionsRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository by inject()

    @Test
    fun `deleteFeedSource removes pending read status actions for deleted source`() = runTest(testDispatcher) {
        val deletedSource = createFeedSource("source-1", "Deleted Feed")
        val keptSource = createFeedSource("source-2", "Kept Feed")
        databaseHelper.insertFeedSourceWithCategory(deletedSource)
        databaseHelper.insertFeedSourceWithCategory(keptSource)
        val deletedItem = buildFeedItem("item-1", "Article 1", 10000L, deletedSource)
        val keptItem = buildFeedItem("item-2", "Article 2", 9000L, keptSource)
        databaseHelper.insertFeedItems(listOf(deletedItem, keptItem), lastSyncTimestamp = 0)
        databaseHelper.upsertReadStatusPendingActions(
            feedItemIds = listOf(FeedItemId(deletedItem.id), FeedItemId(keptItem.id)),
            isRead = true,
            syncAccount = SyncAccounts.FEEDBIN.name,
        )

        databaseHelper.deleteFeedSource(deletedSource.id)

        val pendingActions = databaseHelper.getReadStatusPendingActions(SyncAccounts.FEEDBIN.name)
        assertEquals(listOf(keptItem.id), pendingActions.map { it.feed_item_id })
    }

    @Test
    fun `deleteOldFeedItems removes pending read status actions for deleted items`() = runTest(testDispatcher) {
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)
        val oldItem = buildFeedItem("item-1", "Old Article", 1000L, feedSource)
        val recentItem = buildFeedItem("item-2", "Recent Article", 10000L, feedSource)
        databaseHelper.insertFeedItems(listOf(oldItem, recentItem), lastSyncTimestamp = 0)
        databaseHelper.upsertReadStatusPendingActions(
            feedItemIds = listOf(FeedItemId(oldItem.id), FeedItemId(recentItem.id)),
            isRead = true,
            syncAccount = SyncAccounts.FEEDBIN.name,
        )

        databaseHelper.deleteOldFeedItems(
            timeThreshold = 5000L,
            feedFilter = FeedFilter.Timeline,
        )

        val pendingActions = databaseHelper.getReadStatusPendingActions(SyncAccounts.FEEDBIN.name)
        assertEquals(listOf(recentItem.id), pendingActions.map { it.feed_item_id })
    }

    @Test
    fun `deleteAll removes pending read status actions`() = runTest(testDispatcher) {
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)
        val feedItem = buildFeedItem("item-1", "Article 1", 10000L, feedSource)
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)
        databaseHelper.upsertReadStatusPendingActions(
            feedItemIds = listOf(FeedItemId(feedItem.id)),
            isRead = true,
            syncAccount = SyncAccounts.FEEDBIN.name,
        )

        databaseHelper.deleteAll()

        assertEquals(0, databaseHelper.countReadStatusPendingActions())
    }

    @Test
    fun `markAsRead marks pending notifications as sent`() = runTest(testDispatcher) {
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)
        databaseHelper.updateNotificationEnabledStatus(feedSource.id, true)

        val feedItem = buildFeedItem("item1", "Article 1", 10000L, feedSource)
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)

        assertTrue(databaseHelper.getFeedSourceToNotify().isNotEmpty())

        feedActionsRepository.markAsRead(hashSetOf(FeedItemId(feedItem.id)))
        advanceUntilIdle()

        val updatedItem = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        ).first { it.url_hash == feedItem.id }

        assertTrue(updatedItem.is_read)
        assertTrue(updatedItem.notification_sent)
        assertTrue(databaseHelper.getFeedSourceToNotify().isEmpty())
    }

    @Test
    fun `markAllAboveAsRead with NEWEST_FIRST uses url hash tie breaker for same pub date`() =
        runTest(testDispatcher) {
            val feedSource = createFeedSource("source-1", "Test Feed")
            databaseHelper.insertFeedSourceWithCategory(feedSource)
            val feedItems = listOf(
                buildFeedItem("a", "Article A", 10000L, feedSource),
                buildFeedItem("b", "Article B", 10000L, feedSource),
                buildFeedItem("c", "Article C", 10000L, feedSource),
                buildFeedItem("d", "Article D", 9000L, feedSource),
            )
            databaseHelper.insertFeedItems(feedItems, lastSyncTimestamp = 0)

            feedAppearanceSettingsRepository.setFeedOrder(FeedOrder.NEWEST_FIRST)
            feedStateRepository.updateFeedFilter(FeedFilter.Timeline)
            advanceUntilIdle()

            feedActionsRepository.markAllAboveAsRead("b")
            advanceUntilIdle()

            val readIds = databaseHelper.getFeedItems(
                feedFilter = FeedFilter.Timeline,
                pageSize = 10,
                offset = 0,
                showReadItems = true,
                sortOrder = FeedOrder.NEWEST_FIRST,
            ).filter { it.is_read }.map { it.url_hash }.toSet()

            assertEquals(setOf("b", "c"), readIds)
        }

    @Test
    fun `markAllAboveAsRead with OLDEST_FIRST uses url hash tie breaker for same pub date`() =
        runTest(testDispatcher) {
            val feedSource = createFeedSource("source-1", "Test Feed")
            databaseHelper.insertFeedSourceWithCategory(feedSource)
            val feedItems = listOf(
                buildFeedItem("a", "Article A", 10000L, feedSource),
                buildFeedItem("b", "Article B", 10000L, feedSource),
                buildFeedItem("c", "Article C", 10000L, feedSource),
                buildFeedItem("d", "Article D", 11000L, feedSource),
            )
            databaseHelper.insertFeedItems(feedItems, lastSyncTimestamp = 0)

            feedAppearanceSettingsRepository.setFeedOrder(FeedOrder.OLDEST_FIRST)
            feedStateRepository.updateFeedFilter(FeedFilter.Timeline)
            advanceUntilIdle()

            feedActionsRepository.markAllAboveAsRead("b")
            advanceUntilIdle()

            val readIds = databaseHelper.getFeedItems(
                feedFilter = FeedFilter.Timeline,
                pageSize = 10,
                offset = 0,
                showReadItems = true,
                sortOrder = FeedOrder.OLDEST_FIRST,
            ).filter { it.is_read }.map { it.url_hash }.toSet()

            assertEquals(setOf("a", "b"), readIds)
        }

    @Test
    fun `markAllAboveAndBelowAsRead ignore missing target item`() = runTest(testDispatcher) {
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)
        val feedItems = listOf(
            buildFeedItem("item1", "Article 1", 10000L, feedSource),
            buildFeedItem("item2", "Article 2", 9000L, feedSource),
        )
        databaseHelper.insertFeedItems(feedItems, lastSyncTimestamp = 0)

        feedAppearanceSettingsRepository.setFeedOrder(FeedOrder.NEWEST_FIRST)
        feedStateRepository.updateFeedFilter(FeedFilter.Timeline)
        advanceUntilIdle()

        feedActionsRepository.markAllAboveAsRead("missing-item")
        feedActionsRepository.markAllBelowAsRead("missing-item")
        advanceUntilIdle()

        val readIds = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        ).filter { it.is_read }.map { it.url_hash }.toSet()

        assertEquals(emptySet(), readIds)
    }

    @Test
    fun `markAllCurrentFeedAsRead marks pending notifications as sent`() = runTest(testDispatcher) {
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)
        databaseHelper.updateNotificationEnabledStatus(feedSource.id, true)

        val feedItems = listOf(
            buildFeedItem("item1", "Article 1", 10000L, feedSource),
            buildFeedItem("item2", "Article 2", 9000L, feedSource),
        )
        databaseHelper.insertFeedItems(feedItems, lastSyncTimestamp = 0)
        feedStateRepository.updateFeedSourceFilter(feedSource.id)
        advanceUntilIdle()

        assertTrue(databaseHelper.getFeedSourceToNotify().isNotEmpty())

        feedActionsRepository.markAllCurrentFeedAsRead()
        advanceUntilIdle()

        val updatedItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Source(feedSource),
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )

        updatedItems.forEach { item ->
            assertTrue(item.is_read)
            assertTrue(item.notification_sent)
        }
        assertTrue(databaseHelper.getFeedSourceToNotify().isEmpty())
    }

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
