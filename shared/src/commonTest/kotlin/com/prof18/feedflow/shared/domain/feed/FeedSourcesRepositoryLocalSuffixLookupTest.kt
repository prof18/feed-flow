package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.domain.ParsedFeedContent
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.generators.RssItemGenerator
import com.prof18.feedflow.shared.test.koin.TestModules
import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FeedSourcesRepositoryLocalSuffixLookupTest : KoinTestBase() {

    private val feedSourcesRepository: FeedSourcesRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val fakeRssParserWrapper: FakeRssParserWrapper by inject()

    override fun getTestModules(): List<Module> = TestModules.createTestModules() + module {
        single { FakeRssParserWrapper() }
        single<RssParserWrapper> { get<FakeRssParserWrapper>() }
        single<HtmlParser> { EntityDecodingHtmlParser() }
    }

    @Test
    fun `addFeedSource discovers feed dot rss suffix`() = runTest(testDispatcher) {
        setupLocalAccount()
        fakeRssParserWrapper.reset(supportedUrl = "https://example.com/feed.rss")

        val result = feedSourcesRepository.addFeedSource(
            feedUrl = "https://example.com",
            categoryName = FeedSourceCategory(id = "local-tech", title = "Tech"),
            isNotificationEnabled = false,
        )
        advanceUntilIdle()

        assertIs<FeedAddedState.FeedAdded>(result)
        assertEquals("https://example.com/feed.rss", databaseHelper.getFeedSources().single().url)
        assertEquals("https://example.com/feed.rss", fakeRssParserWrapper.requestedUrls.last())
    }

    @Test
    fun `addFeedSource discovers index dot rss suffix`() = runTest(testDispatcher) {
        setupLocalAccount()
        fakeRssParserWrapper.reset(supportedUrl = "https://example.com/index.rss")

        val result = feedSourcesRepository.addFeedSource(
            feedUrl = "https://example.com",
            categoryName = null,
            isNotificationEnabled = false,
        )
        advanceUntilIdle()

        assertIs<FeedAddedState.FeedAdded>(result)
        assertEquals("https://example.com/index.rss", databaseHelper.getFeedSources().single().url)
        assertEquals("https://example.com/index.rss", fakeRssParserWrapper.requestedUrls.last())
    }

    @Test
    fun `addFeedSource decodes numeric entities in channel title`() = runTest(testDispatcher) {
        setupLocalAccount()
        fakeRssParserWrapper.reset(
            supportedUrl = "https://example.com/feed.rss",
            feedTitle = "BBC &#8238;فارسی",
        )

        val result = feedSourcesRepository.addFeedSource(
            feedUrl = "https://example.com/feed.rss",
            categoryName = null,
            isNotificationEnabled = false,
        )
        advanceUntilIdle()

        assertIs<FeedAddedState.FeedAdded>(result)
        assertEquals("BBC \u202Eفارسی", databaseHelper.getFeedSources().single().title)
    }

    @Test
    fun `only the original URL permits browser headers among guessed candidates`() = runTest(testDispatcher) {
        setupLocalAccount()
        fakeRssParserWrapper.reset(supportedUrl = "unused")

        val result = feedSourcesRepository.addFeedSource("https://example.com", null, false)
        advanceUntilIdle()

        assertIs<FeedAddedState.Error.InvalidUrl>(result)
        assertEquals(listOf("https://example.com" to true), fakeRssParserWrapper.requestedRequests.filter { it.second })
        assertEquals(
            fakeRssParserWrapper.requestedUrls.drop(1).map { it to false },
            fakeRssParserWrapper.requestedRequests.drop(1),
        )
    }

    @Test
    fun `original HTTP refusal survives suffix failures and permits force add`() = runTest(testDispatcher) {
        setupLocalAccount()
        fakeRssParserWrapper.reset(
            supportedUrl = "unused",
            httpErrors = mapOf("https://example.com" to 403, "https://example.com/feed" to 404),
        )

        val result = feedSourcesRepository.addFeedSource("https://example.com", null, false)
        advanceUntilIdle()

        val failure = assertIs<FeedAddedState.Error.FetchFailed>(result)
        assertEquals(403, failure.statusCode)
        assertEquals(true, failure.canForceAdd)
        assertEquals(emptyList(), databaseHelper.getFeedSources())
    }

    @Test
    fun `suffix HTTP failures do not turn an original parse failure into a refusal`() = runTest(testDispatcher) {
        setupLocalAccount()
        fakeRssParserWrapper.reset(
            supportedUrl = "unused",
            httpErrors = mapOf("https://example.com/feed" to 403),
        )

        val result = feedSourcesRepository.addFeedSource("https://example.com", null, false)
        advanceUntilIdle()

        assertIs<FeedAddedState.Error.InvalidUrl>(result)
    }

    @Test
    fun `a successful guessed feed wins over the original HTTP failure`() = runTest(testDispatcher) {
        setupLocalAccount()
        fakeRssParserWrapper.reset(
            supportedUrl = "https://example.com/feed.rss",
            httpErrors = mapOf("https://example.com" to 403),
        )

        val result = feedSourcesRepository.addFeedSource("https://example.com", null, false)
        advanceUntilIdle()

        assertIs<FeedAddedState.FeedAdded>(result)
        assertEquals("https://example.com/feed.rss", databaseHelper.getFeedSources().single().url)
    }

    @Test
    fun `edit reports the original HTTP failure and preserves the stored URL`() = runTest(testDispatcher) {
        addTestFeed("https://example.com/original", emptyList())
        val original = databaseHelper.getFeedSources().single()
        fakeRssParserWrapper.reset(
            supportedUrl = "unused",
            httpErrors = mapOf("https://example.com/new" to 503, "https://example.com/new/feed" to 403),
        )

        val result = feedSourcesRepository.editFeedSource(original.copy(url = "https://example.com/new"), original)
        advanceUntilIdle()

        val failure = assertIs<FeedEditedState.Error.FetchFailed>(result)
        assertEquals(503, failure.statusCode)
        assertEquals(original.url, databaseHelper.getFeedSources().single().url)
    }

    private fun setupLocalAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.LOCAL)
    }

    @Test
    fun `exact existing URL preserves title and preferences without fetching`() = runTest(testDispatcher) {
        addTestFeed("https://example.com/feed", emptyList())
        fakeRssParserWrapper.reset(supportedUrl = "unused")
        val result = feedSourcesRepository.addFeedSource("https://example.com/feed", null, true)

        assertEquals(FeedAddedState.FeedAlreadyExists("Example Feed"), result)
        assertEquals(emptyList(), fakeRssParserWrapper.requestedUrls)
        assertEquals(false, databaseHelper.getFeedSources().single().isNotificationEnabled)
    }

    @Test
    fun `discovered URL already subscribed returns existing source`() = runTest(testDispatcher) {
        addTestFeed("https://example.com/feed.rss", emptyList())
        val result = feedSourcesRepository.addFeedSource("https://example.com", null, false)

        assertEquals(FeedAddedState.FeedAlreadyExists("Example Feed"), result)
        assertEquals(1, databaseHelper.getFeedSources().size)
    }

    @Test
    fun `different URLs are added even when their articles overlap`() = runTest(testDispatcher) {
        val articles = articles(0 until 10)
        addTestFeed("http://www.androidworld.it/feed/", articles)
        val result = addTestFeed("https://www.smartworld.it/", articles)

        assertIs<FeedAddedState.FeedAdded>(result)
        assertEquals(2, databaseHelper.getFeedSources().size)
    }

    private fun articles(indices: IntRange): List<RssItem> = indices.map {
        RssItemGenerator.rssItem(link = "https://articles.example/$it", guid = "article-$it")
    }

    private suspend fun addTestFeed(url: String, items: List<RssItem>): FeedAddedState {
        setupLocalAccount()
        fakeRssParserWrapper.reset(supportedUrl = url, items = items)
        return feedSourcesRepository.addFeedSource(url, null, false)
    }

    private class FakeRssParserWrapper : RssParserWrapper {
        private var supportedUrl: String? = null
        private var feedTitle = "Example Feed"
        private var items: List<RssItem> = emptyList()
        private var httpErrors: Map<String, Int> = emptyMap()
        val requestedUrls = mutableListOf<String>()
        val requestedRequests = mutableListOf<Pair<String, Boolean>>()

        override suspend fun getRssChannel(url: String, allowBrowserTier: Boolean): RssChannel {
            requestedUrls.add(url)
            requestedRequests.add(url to allowBrowserTier)
            httpErrors[url]?.let { throw HttpException(code = it, message = "Refused") }
            check(url == supportedUrl) { "Unsupported url: $url" }
            return RssChannel(
                title = feedTitle,
                link = "https://example.com",
                description = null,
                image = null,
                lastBuildDate = null,
                updatePeriod = null,
                items = items,
                itunesChannelData = null,
                youtubeChannelData = null,
            )
        }

        fun reset(
            supportedUrl: String,
            feedTitle: String = "Example Feed",
            items: List<RssItem> = emptyList(),
            httpErrors: Map<String, Int> = emptyMap(),
        ) {
            this.supportedUrl = supportedUrl
            this.feedTitle = feedTitle
            this.items = items
            this.httpErrors = httpErrors
            requestedUrls.clear()
            requestedRequests.clear()
        }
    }

    private class EntityDecodingHtmlParser : HtmlParser {
        override fun getTextFromHTML(html: String): String = html.replace("&#8238;", "\u202E")
        override fun getFaviconUrl(html: String): String? = null
        override fun getRssUrl(html: String): String? = null
        override fun parseFeedContent(html: String, baseUrl: String?): ParsedFeedContent =
            ParsedFeedContent(text = html, commentsUrl = null)
    }
}
