package com.prof18.feedflow.shared.domain.contentprefetch

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.feedflow.shared.domain.feeditem.ReaderContentFetcher
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.testLogger
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentPrefetchRepositoryImplTest : KoinTestBase() {

    private val fakeParserWorker = FakeFeedItemParserWorker()

    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val feedItemContentFileHandler: FeedItemContentFileHandler by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() + module {
            single<FeedItemParserWorker> { fakeParserWorker }
        }

    private fun readerContentFetcher() = ReaderContentFetcher(
        feedItemParserWorker = fakeParserWorker,
        feedItemContentFileHandler = feedItemContentFileHandler,
    )

    private fun createRepository(): ContentPrefetchRepositoryImpl {
        val contentPrefetcher = ContentPrefetcher(
            logger = testLogger,
            databaseHelper = databaseHelper,
            readerContentFetcher = readerContentFetcher(),
        )
        return ContentPrefetchRepositoryImpl(
            logger = testLogger,
            settingsRepository = settingsRepository,
            databaseHelper = databaseHelper,
            contentPrefetcher = contentPrefetcher,
            backgroundPrefetchScheduler = CoroutineBackgroundPrefetchScheduler(
                logger = testLogger,
                contentPrefetcher = contentPrefetcher,
                dispatcherProvider = TestDispatcherProvider,
            ),
        )
    }

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
            content = "Content",
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
                content = "Content $index",
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
            content = null,
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

    @Test
    fun `cancelFetching cancels immediate parsing before clearing the queue`() =
        runTest(TestDispatcherProvider.testDispatcher) {
            settingsRepository.setPrefetchArticleContent(true)
            val parserStarted = CompletableDeferred<Unit>()
            fakeParserWorker.onParse = {
                parserStarted.complete(Unit)
                awaitCancellation()
            }
            insertFeedItem("item-1")

            val repository = createRepository()
            val prefetchJob = launch { repository.prefetchContent() }
            parserStarted.await()
            repository.cancelFetching()
            runCurrent()

            assertTrue(prefetchJob.isCompleted)
            assertEquals(1, databaseHelper.getUnfetchedItems().size)
            assertFalse(feedItemContentFileHandler.isContentAvailable("item-1"))
        }

    @Test
    fun `background fetching is delegated to the platform scheduler`() =
        runTest(TestDispatcherProvider.testDispatcher) {
            val scheduler = RecordingBackgroundPrefetchScheduler()
            val repository = ContentPrefetchRepositoryImpl(
                logger = testLogger,
                settingsRepository = settingsRepository,
                databaseHelper = databaseHelper,
                contentPrefetcher = ContentPrefetcher(
                    logger = testLogger,
                    databaseHelper = databaseHelper,
                    readerContentFetcher = readerContentFetcher(),
                ),
                backgroundPrefetchScheduler = scheduler,
            )

            settingsRepository.setPrefetchArticleContent(false)
            repository.startBackgroundFetching()
            assertEquals(0, scheduler.startCount)

            settingsRepository.setPrefetchArticleContent(true)
            repository.startBackgroundFetching()
            repository.pauseFetching()
            repository.cancelFetching()

            assertEquals(1, scheduler.startCount)
            assertEquals(1, scheduler.pauseCount)
            assertEquals(1, scheduler.cancelCount)
        }

    private suspend fun insertFeedItem(id: String) {
        val feedSource = createFeedSource("source-1")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.insertFeedItems(
            listOf(
                buildFeedItem(
                    id = id,
                    title = "Item 1",
                    pubDateMillis = 1000,
                    source = feedSource,
                ),
            ),
            lastSyncTimestamp = 0,
        )
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
        articleOpenMode = ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
    )

    private class RecordingBackgroundPrefetchScheduler : BackgroundPrefetchScheduler {
        var startCount = 0
        var pauseCount = 0
        var cancelCount = 0

        override fun start() {
            startCount++
        }

        override fun pause() {
            pauseCount++
        }

        override suspend fun cancel() {
            cancelCount++
        }
    }

    private class FakeFeedItemParserWorker : FeedItemParserWorker {
        private val contentByUrl = mutableMapOf<String, String?>()
        var onParse: suspend (String) -> Unit = {}

        fun setResult(feedItemId: String, content: String?) {
            contentByUrl["https://example.com/$feedItemId"] = content
        }

        override suspend fun parse(url: String): String? {
            val content = contentByUrl[url]
            onParse(url)
            return content
        }

        override suspend fun prepareFeedContent(html: String, baseUrl: String?): String = html
    }
}
