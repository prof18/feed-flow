package com.prof18.feedflow.shared.presentation

import app.cash.sqldelight.db.SqlDriver
import app.cash.turbine.test
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedRecoveryRecord
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCacheInfo
import com.prof18.feedflow.core.model.RecoveryMode
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryCoordinator
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.fakes.FakeWebsiteSessionHttp
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.koin.RecoveryTestModules
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedRefreshVerificationViewModelTest : KoinTestBase() {

    private val viewModel: FeedRefreshVerificationViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val sqlDriver: SqlDriver by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val coordinator: FeedRecoveryCoordinator by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() + RecoveryTestModules.enabled(FakeWebsiteSessionHttp(), null)

    @Test
    fun `persisted challenge is pending without opening the browser`() = runTest(testDispatcher) {
        val source = insertSource("pending")
        recordChallenge(source)

        viewModel.pending.test {
            runCurrent()
            val pending = expectMostRecentItem().single()
            assertEquals(source.id, pending.source.id)
            assertEquals(RecoveryMode.VerificationRequired, pending.record.mode)
            assertNull(coordinator.visibleState.value)
        }
    }

    @Test
    fun `pending verification waits for the server cooldown`() = runTest(testDispatcher) {
        val source = insertSource("cooldown")
        recordChallenge(source)
        val now = getKoin().get<DateFormatter>().currentTimeMillis()
        databaseHelper.updateFeedSourcesCacheInfo(
            listOf(FeedSourceCacheInfo(source.id, null, null, null, null, now + 60_000)),
        )

        viewModel.pending.test {
            runCurrent()
            assertTrue(expectMostRecentItem().isEmpty())
            advanceTimeBy(60_000)
            runCurrent()
            assertEquals(source.id, awaitItem().single().source.id)
        }
    }

    @Test
    fun `challenge from another account epoch is never listed`() = runTest(testDispatcher) {
        val source = insertSource("old-account")
        recordChallenge(source)
        settingsRepository.rotateCloudSyncSession()

        viewModel.pending.test {
            runCurrent()
            assertTrue(expectMostRecentItem().isEmpty())
        }
    }

    @Test
    fun `challenge for a replaced subscription url is filtered out`() = runTest(testDispatcher) {
        val source = insertSource("changed-url")
        recordChallenge(source)
        sqlDriver.execute(null, "UPDATE feed_source SET url = ? WHERE url_hash = ?", 2) {
            bindString(0, "https://example.test/changed/feed.xml")
            bindString(1, source.id)
        }

        viewModel.pending.test {
            runCurrent()
            assertTrue(expectMostRecentItem().isEmpty())
        }
    }

    @Test
    fun `dismissed verification stays hidden until the app is foregrounded`() = runTest(testDispatcher) {
        val source = insertSource("dismissed")
        recordChallenge(source)

        viewModel.pending.test {
            runCurrent()
            val pending = expectMostRecentItem().single()

            viewModel.dismiss(pending)
            runCurrent()
            assertTrue(expectMostRecentItem().isEmpty())

            viewModel.onForeground()
            runCurrent()
            assertEquals(pending, expectMostRecentItem().single())
        }
    }

    @Test
    fun `cancelling the browser leaves the challenge pending`() = runTest(testDispatcher) {
        val source = insertSource("cancel")
        recordChallenge(source)

        viewModel.pending.test {
            runCurrent()
            val pending = expectMostRecentItem().single()
            viewModel.verify(pending)
            runCurrent()
            assertNotNull(coordinator.visibleState.value)

            viewModel.cancel()
            advanceUntilIdle()

            assertNull(coordinator.visibleState.value)
            assertFalse(viewModel.isRecovering.value)
            assertEquals(RecoveryMode.VerificationRequired, databaseHelper.recoveryRecords(epoch).single().mode)
            assertEquals(pending, viewModel.pending.value.single())
        }
    }

    @Test
    fun `without records the feed sources are never subscribed`() = runTest(testDispatcher) {
        var subscriptions = 0
        val sources = MutableStateFlow(emptyList<FeedSource>()).onSubscription { subscriptions++ }
        val countingViewModel = FeedRefreshVerificationViewModel(
            feedSources = { sources },
            recoveryRepository = getKoin().get(),
            coordinator = coordinator,
            recoveryService = getKoin().get(),
            databaseHelper = databaseHelper,
            dateFormatter = getKoin().get(),
        )

        countingViewModel.pending.test {
            runCurrent()
            assertTrue(expectMostRecentItem().isEmpty())
            assertEquals(0, subscriptions)
        }
    }

    @Test
    fun `with a record the feed sources are subscribed`() = runTest(testDispatcher) {
        val source = insertSource("counted")
        recordChallenge(source)
        var subscriptions = 0
        val sources = MutableStateFlow(listOf(source)).onSubscription { subscriptions++ }
        val countingViewModel = FeedRefreshVerificationViewModel(
            feedSources = { sources },
            recoveryRepository = getKoin().get(),
            coordinator = coordinator,
            recoveryService = getKoin().get(),
            databaseHelper = databaseHelper,
            dateFormatter = getKoin().get(),
        )

        countingViewModel.pending.test {
            runCurrent()
            assertEquals(source.id, expectMostRecentItem().single().source.id)
            assertTrue(subscriptions > 0)
        }
    }

    private val epoch: String get() = settingsRepository.cloudSyncSessionState.value

    private suspend fun insertSource(id: String): FeedSource {
        val source = FeedSourceGenerator.feedSource(id = id, url = "https://example.test/$id/feed.xml")
        databaseHelper.insertFeedSource(listOf(source.toParsedFeedSource()))
        return source
    }

    private suspend fun recordChallenge(source: FeedSource) {
        databaseHelper.upsertRecoveryRecord(
            FeedRecoveryRecord(
                accountEpoch = epoch,
                sourceId = source.id,
                subscriptionUrl = source.url,
                effectiveUrl = source.url,
                origin = "https://example.test:443",
                mode = RecoveryMode.VerificationRequired,
                profileId = null,
                statusCode = 403,
                retryAfter = null,
            ),
        ) { it() }
    }
}
