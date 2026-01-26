package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.feedbin.configureFeedbinMocks
import com.prof18.feedflow.feedsync.test.feedbin.configureFeedbinMocksWithLoginFailure
import com.prof18.feedflow.feedsync.test.feedbin.configureFeedbinMocksWithSyncFailure
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

class FeedbinSyncViewModelTest : KoinTestBase() {

    private val networkSettings: NetworkSettings by inject()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + getFeedSyncTestModules(
            feedbinBaseURL = "https://api.feedbin.com/",
            feedbinConfig = {
                configureFeedbinMocks()
            },
        )

    @Test
    fun `initial state is Unlinked when no account is set`() = runTest(testDispatcher) {
        val viewModel: FeedbinSyncViewModel = get()

        viewModel.uiState.test {
            val state = awaitItem()
            assertIs<AccountConnectionUiState.Unlinked>(state)
        }
    }

    @Test
    fun `initial state is Linked when account is set`() = runTest(testDispatcher) {
        // Setup: Set account credentials
        networkSettings.setSyncAccountType(SyncAccounts.FEEDBIN)
        networkSettings.setSyncPwd("testpassword")
        networkSettings.setSyncUsername("testuser")

        val viewModel: FeedbinSyncViewModel = get()
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
        networkSettings.setSyncAccountType(SyncAccounts.FEEDBIN)
        networkSettings.setSyncPwd("testpassword")
        networkSettings.setSyncUsername("testuser")
        networkSettings.setLastSyncDate(1234567890L)

        val viewModel: FeedbinSyncViewModel = get()
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
        val viewModel: FeedbinSyncViewModel = get()

        viewModel.uiState.test {
            val unlinkedState = awaitItem()
            assertIs<AccountConnectionUiState.Unlinked>(unlinkedState)

            viewModel.login(
                username = "testuser",
                password = "testpassword",
            )

            runCurrent()
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountConnectionUiState.Linked>(state)
        }
    }

    @Test
    fun `login sets loading state during login`() = runTest(testDispatcher) {
        val viewModel: FeedbinSyncViewModel = get()
        advanceUntilIdle()

        viewModel.loginLoading.test {
            assertEquals(false, awaitItem())

            viewModel.login(
                username = "testuser",
                password = "testpassword",
            )

            assertEquals(true, awaitItem())
            advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `disconnect clears account and sets state to Unlinked`() = runTest(testDispatcher) {
        // Setup: Set account credentials
        networkSettings.setSyncAccountType(SyncAccounts.FEEDBIN)
        networkSettings.setSyncPwd("testpassword")
        networkSettings.setSyncUsername("testuser")

        val viewModel: FeedbinSyncViewModel = get()

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
        assertTrue(networkSettings.getSyncUsername().isEmpty())
    }

    @Test
    fun `getSyncState returns None when no sync date`() = runTest(testDispatcher) {
        // Setup: Set account credentials without sync date
        networkSettings.setSyncAccountType(SyncAccounts.FEEDBIN)
        networkSettings.setSyncPwd("testpassword")
        networkSettings.setSyncUsername("testuser")

        val viewModel: FeedbinSyncViewModel = get()
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
        networkSettings.setSyncAccountType(SyncAccounts.FEEDBIN)
        networkSettings.setSyncPwd("testpassword")
        networkSettings.setSyncUsername("testuser")
        networkSettings.setLastSyncDate(1234567890L)

        val viewModel: FeedbinSyncViewModel = get()
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
    fun `login success sets Feedbin account type`() = runTest(testDispatcher) {
        val viewModel: FeedbinSyncViewModel = get()

        viewModel.uiState.test {
            val unlinkedState = awaitItem()
            assertIs<AccountConnectionUiState.Unlinked>(unlinkedState)

            viewModel.login(
                username = "testuser",
                password = "testpassword",
            )

            runCurrent()
            advanceUntilIdle()

            val state = awaitItem()
            assertIs<AccountConnectionUiState.Linked>(state)

            val accountType = networkSettings.getSyncAccountType()
            assertEquals(SyncAccounts.FEEDBIN, accountType)
            assertEquals("testuser", networkSettings.getSyncUsername())
        }
    }

    @Test
    fun `disconnect clears account and sets state to Unlinked from Linked after login`() = runTest(testDispatcher) {
        val viewModel: FeedbinSyncViewModel = get()

        viewModel.uiState.test {
            val unlinkedState = awaitItem()
            assertIs<AccountConnectionUiState.Unlinked>(unlinkedState)

            viewModel.login(
                username = "testuser",
                password = "testpassword",
            )

            runCurrent()
            advanceUntilIdle()

            val linkedState = awaitItem()
            assertIs<AccountConnectionUiState.Linked>(linkedState)

            // Disconnect
            viewModel.disconnect()

            runCurrent()
            advanceUntilIdle()

            val loadingState = awaitItem()
            assertIs<AccountConnectionUiState.Loading>(loadingState)

            val finalState = awaitItem()
            assertIs<AccountConnectionUiState.Unlinked>(finalState)
        }

        // Verify account is cleared
        assertTrue(networkSettings.getSyncPwd().isEmpty())
        assertTrue(networkSettings.getSyncUsername().isEmpty())
    }

    @Test
    fun `login failure emits error state`() = runTest(testDispatcher) {
        // Reconfigure Koin with login failure mocks
        stopKoin()
        startKoin {
            allowOverride(true)
            modules(
                TestModules.createTestModules() + getFeedSyncTestModules(
                    feedbinBaseURL = "https://api.feedbin.com/",
                    feedbinConfig = {
                        configureFeedbinMocksWithLoginFailure()
                    },
                ),
            )
        }

        val viewModel: FeedbinSyncViewModel = get()

        viewModel.errorState.test {
            viewModel.login(
                username = "testuser",
                password = "wrongpassword",
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
                    feedbinBaseURL = "https://api.feedbin.com/",
                    feedbinConfig = {
                        configureFeedbinMocksWithSyncFailure()
                    },
                ),
            )
        }

        val viewModel: FeedbinSyncViewModel = get()

        viewModel.errorState.test {
            viewModel.login(
                username = "testuser",
                password = "testpassword",
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
