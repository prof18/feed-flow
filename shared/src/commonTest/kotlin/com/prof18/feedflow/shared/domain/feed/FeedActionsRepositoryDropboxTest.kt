package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.SyncedFeedItem
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedActionsRepositoryDropboxTest : KoinTestBase() {

    private val feedActionsRepository: FeedActionsRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val syncedDatabaseHelper: SyncedDatabaseHelper by inject()
    private val dropboxSettings: DropboxSettings by inject()
    private val settingsRepository: SettingsRepository by inject()

    @Test
    fun `marking item as unread updates Dropbox sync state`() = runTest(testDispatcher) {
        enableDropboxSync()
        val feedItemId = insertFeedItem()
        databaseHelper.updateReadStatus(feedItemId, isRead = true)
        syncedDatabaseHelper.insertFeedItems(
            listOf(SyncedFeedItem(id = feedItemId.id, isRead = true, isBookmarked = true)),
        )

        feedActionsRepository.updateReadStatus(feedItemId, isRead = false)

        val syncedItem = syncedDatabaseHelper.getAllFeedItems().single()
        assertFalse(syncedItem.isRead)
        assertTrue(syncedItem.isBookmarked)
        assertTrue(settingsRepository.getIsSyncUploadRequired())
    }

    @Test
    fun `removing bookmark updates Dropbox sync state`() = runTest(testDispatcher) {
        enableDropboxSync()
        val feedItemId = insertFeedItem()
        syncedDatabaseHelper.insertFeedItems(
            listOf(SyncedFeedItem(id = feedItemId.id, isRead = true, isBookmarked = true)),
        )

        feedActionsRepository.updateBookmarkStatus(feedItemId, isBookmarked = false)

        val syncedItem = syncedDatabaseHelper.getAllFeedItems().single()
        assertTrue(syncedItem.isRead)
        assertFalse(syncedItem.isBookmarked)
        assertTrue(settingsRepository.getIsSyncUploadRequired())
    }

    private fun enableDropboxSync() {
        dropboxSettings.setDropboxData("test-credentials")
    }

    private suspend fun insertFeedItem(): FeedItemId {
        val feedSource = createFeedSource()
        databaseHelper.insertFeedSourceWithCategory(feedSource)
        val feedItem = buildFeedItem("item-1", "Article 1", 10000L, feedSource)
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)
        return FeedItemId(feedItem.id)
    }

    private fun createFeedSource(): FeedSource = FeedSource(
        id = "source-1",
        url = "https://example.com/source-1/feed.xml",
        title = "Test Feed",
        category = null,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = "https://example.com/source-1",
        fetchFailed = false,
        articleOpenMode = ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
    )
}
