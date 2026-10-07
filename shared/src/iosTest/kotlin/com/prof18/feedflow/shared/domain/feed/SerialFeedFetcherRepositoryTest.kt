package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedSourceCacheInfo
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpValidators
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.toParsedFeedSource
import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class SerialFeedFetcherRepositoryTest : KoinTestBase() {

    private val fakeRssParser = FakeRssParser()
    private val databaseHelper: DatabaseHelper by inject()
    private val dateFormatter: DateFormatter by inject()
    private val feedHttpCacheStore: FeedHttpCacheStore by inject()

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<RssParserWrapper> { fakeRssParser }
        single<FeedHttpCacheStore> {
            FeedHttpCacheStore(
                currentTimeMillis = { dateFormatter.currentTimeMillis() },
                logger = Logger.withTag("SerialFeedFetcherRepositoryTest"),
            ).also { fakeRssParser.feedHttpCacheStoreForTest = it }
        }
    }

    private fun repository(): SerialFeedFetcherRepository = SerialFeedFetcherRepository(
        dispatcherProvider = getKoin().get(),
        feedStateRepository = getKoin().get(),
        gReaderRepository = getKoin().get(),
        feedbinRepository = getKoin().get(),
        databaseHelper = databaseHelper,
        feedSyncRepository = getKoin().get(),
        logger = Logger.withTag("SerialFeedFetcherRepositoryTest"),
        rssParserWrapper = fakeRssParser,
        rssChannelMapper = getKoin().get(),
        dateFormatter = dateFormatter,
        feedHttpCacheStore = feedHttpCacheStore,
    )

    @Test
    fun `force refresh bypasses scheduled cache while keeping validators`() = runTest(testDispatcher) {
        val now = dateFormatter.currentTimeMillis()
        val source = FeedSourceGenerator.feedSource(id = "forced", lastSyncTimestamp = now)
        databaseHelper.insertFeedSource(listOf(source.toParsedFeedSource()))
        databaseHelper.updateFeedSourcesCacheInfo(
            listOf(
                FeedSourceCacheInfo(
                    feedSourceId = source.id,
                    etag = "\"feed-v1\"",
                    lastModified = "Wed, 21 Oct 2015 07:28:00 GMT",
                    validatorsTimestamp = now,
                    nextFetchTimestamp = now + 30.minutes.inWholeMilliseconds,
                    backoffTimestamp = null,
                ),
            ),
        )

        repository().fetchFeeds(forceRefresh = true)

        assertEquals(listOf(source.url), fakeRssParser.requestedUrls)
        assertEquals(
            FeedHttpValidators(etag = "\"feed-v1\"", lastModified = "Wed, 21 Oct 2015 07:28:00 GMT"),
            fakeRssParser.validatorsSeenByUrl[source.url],
        )
    }

    @Test
    fun `automatic refresh skips feed inside scheduled cache window`() = runTest(testDispatcher) {
        val now = dateFormatter.currentTimeMillis()
        val source = FeedSourceGenerator.feedSource(id = "scheduled", lastSyncTimestamp = now)
        databaseHelper.insertFeedSource(listOf(source.toParsedFeedSource()))
        databaseHelper.updateFeedSourcesCacheInfo(
            listOf(
                FeedSourceCacheInfo(
                    feedSourceId = source.id,
                    etag = null,
                    lastModified = null,
                    validatorsTimestamp = null,
                    nextFetchTimestamp = now + 30.minutes.inWholeMilliseconds,
                    backoffTimestamp = null,
                ),
            ),
        )

        repository().fetchFeeds()

        assertEquals(emptyList(), fakeRssParser.requestedUrls)
    }

    @Test
    fun `force refresh still respects Retry-After backoff`() = runTest(testDispatcher) {
        val now = dateFormatter.currentTimeMillis()
        val source = FeedSourceGenerator.feedSource(id = "backoff", lastSyncTimestamp = now)
        databaseHelper.insertFeedSource(listOf(source.toParsedFeedSource()))
        databaseHelper.updateFeedSourcesCacheInfo(
            listOf(
                FeedSourceCacheInfo(
                    feedSourceId = source.id,
                    etag = null,
                    lastModified = null,
                    validatorsTimestamp = null,
                    nextFetchTimestamp = now,
                    backoffTimestamp = now + 30.minutes.inWholeMilliseconds,
                ),
            ),
        )

        repository().fetchFeeds(forceRefresh = true)

        assertEquals(emptyList(), fakeRssParser.requestedUrls)
    }

    @Test
    fun `cancellation from parser propagates out of serial feed loop`() = runTest(testDispatcher) {
        val source = FeedSourceGenerator.feedSource(id = "cancelled")
        databaseHelper.insertFeedSource(listOf(source.toParsedFeedSource()))
        fakeRssParser.error = CancellationException("test cancellation")

        val thrown = assertFailsWith<CancellationException> {
            repository().fetchFeeds()
        }

        assertEquals("test cancellation", thrown.message)
    }

    @Test
    fun `cancelled fetch stops the active parser before requesting the next source`() =
        runTest(testDispatcher) {
            val sources = listOf(
                FeedSourceGenerator.feedSource(id = "first", url = "https://example.com/first.xml"),
                FeedSourceGenerator.feedSource(id = "second", url = "https://example.com/second.xml"),
            )
            databaseHelper.insertFeedSource(sources.map { it.toParsedFeedSource() })
            val parserEntered = CompletableDeferred<Unit>()
            var parserObservedCancellation = false
            fakeRssParser.onRequest = {
                parserEntered.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    parserObservedCancellation = true
                }
            }

            val fetchJob = launch { repository().fetchFeeds(forceRefresh = true) }
            parserEntered.await()
            fetchJob.cancelAndJoin()

            assertTrue(parserObservedCancellation)
            assertEquals(1, fakeRssParser.requestedUrls.size)
        }

    @Test
    fun `retry after backoff is saved when a later source is cancelled`() = runTest(testDispatcher) {
        val first = FeedSourceGenerator.feedSource(
            id = "throttled",
            title = "A Throttled",
            url = "https://example.com/a.xml",
        )
        val second = FeedSourceGenerator.feedSource(
            id = "blocked",
            title = "B Blocked",
            url = "https://example.com/b.xml",
        )
        databaseHelper.insertFeedSource(listOf(first, second).map { it.toParsedFeedSource() })
        val secondSourceEntered = CompletableDeferred<Unit>()
        fakeRssParser.onRequest = { url ->
            if (url == first.url) {
                feedHttpCacheStore.recordResponse(
                    url = url,
                    statusCode = 429,
                    etag = null,
                    lastModified = null,
                    cacheControl = null,
                    expires = null,
                    date = null,
                    retryAfter = "3600",
                )
                throw HttpException(code = 429, message = "Too Many Requests")
            }
            secondSourceEntered.complete(Unit)
            awaitCancellation()
        }

        val fetchJob = launch { repository().fetchFeeds(forceRefresh = true) }
        secondSourceEntered.await()
        fetchJob.cancelAndJoin()

        val firstCacheInfo = databaseHelper.getFeedSourcesCacheInfo().singleOrNull {
            it.feedSourceId == first.id
        }
        val backoffTimestamp = assertNotNull(firstCacheInfo?.backoffTimestamp)
        assertTrue(backoffTimestamp > dateFormatter.currentTimeMillis())

        fakeRssParser.onRequest = null
        fakeRssParser.requestedUrls.clear()
        repository().fetchFeeds(forceRefresh = true)

        assertEquals(listOf(second.url), fakeRssParser.requestedUrls)
    }

    private class FakeRssParser : RssParserWrapper {
        val requestedUrls = mutableListOf<String>()
        val validatorsSeenByUrl = mutableMapOf<String, FeedHttpValidators?>()
        var error: Throwable? = null
        var onRequest: (suspend (String) -> Unit)? = null

        override suspend fun getRssChannel(url: String, allowBrowserTier: Boolean): RssChannel {
            requestedUrls += url
            validatorsSeenByUrl[url] = feedHttpCacheStoreForTest?.validatorsFor(url)
            error?.let { throw it }
            onRequest?.invoke(url)
            return RssChannelGenerator.rssChannel(title = "Test Feed", items = emptyList())
        }

        var feedHttpCacheStoreForTest: FeedHttpCacheStore? = null
    }
}
