package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.test.DropboxDataSourceFake
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DropboxSyncViewModelTest : KoinTestBase() {

    private val fakeFeedSyncWorker = FakeFeedSyncWorker()
    private val fakeDropboxDataSource = DropboxDataSourceFake()
    private val viewModel: DropboxSyncViewModel by inject()
    private val dropboxSettings: DropboxSettings by inject()

    override fun getTestModules(): List<Module> {
        return super.getTestModules() + module {
            factory<FeedSyncWorker> { fakeFeedSyncWorker }
            factory<DropboxDataSource> { fakeDropboxDataSource }
        }
    }

    @Test
    fun `initial state is Unlinked when Dropbox is not configured`() = runTest {
        viewModel.dropboxConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Unlinked)
        }
    }

    @Test
    fun `restoreDropboxAuth returns Linked state when Dropbox was previously configured`() = runTest {
        dropboxSettings.setDropboxData("fake-credentials")
        dropboxSettings.setLastUploadTimestamp(1234567890L)
        dropboxSettings.setLastDownloadTimestamp(1234567800L)

        val linkedViewModel: DropboxSyncViewModel = getKoin().get()

        linkedViewModel.dropboxConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            val syncState = state.syncState
            assertTrue(syncState is AccountSyncUIState.Synced)
            assertNotNull(syncState.lastUploadDate)
            assertNotNull(syncState.lastDownloadDate)
        }
    }

    @Test
    fun `restoreDropboxAuth returns Linked with None sync state when no timestamps`() = runTest {
        dropboxSettings.setDropboxData("fake-credentials")

        val linkedViewModel: DropboxSyncViewModel = getKoin().get()

        linkedViewModel.dropboxConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            assertEquals(AccountSyncUIState.None, state.syncState)
        }
    }

    @Test
    fun `restoreDropboxAuth returns Unlinked when credentials are null`() = runTest {
        viewModel.dropboxConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Unlinked)
        }
    }

    @Test
    fun `triggerBackup updates sync state when linked`() = runTest {
        dropboxSettings.setDropboxData("fake-credentials")
        dropboxSettings.setLastUploadTimestamp(1234567890L)

        val linkedViewModel: DropboxSyncViewModel = getKoin().get()

        linkedViewModel.dropboxConnectionUiState.test {
            val initialState = awaitItem()
            assertTrue(initialState is AccountConnectionUiState.Linked)
        }

        linkedViewModel.triggerBackup()

        linkedViewModel.dropboxConnectionUiState.test {
            val finalState = awaitItem()
            assertTrue(finalState is AccountConnectionUiState.Linked)
        }
    }

    @Test
    fun `unlink sets state to Unlinked and clears Dropbox settings`() = runTest {
        dropboxSettings.setDropboxData("fake-credentials")

        val linkedViewModel: DropboxSyncViewModel = getKoin().get()

        linkedViewModel.dropboxConnectionUiState.test {
            val initialState = awaitItem()
            assertTrue(initialState is AccountConnectionUiState.Linked)

            linkedViewModel.unlink()

            val loadingState = awaitItem()
            assertTrue(loadingState is AccountConnectionUiState.Loading)

            val unlinkedState = awaitItem()
            assertTrue(unlinkedState is AccountConnectionUiState.Unlinked)
        }

        assertTrue(fakeDropboxDataSource.revokeAccessCallCount > 0)
    }

    @Test
    fun `getSyncState returns Synced when upload timestamp exists`() = runTest {
        dropboxSettings.setDropboxData("fake-credentials")
        dropboxSettings.setLastUploadTimestamp(1234567890L)

        val linkedViewModel: DropboxSyncViewModel = getKoin().get()

        linkedViewModel.dropboxConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            val syncState = state.syncState
            assertTrue(syncState is AccountSyncUIState.Synced)
            assertNotNull(syncState.lastUploadDate)
        }
    }

    @Test
    fun `getSyncState returns Synced when download timestamp exists`() = runTest {
        dropboxSettings.setDropboxData("fake-credentials")
        dropboxSettings.setLastDownloadTimestamp(1234567800L)

        val linkedViewModel: DropboxSyncViewModel = getKoin().get()

        linkedViewModel.dropboxConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            val syncState = state.syncState
            assertTrue(syncState is AccountSyncUIState.Synced)
            assertNotNull(syncState.lastDownloadDate)
        }
    }
}

private class FakeFeedSyncWorker : FeedSyncWorker {
    var uploadCallCount = 0
        private set
    var uploadImmediateCallCount = 0
        private set
    var downloadCallCount = 0
        private set

    override fun upload() {
        uploadCallCount++
    }

    override suspend fun uploadImmediate() {
        uploadImmediateCallCount++
    }

    override suspend fun download(isFirstSync: Boolean): SyncResult {
        downloadCallCount++
        return SyncResult.Success
    }

    override suspend fun syncFeedSources(): SyncResult = SyncResult.Success

    override suspend fun syncFeedItems(): SyncResult = SyncResult.Success

    fun reset() {
        uploadCallCount = 0
        uploadImmediateCallCount = 0
        downloadCallCount = 0
    }
}
