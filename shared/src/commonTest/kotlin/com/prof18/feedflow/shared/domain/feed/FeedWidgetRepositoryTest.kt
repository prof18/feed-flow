package com.prof18.feedflow.shared.domain.feed

import app.cash.turbine.test
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FeedWidgetRepositoryTest : KoinTestBase() {

    private val databaseHelper: DatabaseHelper by inject()
    private val dateFormatter: DateFormatter by inject()
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository by inject()

    private fun createRepository(): FeedWidgetRepository =
        FeedWidgetRepository(
            databaseHelper = databaseHelper,
            dateFormatter = dateFormatter,
            feedAppearanceSettingsRepository = feedAppearanceSettingsRepository,
        )

    @Test
    fun `getFeeds returns unread items ordered by newest first`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Widget Feed")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))

        val items = listOf(
            buildFeedItem(
                id = "item-1",
                title = "Older",
                pubDateMillis = 1000,
                source = feedSource,
            ),
            buildFeedItem(
                id = "item-2",
                title = "Newer",
                pubDateMillis = 2000,
                source = feedSource,
            ),
        )
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)
        databaseHelper.updateReadStatus(FeedItemId("item-1"), isRead = true)

        val repository = createRepository()

        repository.getFeeds().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("item-2", result.first().id)
            assertNotNull(result.first().dateString)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getFeeds hides date when setting is enabled`() = runTest(testDispatcher) {
        feedAppearanceSettingsRepository.setHideDate(true)

        val feedSource = createFeedSource(id = "source-1", title = "Widget Feed")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.insertFeedItems(
            listOf(
                buildFeedItem(
                    id = "item-1",
                    title = "Item",
                    pubDateMillis = 1000,
                    source = feedSource,
                ),
            ),
            lastSyncTimestamp = 0,
        )

        val repository = createRepository()

        repository.getFeeds().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertNull(result.first().dateString)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getFeedItemById returns url info when available`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Widget Feed")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.insertFeedItems(
            listOf(
                buildFeedItem(
                    id = "item-1",
                    title = "Item",
                    pubDateMillis = 1000,
                    source = feedSource,
                ),
            ),
            lastSyncTimestamp = 0,
        )

        val repository = createRepository()
        val info = repository.getFeedItemById(FeedItemId("item-1"))

        assertNotNull(info)
        assertEquals("item-1", info.id)
        assertEquals("https://example.com/item-1", info.url)
        assertEquals("Item", info.title)
        assertEquals(LinkOpeningPreference.DEFAULT, info.linkOpeningPreference)
    }

    private fun createFeedSource(
        id: String,
        title: String,
    ): FeedSource = FeedSource(
        id = id,
        url = "https://example.com/$id/rss.xml",
        title = title,
        category = null,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = "https://example.com",
        fetchFailed = false,
        linkOpeningPreference = LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
    )
}
