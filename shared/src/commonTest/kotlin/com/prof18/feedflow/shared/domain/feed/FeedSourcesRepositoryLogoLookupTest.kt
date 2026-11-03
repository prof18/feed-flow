package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.domain.ParsedFeedContent
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.koin.TestModules
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FeedSourcesRepositoryLogoLookupTest : KoinTestBase() {

    private val feedSourcesRepository: FeedSourcesRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val fakeRssParserWrapper = FakeLogoLookupRssParser()
    private val fakeHtmlParser = MutableFaviconHtmlParser()

    override fun getTestModules(): List<Module> = TestModules.createTestModules() + module {
        single<RssParserWrapper> { fakeRssParserWrapper }
        single<HtmlParser> { fakeHtmlParser }
    }

    @Test
    fun `a second feed from a domain gets the favicon the site serves now`() = runTest(testDispatcher) {
        setupLocalAccount()
        fakeHtmlParser.faviconUrl = "/build/aaaa/favicon.png"
        addFeed(FIRST_FEED)

        // The site redeploys between the two adds.
        fakeHtmlParser.faviconUrl = "/build/bbbb/favicon.png"
        addFeed(SECOND_FEED)

        val logos = databaseHelper.getFeedSources().associate { it.url to it.logoUrl }
        assertEquals(expected = "https://example.com/build/aaaa/favicon.png", actual = logos[FIRST_FEED])
        assertEquals(expected = "https://example.com/build/bbbb/favicon.png", actual = logos[SECOND_FEED])
    }

    private suspend fun TestScope.addFeed(url: String) {
        fakeRssParserWrapper.supportedUrl = url
        val result = feedSourcesRepository.addFeedSource(
            feedUrl = url,
            categoryName = null,
            isNotificationEnabled = false,
        )
        advanceUntilIdle()
        assertIs<FeedAddedState.FeedAdded>(result)
    }

    // Not a @BeforeTest: the native runner does not order it after the base class's own.
    private fun setupLocalAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.LOCAL)
    }

    private companion object {
        const val FIRST_FEED = "https://example.com/first/feed.xml"
        const val SECOND_FEED = "https://example.com/second/feed.xml"
    }
}

private class FakeLogoLookupRssParser : RssParserWrapper {
    var supportedUrl: String? = null

    override suspend fun getRssChannel(url: String, allowBrowserTier: Boolean): RssChannel {
        check(url == supportedUrl) { "Unsupported url: $url" }
        return RssChannel(
            title = "Example Feed",
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
}

private class MutableFaviconHtmlParser : HtmlParser {
    var faviconUrl: String? = null

    override fun getTextFromHTML(html: String): String? = null

    override fun getFaviconUrl(html: String): String? = faviconUrl

    override fun getRssUrl(html: String): String? = null

    override fun parseFeedContent(html: String, baseUrl: String?): ParsedFeedContent =
        ParsedFeedContent(text = null, commentsUrl = null)
}
