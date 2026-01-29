package com.prof18.feedflow.shared.domain.feed

import app.cash.turbine.test
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.presentation.model.FeedErrorState
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.generators.RssItemGenerator
import com.prof18.feedflow.shared.test.toParsedFeedSource
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import io.kotest.property.arbitrary.next
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class FeedFetcherRepositoryLocalTest : FeedFetcherRepositoryTestBase() {

    private val fakeRssParserWrapper = FakeRssParserWrapper()
    private val feedFetcherRepository: FeedFetcherRepository by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val dateFormatter: DateFormatter by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() + module {
            single<RssParserWrapper> { fakeRssParserWrapper }
        }

    @BeforeTest
    fun resetParserState() {
        fakeRssParserWrapper.reset()
    }

    private fun setupLocalAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.LOCAL)
    }

    @Test
    fun `fetchFeeds emits NoFeedSourcesStatus when no feed sources`() = runTest(testDispatcher) {
        setupLocalAccount()

        feedFetcherRepository.fetchFeeds()
        advanceUntilIdle()

        assertEquals(NoFeedSourcesStatus, feedStateRepository.updateState.value)
    }

    @Test
    fun `fetchFeeds inserts items and updates metadata`() = runTest(testDispatcher) {
        setupLocalAccount()

        val feedSource = createFeedSource(
            id = "source-1",
            title = "Test Feed",
            websiteUrl = null,
            logoUrl = null,
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))

        val rssItems = listOf(
            createRssItem(
                id = "item-1",
                title = "Item 1",
                link = "https://example.com/item-1",
            ),
            createRssItem(
                id = "item-2",
                title = "Item 2",
                link = "https://example.com/item-2",
            ),
        )
        val rssChannel = createRssChannel(
            title = "Test Feed",
            link = "https://example.com",
            items = rssItems,
        )
        fakeRssParserWrapper.setChannel(feedSource.url, rssChannel)

        feedFetcherRepository.fetchFeeds()
        advanceUntilIdle()

        val items = getTimelineItems()
        assertEquals(2, items.size)
        val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertNotNull(updatedFeedSource)
        assertEquals("https://example.com", updatedFeedSource.websiteUrl)
        assertNotNull(updatedFeedSource.logoUrl)
        assertEquals(FinishedFeedUpdateStatus, feedStateRepository.updateState.value)
    }

    @Test
    fun `fetchFeeds skips refresh when feed is too recent`() = runTest(testDispatcher) {
        setupLocalAccount()

        val lastSyncTimestamp = dateFormatter.currentTimeMillis() - (30 * 60 * 1000)
        val feedSource = createFeedSource(
            id = "source-1",
            title = "Test Feed",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.insertFeedItems(
            listOf(
                buildFeedItem(
                    id = "existing-item",
                    title = "Existing Item",
                    pubDateMillis = lastSyncTimestamp,
                    source = feedSource,
                ),
            ),
            lastSyncTimestamp = lastSyncTimestamp,
        )

        val rssChannel = createRssChannel(
            title = "Test Feed",
            link = "https://example.com",
            items = emptyList(),
        )
        fakeRssParserWrapper.setChannel(feedSource.url, rssChannel)

        feedFetcherRepository.fetchFeeds()
        advanceUntilIdle()

        assertEquals(0, fakeRssParserWrapper.callCount)
        assertEquals(1, getTimelineItems().size)
        assertEquals(FinishedFeedUpdateStatus, feedStateRepository.updateState.value)
    }

    @Test
    fun `fetchFeeds forces refresh for non-openrss feeds`() = runTest(testDispatcher) {
        setupLocalAccount()

        val lastSyncTimestamp = dateFormatter.currentTimeMillis() - (10 * 60 * 1000)
        val feedSource = createFeedSource(
            id = "source-1",
            title = "Test Feed",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.insertFeedItems(
            listOf(
                buildFeedItem(
                    id = "existing-item",
                    title = "Existing Item",
                    pubDateMillis = lastSyncTimestamp,
                    source = feedSource,
                ),
            ),
            lastSyncTimestamp = lastSyncTimestamp,
        )

        val rssChannel = createRssChannel(
            title = "Test Feed",
            link = "https://example.com",
            items = emptyList(),
        )
        fakeRssParserWrapper.setChannel(feedSource.url, rssChannel)

        feedFetcherRepository.fetchFeeds(forceRefresh = true)
        advanceUntilIdle()

        assertEquals(1, fakeRssParserWrapper.callCount)
    }

    @Test
    fun `fetchFeeds respects openrss threshold even with forceRefresh`() = runTest(testDispatcher) {
        setupLocalAccount()

        val lastSyncTimestamp = dateFormatter.currentTimeMillis() - (60 * 60 * 1000)
        val feedSource = createFeedSource(
            id = "source-1",
            title = "OpenRSS Feed",
            url = "https://openrss.org/example.xml",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.insertFeedItems(
            listOf(
                buildFeedItem(
                    id = "existing-item",
                    title = "Existing Item",
                    pubDateMillis = lastSyncTimestamp,
                    source = feedSource,
                ),
            ),
            lastSyncTimestamp = lastSyncTimestamp,
        )

        val rssChannel = createRssChannel(
            title = "OpenRSS Feed",
            link = "https://openrss.org",
            items = emptyList(),
        )
        fakeRssParserWrapper.setChannel(feedSource.url, rssChannel)

        feedFetcherRepository.fetchFeeds(forceRefresh = true)
        advanceUntilIdle()

        assertEquals(0, fakeRssParserWrapper.callCount)
    }

    @Test
    fun `getFeedSourceToNotify returns sources with pending notifications`() = runTest(testDispatcher) {
        setupLocalAccount()

        val category = FeedSourceCategory(id = "cat-1", title = "News")
        val feedSource = createFeedSource(
            id = "source-1",
            title = "Notify Feed",
        ).copy(category = category)
        databaseHelper.insertCategories(listOf(category))
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.updateNotificationEnabledStatus(feedSource.id, true)
        databaseHelper.insertFeedItems(
            listOf(
                buildFeedItem(
                    id = "notify-item",
                    title = "Notify Item",
                    pubDateMillis = dateFormatter.currentTimeMillis(),
                    source = feedSource,
                ),
            ),
            lastSyncTimestamp = dateFormatter.currentTimeMillis(),
        )

        val sourcesToNotify = feedFetcherRepository.getFeedSourceToNotify()

        assertEquals(1, sourcesToNotify.size)
        val result = sourcesToNotify.first()
        assertEquals(feedSource.id, result.feedSourceId)
        assertEquals(feedSource.title, result.feedSourceTitle)
        assertEquals(category.id, result.categoryId)
        assertEquals(category.title, result.categoryTitle)
    }

    @Test
    fun `markItemsAsNotified clears pending notifications`() = runTest(testDispatcher) {
        setupLocalAccount()

        val feedSource = createFeedSource(
            id = "source-1",
            title = "Notify Feed",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.updateNotificationEnabledStatus(feedSource.id, true)
        databaseHelper.insertFeedItems(
            listOf(
                buildFeedItem(
                    id = "notify-item",
                    title = "Notify Item",
                    pubDateMillis = dateFormatter.currentTimeMillis(),
                    source = feedSource,
                ),
            ),
            lastSyncTimestamp = dateFormatter.currentTimeMillis(),
        )

        val initialSources = feedFetcherRepository.getFeedSourceToNotify()
        assertEquals(1, initialSources.size)

        feedFetcherRepository.markItemsAsNotified()
        advanceUntilIdle()

        val updatedSources = feedFetcherRepository.getFeedSourceToNotify()
        assertEquals(0, updatedSources.size)
    }

    @Test
    fun `fetchFeeds marks feed as failed and emits error on parser failure`() = runTest(testDispatcher) {
        setupLocalAccount()

        val feedSource = createFeedSource(
            id = "source-1",
            title = "Broken Feed",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        fakeRssParserWrapper.setError(feedSource.url)

        feedStateRepository.errorState.test {
            feedFetcherRepository.fetchFeeds()
            advanceUntilIdle()

            val error = awaitItem()
            assertTrue(error is FeedErrorState)
            assertEquals(feedSource.title, error.failingSourceName)
        }

        val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertNotNull(updatedFeedSource)
        assertTrue(updatedFeedSource.fetchFailed)
    }

    @Test
    fun `fetchFeeds marks feed as failed without emitting error when parsing errors are hidden`() =
        runTest(testDispatcher) {
            setupLocalAccount()
            settingsRepository.setShowRssParsingErrors(false)

            val feedSource = createFeedSource(
                id = "source-1",
                title = "Broken Feed",
            )
            databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
            fakeRssParserWrapper.setError(feedSource.url)

            feedStateRepository.errorState.test {
                feedFetcherRepository.fetchFeeds()
                advanceUntilIdle()

                expectNoEvents()
            }

            val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
            assertNotNull(updatedFeedSource)
            assertTrue(updatedFeedSource.fetchFailed)
        }

    @Test
    fun `fetchFeeds cleans old items when auto delete is enabled`() = runTest(testDispatcher) {
        setupLocalAccount()
        settingsRepository.setAutoDeletePeriod(AutoDeletePeriod.ONE_DAY)

        val feedSource = createFeedSource(
            id = "source-1",
            title = "Test Feed",
            websiteUrl = null,
            logoUrl = null,
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))

        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val oldItem = buildFeedItem(
            id = "old-item",
            title = "Old Item",
            pubDateMillis = nowMillis - 2.days.inWholeMilliseconds,
            source = feedSource,
        )
        val recentItem = buildFeedItem(
            id = "recent-item",
            title = "Recent Item",
            pubDateMillis = nowMillis - 10_000,
            source = feedSource,
        )
        databaseHelper.insertFeedItems(listOf(oldItem, recentItem), lastSyncTimestamp = 0)

        val rssChannel = createRssChannel(
            title = "Test Feed",
            link = "https://example.com",
            items = emptyList(),
        )
        fakeRssParserWrapper.setChannel(feedSource.url, rssChannel)

        feedFetcherRepository.fetchFeeds()
        advanceUntilIdle()

        val items = getTimelineItems()
        assertEquals(1, items.size)
        assertEquals("recent-item", items.first().url_hash)
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
        lastSyncTimestamp: Long? = null,
        url: String = "https://example.com/$id/rss.xml",
        websiteUrl: String? = "https://example.com/$id",
        logoUrl: String? = "https://example.com/$id/logo.png",
    ): FeedSource = FeedSource(
        id = id,
        url = url,
        title = title,
        category = null,
        lastSyncTimestamp = lastSyncTimestamp,
        logoUrl = logoUrl,
        websiteUrl = websiteUrl,
        fetchFailed = false,
        linkOpeningPreference = com.prof18.feedflow.core.model.LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
    )

    private fun createRssChannel(
        title: String,
        link: String,
        items: List<RssItem>,
    ): RssChannel = RssChannelGenerator.rssChannelArb.next().copy(
        title = title,
        link = link,
        items = items,
    )

    private fun createRssItem(
        id: String,
        title: String,
        link: String,
    ): RssItem = RssItemGenerator.rssItemArb.next().copy(
        guid = id,
        title = title,
        link = link,
        pubDate = null,
        categories = emptyList(),
        itunesItemData = null,
        commentsUrl = null,
        youtubeItemData = null,
        rawEnclosure = null,
    )

    private class FakeRssParserWrapper : RssParserWrapper {
        private val channelByUrl = mutableMapOf<String, RssChannel>()
        private val errorUrls = mutableSetOf<String>()
        var callCount: Int = 0
            private set

        fun reset() {
            channelByUrl.clear()
            errorUrls.clear()
            callCount = 0
        }

        fun setChannel(url: String, channel: RssChannel) {
            channelByUrl[url] = channel
        }

        fun setError(url: String) {
            errorUrls.add(url)
        }

        override suspend fun getRssChannel(url: String): RssChannel {
            callCount += 1
            if (errorUrls.contains(url)) {
                error("Failure for $url")
            }
            return requireNotNull(channelByUrl[url]) { "Missing channel for $url" }
        }
    }
}
