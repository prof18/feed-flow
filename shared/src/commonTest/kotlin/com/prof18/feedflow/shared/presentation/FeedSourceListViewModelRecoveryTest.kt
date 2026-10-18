package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.FeedRecoveryRecord
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.RecoveryMode
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.FeedSourceRecoveryUiState
import com.prof18.feedflow.shared.domain.recovery.RecoveryOrigin
import com.prof18.feedflow.shared.domain.recovery.WebsiteProfileCleaner
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.fakes.FakeWebsiteSessionHttp
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.koin.RecoveryTestModules
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class FeedSourceListViewModelRecoveryTestBase : KoinTestBase() {
    protected val viewModel: FeedSourceListViewModel by inject()
    protected val databaseHelper: DatabaseHelper by inject()
    protected val settingsRepository: SettingsRepository by inject()

    protected val epoch: String get() = settingsRepository.cloudSyncSessionState.value

    protected suspend fun insertSource(id: String): FeedSource {
        val source = FeedSourceGenerator.feedSource(id = id, url = "https://example.test/$id/feed.xml")
        databaseHelper.insertFeedSource(listOf(source.toParsedFeedSource()))
        return source
    }

    protected fun record(source: FeedSource, mode: RecoveryMode, profileId: String?) = FeedRecoveryRecord(
        accountEpoch = epoch,
        sourceId = source.id,
        subscriptionUrl = source.url,
        effectiveUrl = source.url,
        origin = "https://example.test:443",
        mode = mode,
        profileId = profileId,
        statusCode = 403,
        retryAfter = null,
    )
}

class FeedSourceListViewModelRecoveryTest : FeedSourceListViewModelRecoveryTestBase() {

    private val cleaner = CountingCleaner()
    private val recoveryRepository: FeedRecoveryRepository by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() +
            RecoveryTestModules.enabled(FakeWebsiteSessionHttp(), null) +
            module { single<WebsiteProfileCleaner> { cleaner } }

    @Test
    fun `recovery states mark waiting and approved sources`() = runTest(testDispatcher) {
        val waiting = insertSource("waiting")
        val approved = insertSource("approved")
        val profileId = acquireProfileId()
        databaseHelper.upsertRecoveryRecord(record(waiting, RecoveryMode.VerificationRequired, null)) { it() }
        databaseHelper.upsertRecoveryRecord(record(approved, RecoveryMode.SessionApproved, profileId)) { it() }

        viewModel.recoveryStates.test {
            runCurrent()
            val states = expectMostRecentItem()
            assertEquals(FeedSourceRecoveryUiState(true, false, null), states[waiting.id])
            assertEquals(FeedSourceRecoveryUiState(false, true, profileId), states[approved.id])
        }
    }

    @Test
    fun `clearing a website removes the approval and cleans the profile once`() = runTest(testDispatcher) {
        val approved = insertSource("approved")
        val profileId = acquireProfileId()
        databaseHelper.upsertRecoveryRecord(record(approved, RecoveryMode.SessionApproved, profileId)) { it() }

        viewModel.recoveryStates.test {
            runCurrent()
            expectMostRecentItem()
            viewModel.clearWebsiteVerification(approved)
            advanceUntilIdle()

            assertTrue(expectMostRecentItem().isEmpty())
        }
        assertTrue(databaseHelper.recoveryRecords(epoch).isEmpty())
        assertTrue(recoveryRepository.profile(profileId)?.retired == true)
        assertEquals(1, cleaner.calls)
    }

    private suspend fun acquireProfileId(): String = recoveryRepository.acquireProfile(
        epoch = epoch,
        origin = requireNotNull(RecoveryOrigin.of("https://example.test/feed.xml")),
        userAgent = "ua",
        engineVersion = "engine",
    ).id

    private class CountingCleaner : WebsiteProfileCleaner {
        var calls = 0
        override suspend fun clean() {
            calls++
        }
    }
}

class FeedSourceListViewModelUnsupportedRecoveryTest : FeedSourceListViewModelRecoveryTestBase() {

    @Test
    fun `unsupported recovery always has an empty state map`() = runTest(testDispatcher) {
        val waiting = insertSource("waiting")
        databaseHelper.upsertRecoveryRecord(record(waiting, RecoveryMode.VerificationRequired, null)) { it() }

        viewModel.recoveryStates.test {
            runCurrent()
            assertTrue(expectMostRecentItem().isEmpty())
        }
    }
}
