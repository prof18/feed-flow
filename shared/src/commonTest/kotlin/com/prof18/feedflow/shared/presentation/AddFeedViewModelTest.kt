package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feed.RssParserWrapper
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.toParsedFeedSource
import com.prof18.rssparser.model.RssChannel
import io.kotest.property.arbitrary.next
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.get
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AddFeedViewModelTest : KoinTestBase() {

    private val databaseHelper: DatabaseHelper by inject()
    private val rssUrl = "https://example.com/rss.xml"
    private val mockHtml = """
        <html>
            <head>
                <link rel="alternate" type="application/rss+xml" href="$rssUrl" />
            </head>
        </html>
    """.trimIndent()

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<RssParserWrapper> {
            FakeRssParserWrapper(
                rssUrl = rssUrl,
                channel = RssChannelGenerator.rssChannelArb.next().copy(
                    title = "Example Feed",
                    link = "https://example.com",
                ),
            )
        }
        single<HtmlParser> { FakeHtmlParser(rssUrl) }
        single<HtmlRetriever> {
            HtmlRetriever(
                logger = get(),
                client = HttpClient(MockEngine) {
                    engine {
                        addHandler {
                            respond(
                                content = mockHtml,
                                status = HttpStatusCode.OK,
                                headers = headersOf(
                                    HttpHeaders.ContentType,
                                    "text/html",
                                ),
                            )
                        }
                    }
                },
            )
        }
    }

    @Test
    fun `updateFeedUrlTextFieldValue emits feed not added`() = runTest(testDispatcher) {
        val viewModel = getViewModel()

        viewModel.feedAddedState.test {
            viewModel.updateFeedUrlTextFieldValue("https://example.com/feed.xml")
            assertEquals(FeedAddedState.FeedNotAdded, awaitItem())
        }
    }

    @Test
    fun `updateNotificationStatus updates state`() = runTest(testDispatcher) {
        val viewModel = getViewModel()

        viewModel.updateNotificationStatus(true)

        assertTrue(viewModel.isNotificationEnabledState.value)
    }

    @Test
    fun `addFeed emits loading when url is empty`() = runTest(testDispatcher) {
        val viewModel = getViewModel()

        viewModel.feedAddedState.test {
            viewModel.addFeed()
            assertEquals(FeedAddedState.Loading, awaitItem())
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun `addFeed emits feed added and resets notification status`() = runTest(testDispatcher) {
        val viewModel = getViewModel()

        viewModel.feedAddedState.test {
            viewModel.updateFeedUrlTextFieldValue("https://example.com/blog")
            assertEquals(FeedAddedState.FeedNotAdded, awaitItem())

            viewModel.updateNotificationStatus(true)
            viewModel.addFeed()

            assertEquals(FeedAddedState.Loading, awaitItem())
            assertIs<FeedAddedState.FeedAdded>(awaitItem())
        }

        advanceUntilIdle()

        assertEquals(false, viewModel.isNotificationEnabledState.value)

        val feedSources = databaseHelper.getFeedSources()
        assertEquals(1, feedSources.size)
        assertEquals(rssUrl, feedSources.first().url)
    }

    @Test
    fun `showNotificationToggleState reflects enabled notifications`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Feed One")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        databaseHelper.updateNotificationEnabledStatus(feedSource.id, true)

        val viewModel = getViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.showNotificationToggleState.value)
    }

    private fun getViewModel(): AddFeedViewModel = get()

    private fun createFeedSource(
        id: String,
        title: String,
    ): FeedSource = FeedSourceGenerator.feedSourceArb.next().copy(
        id = id,
        url = "https://example.com/$id/feed.xml",
        title = title,
        category = null,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = null,
        fetchFailed = false,
    )

    private class FakeRssParserWrapper(
        private val rssUrl: String,
        private val channel: RssChannel,
    ) : RssParserWrapper {
        override suspend fun getRssChannel(url: String): RssChannel {
            check(url == rssUrl) { "Unsupported url: $url" }
            return channel
        }
    }

    private class FakeHtmlParser(
        private val rssUrl: String,
    ) : HtmlParser {
        override fun getTextFromHTML(html: String): String? = html
        override fun getFaviconUrl(html: String): String? = null
        override fun getRssUrl(html: String): String? = rssUrl
    }
}
