package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedSourceCacheInfo
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpValidators
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.toParsedFeedSource
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun `force refresh bypasses scheduled cache while keeping validators`() = runTest {
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
    fun `automatic refresh skips feed inside scheduled cache window`() = runTest {
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
    fun `force refresh still respects Retry-After backoff`() = runTest {
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
    fun `cancellation from parser propagates out of serial feed loop`() = runTest {
        val source = FeedSourceGenerator.feedSource(id = "cancelled")
        databaseHelper.insertFeedSource(listOf(source.toParsedFeedSource()))
        fakeRssParser.error = CancellationException("test cancellation")

        val thrown = assertFailsWith<CancellationException> {
            repository().fetchFeeds()
        }

        assertEquals("test cancellation", thrown.message)
    }

    private class FakeRssParser : RssParserWrapper {
        val requestedUrls = mutableListOf<String>()
        val validatorsSeenByUrl = mutableMapOf<String, FeedHttpValidators?>()
        var error: Throwable? = null

        override suspend fun getRssChannel(url: String, allowBrowserTier: Boolean): RssChannel {
            requestedUrls += url
            validatorsSeenByUrl[url] = feedHttpCacheStoreForTest?.validatorsFor(url)
            error?.let { throw it }
            return RssChannelGenerator.rssChannel(title = "Test Feed", items = emptyList())
        }

        var feedHttpCacheStoreForTest: FeedHttpCacheStore? = null
    }
}
