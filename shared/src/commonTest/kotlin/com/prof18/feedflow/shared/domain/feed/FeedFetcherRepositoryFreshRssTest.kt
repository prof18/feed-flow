package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.greader.configureFreshRssMocks
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedFetcherRepositoryFreshRssTest : FeedFetcherRepositoryTestBase() {

    private val feedFetcherRepository: FeedFetcherRepository by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() + getFeedSyncTestModules(
            gReaderProvider = SyncAccounts.FRESH_RSS,
            gReaderBaseURL = "https://freshrss.example.com/api/greader.php/",
            gReaderConfig = {
                configureFreshRssMocks()
            },
        )

    private fun setupFreshRssAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.FRESH_RSS)
        settings.setSyncUsername("testuser")
        settings.setSyncPwd("testpassword")
        settings.setSyncUrl("https://freshrss.example.com/api/greader.php/")
    }

    @Test
    fun `fetchFeeds syncs with gReader and stores items`() = runTest(testDispatcher) {
        setupFreshRssAccount()

        val feedSource = createFeedSource(
            id = "feed/242",
            title = "20 Percent Berlin",
            url = "https://www.20percent.berlin/feed",
            websiteUrl = "https://www.20percent.berlin/",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))

        feedFetcherRepository.fetchFeeds()
        advanceUntilIdle()

        val items = getTimelineItems()
        assertTrue(items.isNotEmpty())
        assertEquals(FinishedFeedUpdateStatus, feedStateRepository.updateState.value)
    }

    private suspend fun getTimelineItems() = databaseHelper.getFeedItems(
        feedFilter = FeedFilter.Timeline,
        pageSize = 50,
        offset = 0,
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
        linkOpeningPreference = com.prof18.feedflow.core.model.LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
    )
}
