package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.SyncedFeedItem
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.database.DatabaseTables
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.testLogger
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FeedSyncerTest : KoinTestBase() {

    private val databaseHelper: DatabaseHelper by inject()
    private val syncedDatabaseHelper: SyncedDatabaseHelper by inject()

    private fun createSyncer(): FeedSyncer =
        FeedSyncer(
            syncedDatabaseHelper = syncedDatabaseHelper,
            appDatabaseHelper = databaseHelper,
            logger = testLogger,
        )

    @Test
    fun `populateSyncDbIfEmpty copies sources categories and items`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))

        val feedItem = buildFeedItem(
            id = "item-1",
            title = "Item 1",
            pubDateMillis = 1000,
            source = feedSource,
        )
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)
        databaseHelper.updateReadStatus(FeedItemId("item-1"), isRead = true)

        createSyncer().populateSyncDbIfEmpty()

        val syncedSources = syncedDatabaseHelper.getAllFeedSources()
        val syncedItems = syncedDatabaseHelper.getAllFeedItems()

        assertEquals(1, syncedSources.size)
        assertEquals(feedSource.id, syncedSources.first().id)
        assertEquals(1, syncedItems.size)
        assertEquals("item-1", syncedItems.first().id)
        assertEquals(true, syncedItems.first().isRead)
    }

    @Test
    fun `syncFeedSource mirrors synced database and deletes missing sources`() = runTest(testDispatcher) {
        val appSource1 = createFeedSource(id = "source-1", title = "App 1")
        val appSource2 = createFeedSource(id = "source-2", title = "App 2")
        databaseHelper.insertFeedSource(
            listOf(appSource1.toParsedFeedSource(), appSource2.toParsedFeedSource()),
        )

        val syncedSource = createFeedSource(id = "source-3", title = "Synced")
        syncedDatabaseHelper.insertSyncedFeedSource(listOf(syncedSource))

        createSyncer().syncFeedSource()

        val sources = databaseHelper.getFeedSources()
        assertEquals(1, sources.size)
        assertEquals("source-3", sources.first().id)
        assertNotNull(databaseHelper.getLastChangeTimestamp(DatabaseTables.FEED_SOURCE))
    }

    @Test
    fun `syncFeedSourceCategory mirrors synced database and deletes missing categories`() = runTest(testDispatcher) {
        val appCategory1 = FeedSourceCategory(id = "cat-1", title = "App 1")
        val appCategory2 = FeedSourceCategory(id = "cat-2", title = "App 2")
        databaseHelper.insertCategories(listOf(appCategory1, appCategory2))

        val syncedCategory = FeedSourceCategory(id = "cat-3", title = "Synced")
        syncedDatabaseHelper.insertFeedSourceCategories(listOf(syncedCategory))

        createSyncer().syncFeedSourceCategory()

        val categories = databaseHelper.getFeedSourceCategories()
        assertEquals(1, categories.size)
        assertEquals("cat-3", categories.first().id)
        assertNotNull(databaseHelper.getLastChangeTimestamp(DatabaseTables.FEED_SOURCE_CATEGORY))
    }

    @Test
    fun `syncFeedItem updates read and bookmark flags from sync db`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))

        val feedItem = buildFeedItem(
            id = "item-1",
            title = "Item 1",
            pubDateMillis = 1000,
            source = feedSource,
        )
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)

        syncedDatabaseHelper.insertFeedItems(
            listOf(
                SyncedFeedItem(
                    id = "item-1",
                    isRead = true,
                    isBookmarked = true,
                ),
            ),
        )

        createSyncer().syncFeedItem()

        val items = databaseHelper.getFeedItems(
            feedFilter = com.prof18.feedflow.core.model.FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = com.prof18.feedflow.core.model.FeedOrder.NEWEST_FIRST,
        )
        val item = items.first()
        assertEquals(true, item.is_read)
        assertEquals(true, item.is_bookmarked)
    }

    @Test
    fun `updateFeedItemsToSyncDatabase inserts synced read and bookmarked items`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))

        val feedItem = buildFeedItem(
            id = "item-1",
            title = "Item 1",
            pubDateMillis = 1000,
            source = feedSource,
        )
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)
        databaseHelper.updateReadStatus(FeedItemId("item-1"), isRead = true)
        databaseHelper.updateBookmarkStatus(FeedItemId("item-1"), isBookmarked = true)

        createSyncer().updateFeedItemsToSyncDatabase()

        val syncedItems = syncedDatabaseHelper.getAllFeedItems()
        assertEquals(1, syncedItems.size)
        assertEquals("item-1", syncedItems.first().id)
        assertEquals(true, syncedItems.first().isRead)
        assertEquals(true, syncedItems.first().isBookmarked)
    }

    @Test
    fun `syncFeedSource returns early when local database is up-to-date`() = runTest(testDispatcher) {
        val appSource = createFeedSource(id = "source-1", title = "App")
        databaseHelper.insertFeedSource(listOf(appSource.toParsedFeedSource()))

        val syncedSource = createFeedSource(id = "source-2", title = "Synced")
        syncedDatabaseHelper.insertSyncedFeedSource(listOf(syncedSource))

        val remoteTimestamp = requireNotNull(
            syncedDatabaseHelper.getLastChangeTimestamp(
                com.prof18.feedflow.feedsync.database.data.SyncTable.SYNCED_FEED_SOURCE,
            ),
        )
        databaseHelper.updateSyncMetadata(DatabaseTables.FEED_SOURCE, remoteTimestamp)

        createSyncer().syncFeedSource()

        val sources = databaseHelper.getFeedSources()
        assertEquals(1, sources.size)
        assertEquals("source-1", sources.first().id)
    }

    @Test
    fun `syncFeedItem returns early when local database is up-to-date`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))

        val feedItem = buildFeedItem(
            id = "item-1",
            title = "Item 1",
            pubDateMillis = 1000,
            source = feedSource,
        )
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)

        syncedDatabaseHelper.insertFeedItems(
            listOf(
                SyncedFeedItem(
                    id = "item-1",
                    isRead = true,
                    isBookmarked = true,
                ),
            ),
        )

        val remoteTimestamp = requireNotNull(
            syncedDatabaseHelper.getLastChangeTimestamp(
                com.prof18.feedflow.feedsync.database.data.SyncTable.SYNCED_FEED_ITEM,
            ),
        )
        databaseHelper.updateSyncMetadata(DatabaseTables.FEED_ITEM, remoteTimestamp)

        createSyncer().syncFeedItem()

        val items = databaseHelper.getFeedItems(
            feedFilter = com.prof18.feedflow.core.model.FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = com.prof18.feedflow.core.model.FeedOrder.NEWEST_FIRST,
        )
        val item = items.first()
        assertEquals(false, item.is_read)
        assertEquals(false, item.is_bookmarked)
    }

    private fun createFeedSource(
        id: String,
        title: String,
    ): FeedSource = FeedSource(
        id = id,
        url = "https://example.com/$id/rss.xml",
        title = title,
        category = null,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = "https://example.com",
        fetchFailed = false,
        linkOpeningPreference = LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
    )
}
