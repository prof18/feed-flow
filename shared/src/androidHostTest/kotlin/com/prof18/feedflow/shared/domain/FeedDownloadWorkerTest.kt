package com.prof18.feedflow.shared.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.testing.TestListenableWorkerBuilder
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCacheInfo
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feed.RssParserWrapper
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
    private var widgetAction: suspend () -> Unit = {}

    private val feedFetcherRepository: FeedFetcherRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

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
    }

    @Test
    fun `manual refresh runs in foreground with forced fetch and without notifications`() = runTest(testDispatcher) {
        val source = addFreshSource()
        parser.channelFor(source.url)
        foregroundState.onAppForegrounded()

        val result = buildWorker(manual = true).doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(listOf(source.url), parser.requestedUrls)
        assertTrue(notificationCalls.isEmpty())
        assertEquals(listOf(true, false), widgetRefreshes)
        assertFalse(refreshState.isRefreshing.value)
    }

    @Test
    fun `automatic refresh skips while app is foregrounded`() = runTest(testDispatcher) {
        foregroundState.onAppForegrounded()

        val result = buildWorker(manual = false).doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        assertTrue(parser.requestedUrls.isEmpty())
        assertTrue(notificationCalls.isEmpty())
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
}
