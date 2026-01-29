package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.domain.FeedSourceLogoRetriever
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOperation
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.StartedFeedUpdateStatus
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feed.RssParserWrapper
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import com.prof18.feedflow.shared.presentation.model.DeleteFeedSourceError
import com.prof18.feedflow.shared.presentation.model.FeedErrorState
import com.prof18.feedflow.shared.presentation.model.SyncError
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
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
import org.koin.test.get
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import com.prof18.feedflow.core.model.DatabaseError as DatabaseErrorCode

class HomeViewModelTest : KoinTestBase() {

    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val feedStateRepository: FeedStateRepository by inject()

    private val fakeRssParser = FakeRssParserWrapper()
    private val fakeLogoRetriever = FakeFeedSourceLogoRetriever()

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<RssParserWrapper> { fakeRssParser }
        factory<FeedSourceLogoRetriever> { fakeLogoRetriever }
        single<HtmlParser> { FakeHtmlParser() }
    }

    @Test
    fun `drawer state uses uncategorized sources when only null category exists`() = runTest(testDispatcher) {
        val feedSourceA = createFeedSource(id = "source-a", title = "Alpha", isPinned = true)
        val feedSourceB = createFeedSource(id = "source-b", title = "Beta", isPinned = false)
        insertFeedSources(feedSourceA, feedSourceB)
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = feedSourceA.id,
            preference = LinkOpeningPreference.DEFAULT,
            isHidden = false,
            isPinned = true,
            isNotificationEnabled = false,
        )

        val items = listOf(
            buildFeedItem(
                id = "item-1",
                title = "Unread",
                pubDateMillis = 2000,
                source = feedSourceA,
            ),
            buildFeedItem(
                id = "item-2",
                title = "Read",
                pubDateMillis = 1000,
                source = feedSourceB,
            ),
            buildFeedItem(
                id = "item-3",
                title = "Bookmarked",
                pubDateMillis = 1500,
                source = feedSourceB,
            ),
        )
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)
        databaseHelper.updateReadStatus(FeedItemId("item-2"), isRead = true)
        databaseHelper.updateBookmarkStatus(FeedItemId("item-3"), isBookmarked = true)

        val viewModel = getViewModel()
        advanceUntilIdle()

        val state = viewModel.navDrawerState.value
        assertEquals(2L, (state.timeline.first() as DrawerItem.Timeline).unreadCount)
        assertEquals(1L, (state.bookmarks.first() as DrawerItem.Bookmarks).unreadCount)
        assertEquals(1, state.pinnedFeedSources.size)
        assertEquals(2, state.feedSourcesWithoutCategory.size)
        assertTrue(state.feedSourcesByCategory.isEmpty())
    }

    @Test
    fun `drawer state groups sources by category when categories exist`() = runTest(testDispatcher) {
        val category = FeedSourceCategory(id = "category-1", title = "Tech")
        databaseHelper.insertCategories(listOf(category))

        val feedSource = createFeedSource(
            id = "source-1",
            title = "Source 1",
            category = category,
        )
        insertFeedSources(feedSource)

        val viewModel = getViewModel()
        advanceUntilIdle()

        val state = viewModel.navDrawerState.value
        assertTrue(state.feedSourcesWithoutCategory.isEmpty())
        assertEquals(1, state.feedSourcesByCategory.size)
        assertEquals(1, state.categories.size)
    }

    @Test
    fun `onAppLaunch fetches once`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)

        val channel = createChannel(
            title = "Feed",
            link = "https://example.com",
            items = listOf(createItem(id = "item-1", title = "Item 1", link = "https://example.com/1")),
        )
        fakeRssParser.setChannel(feedSource.url, channel)

        val viewModel = getViewModel()

        viewModel.onAppLaunch()
        advanceUntilIdle()

        assertEquals(1, viewModel.feedState.value.size)
        assertEquals(1, fakeRssParser.callCount)

        viewModel.onAppLaunch()
        advanceUntilIdle()

        assertEquals(1, fakeRssParser.callCount)
    }

    @Test
    fun `getNewFeeds skips fetch on first launch when disabled`() = runTest(testDispatcher) {
        settingsRepository.setRefreshFeedsOnLaunch(false)
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        fakeRssParser.setChannel(feedSource.url, createChannel(title = "Feed", link = "https://example.com"))

        val viewModel = getViewModel()

        viewModel.getNewFeeds(isFirstLaunch = true)
        advanceUntilIdle()

        assertEquals(0, fakeRssParser.callCount)
        assertTrue(viewModel.feedState.value.isEmpty())
    }

    @Test
    fun `getNewFeeds fetches on first launch when enabled`() = runTest(testDispatcher) {
        settingsRepository.setRefreshFeedsOnLaunch(true)
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        fakeRssParser.setChannel(feedSource.url, createChannel(title = "Feed", link = "https://example.com"))

        val viewModel = getViewModel()

        viewModel.getNewFeeds(isFirstLaunch = true)
        advanceUntilIdle()

        assertEquals(1, fakeRssParser.callCount)
    }

    @Test
    fun `getNewFeeds fetches when not first launch even if disabled`() = runTest(testDispatcher) {
        settingsRepository.setRefreshFeedsOnLaunch(false)
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        fakeRssParser.setChannel(feedSource.url, createChannel(title = "Feed", link = "https://example.com"))

        val viewModel = getViewModel()

        viewModel.getNewFeeds()
        advanceUntilIdle()

        assertEquals(1, fakeRssParser.callCount)
    }

    @Test
    fun `markAsReadOnScroll does nothing when disabled`() = runTest(testDispatcher) {
        settingsRepository.setMarkFeedAsReadWhenScrolling(false)
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val items = listOf(
            buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 2000, source = feedSource),
            buildFeedItem(id = "item-2", title = "Item 2", pubDateMillis = 1000, source = feedSource),
        )
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.markAsReadOnScroll(1)
        advanceUntilIdle()

        val dbItems = getDbItems()
        assertTrue(dbItems.all { it.is_read.not() })
    }

    @Test
    fun `markAsReadOnScroll does nothing while loading`() = runTest(testDispatcher) {
        settingsRepository.setMarkFeedAsReadWhenScrolling(true)
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val items = listOf(
            buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 2000, source = feedSource),
            buildFeedItem(id = "item-2", title = "Item 2", pubDateMillis = 1000, source = feedSource),
        )
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        feedStateRepository.emitUpdateStatus(StartedFeedUpdateStatus)

        viewModel.markAsReadOnScroll(1)
        advanceUntilIdle()

        val dbItems = getDbItems()
        assertTrue(dbItems.all { it.is_read.not() })
    }

    @Test
    fun `markAsReadOnScroll marks range and ignores lower index`() = runTest(testDispatcher) {
        settingsRepository.setMarkFeedAsReadWhenScrolling(true)
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val items = listOf(
            buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 3000, source = feedSource),
            buildFeedItem(id = "item-2", title = "Item 2", pubDateMillis = 2000, source = feedSource),
            buildFeedItem(id = "item-3", title = "Item 3", pubDateMillis = 1000, source = feedSource),
        )
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.markAsReadOnScroll(1)
        advanceUntilIdle()

        viewModel.markAsReadOnScroll(0)
        advanceUntilIdle()

        val dbItems = getDbItems()
        val readIds = dbItems.filter { it.is_read }.map { it.url_hash }.toSet()
        assertEquals(setOf("item-1", "item-2"), readIds)
    }

    @Test
    fun `requestNewFeedsPage loads more items`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val items = (1..80).map { index ->
            buildFeedItem(
                id = "item-$index",
                title = "Item $index",
                pubDateMillis = 1000L + index,
                source = feedSource,
            )
        }
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        assertEquals(40, viewModel.feedState.value.size)

        viewModel.requestNewFeedsPage()
        advanceUntilIdle()

        assertEquals(80, viewModel.feedState.value.size)
    }

    @Test
    fun `markAllRead updates state and database`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val items = listOf(
            buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 2000, source = feedSource),
            buildFeedItem(id = "item-2", title = "Item 2", pubDateMillis = 1000, source = feedSource),
        )
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.feedOperationState.test {
            assertEquals(FeedOperation.None, awaitItem())

            viewModel.markAllRead()
            assertEquals(FeedOperation.MarkingAllRead, awaitItem())
            assertEquals(FeedOperation.None, awaitItem())
        }

        val dbItems = getDbItems()
        assertTrue(dbItems.all { it.is_read })
    }

    @Test
    fun `markAsRead updates database`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val item = buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 1000, source = feedSource)
        databaseHelper.insertFeedItems(listOf(item), lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.markAsRead(item.id)
        advanceUntilIdle()

        val dbItem = getDbItems().first()
        assertTrue(dbItem.is_read)
    }

    @Test
    fun `markAsRead removes item from feed list when hideReadItems is enabled`() = runTest(testDispatcher) {
        settingsRepository.setHideReadItems(true)
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val items = listOf(
            buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 2000, source = feedSource),
            buildFeedItem(id = "item-2", title = "Item 2", pubDateMillis = 1000, source = feedSource),
        )
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        assertEquals(2, viewModel.feedState.value.size)

        viewModel.markAsRead("item-1")
        advanceUntilIdle()

        assertEquals(1, viewModel.feedState.value.size)
        assertEquals("item-2", viewModel.feedState.value.first().id)
    }

    @Test
    fun `markAsRead keeps item in feed list when hideReadItems is disabled`() = runTest(testDispatcher) {
        settingsRepository.setHideReadItems(false)
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val items = listOf(
            buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 2000, source = feedSource),
            buildFeedItem(id = "item-2", title = "Item 2", pubDateMillis = 1000, source = feedSource),
        )
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        assertEquals(2, viewModel.feedState.value.size)

        viewModel.markAsRead("item-1")
        advanceUntilIdle()

        assertEquals(2, viewModel.feedState.value.size)
        assertTrue(viewModel.feedState.value.any { it.id == "item-1" && it.isRead })
    }

    @Test
    fun `markAllAboveAsRead removes items from feed list when hideReadItems is enabled`() = runTest(testDispatcher) {
        settingsRepository.setHideReadItems(true)
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val items = listOf(
            buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 3000, source = feedSource),
            buildFeedItem(id = "item-2", title = "Item 2", pubDateMillis = 2000, source = feedSource),
            buildFeedItem(id = "item-3", title = "Item 3", pubDateMillis = 1000, source = feedSource),
        )
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        assertEquals(3, viewModel.feedState.value.size)

        viewModel.markAllAboveAsRead("item-2")
        advanceUntilIdle()

        assertEquals(1, viewModel.feedState.value.size)
        assertEquals("item-3", viewModel.feedState.value.first().id)
    }

    @Test
    fun `markAllBelowAsRead removes items from feed list when hideReadItems is enabled`() = runTest(testDispatcher) {
        settingsRepository.setHideReadItems(true)
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val items = listOf(
            buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 3000, source = feedSource),
            buildFeedItem(id = "item-2", title = "Item 2", pubDateMillis = 2000, source = feedSource),
            buildFeedItem(id = "item-3", title = "Item 3", pubDateMillis = 1000, source = feedSource),
        )
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        assertEquals(3, viewModel.feedState.value.size)

        viewModel.markAllBelowAsRead("item-2")
        advanceUntilIdle()

        assertEquals(1, viewModel.feedState.value.size)
        assertEquals("item-1", viewModel.feedState.value.first().id)
    }

    @Test
    fun `markAllAboveAsRead updates database`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val items = listOf(
            buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 3000, source = feedSource),
            buildFeedItem(id = "item-2", title = "Item 2", pubDateMillis = 2000, source = feedSource),
            buildFeedItem(id = "item-3", title = "Item 3", pubDateMillis = 1000, source = feedSource),
        )
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.markAllAboveAsRead("item-2")
        advanceUntilIdle()

        val dbItems = getDbItems().associateBy { it.url_hash }
        assertTrue(dbItems.getValue("item-1").is_read)
        assertTrue(dbItems.getValue("item-2").is_read)
        assertTrue(dbItems.getValue("item-3").is_read.not())
    }

    @Test
    fun `markAllBelowAsRead updates database`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val items = listOf(
            buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 3000, source = feedSource),
            buildFeedItem(id = "item-2", title = "Item 2", pubDateMillis = 2000, source = feedSource),
            buildFeedItem(id = "item-3", title = "Item 3", pubDateMillis = 1000, source = feedSource),
        )
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.markAllBelowAsRead("item-2")
        advanceUntilIdle()

        val dbItems = getDbItems().associateBy { it.url_hash }
        assertTrue(dbItems.getValue("item-1").is_read.not())
        assertTrue(dbItems.getValue("item-2").is_read)
        assertTrue(dbItems.getValue("item-3").is_read)
    }

    @Test
    fun `deleteOldFeedItems removes old entries`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val now = Clock.System.now().toEpochMilliseconds()
        val items = listOf(
            buildFeedItem(
                id = "item-old",
                title = "Old",
                pubDateMillis = now - 8.days.inWholeMilliseconds,
                source = feedSource,
            ),
            buildFeedItem(
                id = "item-new",
                title = "New",
                pubDateMillis = now,
                source = feedSource,
            ),
        )
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.feedOperationState.test {
            awaitItem()
            viewModel.deleteOldFeedItems()
            assertEquals(FeedOperation.Deleting, awaitItem())
            assertEquals(FeedOperation.None, awaitItem())
        }

        val dbItems = getDbItems().map { it.url_hash }
        assertEquals(listOf("item-new"), dbItems)
    }

    @Test
    fun `forceFeedRefresh triggers fetch`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        fakeRssParser.setChannel(feedSource.url, createChannel(title = "Feed", link = "https://example.com"))

        val viewModel = getViewModel()

        viewModel.forceFeedRefresh()
        advanceUntilIdle()

        assertEquals(1, fakeRssParser.callCount)
    }

    @Test
    fun `deleteAllFeeds clears data`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val item = buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 1000, source = feedSource)
        databaseHelper.insertFeedItems(listOf(item), lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.deleteAllFeeds()
        advanceUntilIdle()

        assertTrue(databaseHelper.getFeedSources().isEmpty())
        assertTrue(getDbItems().isEmpty())
    }

    @Test
    fun `updateFeedSourceFilter updates current filter`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)

        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.updateFeedSourceFilter(feedSource.id)
        advanceUntilIdle()

        val filter = viewModel.currentFeedFilter.value
        assertIs<FeedFilter.Source>(filter)
        assertEquals(feedSource.id, filter.feedSource.id)
    }

    @Test
    fun `updateCategoryFilter updates current filter`() = runTest(testDispatcher) {
        val category = FeedSourceCategory(id = "category-1", title = "Tech")
        databaseHelper.insertCategories(listOf(category))

        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.updateCategoryFilter(category.id)
        advanceUntilIdle()

        val filter = viewModel.currentFeedFilter.value
        assertIs<FeedFilter.Category>(filter)
        assertEquals(category.id, filter.feedCategory.id)
    }

    @Test
    fun `onFeedFilterSelected updates current filter`() = runTest(testDispatcher) {
        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.onFeedFilterSelected(FeedFilter.Read)
        advanceUntilIdle()

        assertEquals(FeedFilter.Read, viewModel.currentFeedFilter.value)
    }

    @Test
    fun `updateReadStatus updates database`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val item = buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 1000, source = feedSource)
        databaseHelper.insertFeedItems(listOf(item), lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.updateReadStatus(FeedItemId(item.id), read = true)
        advanceUntilIdle()

        val dbItem = getDbItems().first()
        assertTrue(dbItem.is_read)
    }

    @Test
    fun `updateBookmarkStatus updates database`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)
        val item = buildFeedItem(id = "item-1", title = "Item 1", pubDateMillis = 1000, source = feedSource)
        databaseHelper.insertFeedItems(listOf(item), lastSyncTimestamp = 0)

        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.updateBookmarkStatus(FeedItemId(item.id), bookmarked = true)
        advanceUntilIdle()

        val dbItem = getDbItems().first()
        assertTrue(dbItem.is_bookmarked)
    }

    @Test
    fun `toggleFeedPin updates preference`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1", isPinned = false)
        insertFeedSources(feedSource)

        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.toggleFeedPin(feedSource)
        advanceUntilIdle()

        val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertNotNull(updatedFeedSource)
        assertTrue(updatedFeedSource.isPinned)
    }

    @Test
    fun `enqueueBackup keeps sync upload state unchanged`() = runTest(testDispatcher) {
        val viewModel = getViewModel()
        advanceUntilIdle()

        val before = viewModel.isSyncUploadRequired.value
        viewModel.enqueueBackup()
        advanceUntilIdle()
        val after = viewModel.isSyncUploadRequired.value

        assertEquals(before, after)
    }

    @Test
    fun `deleteFeedSource removes feed source`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Source 1")
        insertFeedSources(feedSource)

        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.deleteFeedSource(feedSource)
        advanceUntilIdle()

        val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertEquals(null, updatedFeedSource)
    }

    @Test
    fun `updateCategoryName updates category`() = runTest(testDispatcher) {
        val category = FeedSourceCategory(id = "category-1", title = "Old")
        databaseHelper.insertCategories(listOf(category))

        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.updateCategoryName(CategoryId(category.id), CategoryName("New"))
        advanceUntilIdle()

        val updatedCategory = databaseHelper.getFeedSourceCategory(category.id)
        assertNotNull(updatedCategory)
        assertEquals("New", updatedCategory.title)
    }

    @Test
    fun `deleteCategory removes category and resets feed sources`() = runTest(testDispatcher) {
        val category = FeedSourceCategory(id = "category-1", title = "Tech")
        databaseHelper.insertCategories(listOf(category))
        val feedSource = createFeedSource(id = "source-1", title = "Source 1", category = category)
        insertFeedSources(feedSource)

        val viewModel = getViewModel()
        advanceUntilIdle()

        viewModel.deleteCategory(CategoryId(category.id))
        advanceUntilIdle()

        val updatedCategory = databaseHelper.getFeedSourceCategory(category.id)
        assertEquals(null, updatedCategory)
        val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertNotNull(updatedFeedSource)
        assertEquals(null, updatedFeedSource.category)
    }

    @Test
    fun `errorState maps errors to ui errors`() = runTest(testDispatcher) {
        val viewModel = getViewModel()

        viewModel.errorState.test {
            feedStateRepository.emitErrorState(FeedErrorState("Feed"))
            assertEquals(UIErrorState.FeedErrorState("Feed"), awaitItem())

            feedStateRepository.emitErrorState(DatabaseError(DatabaseErrorCode.FeedRetrievalFailed))
            assertEquals(UIErrorState.DatabaseError(DatabaseErrorCode.FeedRetrievalFailed), awaitItem())

            feedStateRepository.emitErrorState(SyncError(com.prof18.feedflow.core.model.FeedSyncError.SyncFeedsFailed))
            assertEquals(
                UIErrorState.SyncError(com.prof18.feedflow.core.model.FeedSyncError.SyncFeedsFailed),
                awaitItem(),
            )

            feedStateRepository.emitErrorState(DeleteFeedSourceError())
            assertEquals(UIErrorState.DeleteFeedSourceError, awaitItem())
        }
    }

    @Test
    fun `getCurrentThemeMode reads settings`() = runTest(testDispatcher) {
        settingsRepository.setThemeMode(ThemeMode.DARK)
        val viewModel = getViewModel()
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, viewModel.getCurrentThemeMode())
    }

    private fun getViewModel(): HomeViewModel = get()

    private suspend fun insertFeedSources(vararg sources: FeedSource) {
        val categories = sources.mapNotNull { it.category }.distinctBy { it.id }
        if (categories.isNotEmpty()) {
            databaseHelper.insertCategories(categories)
        }
        databaseHelper.insertFeedSource(sources.map { it.toParsedFeedSource() })
    }

    private fun createFeedSource(
        id: String,
        title: String,
        category: FeedSourceCategory? = null,
        isPinned: Boolean = false,
    ): FeedSource = FeedSourceGenerator.feedSourceArb.next().copy(
        id = id,
        url = "https://example.com/$id/rss.xml",
        title = title,
        category = category,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = null,
        fetchFailed = false,
        isPinned = isPinned,
    )

    private fun createChannel(
        title: String,
        link: String,
        items: List<RssItem> = emptyList(),
    ) = RssChannelGenerator.rssChannelArb.next().copy(
        title = title,
        link = link,
        items = items,
    )

    private fun createItem(
        id: String,
        title: String,
        link: String,
    ) = RssItemGenerator.rssItemArb.next().copy(
        guid = id,
        title = title,
        link = link,
        categories = emptyList(),
        itunesItemData = null,
        commentsUrl = null,
        youtubeItemData = null,
        rawEnclosure = null,
    )

    private suspend fun getDbItems() = databaseHelper.getFeedItems(
        feedFilter = FeedFilter.Timeline,
        pageSize = 100,
        offset = 0,
        showReadItems = true,
        sortOrder = com.prof18.feedflow.core.model.FeedOrder.NEWEST_FIRST,
    )

    private class FakeRssParserWrapper : RssParserWrapper {
        private val channelByUrl = mutableMapOf<String, RssChannel>()
        var callCount: Int = 0
            private set

        fun setChannel(url: String, channel: RssChannel) {
            channelByUrl[url] = channel
        }

        override suspend fun getRssChannel(url: String): RssChannel {
            callCount += 1
            return requireNotNull(channelByUrl[url]) { "Missing channel for $url" }
        }
    }

    private class FakeFeedSourceLogoRetriever : FeedSourceLogoRetriever {
        override suspend fun getFeedSourceLogoUrl(rssChannel: RssChannel): String? = null
        override suspend fun getFeedSourceLogoUrl(websiteLink: String?): String? = null
    }

    private class FakeHtmlParser : HtmlParser {
        override fun getTextFromHTML(html: String): String? = html
        override fun getFaviconUrl(html: String): String? = null
        override fun getRssUrl(html: String): String? = null
    }
}
