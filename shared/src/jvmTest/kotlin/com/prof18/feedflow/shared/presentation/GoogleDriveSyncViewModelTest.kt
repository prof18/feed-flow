package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.GoogleDriveSynMessages
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceJvm
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDownloadParam
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDownloadResult
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveException
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveUploadParam
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveUploadResult
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GoogleDriveSyncViewModelTest : KoinTestBase() {

    private val fakeGoogleDriveDataSource = GoogleDriveDataSourceFake()
    private val viewModel: GoogleDriveSyncViewModel by inject()
    private val googleDriveSettings: GoogleDriveSettings by inject()

    override fun getTestModules(): List<Module> {
        return super.getTestModules() + module {
            factory<GoogleDriveDataSourceJvm> { fakeGoogleDriveDataSource }
        }
    }

    @Test
    fun `initial state is Unlinked when Google Drive is not configured`() = runTest {
        resetState()

        viewModel.googleDriveConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Unlinked)
        }
    }

    @Test
    fun `restoreGoogleDriveAuth returns Linked state when previously configured`() = runTest {
        resetState()
        fakeGoogleDriveDataSource.restoreAuthResult = true
        googleDriveSettings.setLastUploadTimestamp(1234567890L)
        googleDriveSettings.setLastDownloadTimestamp(1234567800L)

        val linkedViewModel: GoogleDriveSyncViewModel = getKoin().get()

        linkedViewModel.googleDriveConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            val syncState = state.syncState
            assertTrue(syncState is AccountSyncUIState.Synced)
            assertNotNull(syncState.lastUploadDate)
            assertNotNull(syncState.lastDownloadDate)
        }
    }

    @Test
    fun `restoreGoogleDriveAuth returns Linked with None sync state when no timestamps`() = runTest {
        resetState()
        fakeGoogleDriveDataSource.restoreAuthResult = true

        val linkedViewModel: GoogleDriveSyncViewModel = getKoin().get()

        linkedViewModel.googleDriveConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            assertEquals(AccountSyncUIState.None, state.syncState)
        }
    }

    @Test
    fun `startGoogleDriveAuthFlow updates state and settings on success`() = runTest {
        resetState()
        fakeGoogleDriveDataSource.startAuthFlowResult = true

        viewModel.startGoogleDriveAuthFlow()

        val state = viewModel.googleDriveConnectionUiState.value
        assertTrue(state is AccountConnectionUiState.Linked)
        assertEquals(AccountSyncUIState.None, state.syncState)
    }

    @Test
    fun `startGoogleDriveAuthFlow emits error and stays unlinked on failure`() = runTest {
        resetState()
        fakeGoogleDriveDataSource.startAuthFlowResult = false

        viewModel.googleDriveSyncMessageState.test {
            viewModel.startGoogleDriveAuthFlow()
            assertEquals(GoogleDriveSynMessages.Error, awaitItem())
        }

        assertTrue(viewModel.googleDriveConnectionUiState.value is AccountConnectionUiState.Unlinked)
    }

    @Test
    fun `disconnect sets state to Unlinked and revokes access`() = runTest {
        resetState()
        fakeGoogleDriveDataSource.restoreAuthResult = true
        googleDriveSettings.setGoogleDriveLinked(true)
        val linkedViewModel: GoogleDriveSyncViewModel = getKoin().get()

        linkedViewModel.googleDriveConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)

            linkedViewModel.disconnect()

            val loadingState = awaitItem()
            assertTrue(loadingState is AccountConnectionUiState.Loading)

            val unlinkedState = awaitItem()
            assertTrue(unlinkedState is AccountConnectionUiState.Unlinked)
        }

        assertTrue(fakeGoogleDriveDataSource.revokeAccessCallCount > 0)
    }

    @Test
    fun `disconnect emits error when revokeAccess fails`() = runTest {
        resetState()
        fakeGoogleDriveDataSource.restoreAuthResult = true
        googleDriveSettings.setGoogleDriveLinked(true)
        fakeGoogleDriveDataSource.revokeAccessException = GoogleDriveException(
            causeException = IllegalStateException("boom"),
            errorMessage = "failed",
        )
        val linkedViewModel: GoogleDriveSyncViewModel = getKoin().get()

        linkedViewModel.googleDriveSyncMessageState.test {
            linkedViewModel.disconnect()
            assertEquals(GoogleDriveSynMessages.Error, awaitItem())
        }
    }

    private fun resetState() {
        fakeGoogleDriveDataSource.reset()
        googleDriveSettings.clearAll()
    }
}

private class GoogleDriveDataSourceFake : GoogleDriveDataSourceJvm {
    var startAuthFlowResult: Boolean = false
    var restoreAuthResult: Boolean = false
    var revokeAccessException: GoogleDriveException? = null
    var revokeAccessCallCount: Int = 0
        private set

    override suspend fun startAuthFlow(): Boolean {
        return startAuthFlowResult
    }

    override fun restoreAuth(): Boolean = restoreAuthResult

    override suspend fun revokeAccess() {
        revokeAccessCallCount++
        revokeAccessException?.let { throw it }
    }

    override fun isClientSet(): Boolean = restoreAuthResult

    override suspend fun performUpload(uploadParam: GoogleDriveUploadParam): GoogleDriveUploadResult =
        GoogleDriveUploadResult

    override suspend fun performDownload(downloadParam: GoogleDriveDownloadParam): GoogleDriveDownloadResult =
        GoogleDriveDownloadResult()

    fun reset() {
        startAuthFlowResult = false
        restoreAuthResult = false
        revokeAccessException = null
        revokeAccessCallCount = 0
    }
}
