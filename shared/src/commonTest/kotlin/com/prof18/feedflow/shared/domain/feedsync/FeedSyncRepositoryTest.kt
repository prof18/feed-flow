package com.prof18.feedflow.shared.domain.feedsync

import app.cash.turbine.test
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncDownloadError
import com.prof18.feedflow.core.model.SyncFeedError
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.model.SyncedFeedItem
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FeedSyncRepositoryTest : KoinTestBase() {

    private val fakeFeedSyncWorker = FakeFeedSyncWorker()
    private val feedSyncRepository: FeedSyncRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val dropboxSettings: DropboxSettings by inject()
    private val syncedDatabaseHelper: SyncedDatabaseHelper by inject()
    private val feedSyncMessageQueue: FeedSyncMessageQueue by inject()

    override fun getTestModules(): List<Module> {
        return super.getTestModules() + module {
            factory<FeedSyncWorker> { fakeFeedSyncWorker }
        }
    }

    @Test
    fun `enqueueBackup triggers upload when sync enabled and upload required`() {
        enableDropboxSync()
        settingsRepository.setIsSyncUploadRequired(true)

        feedSyncRepository.enqueueBackup()

        assertEquals(1, fakeFeedSyncWorker.uploadCallCount)
    }

    @Test
    fun `enqueueBackup does nothing when sync disabled`() {
        settingsRepository.setIsSyncUploadRequired(true)

        feedSyncRepository.enqueueBackup()

        assertEquals(0, fakeFeedSyncWorker.uploadCallCount)
    }

    @Test
    fun `performBackup respects force flag`() = runTest(testDispatcher) {
        enableDropboxSync()

        feedSyncRepository.performBackup()
        assertEquals(0, fakeFeedSyncWorker.uploadImmediateCallCount)

        feedSyncRepository.performBackup(forceBackup = true)
        assertEquals(1, fakeFeedSyncWorker.uploadImmediateCallCount)
    }

    @Test
    fun `onDropboxUploadSuccessAfterResume clears upload required`() {
        settingsRepository.setIsSyncUploadRequired(true)

        feedSyncRepository.onDropboxUploadSuccessAfterResume()

        assertFalse(settingsRepository.getIsSyncUploadRequired())
        assertNotNull(dropboxSettings.getLastUploadTimestamp())
    }

    @Test
    fun `firstSync uploads when download fails`() = runTest(testDispatcher) {
        enableDropboxSync()
        fakeFeedSyncWorker.downloadResult = SyncResult.General(SyncDownloadError.DropboxDownloadFailed)

        feedSyncRepository.firstSync()

        assertEquals(listOf(true), fakeFeedSyncWorker.downloadIsFirstSyncArgs)
        assertEquals(1, fakeFeedSyncWorker.uploadImmediateCallCount)
    }

    @Test
    fun `addSourceAndCategories inserts data and marks upload required`() = runTest(testDispatcher) {
        enableDropboxSync()
        val category = FeedSourceCategory(id = "category-id", title = "Tech")
        val feedSource = createFeedSource(id = "source-id", title = "Feed", category = category)

        feedSyncRepository.addSourceAndCategories(
            sources = listOf(feedSource),
            categories = listOf(category),
        )

        val sources = syncedDatabaseHelper.getAllFeedSources()
        val categories = syncedDatabaseHelper.getAllFeedSourceCategories()
        assertEquals(1, sources.size)
        assertEquals("source-id", sources.first().id)
        assertEquals("category-id", categories.first().id)
        assertTrue(settingsRepository.getIsSyncUploadRequired())
    }

    @Test
    fun `insertSyncedFeedSource inserts sources and marks upload required`() = runTest(testDispatcher) {
        enableDropboxSync()
        val feedSource = createFeedSource(id = "source-id", title = "Feed")

        feedSyncRepository.insertSyncedFeedSource(listOf(feedSource))

        val sources = syncedDatabaseHelper.getAllFeedSources()
        assertEquals(1, sources.size)
        assertEquals("source-id", sources.first().id)
        assertTrue(settingsRepository.getIsSyncUploadRequired())
    }

    @Test
    fun `insertFeedSourceCategories inserts categories and marks upload required`() = runTest(testDispatcher) {
        enableDropboxSync()
        val category = FeedSourceCategory(id = "category-id", title = "Tech")

        feedSyncRepository.insertFeedSourceCategories(listOf(category))

        val categories = syncedDatabaseHelper.getAllFeedSourceCategories()
        assertEquals(1, categories.size)
        assertEquals("category-id", categories.first().id)
        assertTrue(settingsRepository.getIsSyncUploadRequired())
    }

    @Test
    fun `updateCategory updates category and marks upload required`() = runTest(testDispatcher) {
        enableDropboxSync()
        val category = FeedSourceCategory(id = "category-id", title = "Tech")
        syncedDatabaseHelper.insertFeedSourceCategories(listOf(category))

        feedSyncRepository.updateCategory(category.copy(title = "Updated"))

        val updatedCategory = syncedDatabaseHelper.getAllFeedSourceCategories().single()
        assertEquals("Updated", updatedCategory.title)
        assertTrue(settingsRepository.getIsSyncUploadRequired())
    }

    @Test
    fun `deleteFeedSource removes source and marks upload required`() = runTest(testDispatcher) {
        enableDropboxSync()
        val feedSource = createFeedSource(id = "source-id", title = "Feed")
        syncedDatabaseHelper.insertSyncedFeedSource(listOf(feedSource))

        feedSyncRepository.deleteFeedSource(feedSource)

        assertTrue(syncedDatabaseHelper.getAllFeedSources().isEmpty())
        assertTrue(settingsRepository.getIsSyncUploadRequired())
    }

    @Test
    fun `deleteAllFeedSources removes all sources and marks upload required`() = runTest(testDispatcher) {
        enableDropboxSync()
        val feedSources = listOf(
            createFeedSource(id = "source-1", title = "Feed 1"),
            createFeedSource(id = "source-2", title = "Feed 2"),
        )
        syncedDatabaseHelper.insertSyncedFeedSource(feedSources)

        feedSyncRepository.deleteAllFeedSources()

        assertTrue(syncedDatabaseHelper.getAllFeedSources().isEmpty())
        assertTrue(settingsRepository.getIsSyncUploadRequired())
    }

    @Test
    fun `updateFeedSourceName updates name and marks upload required`() = runTest(testDispatcher) {
        enableDropboxSync()
        val feedSource = createFeedSource(id = "source-id", title = "Old Title")
        syncedDatabaseHelper.insertSyncedFeedSource(listOf(feedSource))

        feedSyncRepository.updateFeedSourceName(feedSource.id, "New Title")

        val updatedSource = syncedDatabaseHelper.getAllFeedSources().single()
        assertEquals("New Title", updatedSource.title)
        assertTrue(settingsRepository.getIsSyncUploadRequired())
    }

    @Test
    fun `updateFeedSource updates source and marks upload required`() = runTest(testDispatcher) {
        enableDropboxSync()
        val feedSource = createFeedSource(id = "source-id", title = "Old Title")
        syncedDatabaseHelper.insertSyncedFeedSource(listOf(feedSource))

        feedSyncRepository.updateFeedSource(feedSource.copy(title = "New Title"))

        val updatedSource = syncedDatabaseHelper.getAllFeedSources().single()
        assertEquals("New Title", updatedSource.title)
        assertTrue(settingsRepository.getIsSyncUploadRequired())
    }

    @Test
    fun `deleteFeedSourceCategory removes category and marks upload required`() = runTest(testDispatcher) {
        enableDropboxSync()
        val category = FeedSourceCategory(id = "category-id", title = "Tech")
        syncedDatabaseHelper.insertFeedSourceCategories(listOf(category))

        feedSyncRepository.deleteFeedSourceCategory(category.id)

        assertTrue(syncedDatabaseHelper.getAllFeedSourceCategories().isEmpty())
        assertTrue(settingsRepository.getIsSyncUploadRequired())
    }

    @Test
    fun `deleteFeedItems removes items and marks upload required`() = runTest(testDispatcher) {
        enableDropboxSync()
        val item = SyncedFeedItem(id = "item-1", isRead = false, isBookmarked = false)
        syncedDatabaseHelper.insertFeedItems(listOf(item))

        feedSyncRepository.deleteFeedItems(listOf(FeedItemId("item-1")))

        assertTrue(syncedDatabaseHelper.getAllFeedItems().isEmpty())
        assertTrue(settingsRepository.getIsSyncUploadRequired())
    }

    @Test
    fun `setIsSyncUploadRequired updates flag when sync is enabled`() {
        enableDropboxSync()

        feedSyncRepository.setIsSyncUploadRequired()

        assertTrue(settingsRepository.getIsSyncUploadRequired())
    }

    @Test
    fun `deleteAll clears synced database`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-id", title = "Feed")
        syncedDatabaseHelper.insertSyncedFeedSource(listOf(feedSource))

        feedSyncRepository.deleteAll()

        assertTrue(syncedDatabaseHelper.isDatabaseEmpty())
    }

    @Test
    fun `syncFeedSources emits errors to message queue`() = runTest(testDispatcher) {
        enableDropboxSync()
        val downloadError = SyncResult.General(SyncDownloadError.DropboxDownloadFailed)
        val sourcesError = SyncResult.General(SyncFeedError.FeedSourcesSyncFailed)
        fakeFeedSyncWorker.downloadResult = downloadError
        fakeFeedSyncWorker.syncFeedSourcesResult = sourcesError

        feedSyncMessageQueue.messageQueue.test {
            feedSyncRepository.syncFeedSources()

            assertEquals(downloadError, awaitItem())
            assertEquals(sourcesError, awaitItem())
        }
    }

    @Test
    fun `syncFeedItems emits error to message queue`() = runTest(testDispatcher) {
        enableDropboxSync()
        val itemError = SyncResult.General(SyncFeedError.FeedItemsSyncFailed)
        fakeFeedSyncWorker.syncFeedItemsResult = itemError

        feedSyncMessageQueue.messageQueue.test {
            feedSyncRepository.syncFeedItems()

            assertEquals(itemError, awaitItem())
        }
    }

    private fun enableDropboxSync() {
        dropboxSettings.setDropboxData("test-credentials")
    }

    private fun createFeedSource(
        id: String,
        title: String,
        category: FeedSourceCategory? = null,
    ): FeedSource = FeedSource(
        id = id,
        url = "https://example.com/feed.xml",
        title = title,
        category = category,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = "https://example.com",
        fetchFailed = false,
        linkOpeningPreference = com.prof18.feedflow.core.model.LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
    )
}

private class FakeFeedSyncWorker : FeedSyncWorker {
    var uploadCallCount = 0
        private set
    var uploadImmediateCallCount = 0
        private set
    val downloadIsFirstSyncArgs = mutableListOf<Boolean>()
    var downloadResult: SyncResult = SyncResult.Success
    var syncFeedSourcesResult: SyncResult = SyncResult.Success
    var syncFeedItemsResult: SyncResult = SyncResult.Success

    override fun upload() {
        uploadCallCount++
    }

    override suspend fun uploadImmediate() {
        uploadImmediateCallCount++
    }

    override suspend fun download(isFirstSync: Boolean): SyncResult {
        downloadIsFirstSyncArgs.add(isFirstSync)
        return downloadResult
    }

    override suspend fun syncFeedSources(): SyncResult = syncFeedSourcesResult

    override suspend fun syncFeedItems(): SyncResult = syncFeedItemsResult
}
