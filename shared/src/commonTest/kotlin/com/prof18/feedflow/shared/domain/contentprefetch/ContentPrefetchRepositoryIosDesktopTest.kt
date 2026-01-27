package com.prof18.feedflow.shared.domain.contentprefetch

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.testLogger
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentPrefetchRepositoryIosDesktopTest : KoinTestBase() {

    private val fakeParserWorker = FakeFeedItemParserWorker()

    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val feedItemContentFileHandler: FeedItemContentFileHandler by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() + module {
            single<FeedItemParserWorker> { fakeParserWorker }
        }

    private fun createRepository(): ContentPrefetchRepositoryIosDesktop =
        ContentPrefetchRepositoryIosDesktop(
            logger = testLogger,
            settingsRepository = settingsRepository,
            databaseHelper = databaseHelper,
            feedItemParserWorker = fakeParserWorker,
            feedItemContentFileHandler = feedItemContentFileHandler,
            dispatcherProvider = TestDispatcherProvider,
        )

    @Test
    fun `prefetchContent does nothing when prefetch is disabled`() = runTest(TestDispatcherProvider.testDispatcher) {
        settingsRepository.setPrefetchArticleContent(false)

        val feedSource = createFeedSource("source-1")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.insertFeedItems(
            listOf(
                buildFeedItem(
                    id = "item-1",
                    title = "Item 1",
                    pubDateMillis = 1000,
                    source = feedSource,
                ),
            ),
            lastSyncTimestamp = 0,
        )

        val repository = createRepository()
        repository.prefetchContent()
        advanceUntilIdle()

        assertEquals(1, databaseHelper.getUnfetchedItems().size)
        assertEquals(0, databaseHelper.getNextPrefetchBatch().size)
        assertFalse(feedItemContentFileHandler.isContentAvailable("item-1"))
    }

    @Test
    fun `prefetchContent fetches and saves immediate items`() = runTest(TestDispatcherProvider.testDispatcher) {
        settingsRepository.setPrefetchArticleContent(true)
        fakeParserWorker.setResult(
            feedItemId = "item-1",
            result = ParsingResult.Success(
                htmlContent = "Content",
                title = "Title",
                siteName = "Site",
            ),
        )

        val feedSource = createFeedSource("source-1")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.insertFeedItems(
            listOf(
                buildFeedItem(
                    id = "item-1",
                    title = "Item 1",
                    pubDateMillis = 1000,
                    source = feedSource,
                ),
            ),
            lastSyncTimestamp = 0,
        )

        val repository = createRepository()
        repository.prefetchContent()
        advanceUntilIdle()

        assertTrue(feedItemContentFileHandler.isContentAvailable("item-1"))
        assertEquals(0, databaseHelper.getUnfetchedItems().size)
        assertEquals(0, databaseHelper.getNextPrefetchBatch().size)
    }

    @Test
    fun `prefetchContent processes background queue items`() = runTest(TestDispatcherProvider.testDispatcher) {
        settingsRepository.setPrefetchArticleContent(true)

        val feedSource = createFeedSource("source-1")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))

        val items = (1..16).map { index ->
            val id = "item-$index"
            fakeParserWorker.setResult(
                feedItemId = id,
                result = ParsingResult.Success(
                    htmlContent = "Content $index",
                    title = "Title $index",
                    siteName = "Site",
                ),
            )
            buildFeedItem(
                id = id,
                title = "Item $index",
                pubDateMillis = index.toLong(),
                source = feedSource,
            )
        }
        databaseHelper.insertFeedItems(items, lastSyncTimestamp = 0)

        val repository = createRepository()
        repository.prefetchContent()
        advanceUntilIdle()

        assertEquals(0, databaseHelper.getUnfetchedItems().size)
        assertEquals(0, databaseHelper.getNextPrefetchBatch().size)
        assertTrue(feedItemContentFileHandler.isContentAvailable("item-16"))
    }

    @Test
    fun `prefetchContent marks items fetched when parsing fails`() = runTest(TestDispatcherProvider.testDispatcher) {
        settingsRepository.setPrefetchArticleContent(true)
        fakeParserWorker.setResult(
            feedItemId = "item-1",
            result = ParsingResult.Error,
        )

        val feedSource = createFeedSource("source-1")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.insertFeedItems(
            listOf(
                buildFeedItem(
                    id = "item-1",
                    title = "Item 1",
                    pubDateMillis = 1000,
                    source = feedSource,
                ),
            ),
            lastSyncTimestamp = 0,
        )

        val repository = createRepository()
        repository.prefetchContent()
        advanceUntilIdle()

        assertEquals(0, databaseHelper.getUnfetchedItems().size)
        assertEquals(0, databaseHelper.getNextPrefetchBatch().size)
        assertFalse(feedItemContentFileHandler.isContentAvailable("item-1"))
    }

    private fun createFeedSource(id: String): FeedSource = FeedSource(
        id = id,
        url = "https://example.com/$id/rss.xml",
        title = "Feed $id",
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

    private class FakeFeedItemParserWorker : FeedItemParserWorker {
        private val results = mutableMapOf<String, ParsingResult>()

        fun setResult(feedItemId: String, result: ParsingResult) {
            results[feedItemId] = result
        }

        override suspend fun parse(feedItemId: String, url: String): ParsingResult {
            return results[feedItemId] ?: ParsingResult.Error
        }
    }
}
