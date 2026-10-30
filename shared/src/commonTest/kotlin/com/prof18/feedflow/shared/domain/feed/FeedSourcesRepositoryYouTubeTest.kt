package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.domain.ParsedFeedContent
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.generators.RssItemGenerator
import com.prof18.feedflow.shared.test.koin.TestModules
import com.prof18.rssparser.model.RssChannel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class FeedSourcesRepositoryYouTubeTest : KoinTestBase() {
    private val repository: FeedSourcesRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val rss: RecordingRssParser by inject()
    private val html: RecordingHtml by inject()

    @Test
    fun `cancelled derived request never adds a fallback subscription`() = runTest(testDispatcher) {
        setupLocal()
        rss.supportedUrl = FEED_URL
        rss.cancelledUrl = FILTERED_URL
        assertFailsWith<CancellationException> {
            repository.addFeedSource(CHANNEL_URL, null, false, hideYouTubeShorts = true)
        }
        assertEquals(emptyList(), databaseHelper.getFeedSources())
        assertEquals(listOf(FEED_URL, FILTERED_URL), rss.requestedUrls)
    }

    @Test
    fun `playlist page is saved as canonical RSS without HTML discovery`() = runTest(testDispatcher) {
        setupLocal()
        val feed = "https://www.youtube.com/feeds/videos.xml?playlist_id=PL_test"
        rss.supportedUrl = feed
        assertIs<FeedAddedState.FeedAdded>(repository.addFeedSource("youtube.com/watch?v=x&list=PL_test", null, false))
        assertEquals(feed, databaseHelper.getFeedSources().single().url)
        assertEquals(listOf(feed), rss.requestedUrls)
        assertEquals(emptyList(), html.requestedUrls)
    }

    @Test
    fun `filtered channel keeps channel name and can coexist with full feed`() = runTest(testDispatcher) {
        setupLocal()
        rss.channels[FEED_URL] = RssChannelGenerator.rssChannel(title = "Channel name")
        rss.channels[FILTERED_URL] = RssChannelGenerator.rssChannel(
            title = "Videos",
            items = listOf(RssItemGenerator.rssItem()),
        )
        repository.addFeedSource(FEED_URL, null, false)
        rss.requestedUrls.clear()

        val result = repository.addFeedSource(FEED_URL, null, true, hideYouTubeShorts = true)

        assertEquals(FeedAddedState.FeedAdded("Channel name"), result)
        val source = databaseHelper.getFeedSources().single { it.url == FILTERED_URL }
        assertEquals("Channel name", source.title)
        assertEquals(true, source.isNotificationEnabled)
        assertEquals(2, databaseHelper.getFeedSources().size)
        assertEquals(listOf(FEED_URL, FILTERED_URL), rss.requestedUrls)
        assertEquals(
            FeedAddedState.FeedAlreadyExists("Channel name"),
            repository.addFeedSource(CHANNEL_URL, null, false, hideYouTubeShorts = true),
        )
        assertEquals(true, databaseHelper.getFeedSources().single { it.url == FILTERED_URL }.isNotificationEnabled)
    }

    @Test
    fun `unavailable derived feed falls back after one request`() = runTest(testDispatcher) {
        setupLocal()
        rss.supportedUrl = FEED_URL

        val result = repository.addFeedSource(CHANNEL_URL, null, false, hideYouTubeShorts = true)

        assertIs<FeedAddedState.FeedAdded>(result)
        assertEquals(true, result.shortsFilterFallback)
        assertEquals(FEED_URL, databaseHelper.getFeedSources().single().url)
        assertEquals(listOf(FEED_URL, FILTERED_URL), rss.requestedUrls)
    }

    @Test
    fun `empty derived feed falls back without misinterpreting its title`() = runTest(testDispatcher) {
        setupLocal()
        rss.channels[FEED_URL] = RssChannelGenerator.rssChannel(title = "Channel name")
        rss.channels[FILTERED_URL] = RssChannelGenerator.rssChannel(title = "Videos").copy(items = emptyList())
        val result = repository.addFeedSource(CHANNEL_URL, null, false, hideYouTubeShorts = true)
        assertEquals(FeedAddedState.FeedAdded("Channel name", shortsFilterFallback = true), result)
        assertEquals(FEED_URL, databaseHelper.getFeedSources().single().url)
    }

    @Test
    fun `failing canonical playlist feed never probes suffixes or HTML`() = runTest(testDispatcher) {
        setupLocal()
        val result = repository.addFeedSource("https://youtube.com/playlist?list=PL_test", null, false)
        assertIs<FeedAddedState.Error.InvalidUrl>(result)
        assertEquals(listOf("https://www.youtube.com/feeds/videos.xml?playlist_id=PL_test"), rss.requestedUrls)
        assertEquals(emptyList(), html.requestedUrls)
    }

    override fun getTestModules(): List<Module> = TestModules.createTestModules() + module {
        single { RecordingRssParser() }
        single<RssParserWrapper> { get<RecordingRssParser>() }
        single { RecordingHtml() }
        single<HtmlParser> { get<RecordingHtml>().parser }
        single<HtmlRetriever> { get<RecordingHtml>().retriever }
    }

    @Test
    fun `channel URL is saved as canonical YouTube feed`() = runTest(testDispatcher) {
        setupLocal()
        rss.supportedUrl = FEED_URL

        val result = repository.addFeedSource(CHANNEL_URL, null, false)

        assertIs<FeedAddedState.FeedAdded>(result)
        assertEquals(FEED_URL, databaseHelper.getFeedSources().single().url)
        assertEquals(listOf(FEED_URL), rss.requestedUrls)
        assertEquals(emptyList(), html.requestedUrls)
    }

    @Test
    fun `alternate channel URL is detected as duplicate of saved canonical feed`() = runTest(testDispatcher) {
        setupLocal()
        rss.supportedUrl = FEED_URL
        assertIs<FeedAddedState.FeedAdded>(repository.addFeedSource(CHANNEL_URL, null, false))
        rss.requestedUrls.clear()

        val result = repository.addFeedSource("https://youtube.com/channel/$CHANNEL_ID", null, true)

        assertEquals(FeedAddedState.FeedAlreadyExists("RSS channel title"), result)
        assertEquals(listOf(FEED_URL), rss.requestedUrls)
        assertEquals(1, databaseHelper.getFeedSources().size)
        assertEquals(false, databaseHelper.getFeedSources().single().isNotificationEnabled)
    }

    @Test
    fun `channel handle canonical fallback resolves and saves RSS feed`() = runTest(testDispatcher) {
        setupLocal()
        html.canonicalUrl = CHANNEL_URL
        rss.supportedUrl = FEED_URL

        val result = repository.addFeedSource("https://www.youtube.com/@channel", null, false)

        assertIs<FeedAddedState.FeedAdded>(result)
        assertEquals(FEED_URL, databaseHelper.getFeedSources().single().url)
        assertEquals(listOf("https://www.youtube.com/@channel"), html.requestedUrls)
        assertEquals(listOf(FEED_URL), rss.requestedUrls)
    }

    @Test
    fun `unavailable channel does not guess feed suffixes`() = runTest(testDispatcher) {
        setupLocal()
        html.responseStatus = HttpStatusCode.NotFound

        val result = repository.addFeedSource("https://www.youtube.com/@missing", null, false)

        assertIs<FeedAddedState.Error.InvalidUrl>(result)
        assertEquals(listOf("https://www.youtube.com/@missing"), html.requestedUrls)
        assertEquals(emptyList(), rss.requestedUrls)
    }

    @Test
    fun `bad resolved RSS returns invalid URL after one canonical request`() = runTest(testDispatcher) {
        setupLocal()
        html.rssUrl = FEED_URL
        rss.fail = true

        val result = repository.addFeedSource("https://www.youtube.com/@channel", null, false)

        assertIs<FeedAddedState.Error.InvalidUrl>(result)
        assertEquals(listOf("https://www.youtube.com/@channel"), html.requestedUrls)
        assertEquals(listOf(FEED_URL), rss.requestedUrls)
    }

    private fun setupLocal() {
        getKoin().get<com.prof18.feedflow.feedsync.networkcore.NetworkSettings>()
            .setSyncAccountType(SyncAccounts.LOCAL)
        rss.reset()
        html.reset()
    }

    private class RecordingRssParser : RssParserWrapper {
        var supportedUrl: String? = null
        var fail = false
        var cancelledUrl: String? = null
        val requestedUrls = mutableListOf<String>()
        val channels = mutableMapOf<String, RssChannel>()

        override suspend fun getRssChannel(url: String, allowBrowserTier: Boolean): RssChannel {
            requestedUrls += url
            if (url == cancelledUrl) throw CancellationException("Cancelled request")
            channels[url]?.let { return it }
            if (fail || url != supportedUrl) error("Unsupported RSS URL: $url")
            return RssChannelGenerator.rssChannel(link = null)
        }

        fun reset() {
            supportedUrl = null
            fail = false
            cancelledUrl = null
            requestedUrls.clear()
            channels.clear()
        }
    }

    private class RecordingHtml {
        val requestedUrls = mutableListOf<String>()
        var responseStatus = HttpStatusCode.OK
        var rssUrl: String? = null
        var canonicalUrl: String? = null
        val parser = object : HtmlParser {
            override fun getTextFromHTML(html: String): String? = html
            override fun getFaviconUrl(html: String): String? = null
            override fun getRssUrl(html: String): String? = rssUrl
            override fun getCanonicalUrl(html: String): String? = canonicalUrl
            override fun parseFeedContent(html: String, baseUrl: String?): ParsedFeedContent =
                ParsedFeedContent(null, null)
        }
        val retriever = HtmlRetriever(
            logger = Logger.withTag("YouTubeTest"),
            client = HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        requestedUrls += request.url.toString()
                        respond(
                            content = "<html></html>",
                            status = responseStatus,
                            headers = headersOf("Content-Type", ContentType.Text.Html.toString()),
                        )
                    }
                }
            },
            forbiddenFallbackClient = HttpClient(MockEngine) {
                engine { addHandler { respond(ByteArray(0), HttpStatusCode.Forbidden) } }
            },
        )

        fun reset() {
            requestedUrls.clear()
            responseStatus = HttpStatusCode.OK
            rssUrl = null
            canonicalUrl = null
        }
    }

    private companion object {
        const val CHANNEL_ID = "UCsBjURrPoezykLs9EqgamOA"
        const val CHANNEL_URL = "https://www.youtube.com/channel/$CHANNEL_ID"
        const val FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=$CHANNEL_ID"
        const val FILTERED_URL = "https://www.youtube.com/feeds/videos.xml?playlist_id=UULFsBjURrPoezykLs9EqgamOA"
    }
}
