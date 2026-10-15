package com.prof18.feedflow.shared.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.testing.TestListenableWorkerBuilder
import com.prof18.feedflow.core.model.ArticleNotificationBatch
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCacheInfo
import com.prof18.feedflow.core.model.NotificationMode
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feed.RssParserWrapper
import com.prof18.feedflow.shared.domain.notification.ArticleNotifier
import com.prof18.feedflow.shared.domain.notification.Notifier
import com.prof18.feedflow.shared.presentation.WidgetRefreshState
import com.prof18.feedflow.shared.presentation.WidgetUpdater
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.koin.TestModules
import com.prof18.feedflow.shared.test.toParsedFeedSource
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FeedDownloadWorkerTest : KoinTestBase() {
    private val parser = FakeRssParserWrapper()
    private val foregroundState = AppForegroundState()
    private val refreshState = WidgetRefreshState()
    private val widgetRefreshes = mutableListOf<Boolean>()
    private val notificationCalls = mutableListOf<Unit>()
    private var notifierAction: () -> Boolean = { false }
    private val articleNotifier = RecordingArticleNotifier()
    private var widgetAction: suspend () -> Unit = {}

    private val feedFetcherRepository: FeedFetcherRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()

    override fun getTestModules(): List<Module> = TestModules.createTestModules() + module {
        single<RssParserWrapper> { parser }
        single { foregroundState }
        single<WidgetRefreshState> { refreshState }
        single<WidgetUpdater> {
            WidgetUpdater {
                widgetRefreshes += refreshState.isRefreshing.value
                widgetAction()
            }
        }
        single<Notifier> {
            Notifier {
                notificationCalls += Unit
                notifierAction()
            }
        }
        single<ArticleNotifier> { articleNotifier }
    }

    @Test
    fun `manual refresh runs in foreground with forced fetch and without notifications`() = runTest(testDispatcher) {
        settingsRepository.setNotificationMode(NotificationMode.ARTICLE)
        val source = addFreshSource()
        parser.channelFor(source.url)
        foregroundState.onAppForegrounded()

        val result = buildWorker(manual = true).doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(listOf(source.url), parser.requestedUrls)
        assertTrue(notificationCalls.isEmpty())
        assertTrue(articleNotifier.batches.isEmpty())
        assertEquals(listOf(true, false), widgetRefreshes)
        assertFalse(refreshState.isRefreshing.value)
    }

    @Test
    fun `automatic refresh skips while app is foregrounded`() = runTest(testDispatcher) {
        settingsRepository.setNotificationMode(NotificationMode.ARTICLE)
        foregroundState.onAppForegrounded()

        val result = buildWorker(manual = false).doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        assertTrue(parser.requestedUrls.isEmpty())
        assertTrue(notificationCalls.isEmpty())
        assertTrue(articleNotifier.batches.isEmpty())
        assertTrue(widgetRefreshes.isEmpty())
    }

    @Test
    fun `manual refresh failure clears spinner before repainting widget`() = runTest(testDispatcher) {
        val source = addFreshSource()
        parser.channelFor(source.url)
        widgetAction = {
            if (refreshState.isRefreshing.value) error("Widget update failure")
        }

        val result = buildWorker(manual = true).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        assertEquals(listOf(true, false), widgetRefreshes)
        assertFalse(refreshState.isRefreshing.value)
    }

    @Test
    fun `manual refresh cancellation clears spinner before repainting widget`() = runTest(testDispatcher) {
        val enteredWidgetUpdate = CompletableDeferred<Unit>()
        widgetAction = {
            if (!enteredWidgetUpdate.isCompleted) {
                enteredWidgetUpdate.complete(Unit)
                awaitCancellation()
            }
        }
        val worker = buildWorker(manual = true)
        val job = launch { worker.doWork() }

        enteredWidgetUpdate.await()
        assertTrue(refreshState.isRefreshing.value)
        job.cancelAndJoin()

        assertEquals(listOf(true, false), widgetRefreshes)
        assertFalse(refreshState.isRefreshing.value)
    }

    @BeforeTest
    fun resetWorkerState() {
        foregroundState.onAppBackgrounded()
        parser.reset()
        widgetRefreshes.clear()
        notificationCalls.clear()
        notifierAction = { false }
        articleNotifier.reset()
        widgetAction = {}
    }

    private suspend fun addFreshSource(): FeedSource {
        val source = FeedSource(
            id = "worker-source",
            url = "https://example.com/worker/rss.xml",
            title = "Worker source",
            category = null,
            lastSyncTimestamp = Clock.System.now().toEpochMilliseconds(),
            logoUrl = "https://example.com/logo.png",
            websiteUrl = "https://example.com",
            fetchFailed = false,
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHiddenFromTimeline = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )
        databaseHelper.insertFeedSource(listOf(source.toParsedFeedSource()))
        enableNotifications(source)
        databaseHelper.updateFeedSourcesCacheInfo(
            listOf(
                FeedSourceCacheInfo(
                    feedSourceId = source.id,
                    etag = null,
                    lastModified = null,
                    validatorsTimestamp = null,
                    nextFetchTimestamp = Clock.System.now().toEpochMilliseconds() + 30.minutes.inWholeMilliseconds,
                    backoffTimestamp = null,
                ),
            ),
        )
        val networkSettings: NetworkSettings = getKoin().get()
        networkSettings.setSyncAccountType(SyncAccounts.LOCAL)
        return source
    }

    @Test
    fun `article mode passes prepared snapshot and only exact acknowledgement clears pending rows`() =
        runTest(testDispatcher) {
            settingsRepository.setNotificationMode(NotificationMode.ARTICLE)
            val source = addFreshSource()
            insertNotificationItems(source, listOf("article-one", "article-two", "article-three"))
            articleNotifier.action = { batch, acknowledge ->
                val selectedId = batch.articles.first().id
                insertNotificationItems(source, listOf("arrived-after-snapshot"))
                acknowledge(listOf(selectedId))
            }

            val result = buildWorker(manual = false).doWork()

            assertTrue(result is ListenableWorker.Result.Success)
            assertEquals(1, articleNotifier.batches.size)
            assertEquals(3, articleNotifier.batches.single().articles.size)
            assertTrue(notificationCalls.isEmpty())
            assertEquals(
                setOf("article-one", "article-two", "article-three", "arrived-after-snapshot") -
                    articleNotifier.batches.single().articles.first().id,
                databaseHelper.getArticlesToNotify().map { it.id }.toSet(),
            )
        }

    @Test
    fun `article mode with sender that does not acknowledge leaves whole batch pending`() =
        runTest(testDispatcher) {
            settingsRepository.setNotificationMode(NotificationMode.ARTICLE)
            val source = addFreshSource()
            insertNotificationItems(source, listOf("unacknowledged-one", "unacknowledged-two"))

            val result = buildWorker(manual = false).doWork()

            assertTrue(result is ListenableWorker.Result.Success)
            assertEquals(1, articleNotifier.batches.size)
            assertEquals(
                setOf("unacknowledged-one", "unacknowledged-two"),
                databaseHelper.getArticlesToNotify().map { it.id }.toSet(),
            )
            assertTrue(notificationCalls.isEmpty())
        }

    @Test
    fun `legacy modes keep broad acknowledgement on success and pending rows on failure`() = runTest(testDispatcher) {
        for (mode in listOf(NotificationMode.FEED_SOURCE, NotificationMode.CATEGORY, NotificationMode.GROUPED)) {
            for (sent in listOf(true, false)) {
                databaseHelper.deleteAll()
                settingsRepository.setNotificationMode(mode)
                val source = addFreshSource()
                insertNotificationItems(source, listOf("legacy-$mode-$sent-one", "legacy-$mode-$sent-two"))
                notifierAction = { sent }

                val result = buildWorker(manual = false).doWork()

                assertTrue(result is ListenableWorker.Result.Success)
                assertEquals(1, notificationCalls.size, "$mode sent=$sent")
                assertTrue(articleNotifier.batches.isEmpty(), "$mode sent=$sent")
                assertEquals(
                    if (sent) emptySet() else setOf("legacy-$mode-$sent-one", "legacy-$mode-$sent-two"),
                    databaseHelper.getArticlesToNotify().map { it.id }.toSet(),
                    "$mode sent=$sent",
                )
                notificationCalls.clear()
                articleNotifier.reset()
            }
        }
    }

    private suspend fun enableNotifications(source: FeedSource) {
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = source.id,
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHidden = false,
            isPinned = false,
            isNotificationEnabled = true,
            isHideImagesEnabled = false,
        )
    }

    private suspend fun insertNotificationItems(source: FeedSource, ids: List<String>) {
        val now = Clock.System.now().toEpochMilliseconds()
        databaseHelper.insertFeedItems(
            ids.mapIndexed { index, id ->
                FeedItem(
                    id = id,
                    url = "https://example.com/$id",
                    title = id,
                    subtitle = null,
                    content = null,
                    imageUrl = null,
                    feedSource = source,
                    pubDateMillis = now - index,
                    isRead = false,
                    dateString = null,
                    commentsUrl = null,
                    isBookmarked = false,
                )
            },
            lastSyncTimestamp = now,
        )
    }

    private fun buildWorker(manual: Boolean): FeedDownloadWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inputData = Data.Builder()
            .putBoolean(FeedDownloadWorker.IS_MANUAL_REFRESH_KEY, manual)
            .build()
        return TestListenableWorkerBuilder<FeedDownloadWorker>(context)
            .setInputData(inputData)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: androidx.work.WorkerParameters,
                    ): ListenableWorker? = if (workerClassName == FeedDownloadWorker::class.java.name) {
                        FeedDownloadWorker(
                            feedFetcherRepository = feedFetcherRepository,
                            widgetUpdater = getKoin().get(),
                            databaseHelper = databaseHelper,
                            notifier = getKoin().get(),
                            articleNotifier = getKoin().get(),
                            articleNotificationRepository = getKoin().get(),
                            settingsRepository = settingsRepository,
                            appForegroundState = foregroundState,
                            widgetRefreshState = refreshState,
                            appContext = appContext,
                            workerParams = workerParameters,
                        )
                    } else {
                        null
                    }
                },
            )
            .build()
    }

    private class FakeRssParserWrapper : RssParserWrapper {
        private val channels = mutableMapOf<String, RssChannel>()
        val requestedUrls = mutableListOf<String>()

        fun reset() {
            channels.clear()
            requestedUrls.clear()
        }

        fun channelFor(url: String) {
            channels[url] = RssChannelGenerator.rssChannel(items = emptyList())
        }

        override suspend fun getRssChannel(url: String, allowBrowserTier: Boolean): RssChannel {
            requestedUrls += url
            return requireNotNull(channels[url]) { "Unexpected feed request: $url" }
        }
    }

    private class RecordingArticleNotifier : ArticleNotifier {
        val batches = mutableListOf<ArticleNotificationBatch>()
        var action: suspend (ArticleNotificationBatch, suspend (List<String>) -> Unit) -> Unit = { _, _ -> }

        fun reset() {
            batches.clear()
            action = { _, _ -> }
        }

        override suspend fun showArticleNotifications(
            prepareBatch: suspend () -> ArticleNotificationBatch,
            acknowledge: suspend (List<String>) -> Unit,
        ) {
            val batch = prepareBatch()
            batches += batch
            action(batch, acknowledge)
        }
    }
}
