package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.feedbin.di.getFeedbinTestModule
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.feedbin.configureFeedbinMocks
import com.prof18.feedflow.feedsync.test.feedbin.createMockFeedbinHttpClient
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import com.prof18.feedflow.shared.test.koin.TestModules
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedSourcesRepositoryFeedbinTest : KoinTestBase() {

    private val feedSourcesRepository: FeedSourcesRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    private val recordingClient by lazy { createMockFeedbinHttpClient { configureFeedbinMocks() } }

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + getFeedSyncTestModules(
            feedbinBaseURL = "https://api.feedbin.com/",
            feedbinConfig = {
                configureFeedbinMocks()
            },
        ) + getFeedbinTestModule(recordingClient)

    fun setupFeedbinAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.FEEDBIN)
        settings.setSyncUsername("testuser")
        settings.setSyncPwd("testpassword")
    }

    @Test
    fun `YouTube channel subscribes once with canonical RSS URL`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val channelId = "UCsBjURrPoezykLs9EqgamOA"
        val result = feedSourcesRepository.addFeedSource("https://youtube.com/channel/$channelId", null, false)

        assertIs<FeedAddedState.FeedAdded>(result)
        val request = (recordingClient.engine as MockEngine).requestHistory.single {
            it.method == HttpMethod.Post && it.url.encodedPath == "/v2/subscriptions.json"
        }
        assertEquals(
            """{"feed_url":"https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"}""",
            (request.body as TextContent).text,
        )
    }

    @Test
    fun `server can discover channel when client cannot resolve its page`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val originalUrl = "https://www.youtube.com/@Fireship"
        val result = feedSourcesRepository.addFeedSource(originalUrl, null, false)

        assertIs<FeedAddedState.FeedAdded>(result)
        val request = (recordingClient.engine as MockEngine).requestHistory.single {
            it.method == HttpMethod.Post && it.url.encodedPath == "/v2/subscriptions.json"
        }
        assertEquals("""{"feed_url":"$originalUrl"}""", (request.body as TextContent).text)
    }

    @Test
    fun `deleteFeed should call feedbinRepository and delete feed source`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val feedSource = createFeedSource(
            id = "feedbin/9115993/1240842",
            title = "Test Feed",
        )
        databaseHelper.insertFeedSourceWithCategory(feedSource)
        advanceUntilIdle()

        feedSourcesRepository.deleteFeed(feedSource)
        advanceUntilIdle()

        val deletedFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertNull(deletedFeedSource, "Feed source should be deleted")
    }

    @Test
    fun `updateFeedSourceName should call feedbinRepository and update name`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val feedSource = createFeedSource(
            id = "feedbin/9115993/1240842",
            title = "Original Name",
        )
        databaseHelper.insertFeedSourceWithCategory(feedSource)
        advanceUntilIdle()

        val newName = "Updated Name"
        feedSourcesRepository.updateFeedSourceName(feedSource.id, newName)
        advanceUntilIdle()

        val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertTrue(updatedFeedSource != null, "Feed source should exist")
        assertEquals(updatedFeedSource.title, newName, "Feed source name should be updated")
    }

    @Test
    fun `addFeedSource should call feedbinRepository and return success`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val feedUrl = "https://example.com/feed"
        val category = FeedSourceCategory(id = "user/-/label/Tech", title = "Tech")

        val result = feedSourcesRepository.addFeedSource(
            feedUrl = feedUrl,
            categoryName = category,
            isNotificationEnabled = false,
        )
        advanceUntilIdle()

        assertIs<FeedAddedState.FeedAdded>(result, "Should return FeedAdded on success")
    }

    @Test
    fun `editFeedSource should call feedbinRepository and return success`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val originalFeedSource = createFeedSource(
            id = "feedbin/9115993/1240842",
            title = "Original Name",
        )
        databaseHelper.insertFeedSourceWithCategory(originalFeedSource)
        advanceUntilIdle()

        val newFeedSource = originalFeedSource.copy(
            title = "Updated Name",
        )

        val result = feedSourcesRepository.editFeedSource(
            newFeedSource = newFeedSource,
            originalFeedSource = originalFeedSource,
        )
        advanceUntilIdle()

        assertIs<FeedEditedState.FeedEdited>(result, "Should return FeedEdited on success")
        assertEquals(result.feedName, "Updated Name", "Should return updated feed name")
    }

    private fun createFeedSource(
        id: String,
        title: String,
        category: FeedSourceCategory? = null,
    ): FeedSource = FeedSource(
        id = id,
        url = "https://example.com/feed.xml",
        title = title,
        category = category,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = "https://example.com",
        fetchFailed = false,
        articleOpenMode = com.prof18.feedflow.core.model.ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
    )
}
