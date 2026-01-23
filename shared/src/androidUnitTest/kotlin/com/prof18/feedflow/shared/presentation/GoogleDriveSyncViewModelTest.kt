package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.GoogleDriveSynMessages
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.googledrive.AuthorizationValidationResult
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceAndroid
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDownloadParam
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDownloadResult
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveUploadParam
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveUploadResult
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GoogleDriveSyncViewModelTest : KoinTestBase() {

    private val fakeGoogleDriveDataSource = GoogleDriveDataSourceAndroidFake()
    private val viewModel: GoogleDriveSyncViewModel by inject()
    private val googleDriveSettings: GoogleDriveSettings by inject()
    private val accountsRepository: AccountsRepository by inject()

    override fun getTestModules(): List<Module> {
        return super.getTestModules() + module {
            factory<GoogleDriveDataSourceAndroid> { fakeGoogleDriveDataSource }
        }
    }

    @Test
    fun `initial state is Unlinked when Google Drive is not authorized`() = runTest {
        viewModel.googleDriveConnectionUiState.test {
            awaitItem() shouldBe AccountConnectionUiState.Unlinked
        }
    }

    @Test
    fun `restoreAccount returns Linked state when Google Drive was previously authorized`() = runTest {
        fakeGoogleDriveDataSource.authorized = true
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
        fakeGoogleDriveDataSource.authorized = true

        val linkedViewModel: GoogleDriveSyncViewModel = getKoin().get()

        linkedViewModel.googleDriveConnectionUiState.test {
            val state = awaitItem()
            assertTrue(state is AccountConnectionUiState.Linked)
            assertEquals(AccountSyncUIState.None, state.syncState)
        }
    }

    @Test
    fun `onAuthorizationSuccess updates state and settings`() = runTest {
        viewModel.onAuthorizationSuccess()

        val state = viewModel.googleDriveConnectionUiState.value
        assertTrue(state is AccountConnectionUiState.Linked)
        assertEquals(AccountSyncUIState.None, state.syncState)
        assertTrue(googleDriveSettings.isGoogleDriveLinked())
        assertEquals(SyncAccounts.GOOGLE_DRIVE, accountsRepository.getCurrentSyncAccount())
    }

    @Test
    fun `onAuthorizationFailed emits error and sets state to Unlinked`() = runTest {
        viewModel.googleDriveSyncMessageState.test {
            viewModel.onAuthorizationFailed()
            assertEquals(GoogleDriveSynMessages.Error, awaitItem())
        }
        assertTrue(viewModel.googleDriveConnectionUiState.value is AccountConnectionUiState.Unlinked)
    }

    @Test
    fun `triggerBackup keeps linked state`() = runTest {
        fakeGoogleDriveDataSource.authorized = true

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
        fakeGoogleDriveDataSource.authorized = true
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

        assertTrue(fakeGoogleDriveDataSource.revokeAccessCallCount > 0)
    }

    @Test
    fun `showLoading updates state to Loading`() = runTest {
        viewModel.showLoading()
        assertTrue(viewModel.googleDriveConnectionUiState.value is AccountConnectionUiState.Loading)
    }

    @Test
    fun `validateAuthorization delegates to data source`() = runTest {
        fakeGoogleDriveDataSource.validateAuthorizationResult = AuthorizationValidationResult.Failed

        val result = viewModel.validateAuthorization()

        assertEquals(AuthorizationValidationResult.Failed, result)
    }
}

private class GoogleDriveDataSourceAndroidFake : GoogleDriveDataSourceAndroid {
    var authorized: Boolean = false
    var revokeAccessCallCount = 0
        private set
    var validateAuthorizationResult: AuthorizationValidationResult = AuthorizationValidationResult.Valid

    override suspend fun isAuthorized(): Boolean = authorized

    override fun revokeAccess() {
        revokeAccessCallCount++
    }

    override suspend fun validateAuthorization(): AuthorizationValidationResult =
        validateAuthorizationResult

    override suspend fun performDownload(downloadParam: GoogleDriveDownloadParam): GoogleDriveDownloadResult =
        error("Not used in tests")

    override suspend fun performUpload(uploadParam: GoogleDriveUploadParam): GoogleDriveUploadResult =
        error("Not used in tests")
}
