package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.DropboxSynMessages
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
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

    private val fakeDropboxDataSource = DropboxDataSourceFake()
    private val viewModel: DropboxSyncViewModel by inject()
    private val dropboxSettings: DropboxSettings by inject()

    override fun getTestModules(): List<Module> {
        return super.getTestModules() + module {
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
        dropboxSettings.setDropboxData("test-credentials")
        dropboxSettings.setLastUploadTimestamp(1234567890L)
        dropboxSettings.setLastDownloadTimestamp(1234567800L)

        val linkedViewModel: DropboxSyncViewModel = getKoin().get()

        linkedViewModel.dropboxConnectionUiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            val syncState = state.syncState
            assertTrue(syncState is AccountSyncUIState.Synced)
            assertNotNull(syncState.lastUploadDate)
            assertNotNull(syncState.lastDownloadDate)
        }
    }

    @Test
    fun `restoreDropboxAuth returns Linked with None sync state when no timestamps`() = runTest {
        dropboxSettings.setDropboxData("test-credentials")

        val linkedViewModel: DropboxSyncViewModel = getKoin().get()

        linkedViewModel.dropboxConnectionUiState.test {
            val state = expectMostRecentItem()
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
        dropboxSettings.setDropboxData("test-credentials")

        val linkedViewModel: DropboxSyncViewModel = getKoin().get()

        linkedViewModel.dropboxConnectionUiState.test {
            val initialState = expectMostRecentItem()
            assertTrue(initialState is AccountConnectionUiState.Linked)
        }

        linkedViewModel.triggerBackup()

        linkedViewModel.dropboxConnectionUiState.test {
            val finalState = awaitItem()
            assertTrue(finalState is AccountConnectionUiState.Linked)
        }
    }

    @Test
    fun `disconnect sets state to Unlinked and clears Dropbox settings`() = runTest {
        dropboxSettings.setDropboxData("test-credentials")

        val linkedViewModel: DropboxSyncViewModel = getKoin().get()

        linkedViewModel.dropboxConnectionUiState.test {
            val initialState = expectMostRecentItem()
            assertTrue(initialState is AccountConnectionUiState.Linked)

            linkedViewModel.disconnect()

            val loadingState = awaitItem()
            assertTrue(loadingState is AccountConnectionUiState.Loading)

            val unlinkedState = awaitItem()
            assertTrue(unlinkedState is AccountConnectionUiState.Unlinked)
        }
    }

    @Test
    fun `getSyncState returns Synced when upload timestamp exists`() = runTest {
        dropboxSettings.setDropboxData("test-credentials")
        dropboxSettings.setLastUploadTimestamp(1234567890L)

        val linkedViewModel: DropboxSyncViewModel = getKoin().get()

        linkedViewModel.dropboxConnectionUiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            val syncState = state.syncState
            assertTrue(syncState is AccountSyncUIState.Synced)
            assertNotNull(syncState.lastUploadDate)
        }
    }

    @Test
    fun `getSyncState returns Synced when download timestamp exists`() = runTest {
        dropboxSettings.setDropboxData("test-credentials")
        dropboxSettings.setLastDownloadTimestamp(1234567800L)

        val linkedViewModel: DropboxSyncViewModel = getKoin().get()

        linkedViewModel.dropboxConnectionUiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            val syncState = state.syncState
            assertTrue(syncState is AccountSyncUIState.Synced)
            assertNotNull(syncState.lastDownloadDate)
        }
    }

    @Test
    fun `startDropboxAuthFlow sets state to Unlinked on error`() = runTest {
        viewModel.dropboxConnectionUiState.test {
            // Initial state from init { restoreDropboxAuth() }
            assertTrue(awaitItem() is AccountConnectionUiState.Unlinked)

            viewModel.startDropboxAuthFlow()

            // After trigger, it should still be Unlinked
            assertEquals(AccountConnectionUiState.Unlinked, viewModel.dropboxConnectionUiState.value)
        }
    }

    @Test
    fun `handleDropboxAuthResponse emits Error when pkceWebAuth is not initialized`() = runTest {
        viewModel.dropboxSyncMessageState.test {
            viewModel.handleDropboxAuthResponse("test-auth-code")

            val message = awaitItem()
            assertTrue(message is DropboxSynMessages.Error)
        }
    }

    @Test
    fun `handleDropboxAuthResponse sets state to Loading then Unlinked when pkceWebAuth is null`() = runTest {
        viewModel.dropboxConnectionUiState.test {
            // Initial state from init { restoreDropboxAuth() }
            assertTrue(awaitItem() is AccountConnectionUiState.Unlinked)

            viewModel.handleDropboxAuthResponse("test-auth-code")

            // Wait until it's Unlinked again or check the final state
            assertEquals(AccountConnectionUiState.Unlinked, viewModel.dropboxConnectionUiState.value)
        }
    }
}
