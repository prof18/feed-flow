package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.feedbin.domain.FeedbinRepository
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.feedbin.configureFeedbinMocks
import com.prof18.feedflow.shared.test.toParsedFeedSource
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedFetcherRepositoryFeedbinTest : FeedFetcherRepositoryTestBase() {

    private val feedFetcherRepository: FeedFetcherRepository by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val feedbinRepository: FeedbinRepository by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() + getFeedSyncTestModules(
            feedbinBaseURL = "https://api.feedbin.com/",
            feedbinConfig = {
                configureFeedbinMocks()
                addMockResponse(
                    urlPattern = "/v2/entries.json",
                    statusCode = HttpStatusCode.BadRequest,
                    responseContent = "{}",
                )
                addMockResponse(
                    urlPattern = "include_enclosure=true",
                    responseContent = AUDIO_ENTRY_RESPONSE,
                )
            },
        )

    private fun setupFeedbinAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.FEEDBIN)
        settings.setSyncUsername("testuser")
        settings.setSyncPwd("testpassword")
    }

    @Test
    fun `ID entry fetch requests enclosures and persists signed audio`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        feedbinRepository.syncUnreadAndStarredAfterLogin()
        advanceUntilIdle()
        assertEquals(AUDIO_URL, databaseHelper.getFeedItemAudioUrl("5031084432"))
    }

    @Test
    fun `page entry fetch requests enclosures and persists signed audio`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val feedSource = createFeedSource(
            id = "feedbin/9115993/1240842",
            title = "stories – MacStories",
            url = "https://www.macstories.net/category/stories/feed/",
            websiteUrl = "https://www.macstories.net/category/stories/",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        feedFetcherRepository.fetchFeeds()
        advanceUntilIdle()
        assertEquals(AUDIO_URL, databaseHelper.getFeedItemAudioUrl("5031084432"))
        assertEquals(FinishedFeedUpdateStatus, feedStateRepository.updateState.value)
    }

    @Test
    fun `fetchFeeds syncs with feedbin and stores items`() = runTest(testDispatcher) {
        setupFeedbinAccount()

        val feedSource = createFeedSource(
            id = "feedbin/9115993/1240842",
            title = "stories – MacStories",
            url = "https://www.macstories.net/category/stories/feed/",
            websiteUrl = "https://www.macstories.net/category/stories/",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))

        feedFetcherRepository.fetchFeeds()
        advanceUntilIdle()

        val items = getTimelineItems()
        assertTrue(items.isNotEmpty())
        assertEquals(FinishedFeedUpdateStatus, feedStateRepository.updateState.value)
    }

    @Test
    fun `fetchFeeds with a Source filter performs full account sync`() = runTest(testDispatcher) {
        setupFeedbinAccount()

        val feedSource = createFeedSource(
            id = "feedbin/9115993/1240842",
            title = "stories – MacStories",
            url = "https://www.macstories.net/category/stories/feed/",
            websiteUrl = "https://www.macstories.net/category/stories/",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))

        feedFetcherRepository.fetchFeeds(feedFilter = FeedFilter.Source(feedSource.copy(id = "other-feed")))
        advanceUntilIdle()

        assertTrue(getTimelineItems().isNotEmpty())
    }

    private suspend fun getTimelineItems() = databaseHelper.getFeedItems(
        feedFilter = FeedFilter.Timeline,
        pageSize = 50,
        showReadItems = true,
        sortOrder = FeedOrder.NEWEST_FIRST,
    )

    private fun createFeedSource(
        id: String,
        title: String,
        url: String,
        websiteUrl: String,
    ): FeedSource = FeedSource(
        id = id,
        url = url,
        title = title,
        category = null,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = websiteUrl,
        fetchFailed = false,
        articleOpenMode = com.prof18.feedflow.core.model.ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
    )

    private companion object {
        const val AUDIO_URL = "https://example.com/audio?sig=a%2Bb&part=1"
        const val AUDIO_ENTRY_RESPONSE = """[{
            "id":5031084432,"feed_id":1240842,"title":"Audio episode","author":null,
            "content":"<p>Show notes</p>","summary":"Show notes","url":"https://example.com/notes",
            "extracted_content_url":null,"published":"2026-01-01T00:00:00Z","created_at":"2026-01-01T00:00:00Z",
            "enclosure":{"enclosure_url":"https://example.com/audio?sig=a%2Bb&part=1","enclosure_type":"audio/mpeg"}
        }]"""
    }
}
