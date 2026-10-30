package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.RssParserWrapper
import com.prof18.feedflow.shared.domain.feed.ScriptedRssParserWrapper
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.domain.recovery.CapturedFeedParser
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryCoordinator
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.fakes.FakeVerificationBrowser
import com.prof18.feedflow.shared.test.fakes.FakeWebsiteSessionHttp
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.generators.RssItemGenerator
import com.prof18.feedflow.shared.test.koin.RecoveryTestModules
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.get
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddFeedViewModelRecoveryTest : KoinTestBase() {

    private val wrapper = ScriptedRssParserWrapper()
    private val channel = RssChannelGenerator.rssChannel(
        title = "Recovered feed",
        link = SITE,
        items = listOf(RssItemGenerator.rssItem(link = "$SITE/post", pubDate = null, commentsUrl = null)),
    )
    private val databaseHelper: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() +
            module { single<RssParserWrapper> { wrapper } } +
            RecoveryTestModules.enabled(FakeWebsiteSessionHttp(), channel) +
            module { single { CapturedFeedParser { channel } } }

    @Test
    fun `changing category and notifications while adding does not leave the spinner`() = runTest(testDispatcher) {
        val viewModel: AddFeedViewModel = get()
        wrapper.channels["$SITE/feed.xml"] = channel
        wrapper.gate = CompletableDeferred()
        viewModel.updateFeedUrlTextFieldValue("$SITE/feed.xml")
        advanceUntilIdle()

        viewModel.feedAddedState.test {
            viewModel.addFeed()
            assertEquals(FeedAddedState.Loading, awaitItem())

            viewModel.onCategorySelected(CategoryId("missing"))
            viewModel.updateNotificationStatus(true)
            viewModel.addNewCategory(CategoryName("Tech"))
            advanceUntilIdle()
            requireNotNull(wrapper.gate).complete(Unit)

            assertIs<FeedAddedState.FeedAdded>(awaitItem())
        }
        assertFalse(viewModel.isAddingFeedState.value)
    }

    @Test
    fun `a challenged add requires verification`() = runTest(testDispatcher) {
        val viewModel: AddFeedViewModel = get()
        wrapper.challengeEverything(retryAfter = null)
        viewModel.updateFeedUrlTextFieldValue(SITE)
        advanceUntilIdle()

        viewModel.feedAddedState.test {
            viewModel.addFeed()
            assertEquals(FeedAddedState.Loading, awaitItem())
            val failed = assertIs<FeedAddedState.Error.FetchFailed>(awaitItem())
            assertEquals("https://challenged.test?challenged", failed.verificationUrl)
        }
        assertTrue(viewModel.verificationRequiredState.value)
    }

    @Test
    fun `verification uses the category chosen after the challenge`() = runTest(testDispatcher) {
        val viewModel: AddFeedViewModel = get()
        FakeVerificationBrowser(get<FeedRecoveryCoordinator>(), get<FeedRecoveryRepository>()).start(backgroundScope)
        wrapper.challengeEverything(retryAfter = null)
        addChallenged(viewModel)

        viewModel.addNewCategory(CategoryName("Tech"))
        viewModel.categoriesState.first { state -> state.categories.any { it.isSelected && it.name == "Tech" } }
        viewModel.feedAddedState.test {
            viewModel.verifyWebsite()
            assertIs<FeedAddedState.FeedAdded>(awaitItem())
        }

        assertEquals("Tech", databaseHelper.getFeedSources().single().category?.title)
        assertFalse(viewModel.verificationRequiredState.value)
    }

    @Test
    fun `editing the url clears the pending verification`() = runTest(testDispatcher) {
        val viewModel: AddFeedViewModel = get()
        val coordinator: FeedRecoveryCoordinator = get()
        wrapper.challengeEverything(retryAfter = null)
        addChallenged(viewModel)
        viewModel.verifyWebsite()
        coordinator.visibleState.first { it != null }

        viewModel.updateFeedUrlTextFieldValue("https://other.test")
        coordinator.visibleState.first { it == null }

        assertFalse(viewModel.verificationRequiredState.value)
        viewModel.verifyWebsite()
        advanceUntilIdle()
        assertNull(coordinator.visibleState.value)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.addChallenged(viewModel: AddFeedViewModel) {
        viewModel.updateFeedUrlTextFieldValue(SITE)
        advanceUntilIdle()
        viewModel.feedAddedState.test {
            viewModel.addFeed()
            assertEquals(FeedAddedState.Loading, awaitItem())
            assertIs<FeedAddedState.Error.FetchFailed>(awaitItem())
        }
    }

    private companion object {
        const val SITE = "https://challenged.test"
    }
}
