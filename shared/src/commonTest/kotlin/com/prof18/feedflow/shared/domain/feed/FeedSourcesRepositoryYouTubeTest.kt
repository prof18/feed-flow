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
import com.prof18.feedflow.shared.test.koin.TestModules
import com.prof18.rssparser.model.RssChannel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FeedSourcesRepositoryYouTubeTest : KoinTestBase() {
    private val repository: FeedSourcesRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val rss: RecordingRssParser by inject()
    private val html: RecordingHtml by inject()

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
        val requestedUrls = mutableListOf<String>()

        override suspend fun getRssChannel(url: String): RssChannel {
            requestedUrls += url
            if (fail || url != supportedUrl) error("Unsupported RSS URL: $url")
            return RssChannelGenerator.rssChannel(link = null)
        }

        fun reset() {
            supportedUrl = null
            fail = false
            requestedUrls.clear()
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
    }
}
