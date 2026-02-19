package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository.Companion.DEFAULT_READER_MODE_FONT_SIZE
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class ReaderModeViewModelTest : KoinTestBase() {

    private val viewModel: ReaderModeViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val feedItemContentFileHandler: FeedItemContentFileHandler by inject()
    private var parserBehavior: ParserBehavior = ParserBehavior.Success

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<FeedItemParserWorker> {
            object : FeedItemParserWorker {
                override suspend fun parse(feedItemId: String, url: String, imageUrl: String?): ParsingResult =
                    when (parserBehavior) {
                        ParserBehavior.Success -> ParsingResult.Success(
                            htmlContent = "Content",
                            title = "Title",
                            siteName = "Site Name",
                        )
                        ParserBehavior.HtmlNull -> ParsingResult.Success(
                            htmlContent = null,
                            title = "Title",
                            siteName = "Site Name",
                        )
                        ParserBehavior.Error -> ParsingResult.Error
                    }
            }
        }
    }

    @BeforeTest
    fun resetParserBehavior() {
        parserBehavior = ParserBehavior.Success
    }

    @Test
    fun `initial state is loading and font size is from settings`() = runTest {
        assertEquals(ReaderModeState.Loading, viewModel.readerModeState.value)
        assertEquals(DEFAULT_READER_MODE_FONT_SIZE, viewModel.readerFontSizeState.value)
    }

    @Test
    fun `getReaderModeHtml uses cached content when available`() = runTest {
        val urlInfo = FeedItemUrlInfo(
            id = "cached-1",
            url = "https://example.com/articles/1",
            title = "Cached Article",
            isBookmarked = false,
            linkOpeningPreference = LinkOpeningPreference.READER_MODE,
            commentsUrl = null,
        )
        feedItemContentFileHandler.saveFeedItemContentToFile(urlInfo.id, "Cached content")

        viewModel.readerModeState.test {
            assertEquals(ReaderModeState.Loading, awaitItem())

            viewModel.getReaderModeHtml(urlInfo)

            val successState = awaitItem() as ReaderModeState.Success
            assertEquals("Cached content", successState.readerModeData.content)
            assertEquals("Cached Article", successState.readerModeData.title)
            assertEquals("https://example.com", successState.readerModeData.baseUrl)
        }
    }

    @Test
    fun `getReaderModeHtml uses parser result when cache missing`() = runTest {
        val urlInfo = FeedItemUrlInfo(
            id = "parser-1",
            url = "https://example.com/articles/2",
            title = "Original title",
            isBookmarked = false,
            linkOpeningPreference = LinkOpeningPreference.READER_MODE,
            commentsUrl = null,
        )

        viewModel.readerModeState.test {
            assertEquals(ReaderModeState.Loading, awaitItem())

            viewModel.getReaderModeHtml(urlInfo)

            val successState = awaitItem() as ReaderModeState.Success
            assertEquals("Content", successState.readerModeData.content)
            assertEquals("Title", successState.readerModeData.title)
            assertEquals(urlInfo.id, successState.readerModeData.id.id)
        }
    }

    @Test
    fun `setLoading updates readerModeState to Loading`() = runTest {
        val urlInfo = FeedItemUrlInfo(
            id = "loading-1",
            url = "https://example.com/articles/3",
            title = "Loading Article",
            isBookmarked = false,
            linkOpeningPreference = LinkOpeningPreference.READER_MODE,
            commentsUrl = null,
        )

        viewModel.getReaderModeHtml(urlInfo)
        assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)

        viewModel.setLoading()
        assertEquals(ReaderModeState.Loading, viewModel.readerModeState.value)
    }

    @Test
    fun `getReaderModeHtml sets HtmlNotAvailable when parser returns null html`() = runTest {
        parserBehavior = ParserBehavior.HtmlNull
        val urlInfo = FeedItemUrlInfo(
            id = "null-html-1",
            url = "https://example.com/articles/null-html",
            title = "Null Html Article",
            isBookmarked = false,
            linkOpeningPreference = LinkOpeningPreference.READER_MODE,
            commentsUrl = null,
        )

        viewModel.getReaderModeHtml(urlInfo)

        val state = viewModel.readerModeState.value
        assertIs<ReaderModeState.HtmlNotAvailable>(state)
        assertEquals(urlInfo.url, state.url)
        assertEquals(urlInfo.id, state.id)
    }

    @Test
    fun `getReaderModeHtml sets HtmlNotAvailable when parser returns error`() = runTest {
        parserBehavior = ParserBehavior.Error
        val urlInfo = FeedItemUrlInfo(
            id = "error-1",
            url = "https://example.com/articles/error",
            title = "Error Article",
            isBookmarked = false,
            linkOpeningPreference = LinkOpeningPreference.READER_MODE,
            commentsUrl = null,
        )

        viewModel.getReaderModeHtml(urlInfo)

        val state = viewModel.readerModeState.value
        assertIs<ReaderModeState.HtmlNotAvailable>(state)
        assertEquals(urlInfo.url, state.url)
        assertEquals(urlInfo.id, state.id)
    }

    @Test
    fun `updateFontSize updates settings and state`() = runTest {
        viewModel.updateFontSize(22)

        assertEquals(22, viewModel.readerFontSizeState.value)
    }

    @Test
    fun `updateBookmarkStatus updates database`() = runTest {
        val feedItems = seedFeedItems()

        viewModel.updateBookmarkStatus(FeedItemId(feedItems.first().id), true)

        val feeds = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        val item = feeds.first { it.url_hash == feedItems.first().id }
        assertTrue(item.is_bookmarked)
    }

    @Test
    fun `navigation loads next article and updates navigation flags`() = runTest {
        val feedItems = seedFeedItems()

        val middleItem = feedItems[1]
        viewModel.getReaderModeHtml(middleItem.toUrlInfo())

        assertTrue(viewModel.canNavigateToPreviousState.value)
        assertTrue(viewModel.canNavigateToNextState.value)

        viewModel.navigateToNextArticle()

        val nextState = viewModel.readerModeState.value
        assertIs<ReaderModeState.Success>(nextState)
        assertEquals(feedItems[2].id, nextState.readerModeData.id.id)
        assertFalse(viewModel.canNavigateToNextState.value)
        assertTrue(viewModel.canNavigateToPreviousState.value)

        viewModel.navigateToPreviousArticle()

        val previousState = viewModel.readerModeState.value
        assertIs<ReaderModeState.Success>(previousState)
        assertEquals(middleItem.id, previousState.readerModeData.id.id)
        assertTrue(viewModel.canNavigateToPreviousState.value)
        assertTrue(viewModel.canNavigateToNextState.value)
    }

    private suspend fun seedFeedItems(): List<FeedItem> {
        val feedSource = FeedSource(
            id = "source-1",
            url = "https://example.com/feed.xml",
            title = "Example Feed",
            category = null,
            lastSyncTimestamp = null,
            logoUrl = null,
            websiteUrl = "https://example.com",
            fetchFailed = false,
            linkOpeningPreference = LinkOpeningPreference.READER_MODE,
            isHiddenFromTimeline = false,
            isPinned = false,
            isNotificationEnabled = false,
        )

        databaseHelper.insertFeedSource(
            listOf(
                ParsedFeedSource(
                    id = feedSource.id,
                    url = feedSource.url,
                    title = feedSource.title,
                    category = feedSource.category,
                    logoUrl = feedSource.logoUrl,
                    websiteUrl = feedSource.websiteUrl,
                ),
            ),
        )

        val feedItems = listOf(
            createFeedItem(
                id = "item-1",
                url = "https://example.com/articles/1",
                title = "Article 1",
                pubDateMillis = 3000,
                feedSource = feedSource,
            ),
            createFeedItem(
                id = "item-2",
                url = "https://example.com/articles/2",
                title = "Article 2",
                pubDateMillis = 2000,
                feedSource = feedSource,
            ),
            createFeedItem(
                id = "item-3",
                url = "https://example.com/articles/3",
                title = "Article 3",
                pubDateMillis = 1000,
                feedSource = feedSource,
            ),
        )

        databaseHelper.insertFeedItems(feedItems, lastSyncTimestamp = 0)
        feedStateRepository.getFeeds()

        return feedItems
    }

    private fun createFeedItem(
        id: String,
        url: String,
        title: String,
        pubDateMillis: Long,
        feedSource: FeedSource,
    ) = FeedItem(
        id = id,
        url = url,
        title = title,
        subtitle = null,
        content = null,
        imageUrl = null,
        feedSource = feedSource,
        pubDateMillis = pubDateMillis,
        isRead = false,
        dateString = null,
        commentsUrl = null,
        isBookmarked = false,
    )

    private fun FeedItem.toUrlInfo() = FeedItemUrlInfo(
        id = id,
        url = url,
        title = title,
        isBookmarked = isBookmarked,
        linkOpeningPreference = LinkOpeningPreference.READER_MODE,
        commentsUrl = commentsUrl,
    )

    private sealed interface ParserBehavior {
        data object Success : ParserBehavior
        data object HtmlNull : ParserBehavior
        data object Error : ParserBehavior
    }
}

class ReaderModeViewModelTimeoutTest : KoinTestBase() {

    private val viewModel: ReaderModeViewModel by inject()

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<FeedItemParserWorker> {
            object : FeedItemParserWorker {
                override suspend fun parse(feedItemId: String, url: String, imageUrl: String?): ParsingResult {
                    delay(2.minutes)
                    return ParsingResult.Success(
                        htmlContent = "Content",
                        title = "Title",
                        siteName = "Site Name",
                    )
                }
            }
        }
    }

    private val standardTestDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(standardTestDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getReaderModeHtml sets HtmlNotAvailable when parser returns null html`() = runTest {
        val urlInfo = FeedItemUrlInfo(
            id = "null-html-1",
            url = "https://example.com/articles/null-html",
            title = "Null Html Article",
            isBookmarked = false,
            linkOpeningPreference = LinkOpeningPreference.READER_MODE,
            commentsUrl = null,
        )

        viewModel.getReaderModeHtml(urlInfo)
        advanceTimeBy(1.minutes)

        val state = viewModel.readerModeState.value
        assertIs<ReaderModeState.HtmlNotAvailable>(state)
        assertEquals(urlInfo.url, state.url)
        assertEquals(urlInfo.id, state.id)
    }
}
