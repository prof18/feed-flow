package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.RecoveryMode
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.RssParserWrapper
import com.prof18.feedflow.shared.domain.feed.ScriptedRssParserWrapper
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import com.prof18.feedflow.shared.domain.recovery.CapturedFeedParser
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryCoordinator
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.fakes.FakeVerificationBrowser
import com.prof18.feedflow.shared.test.fakes.FakeWebsiteSessionHttp
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.generators.RssItemGenerator
import com.prof18.feedflow.shared.test.koin.RecoveryTestModules
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.get
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EditFeedViewModelRecoveryTest : KoinTestBase() {

    private val wrapper = ScriptedRssParserWrapper()
    private val channel = RssChannelGenerator.rssChannel(
        title = "Moved feed",
        link = "https://moved.test",
        items = listOf(RssItemGenerator.rssItem(link = "https://moved.test/post", pubDate = null, commentsUrl = null)),
    )
    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() +
            module { single<RssParserWrapper> { wrapper } } +
            RecoveryTestModules.enabled(FakeWebsiteSessionHttp(), channel) +
            module { single { CapturedFeedParser { channel } } }

    @Test
    fun `a challenged edit is saved with an approved record after verification`() = runTest(testDispatcher) {
        val original = FeedSourceGenerator.feedSource(id = "edit", title = "Old", url = "https://old.test/rss.xml")
        databaseHelper.insertFeedSource(listOf(original.toParsedFeedSource()))
        FakeVerificationBrowser(get<FeedRecoveryCoordinator>(), get<FeedRecoveryRepository>()).start(backgroundScope)
        wrapper.challengeEverything(retryAfter = null)
        val viewModel: EditFeedViewModel = get()
        viewModel.loadFeedToEdit(original)
        advanceUntilIdle()
        viewModel.updateFeedUrlTextFieldValue(NEW_URL)
        advanceUntilIdle()

        viewModel.feedEditedState.test {
            viewModel.editFeed()
            assertEquals(FeedEditedState.Loading, awaitItem())
            assertIs<FeedEditedState.Error.FetchFailed>(awaitItem())
            assertTrue(viewModel.verificationRequiredState.value)

            viewModel.verifyWebsite()
            assertIs<FeedEditedState.FeedEdited>(awaitItem())
        }

        assertEquals(NEW_URL, databaseHelper.getFeedSource(original.id)?.url)
        val record = databaseHelper.recoveryRecord(settingsRepository.cloudSyncSessionState.value, original.id)
        assertEquals(RecoveryMode.SessionApproved, record?.mode)
    }

    private companion object {
        const val NEW_URL = "https://moved.test/rss.xml"
    }
}
