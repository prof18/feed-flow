package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.model.VisibleFeedItem
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.get
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeViewModelBackgroundHandOffTest : KoinTestBase() {

    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val dropboxSettings: DropboxSettings by inject()
    private val syncWorker = RecordingFeedSyncWorker()

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<FeedSyncWorker> { syncWorker }
    }

    @Test
    fun `going to the background waits for the upload instead of launching it`() = runTest(testDispatcher) {
        dropboxSettings.setDropboxData("test-credentials")
        settingsRepository.setIsSyncUploadRequired(true)
        val viewModel = get<HomeViewModel>()
        advanceUntilIdle()

        viewModel.onAppBackgrounded()
        advanceUntilIdle()

        assertEquals(1, syncWorker.uploadImmediateCount)
        assertEquals(0, syncWorker.uploadCount)
    }

    @Test
    fun `going to the background saves scroll-read articles before uploading`() = runTest(testDispatcher) {
        dropboxSettings.setDropboxData("test-credentials")
        settingsRepository.setIsSyncUploadRequired(true)
        settingsRepository.setMarkFeedAsReadWhenScrolling(true)
        val source = FeedSourceGenerator.feedSource(id = "source-1", url = "https://example.com/rss.xml")
        databaseHelper.insertFeedSource(listOf(source.toParsedFeedSource()))
        databaseHelper.insertFeedItems(
            listOf(
                buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 2000, source = source),
                buildFeedItem(id = "item-2", title = "Item 2", pubDateMillis = 1000, source = source),
            ),
            lastSyncTimestamp = 0,
        )
        syncWorker.onUploadImmediate = {
            readIdsAtUpload = databaseHelper.getFeedItems(
                feedFilter = FeedFilter.Timeline,
                pageSize = 100,
                showReadItems = true,
                sortOrder = FeedOrder.NEWEST_FIRST,
            ).filter { it.is_read }.map { it.url_hash }.toSet()
        }
        val viewModel = get<HomeViewModel>()
        advanceUntilIdle()

        viewModel.onVisibleFeedItemsChanged(listOf(VisibleFeedItem(id = "item-1", index = 0)))
        viewModel.onVisibleFeedItemsChanged(listOf(VisibleFeedItem(id = "item-2", index = 1)))
        viewModel.onAppBackgrounded()
        advanceUntilIdle()

        assertEquals(setOf("item-1"), readIdsAtUpload)
    }

    private var readIdsAtUpload: Set<String>? = null

    private class RecordingFeedSyncWorker : FeedSyncWorker {
        var uploadCount = 0
            private set
        var uploadImmediateCount = 0
            private set
        var onUploadImmediate: suspend () -> Unit = {}

        override fun upload() {
            uploadCount++
        }

        override suspend fun uploadImmediate() {
            uploadImmediateCount++
            onUploadImmediate()
        }

        override suspend fun download(isFirstSync: Boolean): SyncResult = SyncResult.Success
        override suspend fun syncFeedSources(): SyncResult = SyncResult.Success
        override suspend fun syncFeedItems(): SyncResult = SyncResult.Success
    }
}
