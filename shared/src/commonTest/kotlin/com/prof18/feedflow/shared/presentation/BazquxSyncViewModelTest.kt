package com.prof18.feedflow.shared.presentation

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.greader.configureBazquxMocks
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.koin.TestModules
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class BazquxSyncViewModelTest : KoinTestBase() {

    private val uiTimeout = 10.seconds
    private val feedStateRepository: FeedStateRepository by inject()
    private val networkSettings: NetworkSettings by inject()
    private val viewModel: BazquxSyncViewModel by inject()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + getFeedSyncTestModules(
            gReaderProvider = SyncAccounts.BAZQUX,
            gReaderBaseURL = "https://bazqux.com/",
            gReaderConfig = {
                configureBazquxMocks()
            },
        )

    @Test
    fun `initial state is Unlinked when no account is set`() = runTest(testDispatcher) {
        viewModel.uiState.test(timeout = uiTimeout) {
            val state = awaitItemMatching { it is AccountConnectionUiState.Unlinked }
            assertIs<AccountConnectionUiState.Unlinked>(state)
        }
    }

    @Test
    fun `initial state is Linked when account is set`() = runTest(testDispatcher) {
        // Setup: Set account credentials
        networkSettings.setSyncAccountType(SyncAccounts.BAZQUX)
        networkSettings.setSyncPwd("test-auth-token")
        networkSettings.setSyncUrl("https://bazqux.com")
        networkSettings.setSyncUsername("testuser")

        advanceUntilIdle()

        viewModel.uiState.test(timeout = uiTimeout) {
            val state = awaitItemMatching { it is AccountConnectionUiState.Linked }
            assertIs<AccountConnectionUiState.Linked>(state)
            assertEquals(AccountSyncUIState.None, state.syncState)
        }
    }

    @Test
    fun `initial state is Linked with Synced state when account is set and has sync date`() =
        runTest(testDispatcher) {
            // Setup: Set account credentials and sync date
            networkSettings.setSyncAccountType(SyncAccounts.BAZQUX)
            networkSettings.setSyncPwd("test-auth-token")
            networkSettings.setSyncUrl("https://bazqux.com")
            networkSettings.setSyncUsername("testuser")
            networkSettings.setLastSyncDate(1234567890L)

            advanceUntilIdle()

            viewModel.uiState.test(timeout = uiTimeout) {
                val state = awaitItemMatching { it is AccountConnectionUiState.Linked }
                assertIs<AccountConnectionUiState.Linked>(state)
                val syncState = assertIs<AccountSyncUIState.Synced>(state.syncState)
                assertNotNull(syncState.lastDownloadDate)
            }
        }

    @Test
    fun `login success with sync success sets state to Linked`() = runTest(testDispatcher) {
        viewModel.uiState.test(timeout = uiTimeout) {
            val unlinkedState = awaitItemMatching { it is AccountConnectionUiState.Unlinked }
            assertIs<AccountConnectionUiState.Unlinked>(unlinkedState)

            viewModel.login(
                username = "testuser",
                password = "testpassword",
            )

            val state = awaitItemMatching { it is AccountConnectionUiState.Linked }
            assertIs<AccountConnectionUiState.Linked>(state)
        }
    }

    @Test
    fun `login sets loading state during login`() = runTest(testDispatcher) {
        viewModel.loginLoading.test(timeout = uiTimeout) {
            assertEquals(false, awaitItem())

            viewModel.login(
                username = "testuser",
                password = "testpassword",
            )

            // Wait for loading to become true
            var loadingState: Boolean
            do {
                loadingState = awaitItem()
            } while (!loadingState)
            assertTrue(loadingState)

            // Wait for loading to become false again
            do {
                loadingState = awaitItem()
            } while (loadingState)
            assertFalse(loadingState)
        }
    }

    @Test
    fun `disconnect clears account and sets state to Unlinked`() = runTest(testDispatcher) {
        // Setup: Set account credentials
        networkSettings.setSyncAccountType(SyncAccounts.BAZQUX)
        networkSettings.setSyncPwd("test-auth-token")
        networkSettings.setSyncUrl("https://bazqux.com")
        networkSettings.setSyncUsername("testuser")

        viewModel.uiState.test(timeout = uiTimeout) {
            // Skip initial Loading and Linked states
            val linkedState = awaitItemMatching { it is AccountConnectionUiState.Linked }
            assertIs<AccountConnectionUiState.Linked>(linkedState)

            viewModel.disconnect()

            runCurrent()
            advanceUntilIdle()

            val unlinkedState = awaitItemMatching { it is AccountConnectionUiState.Unlinked }
            assertIs<AccountConnectionUiState.Unlinked>(unlinkedState)
        }

        // Verify account is cleared
        assertTrue(networkSettings.getSyncPwd().isEmpty())
        assertTrue(networkSettings.getSyncUrl().isEmpty())
    }

    @Test
    fun `getSyncState returns None when no sync date`() = runTest(testDispatcher) {
        // Setup: Set account credentials without sync date
        networkSettings.setSyncAccountType(SyncAccounts.BAZQUX)
        networkSettings.setSyncPwd("test-auth-token")
        networkSettings.setSyncUrl("https://bazqux.com")
        networkSettings.setSyncUsername("testuser")

        advanceUntilIdle()

        viewModel.uiState.test(timeout = uiTimeout) {
            val state = awaitItemMatching { it is AccountConnectionUiState.Linked }
            assertIs<AccountConnectionUiState.Linked>(state)
            assertEquals(AccountSyncUIState.None, state.syncState)
        }
    }

    @Test
    fun `getSyncState returns Synced when sync date exists`() = runTest(testDispatcher) {
        // Setup: Set account credentials with sync date
        networkSettings.setSyncAccountType(SyncAccounts.BAZQUX)
        networkSettings.setSyncPwd("test-auth-token")
        networkSettings.setSyncUrl("https://bazqux.com")
        networkSettings.setSyncUsername("testuser")
        networkSettings.setLastSyncDate(1234567890L)

        advanceUntilIdle()

        viewModel.uiState.test(timeout = uiTimeout) {
            val state = awaitItemMatching { it is AccountConnectionUiState.Linked }
            assertIs<AccountConnectionUiState.Linked>(state)
            val syncState = assertIs<AccountSyncUIState.Synced>(state.syncState)
            assertNotNull(syncState.lastDownloadDate)
            assertEquals(null, syncState.lastUploadDate)
        }
    }

    @Test
    fun `login success sets Bazqux account type`() = runTest(testDispatcher) {
        viewModel.uiState.test(timeout = uiTimeout) {
            val unlinkedState = awaitItemMatching { it is AccountConnectionUiState.Unlinked }
            assertIs<AccountConnectionUiState.Unlinked>(unlinkedState)

            viewModel.login(
                username = "testuser",
                password = "testpassword",
            )

            val state = awaitItemMatching { it is AccountConnectionUiState.Linked }
            assertIs<AccountConnectionUiState.Linked>(state)

            val accountType = networkSettings.getSyncAccountType()
            assertEquals(SyncAccounts.BAZQUX, accountType)
            assertEquals("testuser", networkSettings.getSyncUsername())
            assertEquals("https://bazqux.com", networkSettings.getSyncUrl())
        }
    }

    @Test
    fun `login success syncs triggers feed item refresh`() = runTest(testDispatcher) {
        feedStateRepository.feedState.test(timeout = uiTimeout) {
            assertTrue(awaitItem().isEmpty())

            viewModel.login(
                username = "testuser",
                password = "testpassword",
            )

            // Wait for feeds to be populated
            var feeds = awaitItem()
            while (feeds.isEmpty()) {
                feeds = awaitItem()
            }
            assertTrue(feeds.isNotEmpty())
        }
    }

    @Test
    fun `disconnect clears feed data from database`() = runTest(testDispatcher) {
        feedStateRepository.feedState.test(timeout = uiTimeout) {
            assertTrue(awaitItem().isEmpty())

            viewModel.login(
                username = "testuser",
                password = "testpassword",
            )

            // Wait for feeds to be populated
            var feeds = awaitItem()
            while (feeds.isEmpty()) {
                feeds = awaitItem()
            }
            assertTrue(feeds.isNotEmpty())

            viewModel.disconnect()

            // Wait for feeds to be cleared
            do {
                feeds = awaitItem()
            } while (feeds.isNotEmpty())
            assertTrue(feeds.isEmpty())
        }
    }
}

private suspend fun <T> TurbineTestContext<T>.awaitItemMatching(
    predicate: (T) -> Boolean,
): T {
    while (true) {
        val item = awaitItem()
        if (predicate(item)) {
            return item
        }
    }
}
