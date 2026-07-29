package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.domain.ParsedFeedContent
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.koin.TestModules
import com.prof18.rssparser.model.RssChannel
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

    private fun setupLocalAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.LOCAL)
    }

    private class FakeRssParserWrapper : RssParserWrapper {
        private var supportedUrl: String? = null
        private var feedTitle = "Example Feed"
        val requestedUrls = mutableListOf<String>()

        override suspend fun getRssChannel(url: String): RssChannel {
            requestedUrls.add(url)
            check(url == supportedUrl) { "Unsupported url: $url" }
            return RssChannel(
                title = feedTitle,
                link = "https://example.com",
                description = null,
                image = null,
                lastBuildDate = null,
                updatePeriod = null,
                items = emptyList(),
                itunesChannelData = null,
                youtubeChannelData = null,
            )
        }

        fun reset(supportedUrl: String, feedTitle: String = "Example Feed") {
            this.supportedUrl = supportedUrl
            this.feedTitle = feedTitle
            requestedUrls.clear()
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
