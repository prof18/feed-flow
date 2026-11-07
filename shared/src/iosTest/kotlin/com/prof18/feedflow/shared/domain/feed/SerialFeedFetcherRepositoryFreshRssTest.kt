package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ArticleOpenMode
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

class SerialFeedFetcherRepositoryFreshRssTest : FeedFetcherRepositoryTestBase() {

    private val databaseHelper: DatabaseHelper by inject()
    private val feedStateRepository: FeedStateRepository by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() + getFeedSyncTestModules(
            gReaderProvider = SyncAccounts.FRESH_RSS,
            gReaderBaseURL = "https://freshrss.example.com/api/greader.php/",
            gReaderConfig = {
                configureFreshRssMocks()
            },
        )

    @Test
    fun `quick refresh syncs a FreshRSS account and stores its items`() = runTest(testDispatcher) {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.FRESH_RSS)
        settings.setSyncUsername("testuser")
        settings.setSyncPwd("testpassword")
        settings.setSyncUrl("https://freshrss.example.com/api/greader.php/")
        databaseHelper.insertFeedSource(listOf(berlinFeedSource().toParsedFeedSource()))

        repository().refreshFeedsQuickly(timeBudgetMillis = 20_000)
        advanceUntilIdle()

        val items = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 50,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        assertTrue(items.isNotEmpty())
        assertEquals(FinishedFeedUpdateStatus, feedStateRepository.updateState.value)
    }

    private fun repository(): SerialFeedFetcherRepository = SerialFeedFetcherRepository(
        dispatcherProvider = getKoin().get(),
        feedStateRepository = feedStateRepository,
        gReaderRepository = getKoin().get(),
        feedbinRepository = getKoin().get(),
        databaseHelper = databaseHelper,
        feedSyncRepository = getKoin().get(),
        logger = Logger.withTag("SerialFeedFetcherRepositoryFreshRssTest"),
        rssChannelMapper = getKoin().get(),
        dateFormatter = getKoin().get(),
        feedHttpCacheStore = getKoin().get(),
        feedAttempt = getKoin().get(),
        recoveryRepository = getKoin().get(),
    )

    private fun berlinFeedSource() = FeedSource(
        id = "feed/242",
        url = "https://www.20percent.berlin/feed",
        title = "20 Percent Berlin",
        category = null,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = "https://www.20percent.berlin/",
        fetchFailed = false,
        articleOpenMode = ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
    )
}
