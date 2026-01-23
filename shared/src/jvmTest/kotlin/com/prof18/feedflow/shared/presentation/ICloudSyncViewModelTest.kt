package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ICloudSyncViewModelTest : KoinTestBase() {

    private val viewModel: ICloudSyncViewModel by inject()
    private val iCloudSettings: ICloudSettings by inject()

    @Test
    fun `initial state is Unlinked when iCloud is not configured`() = runTest {
        viewModel.iCloudConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Unlinked)
        }
    }

    @Test
    fun `setICloudAuth transitions to Linked state`() = runTest {
        viewModel.iCloudConnectionUiState.test {
            val initialState = awaitItem()
            assertTrue(initialState is AccountConnectionUiState.Unlinked)

            viewModel.setICloudAuth()

            val finalState = expectMostRecentItem()
            assertTrue(finalState is AccountConnectionUiState.Linked)
        }
    }

    @Test
    fun `triggerBackup updates sync state when linked`() = runTest {
        iCloudSettings.setUseICloud(true)

        val linkedViewModel: ICloudSyncViewModel = getKoin().get()

        linkedViewModel.iCloudConnectionUiState.test {
            val initialState = awaitItem()
            assertTrue(initialState is AccountConnectionUiState.Linked)
        }

        linkedViewModel.triggerBackup()

        linkedViewModel.iCloudConnectionUiState.test {
            val finalState = awaitItem()
            assertTrue(finalState is AccountConnectionUiState.Linked)
        }
    }

    @Test
    fun `triggerBackup does nothing when unlinked`() = runTest {
        viewModel.iCloudConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Unlinked)

            viewModel.triggerBackup()

            expectNoEvents()
        }
    }

    @Test
    fun `unlink sets state to Unlinked and clears iCloud settings`() = runTest {
        iCloudSettings.setUseICloud(true)

        val linkedViewModel: ICloudSyncViewModel = getKoin().get()

        linkedViewModel.iCloudConnectionUiState.test {
            val initialState = awaitItem()
            assertTrue(initialState is AccountConnectionUiState.Linked)

            linkedViewModel.unlink()

            val loadingState = awaitItem()
            assertTrue(loadingState is AccountConnectionUiState.Loading)

            val unlinkedState = awaitItem()
            assertTrue(unlinkedState is AccountConnectionUiState.Unlinked)
        }
    }

    @Test
    fun `restoreICloudAuth returns Linked state when iCloud was previously configured`() = runTest {
        iCloudSettings.setUseICloud(true)
        iCloudSettings.setLastUploadTimestamp(1234567890L)
        iCloudSettings.setLastDownloadTimestamp(1234567800L)

        val linkedViewModel: ICloudSyncViewModel = getKoin().get()

        linkedViewModel.iCloudConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            val syncState = state.syncState
            assertTrue(syncState is AccountSyncUIState.Synced)
            assertNotNull(syncState.lastUploadDate)
            assertNotNull(syncState.lastDownloadDate)
        }
    }

    @Test
    fun `restoreICloudAuth returns Linked with None sync state when no timestamps`() = runTest {
        iCloudSettings.setUseICloud(true)

        val linkedViewModel: ICloudSyncViewModel = getKoin().get()

        linkedViewModel.iCloudConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            assertEquals(AccountSyncUIState.None, state.syncState)
        }
    }

    @Test
    fun `getSyncState returns Synced when upload timestamp exists`() = runTest {
        iCloudSettings.setUseICloud(true)
        iCloudSettings.setLastUploadTimestamp(1234567890L)

        val linkedViewModel: ICloudSyncViewModel = getKoin().get()

        linkedViewModel.iCloudConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            val syncState = state.syncState
            assertTrue(syncState is AccountSyncUIState.Synced)
            assertNotNull(syncState.lastUploadDate)
        }
    }

    @Test
    fun `getSyncState returns Synced when download timestamp exists`() = runTest {
        iCloudSettings.setUseICloud(true)
        iCloudSettings.setLastDownloadTimestamp(1234567800L)

        val linkedViewModel: ICloudSyncViewModel = getKoin().get()

        linkedViewModel.iCloudConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            val syncState = state.syncState
            assertTrue(syncState is AccountSyncUIState.Synced)
            assertNotNull(syncState.lastDownloadDate)
        }
    }
}
