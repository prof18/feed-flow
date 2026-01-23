package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.GoogleDriveSynMessages
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.googledrive.GoogleDrivePlatformClientIos
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import platform.Foundation.NSData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GoogleDriveSyncViewModelTest : KoinTestBase() {

    private val fakePlatformClient = GoogleDrivePlatformClientIosFake()
    private val viewModel: GoogleDriveSyncViewModel by inject()
    private val googleDriveSettings: GoogleDriveSettings by inject()
    private val accountsRepository: AccountsRepository by inject()

    override fun getTestModules(): List<Module> {
        return super.getTestModules() + module {
            factory<GoogleDrivePlatformClientIos> { fakePlatformClient }
        }
    }

    @Test
    fun `initial state is Unlinked when Google Drive is not authorized`() = runTest {
        viewModel.googleDriveConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Unlinked)
        }
    }

    @Test
    fun `restoreAccount returns Linked state when Google Drive was previously authorized`() = runTest {
        fakePlatformClient.restoreSuccess = true
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
    fun `restoreAccount returns Linked with None sync state when no timestamps`() = runTest {
        fakePlatformClient.restoreSuccess = true

        val linkedViewModel: GoogleDriveSyncViewModel = getKoin().get()

        linkedViewModel.googleDriveConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            assertEquals(AccountSyncUIState.None, state.syncState)
        }
    }

    @Test
    fun `startAuthentication updates state and settings on success`() = runTest {
        fakePlatformClient.authenticateSuccess = true

        viewModel.startAuthentication()
        val state = viewModel.googleDriveConnectionUiState.value
        assertTrue(state is AccountConnectionUiState.Linked)
        assertEquals(AccountSyncUIState.None, state.syncState)
        assertTrue(googleDriveSettings.isGoogleDriveLinked())
        assertEquals(SyncAccounts.GOOGLE_DRIVE, accountsRepository.getCurrentSyncAccount())
    }

    @Test
    fun `startAuthentication emits error and sets state to Unlinked on failure`() = runTest {
        fakePlatformClient.authenticateSuccess = false

        viewModel.googleDriveSyncMessageState.test {
            viewModel.startAuthentication()
            assertEquals(GoogleDriveSynMessages.Error, awaitItem())
        }
        assertTrue(viewModel.googleDriveConnectionUiState.value is AccountConnectionUiState.Unlinked)
    }

    @Test
    fun `triggerBackup keeps linked state`() = runTest {
        fakePlatformClient.restoreSuccess = true
        val linkedViewModel: GoogleDriveSyncViewModel = getKoin().get()

        linkedViewModel.googleDriveConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)
        }

        linkedViewModel.triggerBackup()

        assertTrue(linkedViewModel.googleDriveConnectionUiState.value is AccountConnectionUiState.Linked)
    }

    @Test
    fun `unlink sets state to Unlinked and revokes access`() = runTest {
        fakePlatformClient.restoreSuccess = true
        val linkedViewModel: GoogleDriveSyncViewModel = getKoin().get()

        linkedViewModel.googleDriveConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)

            linkedViewModel.unlink()

            val loadingState = awaitItem()
            assertTrue(loadingState is AccountConnectionUiState.Loading)

            val unlinkedState = awaitItem()
            assertTrue(unlinkedState is AccountConnectionUiState.Unlinked)
        }

        assertTrue(fakePlatformClient.signOutCallCount > 0)
        assertTrue(!googleDriveSettings.isGoogleDriveLinked())
    }
}

private class GoogleDrivePlatformClientIosFake : GoogleDrivePlatformClientIos {
    var authenticateSuccess: Boolean = false
    var restoreSuccess: Boolean = false
    var signOutCallCount: Int = 0
        private set

    override fun authenticate(onResult: (Boolean) -> Unit) {
        onResult(authenticateSuccess)
    }

    override fun restorePreviousSignIn(onResult: (Boolean) -> Unit) {
        onResult(restoreSuccess)
    }

    override fun isAuthorized(): Boolean = restoreSuccess

    override fun isServiceSet(): Boolean = true

    override fun signOut() {
        signOutCallCount++
    }

    override fun uploadFile(
        data: NSData,
        fileName: String,
        existingFileId: String?,
        completionHandler: (String?, Throwable?) -> Unit,
    ) {
        completionHandler(null, Exception("Not used in tests"))
    }

    override fun downloadFile(
        fileName: String,
        existingFileId: String?,
        completionHandler: (NSData?, Throwable?) -> Unit,
    ) {
        completionHandler(null, Exception("Not used in tests"))
    }
}
