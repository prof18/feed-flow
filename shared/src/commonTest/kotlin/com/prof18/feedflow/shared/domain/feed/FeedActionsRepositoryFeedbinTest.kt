package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.feedbin.configureFeedbinMocks
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import com.prof18.feedflow.shared.test.koin.TestModules
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FeedActionsRepositoryFeedbinTest : KoinTestBase() {

    private val feedActionsRepository: FeedActionsRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val feedStateRepository: FeedStateRepository by inject()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + getFeedSyncTestModules(
            feedbinBaseURL = "https://api.feedbin.com/",
            feedbinConfig = {
                configureFeedbinMocks()
            },
        )

    fun setupFeedbinAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.FEEDBIN)
        settings.setSyncUsername("testuser")
        settings.setSyncPwd("testpassword")
    }

    @Test
    fun `markAsRead should call feedbinRepository and update state`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)

        val feedItems = listOf(
            buildFeedItem("5031084432", "Article 1", 10000L, feedSource),
            buildFeedItem("5050623384", "Article 2", 9000L, feedSource),
            buildFeedItem("5050623385", "Article 3", 8000L, feedSource),
        )
        databaseHelper.insertFeedItems(feedItems, lastSyncTimestamp = 0)
        feedStateRepository.getFeeds()
        advanceUntilIdle()

        val itemIds = feedItems.map { FeedItemId(it.id) }.toHashSet()

        feedActionsRepository.markAsRead(itemIds)
        advanceUntilIdle()

        val updatedItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        itemIds.forEach { itemId ->
            val item = updatedItems.find { it.url_hash == itemId.id }
            assertNotNull(item, "Item ${itemId.id} should exist")
            assertTrue(item.is_read, "Item ${itemId.id} should be marked as read")
        }
    }

    @Test
    fun `markAllAboveAsRead should mark items above target as read`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)

        val feedItems = listOf(
            buildFeedItem("5031084432", "Article 1", 10000L, feedSource),
            buildFeedItem("5050623384", "Article 2", 9000L, feedSource),
            buildFeedItem("5050623385", "Article 3", 8000L, feedSource),
            buildFeedItem("5058157281", "Article 4", 7000L, feedSource),
            buildFeedItem("5058832279", "Article 5", 6000L, feedSource),
        )
        databaseHelper.insertFeedItems(feedItems, lastSyncTimestamp = 0)
        feedStateRepository.getFeeds()
        advanceUntilIdle()

        val targetItem = feedItems[2]
        feedStateRepository.updateFeedFilter(FeedFilter.Timeline)
        advanceUntilIdle()

        feedActionsRepository.markAllAboveAsRead(targetItem.id)
        advanceUntilIdle()

        val updatedItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        val targetIndex = feedItems.indexOfFirst { it.id == targetItem.id }
        feedItems.forEachIndexed { index, feedItem ->
            val item = updatedItems.find { it.url_hash == feedItem.id }
            assertNotNull(item, "Item ${feedItem.id} should exist")
            if (index <= targetIndex) {
                assertTrue(item.is_read, "Item above or equal to target should be marked as read: ${feedItem.id}")
            } else {
                assertFalse(item.is_read, "Item below target should not be marked as read: ${feedItem.id}")
            }
        }
    }

    @Test
    fun `markAllBelowAsRead should mark items below target as read`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)

        val feedItems = listOf(
            buildFeedItem("5031084432", "Article 1", 10000L, feedSource),
            buildFeedItem("5050623384", "Article 2", 9000L, feedSource),
            buildFeedItem("5050623385", "Article 3", 8000L, feedSource),
            buildFeedItem("5058157281", "Article 4", 7000L, feedSource),
            buildFeedItem("5058832279", "Article 5", 6000L, feedSource),
        )
        databaseHelper.insertFeedItems(feedItems, lastSyncTimestamp = 0)
        feedStateRepository.getFeeds()
        advanceUntilIdle()

        val targetItem = feedItems[2]
        feedStateRepository.updateFeedFilter(FeedFilter.Timeline)
        advanceUntilIdle()

        feedActionsRepository.markAllBelowAsRead(targetItem.id)
        advanceUntilIdle()

        val updatedItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        val targetIndex = feedItems.indexOfFirst { it.id == targetItem.id }
        feedItems.forEachIndexed { index, feedItem ->
            val item = updatedItems.find { it.url_hash == feedItem.id }
            assertNotNull(item, "Item ${feedItem.id} should exist")
            if (index >= targetIndex) {
                assertTrue(item.is_read, "Item below or equal to target should be marked as read: ${feedItem.id}")
            } else {
                assertFalse(item.is_read, "Item above target should not be marked as read: ${feedItem.id}")
            }
        }
    }

    @Test
    fun `markAllCurrentFeedAsRead should mark all items in current feed as read`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)

        val feedItems = listOf(
            buildFeedItem("5031084432", "Article 1", 10000L, feedSource),
            buildFeedItem("5050623384", "Article 2", 9000L, feedSource),
            buildFeedItem("5050623385", "Article 3", 8000L, feedSource),
            buildFeedItem("5058157281", "Article 4", 7000L, feedSource),
            buildFeedItem("5058832279", "Article 5", 6000L, feedSource),
        )
        databaseHelper.insertFeedItems(feedItems, lastSyncTimestamp = 0)
        feedStateRepository.getFeeds()
        advanceUntilIdle()

        feedStateRepository.updateFeedSourceFilter(feedSource.id)
        advanceUntilIdle()

        feedActionsRepository.markAllCurrentFeedAsRead()
        advanceUntilIdle()

        val dbFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertNotNull(dbFeedSource, "Feed source should exist")
        val updatedItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Source(dbFeedSource),
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        feedItems.forEach { feedItem ->
            val item = updatedItems.find { it.url_hash == feedItem.id }
            assertNotNull(item, "Item ${feedItem.id} should exist")
            assertTrue(item.is_read, "All items should be marked as read: ${feedItem.id}")
        }
    }

    @Test
    fun `updateBookmarkStatus should star item when isBookmarked is true`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)

        val feedItem = buildFeedItem(
            id = "5095737792",
            title = "Test Article",
            pubDateMillis = 10000L,
            source = feedSource,
        )
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)
        feedStateRepository.getFeeds()
        advanceUntilIdle()

        val feedItemId = FeedItemId(feedItem.id)

        feedActionsRepository.updateBookmarkStatus(feedItemId, isBookmarked = true)
        advanceUntilIdle()

        val updatedItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        val updatedItem = updatedItems.find { it.url_hash == feedItem.id }
        assertNotNull(updatedItem, "Item should exist")
        assertTrue(updatedItem.is_bookmarked, "Item should be bookmarked")
    }

    @Test
    fun `updateBookmarkStatus should unstar item when isBookmarked is false`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)

        val feedItem = buildFeedItem(
            id = "5095737792",
            title = "Test Article",
            pubDateMillis = 10000L,
            source = feedSource,
        )
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)
        databaseHelper.updateBookmarkStatus(feedItemId = FeedItemId(feedItem.id), isBookmarked = true)
        feedStateRepository.getFeeds()
        advanceUntilIdle()

        val feedItemId = FeedItemId(feedItem.id)

        feedActionsRepository.updateBookmarkStatus(feedItemId, isBookmarked = false)
        advanceUntilIdle()

        val updatedItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        val updatedItem = updatedItems.find { it.url_hash == feedItem.id }
        assertNotNull(updatedItem, "Item should exist")
        assertFalse(updatedItem.is_bookmarked, "Item should not be bookmarked")
    }

    @Test
    fun `updateReadStatus should mark item as read when isRead is true`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)

        val feedItem = buildFeedItem(
            id = "5095737791",
            title = "Test Article",
            pubDateMillis = 10000L,
            source = feedSource,
        )
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)
        feedStateRepository.getFeeds()
        advanceUntilIdle()

        val feedItemId = FeedItemId(feedItem.id)

        feedActionsRepository.updateReadStatus(feedItemId, isRead = true)
        advanceUntilIdle()

        val updatedItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        val updatedItem = updatedItems.find { it.url_hash == feedItem.id }
        assertNotNull(updatedItem, "Item should exist")
        assertTrue(updatedItem.is_read, "Item should be marked as read")
    }

    @Test
    fun `updateReadStatus should mark item as unread when isRead is false`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)

        val feedItem = buildFeedItem(
            id = "5095737791",
            title = "Test Article",
            pubDateMillis = 10000L,
            source = feedSource,
        )
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)
        databaseHelper.updateReadStatus(feedItemId = FeedItemId(feedItem.id), isRead = true)
        feedStateRepository.getFeeds()
        advanceUntilIdle()

        val feedItemId = FeedItemId(feedItem.id)

        feedActionsRepository.updateReadStatus(feedItemId, isRead = false)
        advanceUntilIdle()

        val updatedItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        val updatedItem = updatedItems.find { it.url_hash == feedItem.id }
        assertNotNull(updatedItem, "Item should exist")
        assertFalse(updatedItem.is_read, "Item should be marked as unread")
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
        linkOpeningPreference = com.prof18.feedflow.core.model.LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
    )
}
