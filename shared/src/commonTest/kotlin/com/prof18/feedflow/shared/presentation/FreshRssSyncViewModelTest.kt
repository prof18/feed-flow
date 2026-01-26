package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.greader.configureFreshRssMocks
import com.prof18.feedflow.feedsync.test.greader.configureFreshRssMocksWithLoginFailure
import com.prof18.feedflow.feedsync.test.greader.configureFreshRssMocksWithSyncFailure
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.koin.TestModules
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.test.get
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FreshRssSyncViewModelTest : KoinTestBase() {

    private val networkSettings: NetworkSettings by inject()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + getFeedSyncTestModules(
            gReaderProvider = SyncAccounts.FRESH_RSS,
            gReaderBaseURL = "https://freshrss.example.com/api/greader.php/",
            gReaderConfig = {
                configureFreshRssMocks()
            },
        )

    @Test
    fun `initial state is Unlinked when no account is set`() = runTest(testDispatcher) {
        val viewModel: FreshRssSyncViewModel = get()

        viewModel.uiState.test {
            val state = awaitItem()
            assertIs<AccountConnectionUiState.Unlinked>(state)
        }
    }

    @Test
    fun `initial state is Linked when account is set`() = runTest(testDispatcher) {
        // Setup: Set account credentials
        networkSettings.setSyncAccountType(SyncAccounts.FRESH_RSS)
        networkSettings.setSyncPwd("test-auth-token")
        networkSettings.setSyncUrl("https://freshrss.example.com/api/greader.php")
        networkSettings.setSyncUsername("testuser")

        val viewModel: FreshRssSyncViewModel = get()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertIs<AccountConnectionUiState.Linked>(state)
            assertEquals(AccountSyncUIState.None, state.syncState)
        }
    }

    @Test
    fun `initial state is Linked with Synced state when account is set and has sync date`() = runTest(testDispatcher) {
        // Setup: Set account credentials and sync date
        networkSettings.setSyncAccountType(SyncAccounts.FRESH_RSS)
        networkSettings.setSyncPwd("test-auth-token")
        networkSettings.setSyncUrl("https://freshrss.example.com/api/greader.php")
        networkSettings.setSyncUsername("testuser")
        networkSettings.setLastSyncDate(1234567890L)

        val viewModel: FreshRssSyncViewModel = get()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertIs<AccountConnectionUiState.Linked>(state)
            val syncState = assertIs<AccountSyncUIState.Synced>(state.syncState)
            assertNotNull(syncState.lastDownloadDate)
        }
    }

    @Test
    fun `login success with sync success sets state to Linked`() = runTest(testDispatcher) {
        val viewModel: FreshRssSyncViewModel = get()

        viewModel.uiState.test {
            val unlinkedState = awaitItem()
            assertIs<AccountConnectionUiState.Unlinked>(unlinkedState)

            viewModel.login(
                username = "testuser",
                password = "testpassword",
                url = "https://freshrss.example.com/api/greader.php",
            )

            runCurrent()
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountConnectionUiState.Linked>(state)
        }
    }

    @Test
    fun `login sets loading state during login`() = runTest(testDispatcher) {
        val viewModel: FreshRssSyncViewModel = get()
        advanceUntilIdle()

        viewModel.loginLoading.test {
            assertEquals(false, awaitItem())

            viewModel.login(
                username = "testuser",
                password = "testpassword",
                url = "https://freshrss.example.com/api/greader.php",
            )

            assertEquals(true, awaitItem())
            advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `disconnect clears account and sets state to Unlinked`() = runTest(testDispatcher) {
        // Setup: Set account credentials
        networkSettings.setSyncAccountType(SyncAccounts.FRESH_RSS)
        networkSettings.setSyncPwd("test-auth-token")
        networkSettings.setSyncUrl("https://freshrss.example.com/api/greader.php")
        networkSettings.setSyncUsername("testuser")

        val viewModel: FreshRssSyncViewModel = get()

        viewModel.uiState.test {
            // Skip initial Loading and Linked states
            val linkedState = awaitItem()
            assertIs<AccountConnectionUiState.Linked>(linkedState)

            viewModel.disconnect()

            runCurrent()
            advanceUntilIdle()

            val loadingState = awaitItem()
            assertIs<AccountConnectionUiState.Loading>(loadingState)

            val unlinkedState = awaitItem()
            assertIs<AccountConnectionUiState.Unlinked>(unlinkedState)
        }

        // Verify account is cleared
        assertTrue(networkSettings.getSyncPwd().isEmpty())
        assertTrue(networkSettings.getSyncUrl().isEmpty())
    }

    @Test
    fun `getSyncState returns None when no sync date`() = runTest(testDispatcher) {
        // Setup: Set account credentials without sync date
        networkSettings.setSyncAccountType(SyncAccounts.FRESH_RSS)
        networkSettings.setSyncPwd("test-auth-token")
        networkSettings.setSyncUrl("https://freshrss.example.com/api/greader.php")
        networkSettings.setSyncUsername("testuser")

        val viewModel: FreshRssSyncViewModel = get()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertIs<AccountConnectionUiState.Linked>(state)
            assertEquals(AccountSyncUIState.None, state.syncState)
        }
    }

    @Test
    fun `getSyncState returns Synced when sync date exists`() = runTest(testDispatcher) {
        // Setup: Set account credentials with sync date
        networkSettings.setSyncAccountType(SyncAccounts.FRESH_RSS)
        networkSettings.setSyncPwd("test-auth-token")
        networkSettings.setSyncUrl("https://freshrss.example.com/api/greader.php")
        networkSettings.setSyncUsername("testuser")
        networkSettings.setLastSyncDate(1234567890L)

        val viewModel: FreshRssSyncViewModel = get()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertIs<AccountConnectionUiState.Linked>(state)
            val syncState = assertIs<AccountSyncUIState.Synced>(state.syncState)
            assertNotNull(syncState.lastDownloadDate)
            assertEquals(null, syncState.lastUploadDate)
        }
    }

    @Test
    fun `login success sets FreshRSS account type`() = runTest(testDispatcher) {
        val viewModel: FreshRssSyncViewModel = get()

        viewModel.uiState.test {
            val unlinkedState = awaitItem()
            assertIs<AccountConnectionUiState.Unlinked>(unlinkedState)

            viewModel.login(
                username = "testuser",
                password = "testpassword",
                url = "https://freshrss.example.com/api/greader.php",
            )

            runCurrent()
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountConnectionUiState.Linked>(state)

            val accountType = networkSettings.getSyncAccountType()
            assertEquals(SyncAccounts.FRESH_RSS, accountType)
            assertEquals("testuser", networkSettings.getSyncUsername())
            assertEquals("https://freshrss.example.com/api/greader.php", networkSettings.getSyncUrl())
        }
    }

    @Test
    fun `login success syncs feed sources to database`() = runTest(testDispatcher) {
        val databaseHelper: DatabaseHelper = get()
        val viewModel: FreshRssSyncViewModel = get()

        // Verify database is empty before login
        assertTrue(databaseHelper.getFeedSources().isEmpty())

        viewModel.uiState.test {
            val unlinkedState = awaitItem()
            assertIs<AccountConnectionUiState.Unlinked>(unlinkedState)

            viewModel.login(
                username = "testuser",
                password = "testpassword",
                url = "https://freshrss.example.com/api/greader.php",
            )

            runCurrent()
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountConnectionUiState.Linked>(state)
        }

        // Verify feed sources were synced to database
        val feedSources = databaseHelper.getFeedSources()
        assertTrue(feedSources.isNotEmpty())
    }

    @Test
    fun `disconnect clears feed data from database`() = runTest(testDispatcher) {
        val databaseHelper: DatabaseHelper = get()
        val viewModel: FreshRssSyncViewModel = get()

        viewModel.uiState.test {
            val unlinkedState = awaitItem()
            assertIs<AccountConnectionUiState.Unlinked>(unlinkedState)

            viewModel.login(
                username = "testuser",
                password = "testpassword",
                url = "https://freshrss.example.com/api/greader.php",
            )

            runCurrent()
            advanceUntilIdle()

            val linkedState = awaitItem()
            assertIs<AccountConnectionUiState.Linked>(linkedState)

            // Verify we have feed sources after login
            assertTrue(databaseHelper.getFeedSources().isNotEmpty())

            // Disconnect
            viewModel.disconnect()

            runCurrent()
            advanceUntilIdle()

            val loadingState = awaitItem()
            assertIs<AccountConnectionUiState.Loading>(loadingState)

            val finalState = awaitItem()
            assertIs<AccountConnectionUiState.Unlinked>(finalState)
        }

        // Verify database is cleared after disconnect
        assertTrue(databaseHelper.getFeedSources().isEmpty())
    }

    @Test
    fun `login failure emits error state`() = runTest(testDispatcher) {
        // Reconfigure Koin with login failure mocks
        stopKoin()
        startKoin {
            allowOverride(true)
            modules(
                TestModules.createTestModules() + getFeedSyncTestModules(
                    gReaderProvider = SyncAccounts.FRESH_RSS,
                    gReaderBaseURL = "https://freshrss.example.com/api/greader.php/",
                    gReaderConfig = {
                        configureFreshRssMocksWithLoginFailure()
                    },
                ),
            )
        }

        val viewModel: FreshRssSyncViewModel = get()

        viewModel.errorState.test {
            viewModel.login(
                username = "testuser",
                password = "wrongpassword",
                url = "https://freshrss.example.com/api/greader.php",
            )

            runCurrent()
            advanceUntilIdle()

            // Verify error was emitted
            val error = awaitItem()
            assertNotNull(error)
        }

        // Verify state is still Unlinked
        assertIs<AccountConnectionUiState.Unlinked>(viewModel.uiState.value)
    }

    @Test
    fun `sync failure after login emits error but sets linked state`() = runTest(testDispatcher) {
        // Reconfigure Koin with sync failure mocks
        stopKoin()
        startKoin {
            allowOverride(true)
            modules(
                TestModules.createTestModules() + getFeedSyncTestModules(
                    gReaderProvider = SyncAccounts.FRESH_RSS,
                    gReaderBaseURL = "https://freshrss.example.com/api/greader.php/",
                    gReaderConfig = {
                        configureFreshRssMocksWithSyncFailure()
                    },
                ),
            )
        }

        val viewModel: FreshRssSyncViewModel = get()

        viewModel.errorState.test {
            viewModel.login(
                username = "testuser",
                password = "testpassword",
                url = "https://freshrss.example.com/api/greader.php",
            )

            runCurrent()
            advanceUntilIdle()

            // Verify error was emitted for sync failure
            val error = awaitItem()
            assertNotNull(error)
        }

        // Login succeeded but sync failed - should still be Linked
        assertIs<AccountConnectionUiState.Linked>(viewModel.uiState.value)
    }
}
