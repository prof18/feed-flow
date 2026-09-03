package com.prof18.feedflow.shared.domain.feed

import app.cash.turbine.test
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCacheInfo
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpValidators
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.generators.RssItemGenerator
import com.prof18.feedflow.shared.test.toParsedFeedSource
import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class FeedFetcherRepositoryLocalTest : FeedFetcherRepositoryTestBase() {

    private val fakeRssParserWrapper = FakeRssParserWrapper()
    private val feedFetcherRepository: FeedFetcherRepository by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val dateFormatter: DateFormatter by inject()
    private val feedHttpCacheStore: FeedHttpCacheStore by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() + module {
            single<RssParserWrapper> { fakeRssParserWrapper }
        }

    @BeforeTest
    fun resetParserState() {
        fakeRssParserWrapper.reset()
        fakeRssParserWrapper.validatorsFor = { feedHttpCacheStore.validatorsFor(it) }
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
    fun `fetchFeeds with Source filter only parses that source`() = runTest(testDispatcher) {
        setupLocalAccount()
        val sources = createScopedSources()
        insertScopedSources(sources)
        setEmptyChannels(sources)

        feedFetcherRepository.fetchFeeds(feedFilter = FeedFilter.Source(sources.first()))
        advanceUntilIdle()

        assertEquals(listOf(sources.first().url), fakeRssParserWrapper.requestedUrls)
    }

    @Test
    fun `fetchFeeds with Category filter only parses sources in that category`() = runTest(testDispatcher) {
        setupLocalAccount()
        val sources = createScopedSources()
        insertScopedSources(sources)
        setEmptyChannels(sources)

        feedFetcherRepository.fetchFeeds(feedFilter = FeedFilter.Category(requireNotNull(sources.first().category)))
        advanceUntilIdle()

        assertEquals(sources.take(2).map { it.url }.toSet(), fakeRssParserWrapper.requestedUrls.toSet())
    }

    @Test
    fun `fetchFeeds with Uncategorized filter only parses uncategorized sources`() = runTest(testDispatcher) {
        setupLocalAccount()
        val sources = createScopedSources()
        insertScopedSources(sources)
        setEmptyChannels(sources)

        feedFetcherRepository.fetchFeeds(feedFilter = FeedFilter.Uncategorized)
        advanceUntilIdle()

        assertEquals(listOf(sources.last().url), fakeRssParserWrapper.requestedUrls)
    }

    @Test
    fun `fetchFeeds with library filters parses every source`() = runTest(testDispatcher) {
        setupLocalAccount()
        val sources = createScopedSources()
        insertScopedSources(sources)
        setEmptyChannels(sources)

        listOf(FeedFilter.Timeline, FeedFilter.Read, FeedFilter.Bookmarks).forEach { feedFilter ->
            fakeRssParserWrapper.reset()
            setEmptyChannels(sources)
            feedFetcherRepository.fetchFeeds(forceRefresh = true, feedFilter = feedFilter)
            advanceUntilIdle()

            assertEquals(sources.map { it.url }.toSet(), fakeRssParserWrapper.requestedUrls.toSet())
        }
    }

    @Test
    fun `concurrent fetches are serialized`() = runTest(testDispatcher) {
        setupLocalAccount()
        val feedSource = createFeedSource(id = "source-1", title = "Test Feed")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        fakeRssParserWrapper.setChannel(
            feedSource.url,
            createRssChannel(title = "Test Feed", link = "https://example.com", items = emptyList()),
        )

        val firstFetchStarted = CompletableDeferred<Unit>()
        val releaseFirstFetch = CompletableDeferred<Unit>()
        fakeRssParserWrapper.onRequest = {
            if (!firstFetchStarted.isCompleted) {
                firstFetchStarted.complete(Unit)
                releaseFirstFetch.await()
            }
        }

        val firstFetch = launch {
            feedFetcherRepository.fetchFeeds(forceRefresh = true)
        }
        runCurrent()
        firstFetchStarted.await()

        val secondFetch = launch {
            feedFetcherRepository.fetchFeeds(forceRefresh = true)
        }
        runCurrent()

        assertEquals(1, fakeRssParserWrapper.callCount)

        releaseFirstFetch.complete(Unit)
        firstFetch.join()
        secondFetch.join()

        assertEquals(2, fakeRssParserWrapper.callCount)
        assertEquals(FinishedFeedUpdateStatus, feedStateRepository.updateState.value)
    }

    @Test
    fun `fetchFeeds with filter matching no source finishes instead of hanging`() = runTest(testDispatcher) {
        setupLocalAccount()
        val sources = createScopedSources()
        insertScopedSources(sources)

        feedFetcherRepository.fetchFeeds(
            feedFilter = FeedFilter.Category(FeedSourceCategory(id = "empty-category", title = "Empty")),
        )
        advanceUntilIdle()

        assertTrue(fakeRssParserWrapper.requestedUrls.isEmpty())
        assertEquals(FinishedFeedUpdateStatus, feedStateRepository.updateState.value)
    }

    @Test
    fun `scoped fetch leaves out of scope source metadata unchanged`() = runTest(testDispatcher) {
        setupLocalAccount()
        val sources = createScopedSources()
        insertScopedSources(sources)
        setEmptyChannels(sources)
        val outOfScopeSource = sources.last()
        databaseHelper.setFeedFetchFailed(outOfScopeSource.id, true)

        feedFetcherRepository.fetchFeeds(feedFilter = FeedFilter.Category(requireNotNull(sources.first().category)))
        advanceUntilIdle()

        val updatedOutOfScopeSource = requireNotNull(databaseHelper.getFeedSource(outOfScopeSource.id))
        assertTrue(updatedOutOfScopeSource.fetchFailed)
        assertEquals(null, updatedOutOfScopeSource.lastSyncTimestamp)
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
    fun `silent fetch inserts items without publishing feed state`() = runTest(testDispatcher) {
        setupLocalAccount()
        val feedSource = createFeedSource(id = "source-1", title = "Test Feed")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.insertFeedItems(
            listOf(buildFeedItem(id = "existing", title = "Existing", pubDateMillis = 1, source = feedSource)),
            lastSyncTimestamp = 0,
        )
        feedStateRepository.getFeeds()
        val initialVersion = feedStateRepository.feedListVersion.value
        settingsRepository.setAutoDeletePeriod(AutoDeletePeriod.ONE_DAY)
        fakeRssParserWrapper.setChannel(
            feedSource.url,
            createRssChannel(
                title = "Test Feed",
                link = "https://example.com",
                items = listOf(
                    createRssItem(
                        id = "new-item",
                        title = "New Item",
                        link = "https://example.com/new-item",
                    ),
                ),
            ),
        )

        feedFetcherRepository.fetchFeeds(publishToFeedList = false)
        advanceUntilIdle()

        assertEquals(listOf("https://example.com/new-item"), getTimelineItems().map { it.url })
        assertEquals(listOf("existing"), feedStateRepository.feedState.value.map { it.id })
        assertEquals(initialVersion, feedStateRepository.feedListVersion.value)
    }

    @Test
    fun `foreground fetch continues publishing feed state`() = runTest(testDispatcher) {
        setupLocalAccount()
        val feedSource = createFeedSource(id = "source-1", title = "Test Feed")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.insertFeedItems(
            listOf(buildFeedItem(id = "existing", title = "Existing", pubDateMillis = 1, source = feedSource)),
            lastSyncTimestamp = 0,
        )
        feedStateRepository.getFeeds()
        val initialVersion = feedStateRepository.feedListVersion.value
        fakeRssParserWrapper.setChannel(
            feedSource.url,
            createRssChannel(
                title = "Test Feed",
                link = "https://example.com",
                items = listOf(
                    createRssItem(
                        id = "new-item",
                        title = "New Item",
                        link = "https://example.com/new-item",
                    ),
                ),
            ),
        )

        feedFetcherRepository.fetchFeeds(publishToFeedList = true)
        advanceUntilIdle()

        assertEquals(2, feedStateRepository.feedState.value.size)
        assertTrue(feedStateRepository.feedState.value.any { it.id == "existing" })
        assertTrue(feedStateRepository.feedState.value.any { it.url == "https://example.com/new-item" })
        assertTrue(feedStateRepository.feedListVersion.value > initialVersion)
    }

    @Test
    fun `fetchFeeds skips refresh when feed is too recent`() = runTest(testDispatcher) {
        setupLocalAccount()

        val lastSyncTimestamp = dateFormatter.currentTimeMillis() - 30.minutes.inWholeMilliseconds
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
    fun `fetchFeeds respects minimum interval`() = runTest(testDispatcher) {
        setupLocalAccount()

        val lastSyncTimestamp = dateFormatter.currentTimeMillis() - 10.minutes.inWholeMilliseconds
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
    }

    @Test
    fun `fetchFeeds preserves pending notifications after foreground refresh`() = runTest(testDispatcher) {
        setupLocalAccount()

        val feedSource = createFeedSource(
            id = "source-1",
            title = "Notify Feed",
            websiteUrl = null,
            logoUrl = null,
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.updateNotificationEnabledStatus(feedSource.id, true)

        val rssChannel = createRssChannel(
            title = "Notify Feed",
            link = "https://example.com",
            items = listOf(
                createRssItem(
                    id = "item-1",
                    title = "Item 1",
                    link = "https://example.com/item-1",
                ),
            ),
        )
        fakeRssParserWrapper.setChannel(feedSource.url, rssChannel)

        feedFetcherRepository.fetchFeeds()
        advanceUntilIdle()

        val sourcesToNotify = feedFetcherRepository.getFeedSourceToNotify()
        assertEquals(1, sourcesToNotify.size)
        assertEquals(feedSource.id, sourcesToNotify.single().feedSourceId)
    }

    @Test
    fun `fetchFeeds respects OpenRSS refresh window`() = runTest(testDispatcher) {
        setupLocalAccount()

        val now = dateFormatter.currentTimeMillis()
        val feedSource = createFeedSource(
            id = "source-1",
            title = "OpenRSS Feed",
            url = "https://openrss.org/example.xml",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.updateFeedSourcesCacheInfo(
            listOf(
                FeedSourceCacheInfo(
                    feedSourceId = feedSource.id,
                    etag = null,
                    lastModified = null,
                    validatorsTimestamp = null,
                    nextFetchTimestamp = now + 30.minutes.inWholeMilliseconds,
                    backoffTimestamp = null,
                ),
            ),
        )

        val rssChannel = createRssChannel(
            title = "OpenRSS Feed",
            link = "https://openrss.org",
            items = emptyList(),
        )
        fakeRssParserWrapper.setChannel(feedSource.url, rssChannel)

        feedFetcherRepository.fetchFeeds()
        advanceUntilIdle()

        assertEquals(0, fakeRssParserWrapper.callCount)
    }

    @Test
    fun `fetchFeeds respects refresh window for regular feeds`() = runTest(testDispatcher) {
        setupLocalAccount()

        val now = dateFormatter.currentTimeMillis()
        val feedSource = createFeedSource(
            id = "source-1",
            title = "Test Feed",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.updateFeedSourcesCacheInfo(
            listOf(
                FeedSourceCacheInfo(
                    feedSourceId = feedSource.id,
                    etag = null,
                    lastModified = null,
                    validatorsTimestamp = null,
                    nextFetchTimestamp = now + 30.minutes.inWholeMilliseconds,
                    backoffTimestamp = null,
                ),
            ),
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
    }

    @Test
    fun `fetchFeeds skips refresh when next fetch timestamp is in the future`() = runTest(testDispatcher) {
        setupLocalAccount()

        val now = dateFormatter.currentTimeMillis()
        val feedSource = createFeedSource(
            id = "source-1",
            title = "Test Feed",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.updateFeedSourcesCacheInfo(
            listOf(
                FeedSourceCacheInfo(
                    feedSourceId = feedSource.id,
                    etag = null,
                    lastModified = null,
                    validatorsTimestamp = null,
                    nextFetchTimestamp = now + 4.hours.inWholeMilliseconds,
                    backoffTimestamp = null,
                ),
            ),
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
    }

    @Test
    fun `fetchFeeds honors backoff`() = runTest(testDispatcher) {
        setupLocalAccount()

        val now = dateFormatter.currentTimeMillis()
        val feedSource = createFeedSource(
            id = "source-1",
            title = "Test Feed",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.updateFeedSourcesCacheInfo(
            listOf(
                FeedSourceCacheInfo(
                    feedSourceId = feedSource.id,
                    etag = null,
                    lastModified = null,
                    validatorsTimestamp = null,
                    nextFetchTimestamp = null,
                    backoffTimestamp = now + 1.hours.inWholeMilliseconds,
                ),
            ),
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
    }

    @Test
    fun `fetchFeeds with force refresh ignores the refresh window`() = runTest(testDispatcher) {
        setupLocalAccount()

        val now = dateFormatter.currentTimeMillis()
        val feedSource = createFeedSource(
            id = "source-1",
            title = "Test Feed",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.updateFeedSourcesCacheInfo(
            listOf(
                FeedSourceCacheInfo(
                    feedSourceId = feedSource.id,
                    etag = null,
                    lastModified = null,
                    validatorsTimestamp = null,
                    nextFetchTimestamp = now + 4.hours.inWholeMilliseconds,
                    backoffTimestamp = null,
                ),
            ),
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
    fun `fetchFeeds with force refresh still honors backoff`() = runTest(testDispatcher) {
        setupLocalAccount()

        val now = dateFormatter.currentTimeMillis()
        val feedSource = createFeedSource(
            id = "source-1",
            title = "Test Feed",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.updateFeedSourcesCacheInfo(
            listOf(
                FeedSourceCacheInfo(
                    feedSourceId = feedSource.id,
                    etag = null,
                    lastModified = null,
                    validatorsTimestamp = null,
                    nextFetchTimestamp = null,
                    backoffTimestamp = now + 1.hours.inWholeMilliseconds,
                ),
            ),
        )

        val rssChannel = createRssChannel(
            title = "Test Feed",
            link = "https://example.com",
            items = emptyList(),
        )
        fakeRssParserWrapper.setChannel(feedSource.url, rssChannel)

        feedFetcherRepository.fetchFeeds(forceRefresh = true)
        advanceUntilIdle()

        assertEquals(0, fakeRssParserWrapper.callCount)
    }

    @Test
    fun `fetchFeeds sends fresh validators`() = runTest(testDispatcher) {
        setupLocalAccount()

        val now = dateFormatter.currentTimeMillis()
        val feedSource = createFeedSource(
            id = "source-1",
            title = "Test Feed",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.updateFeedSourcesCacheInfo(
            listOf(
                FeedSourceCacheInfo(
                    feedSourceId = feedSource.id,
                    etag = "\"abc\"",
                    lastModified = "Wed, 01 Jan 2025 00:00:00 GMT",
                    validatorsTimestamp = now - 7.days.inWholeMilliseconds,
                    nextFetchTimestamp = null,
                    backoffTimestamp = null,
                ),
            ),
        )
        fakeRssParserWrapper.setChannel(
            feedSource.url,
            createRssChannel(title = "Test Feed", link = "https://example.com", items = emptyList()),
        )

        feedFetcherRepository.fetchFeeds()
        advanceUntilIdle()

        assertEquals(
            FeedHttpValidators(etag = "\"abc\"", lastModified = "Wed, 01 Jan 2025 00:00:00 GMT"),
            fakeRssParserWrapper.validatorsSeenByUrl[feedSource.url],
        )
    }

    @Test
    fun `fetchFeeds drops validators older than eight days`() = runTest(testDispatcher) {
        setupLocalAccount()

        val now = dateFormatter.currentTimeMillis()
        val feedSource = createFeedSource(
            id = "source-1",
            title = "Test Feed",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.updateFeedSourcesCacheInfo(
            listOf(
                FeedSourceCacheInfo(
                    feedSourceId = feedSource.id,
                    etag = "\"abc\"",
                    lastModified = "Wed, 01 Jan 2025 00:00:00 GMT",
                    validatorsTimestamp = now - 9.days.inWholeMilliseconds,
                    nextFetchTimestamp = null,
                    backoffTimestamp = null,
                ),
            ),
        )
        fakeRssParserWrapper.setChannel(
            feedSource.url,
            createRssChannel(title = "Test Feed", link = "https://example.com", items = emptyList()),
        )

        feedFetcherRepository.fetchFeeds()
        advanceUntilIdle()

        assertEquals(null, fakeRssParserWrapper.validatorsSeenByUrl[feedSource.url])
    }

    @Test
    fun `fetchFeeds treats 304 as unchanged feed without error`() = runTest(testDispatcher) {
        setupLocalAccount()

        val feedSource = createFeedSource(
            id = "source-1",
            title = "Unchanged Feed",
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        fakeRssParserWrapper.setNotModified(feedSource.url)

        feedStateRepository.errorState.test {
            feedFetcherRepository.fetchFeeds()
            advanceUntilIdle()

            expectNoEvents()
        }

        val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertNotNull(updatedFeedSource)
        assertTrue(!updatedFeedSource.fetchFailed)
        assertNotNull(updatedFeedSource.lastSyncTimestamp)
        assertEquals(FinishedFeedUpdateStatus, feedStateRepository.updateState.value)
    }

    @Test
    fun `fetchFeeds persists cache info after a fetch`() = runTest(testDispatcher) {
        setupLocalAccount()

        val feedSource = createFeedSource(
            id = "source-1",
            title = "Test Feed",
            websiteUrl = null,
            logoUrl = null,
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))

        val rssChannel = createRssChannel(
            title = "Test Feed",
            link = "https://example.com",
            items = emptyList(),
        )
        fakeRssParserWrapper.setChannel(feedSource.url, rssChannel)

        feedFetcherRepository.fetchFeeds()
        advanceUntilIdle()

        val cacheInfo = databaseHelper.getFeedSourcesCacheInfo().single()
        assertEquals(feedSource.id, cacheInfo.feedSourceId)
        val nextFetchTimestamp = assertNotNull(cacheInfo.nextFetchTimestamp)
        assertTrue(nextFetchTimestamp > dateFormatter.currentTimeMillis())
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
    fun `fetchFeeds marks feed as failed without emitting error on parser failure`() =
        runTest(testDispatcher) {
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

                expectNoEvents()
            }

            val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
            assertNotNull(updatedFeedSource)
            assertTrue(updatedFeedSource.fetchFailed)
        }

    @Test
    fun `fetchFeeds clears fetch-failed flag on success even when the feed has no items`() =
        runTest(testDispatcher) {
            setupLocalAccount()

            val feedSource = createFeedSource(
                id = "source-1",
                title = "Recovered Feed",
            )
            databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
            databaseHelper.setFeedFetchFailed(feedSource.id, true)
            fakeRssParserWrapper.setChannel(
                feedSource.url,
                createRssChannel(title = "Recovered Feed", link = "https://example.com", items = emptyList()),
            )

            feedFetcherRepository.fetchFeeds()
            advanceUntilIdle()

            val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
            assertNotNull(updatedFeedSource)
            assertTrue(!updatedFeedSource.fetchFailed)
            assertNotNull(updatedFeedSource.lastSyncTimestamp)
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
        category: FeedSourceCategory? = null,
    ): FeedSource = FeedSource(
        id = id,
        url = url,
        title = title,
        category = category,
        lastSyncTimestamp = lastSyncTimestamp,
        logoUrl = logoUrl,
        websiteUrl = websiteUrl,
        fetchFailed = false,
        articleOpenMode = com.prof18.feedflow.core.model.ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
    )

    private fun createScopedSources(): List<FeedSource> {
        val category = FeedSourceCategory(id = "category-tech", title = "Technology")
        return listOf(
            createFeedSource(id = "source-a", title = "Source A", category = category),
            createFeedSource(id = "source-b", title = "Source B", category = category),
            createFeedSource(id = "source-c", title = "Source C"),
        )
    }

    private suspend fun insertScopedSources(sources: List<FeedSource>) {
        databaseHelper.insertCategories(listOf(requireNotNull(sources.first().category)))
        databaseHelper.insertFeedSource(sources.map { it.toParsedFeedSource() })
    }

    private fun setEmptyChannels(sources: List<FeedSource>) {
        sources.forEach { source ->
            fakeRssParserWrapper.setChannel(
                source.url,
                createRssChannel(title = source.title, link = "https://example.com", items = emptyList()),
            )
        }
    }

    private fun createRssChannel(
        title: String,
        link: String,
        items: List<RssItem>,
    ): RssChannel = RssChannelGenerator.rssChannel(
        title = title,
        link = link,
        items = items,
    )

    private fun createRssItem(
        id: String,
        title: String,
        link: String,
    ): RssItem = RssItemGenerator.rssItem(
        guid = id,
        title = title,
        link = link,
        pubDate = null,
        categories = emptyList(),
        commentsUrl = null,
    )

    private class FakeRssParserWrapper : RssParserWrapper {
        private val channelByUrl = mutableMapOf<String, RssChannel>()
        private val errorUrls = mutableSetOf<String>()
        private val notModifiedUrls = mutableSetOf<String>()
        val validatorsSeenByUrl = mutableMapOf<String, FeedHttpValidators?>()
        val requestedUrls = mutableListOf<String>()
        var validatorsFor: (String) -> FeedHttpValidators? = { null }
        var onRequest: suspend (String) -> Unit = {}
        var callCount: Int = 0
            private set

        fun reset() {
            channelByUrl.clear()
            errorUrls.clear()
            notModifiedUrls.clear()
            validatorsSeenByUrl.clear()
            requestedUrls.clear()
            callCount = 0
            onRequest = {}
        }

        fun setChannel(url: String, channel: RssChannel) {
            channelByUrl[url] = channel
        }

        fun setError(url: String) {
            errorUrls.add(url)
        }

        fun setNotModified(url: String) {
            notModifiedUrls.add(url)
        }

        override suspend fun getRssChannel(url: String): RssChannel {
            callCount += 1
            requestedUrls.add(url)
            validatorsSeenByUrl[url] = validatorsFor(url)
            onRequest(url)
            if (notModifiedUrls.contains(url)) {
                throw HttpException(code = 304, message = "Not Modified")
            }
            if (errorUrls.contains(url)) {
                error("Failure for $url")
            }
            return requireNotNull(channelByUrl[url]) { "Missing channel for $url" }
        }
    }
}
