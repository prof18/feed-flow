package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.feedsync.icloud.ICloudDataSource
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.test.ICloudDataSourceFake
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import platform.Foundation.NSURL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ICloudSyncViewModelTest : KoinTestBase() {

    private val fakeFeedSyncWorker = FakeFeedSyncWorker()
    private val fakeICloudDataSource = ICloudDataSourceFake()
    private val viewModel: ICloudSyncViewModel by inject()
    private val iCloudSettings: ICloudSettings by inject()

    override fun getTestModules(): List<Module> {
        return super.getTestModules() + module {
            factory<FeedSyncWorker> { fakeFeedSyncWorker }
            factory<ICloudDataSource> { fakeICloudDataSource }
        }
    }

    @Test
    fun `initial state is Unlinked when iCloud is not configured`() = runTest {
        viewModel.iCloudConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Unlinked)
        }
    }

    @Test
    fun `triggerBackup calls upload on data source`() = runTest {
        // Set up iCloud as the sync account
        iCloudSettings.setUseICloud(true)

        // Create a new viewModel that will restore the iCloud auth in init
        val linkedViewModel: ICloudSyncViewModel = getKoin().get()

        // Verify initial state is Linked
        linkedViewModel.iCloudConnectionUiState.test {
            val initialState = awaitItem()
            assertTrue(initialState is AccountConnectionUiState.Linked)
        }

        // Trigger backup and verify uploadImmediate was called
        linkedViewModel.triggerBackup()

        // Verify that uploadImmediate was called
        assertTrue(fakeFeedSyncWorker.uploadImmediateCallCount > 0)
    }

    @Test
    fun `triggerBackup handles upload failure gracefully`() = runTest {
        viewModel.iCloudConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Unlinked)

            viewModel.triggerBackup()

            // State should remain Unlinked even after failure
            expectNoEvents()
        }
    }

    @Test
    fun `triggerBackup updates sync state after completion`() = runTest {
        // Set up iCloud as the sync account with existing timestamps
        iCloudSettings.setUseICloud(true)
        iCloudSettings.setLastUploadTimestamp(1234567890L)

        // Create a new viewModel that will restore the iCloud auth in init
        val linkedViewModel: ICloudSyncViewModel = getKoin().get()

        // Verify initial state is Linked with Synced sync state
        linkedViewModel.iCloudConnectionUiState.test {
            val initialState = awaitItem()
            assertTrue(initialState is AccountConnectionUiState.Linked)
            assertTrue(initialState.syncState is AccountSyncUIState.Synced)
        }

        // Trigger backup
        linkedViewModel.triggerBackup()

        // Verify final state is still Linked (backup completed successfully)
        linkedViewModel.iCloudConnectionUiState.test {
            val finalState = awaitItem()
            assertTrue(finalState is AccountConnectionUiState.Linked)
        }
    }

    @Test
    fun `unlink sets state to Unlinked and clears iCloud settings`() = runTest {
        iCloudSettings.setUseICloud(true)

        // Create a new viewModel that will restore the iCloud auth in init
        val linkedViewModel: ICloudSyncViewModel = getKoin().get()

        linkedViewModel.iCloudConnectionUiState.test {
            // Initial state should be Linked since iCloud was configured
            val initialState = awaitItem()
            assertTrue(initialState is AccountConnectionUiState.Linked)

            // Call unlink
            linkedViewModel.unlink()

            // Should transition to Loading
            val loadingState = awaitItem()
            assertTrue(loadingState is AccountConnectionUiState.Loading)

            // Should transition to Unlinked
            val unlinkedState = awaitItem()
            assertTrue(unlinkedState is AccountConnectionUiState.Unlinked)
        }
    }

    @Test
    fun `restoreICloudAuth returns Linked state when iCloud was previously configured`() = runTest {
        // Set up iCloud settings before creating viewModel
        iCloudSettings.setUseICloud(true)
        iCloudSettings.setLastUploadTimestamp(1234567890L)
        iCloudSettings.setLastDownloadTimestamp(1234567800L)

        // Create a new viewModel that will restore the iCloud auth in init
        val linkedViewModel: ICloudSyncViewModel = getKoin().get()

        linkedViewModel.iCloudConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            val syncState = state.syncState
            assertTrue(syncState is AccountSyncUIState.Synced)
            // Verify the Synced content - dates should be formatted (not null since timestamps were set)
            assertNotNull(syncState.lastUploadDate)
            assertNotNull(syncState.lastDownloadDate)
        }
    }

    @Test
    fun `restoreICloudAuth returns Linked with None sync state when no timestamps`() = runTest {
        // Set up iCloud settings without timestamps
        iCloudSettings.setUseICloud(true)

        // Create a new viewModel that will restore the iCloud auth in init
        val linkedViewModel: ICloudSyncViewModel = getKoin().get()

        linkedViewModel.iCloudConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            assertEquals(AccountSyncUIState.None, state.syncState)
        }
    }

    @Test
    fun `setICloudAuth transitions to Linked state when iCloud is available`() = runTest {
        fakeICloudDataSource.iCloudBaseFolderURL = NSURL(string = "file:///icloud/documents")

        viewModel.iCloudConnectionUiState.test {
            val initialState = awaitItem()
            assertTrue(initialState is AccountConnectionUiState.Unlinked)

            viewModel.setICloudAuth()

            val finalState = expectMostRecentItem()
            assertTrue(finalState is AccountConnectionUiState.Linked)
        }
    }

    @Test
    fun `setICloudAuth transitions to Unlinked state when iCloud is not available`() = runTest {
        fakeICloudDataSource.iCloudBaseFolderURL = null

        viewModel.iCloudConnectionUiState.test {
            val initialState = awaitItem()
            assertTrue(initialState is AccountConnectionUiState.Unlinked)

            viewModel.setICloudAuth()

            val finalState = viewModel.iCloudConnectionUiState.value
            assertTrue(finalState is AccountConnectionUiState.Unlinked)
        }
    }

    @Test
    fun `setICloudAuth emits ICloudNotAvailable error when iCloud URL is null`() = runTest {
        fakeICloudDataSource.iCloudBaseFolderURL = null

        viewModel.syncMessageQueue.test {
            viewModel.setICloudAuth()

            val result = awaitItem()
            assertTrue(result is SyncResult.ICloudNotAvailable)
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
