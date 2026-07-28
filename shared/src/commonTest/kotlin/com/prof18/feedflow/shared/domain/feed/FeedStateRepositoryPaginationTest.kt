package com.prof18.feedflow.shared.domain.feed

import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository.Companion.FEED_DB_PAGE_SIZE
import com.prof18.feedflow.shared.test.FailableSqlDriver
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.createInMemoryDriver
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import com.prof18.feedflow.shared.test.koin.TestModules
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedStateRepositoryPaginationTest : KoinTestBase() {

    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository by inject()

    private val feedSource: FeedSource = FeedSourceGenerator.feedSource()

    private val failableDriver = FailableSqlDriver(createInMemoryDriver())

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + module {
            single<SqlDriver> { failableDriver }
        }

    @Test
    fun `loadMoreFeeds does not skip unread items marked as read between pages`() = runTest {
        val itemCount = PAGE_SIZE * 2 + 10
        val items = List(itemCount) { index ->
            unreadItem(index, pubDate = BASE_PUB_DATE - index * PUB_DATE_STEP)
        }
        seed(items)

        feedStateRepository.getFeeds()
        markCurrentPageAsReadInDb()
        feedStateRepository.loadMoreFeeds()
        markCurrentPageAsReadInDb()
        feedStateRepository.loadMoreFeeds()

        val loadedIds = feedStateRepository.feedState.value.map { it.id }
        assertEquals(items.map { it.id }, loadedIds)
        assertEquals(loadedIds.size, loadedIds.toSet().size)
    }

    @Test
    fun `loadMoreFeeds does not skip unread items marked as read between pages with oldest first`() = runTest {
        val itemCount = PAGE_SIZE * 2 + 10
        val items = List(itemCount) { index ->
            unreadItem(index, pubDate = BASE_PUB_DATE + index * PUB_DATE_STEP)
        }
        seed(items, feedOrder = FeedOrder.OLDEST_FIRST)

        feedStateRepository.getFeeds()
        markCurrentPageAsReadInDb()
        feedStateRepository.loadMoreFeeds()
        markCurrentPageAsReadInDb()
        feedStateRepository.loadMoreFeeds()

        val loadedIds = feedStateRepository.feedState.value.map { it.id }
        assertEquals(items.map { it.id }, loadedIds)
        assertEquals(loadedIds.size, loadedIds.toSet().size)
    }

    @Test
    fun `loadMoreFeeds stops paginating after a short page`() = runTest {
        val itemCount = PAGE_SIZE + 10
        val items = List(itemCount) { index ->
            unreadItem(index, pubDate = BASE_PUB_DATE - index * PUB_DATE_STEP)
        }
        seed(items)

        feedStateRepository.getFeeds()
        feedStateRepository.loadMoreFeeds()
        assertEquals(itemCount, feedStateRepository.feedState.value.size)

        val olderItems = List(10) { index ->
            unreadItem(itemCount + index, pubDate = BASE_PUB_DATE - (itemCount + index) * PUB_DATE_STEP)
        }
        databaseHelper.insertFeedItems(olderItems, lastSyncTimestamp = 0)
        feedStateRepository.loadMoreFeeds()

        assertEquals(itemCount, feedStateRepository.feedState.value.size)
    }

    @Test
    fun `loadMoreFeeds keeps paginating when read items are removed from the list`() = runTest {
        val itemCount = PAGE_SIZE * 2 + 10
        val items = List(itemCount) { index ->
            unreadItem(index, pubDate = BASE_PUB_DATE - index * PUB_DATE_STEP)
        }
        seed(items, hideReadItems = true)

        feedStateRepository.getFeeds()
        val removedIds = feedStateRepository.feedState.value.take(REMOVED_ITEMS_COUNT).map { FeedItemId(it.id) }
        databaseHelper.updateReadStatus(removedIds, isRead = true)
        feedStateRepository.markAsRead(removedIds.toHashSet())
        assertEquals(PAGE_SIZE - REMOVED_ITEMS_COUNT, feedStateRepository.feedState.value.size)

        feedStateRepository.loadMoreFeeds()

        val expectedIds = items.map { it.id }.subList(REMOVED_ITEMS_COUNT, PAGE_SIZE * 2)
        assertEquals(expectedIds, feedStateRepository.feedState.value.map { it.id })
    }

    @Test
    fun `loadMoreFeeds paginates across null pub dates with newest first order`() = runTest {
        val datedItems = List(PAGE_SIZE - 2) { index ->
            unreadItem(index, pubDate = BASE_PUB_DATE - index * PUB_DATE_STEP)
        }
        val undatedItems = List(10) { index -> undatedItem(index) }
        seed(datedItems + undatedItems)

        feedStateRepository.getFeeds()
        assertEquals(PAGE_SIZE, feedStateRepository.feedState.value.size)
        feedStateRepository.loadMoreFeeds()

        val expectedIds = datedItems.map { it.id } + undatedItems.map { it.id }.reversed()
        assertEquals(expectedIds, feedStateRepository.feedState.value.map { it.id })
    }

    @Test
    fun `loadMoreFeeds paginates across null pub dates with oldest first order`() = runTest {
        val undatedItems = List(PAGE_SIZE + 5) { index -> undatedItem(index) }
        val datedItems = List(5) { index ->
            unreadItem(index, pubDate = BASE_PUB_DATE + index * PUB_DATE_STEP)
        }
        seed(undatedItems + datedItems, feedOrder = FeedOrder.OLDEST_FIRST)

        feedStateRepository.getFeeds()
        assertEquals(PAGE_SIZE, feedStateRepository.feedState.value.size)
        feedStateRepository.loadMoreFeeds()

        val expectedIds = undatedItems.map { it.id } + datedItems.map { it.id }
        assertEquals(expectedIds, feedStateRepository.feedState.value.map { it.id })
    }

    @Test
    fun `a failed refresh keeps the cursor so the next page does not repeat the first one`() = runTest {
        val itemCount = PAGE_SIZE * 2 + 10
        val items = List(itemCount) { index ->
            unreadItem(index, pubDate = BASE_PUB_DATE - index * PUB_DATE_STEP)
        }
        seed(items)

        feedStateRepository.getFeeds()
        feedStateRepository.loadMoreFeeds()
        assertEquals(PAGE_SIZE * 2, feedStateRepository.feedState.value.size)

        failableDriver.failReads = true
        feedStateRepository.getFeeds()
        failableDriver.failReads = false

        // The refresh failed, so the already loaded list is still on screen and pagination has to
        // resume after it instead of appending the first page again.
        assertEquals(PAGE_SIZE * 2, feedStateRepository.feedState.value.size)

        feedStateRepository.loadMoreFeeds()

        val loadedIds = feedStateRepository.feedState.value.map { it.id }
        assertEquals(loadedIds.size, loadedIds.toSet().size)
        assertEquals(items.map { it.id }, loadedIds)
    }

    @Test
    fun `a successful refresh after a failed one restarts from the first page`() = runTest {
        val itemCount = PAGE_SIZE * 2
        val items = List(itemCount) { index ->
            unreadItem(index, pubDate = BASE_PUB_DATE - index * PUB_DATE_STEP)
        }
        seed(items)

        feedStateRepository.getFeeds()
        feedStateRepository.loadMoreFeeds()

        failableDriver.failReads = true
        feedStateRepository.getFeeds()
        failableDriver.failReads = false

        feedStateRepository.getFeeds()

        val expectedIds = items.map { it.id }.subList(0, PAGE_SIZE)
        assertEquals(expectedIds, feedStateRepository.feedState.value.map { it.id })
    }

    private suspend fun markCurrentPageAsReadInDb() {
        val ids = feedStateRepository.feedState.value.map { FeedItemId(it.id) }
        databaseHelper.updateReadStatus(ids, isRead = true)
    }

    private suspend fun seed(
        items: List<FeedItem>,
        feedOrder: FeedOrder = FeedOrder.NEWEST_FIRST,
        hideReadItems: Boolean = false,
    ) {
        settingsRepository.setShowReadArticlesTimeline(false)
        settingsRepository.setHideReadItems(hideReadItems)
        feedAppearanceSettingsRepository.setFeedOrder(feedOrder)
        databaseHelper.insertFeedSourceWithCategory(feedSource)
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)
    }

    private fun unreadItem(index: Int, pubDate: Long): FeedItem =
        FeedItemGenerator.unreadFeedItem(
            id = "item-${index.toString().padStart(ID_PADDING, '0')}",
            feedSource = feedSource,
            pubDateMillis = pubDate,
        )

    private fun undatedItem(index: Int): FeedItem =
        FeedItemGenerator.unreadFeedItem(
            id = "undated-${index.toString().padStart(ID_PADDING, '0')}",
            feedSource = feedSource,
            pubDateMillis = null,
        )

    private companion object {
        val PAGE_SIZE = FEED_DB_PAGE_SIZE.toInt()
        const val BASE_PUB_DATE = 1_704_067_200_000L
        const val PUB_DATE_STEP = 1_000L
        const val REMOVED_ITEMS_COUNT = 10
        const val ID_PADDING = 3
    }
}
