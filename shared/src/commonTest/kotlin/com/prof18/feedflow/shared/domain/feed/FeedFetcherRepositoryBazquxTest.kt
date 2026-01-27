package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.greader.configureBazquxMocks
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedFetcherRepositoryBazquxTest : FeedFetcherRepositoryTestBase() {

    private val feedFetcherRepository: FeedFetcherRepository by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() + getFeedSyncTestModules(
            gReaderProvider = SyncAccounts.BAZQUX,
            gReaderBaseURL = "https://bazqux.com/",
            gReaderConfig = {
                configureBazquxMocks()
            },
        )

    private fun setupBazquxAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.BAZQUX)
        settings.setSyncUsername("testuser")
        settings.setSyncPwd("test-auth-token")
        settings.setSyncUrl("https://bazqux.com")
    }

    @Test
    fun `fetchFeeds syncs with gReader and stores items`() = runTest(testDispatcher) {
        setupBazquxAccount()

        val feedSource = createFeedSource(
            id = "feed/https://9to5google.com/feed/",
            title = "9to5Google",
            url = "https://9to5google.com/feed/",
            websiteUrl = "https://9to5google.com/",
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
