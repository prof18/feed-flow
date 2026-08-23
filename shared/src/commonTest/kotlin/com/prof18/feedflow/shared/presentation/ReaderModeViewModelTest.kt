package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.model.ReaderModeDefaults
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.core.model.ShownContentSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.data.SettingsRepository.Companion.DEFAULT_READER_MODE_FONT_SIZE
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class ReaderModeViewModelTest : KoinTestBase() {

    private val viewModel: ReaderModeViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val feedItemContentFileHandler: FeedItemContentFileHandler by inject()
    private val settingsRepository: SettingsRepository by inject()
    private var parserBehavior: ParserBehavior = ParserBehavior.Success

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<FeedItemParserWorker> {
            object : FeedItemParserWorker {
                override suspend fun parse(feedItemId: String, url: String, imageUrl: String?): ParsingResult {
                    val currentParserBehavior = parserBehavior
                    return when (currentParserBehavior) {
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
                        ParserBehavior.HtmlBlank -> ParsingResult.Success(
                            htmlContent = "   ",
                            title = "Title",
                            siteName = "Site Name",
                        )
                        ParserBehavior.Error -> ParsingResult.Error
                        is ParserBehavior.DelayedSuccessById -> {
                            val delayMillis = currentParserBehavior.delaysByArticleId[feedItemId] ?: 0
                            delay(delayMillis)
                            ParsingResult.Success(
                                htmlContent = "Content-$feedItemId",
                                title = "Title-$feedItemId",
                                siteName = "Site Name",
                            )
                        }
                    }
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
        assertEquals(DEFAULT_READER_MODE_FONT_SIZE, viewModel.readerFontSettingsState.value.fontSize)
        assertNull(viewModel.currentArticleState.value)
    }

    @Test
    fun `getReaderModeHtml updates selected article`() = runTest {
        val urlInfo = FeedItemUrlInfo(
            id = "open-1",
            url = "https://example.com/articles/open-1",
            title = "Open Article",
            isBookmarked = false,
            articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
            commentsUrl = null,
        )

        viewModel.getReaderModeHtml(urlInfo)

        assertEquals(urlInfo.id, viewModel.currentArticleState.value?.id)
    }

    @Test
    fun `clearSelection clears selected article only`() = runTest {
        val urlInfo = FeedItemUrlInfo(
            id = "clear-1",
            url = "https://example.com/articles/clear-1",
            title = "Clear Article",
            isBookmarked = false,
            articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
            commentsUrl = null,
        )

        viewModel.getReaderModeHtml(urlInfo)
        assertEquals(urlInfo.id, viewModel.currentArticleState.value?.id)

        viewModel.clearSelection()

        assertNull(viewModel.currentArticleState.value)
    }

    @Test
    fun `resetState clears selected article and navigation flags`() = runTest {
        val feedItems = seedFeedItems()
        viewModel.getReaderModeHtml(feedItems[1].toUrlInfo())

        assertTrue(viewModel.canNavigateToPreviousState.value)
        assertTrue(viewModel.canNavigateToNextState.value)
        assertEquals(feedItems[1].id, viewModel.currentArticleState.value?.id)

        viewModel.resetState()

        assertEquals(ReaderModeState.Loading, viewModel.readerModeState.value)
        assertNull(viewModel.currentArticleState.value)
        assertFalse(viewModel.canNavigateToPreviousState.value)
        assertFalse(viewModel.canNavigateToNextState.value)
    }

    @Test
    fun `getReaderModeHtml uses cached content when available`() = runTest {
        val urlInfo = FeedItemUrlInfo(
            id = "cached-1",
            url = "https://example.com/articles/1",
            title = "Cached Article",
            isBookmarked = false,
            articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
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
            articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
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
            articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
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
            articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
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
            articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
            commentsUrl = null,
        )

        viewModel.getReaderModeHtml(urlInfo)

        val state = viewModel.readerModeState.value
        assertIs<ReaderModeState.HtmlNotAvailable>(state)
        assertEquals(urlInfo.url, state.url)
        assertEquals(urlInfo.id, state.id)
    }

    @Test
    fun `getReaderModeHtml keeps latest requested article when previous request finishes later`() = runTest {
        parserBehavior = ParserBehavior.DelayedSuccessById(
            delaysByArticleId = mapOf(
                "slow-article" to 300,
                "fast-article" to 10,
            ),
        )

        val slowArticle = FeedItemUrlInfo(
            id = "slow-article",
            url = "https://example.com/articles/slow",
            title = "Slow Article",
            isBookmarked = false,
            articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
            commentsUrl = null,
        )
        val fastArticle = FeedItemUrlInfo(
            id = "fast-article",
            url = "https://example.com/articles/fast",
            title = "Fast Article",
            isBookmarked = false,
            articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
            commentsUrl = null,
        )

        viewModel.getReaderModeHtml(slowArticle)
        viewModel.getReaderModeHtml(fastArticle)
        advanceUntilIdle()

        val state = viewModel.readerModeState.value
        assertIs<ReaderModeState.Success>(state)
        assertEquals("fast-article", state.readerModeData.id.id)
    }

    @Test
    fun `updateFontSize updates settings and state`() = runTest {
        viewModel.updateFontSize(22)

        assertEquals(22, viewModel.readerFontSettingsState.value.fontSize)
    }

    @Test
    fun `initial line height is default value`() = runTest {
        assertEquals(
            ReaderModeDefaults.LINE_HEIGHT,
            viewModel.readerFontSettingsState.value.lineHeight,
        )
    }

    @Test
    fun `updateLineHeight updates settings and state`() = runTest {
        viewModel.readerFontSettingsState.test {
            assertEquals(ReaderModeDefaults.LINE_HEIGHT, awaitItem().lineHeight)

            viewModel.updateLineHeight(4)

            assertEquals(4, awaitItem().lineHeight)
        }
    }

    @Test
    fun `reset restores defaults`() = runTest {
        viewModel.updateFontSize(30)
        viewModel.updateLineHeight(6)

        viewModel.updateFontSize(ReaderModeDefaults.FONT_SIZE)
        viewModel.updateLineHeight(ReaderModeDefaults.LINE_HEIGHT)

        assertEquals(ReaderModeDefaults.FONT_SIZE, viewModel.readerFontSettingsState.value.fontSize)
        assertEquals(
            ReaderModeDefaults.LINE_HEIGHT,
            viewModel.readerFontSettingsState.value.lineHeight,
        )
    }

    @Test
    fun `updateBookmarkStatus updates database`() = runTest {
        val feedItems = seedFeedItems()

        viewModel.updateBookmarkStatus(FeedItemId(feedItems.first().id), true)

        val feeds = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
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

    @Test
    fun `navigation shows fallback for next article when URL is not eligible for reader mode`() = runTest {
        val feedItems = seedFeedItems(item3Url = "https://example.com/audio/episode.mp3")
        val middleItem = feedItems[1]
        val nextItem = feedItems[2]

        viewModel.getReaderModeHtml(middleItem.toUrlInfo())
        assertTrue(viewModel.canNavigateToNextState.value)

        viewModel.navigateToNextArticle()
        advanceUntilIdle()

        val state = viewModel.readerModeState.value
        assertIs<ReaderModeState.HtmlNotAvailable>(state)
        assertEquals(nextItem.id, state.id)
        assertEquals(nextItem.url, state.url)
        assertEquals(nextItem.id, viewModel.currentArticleState.value?.id)
        assertFalse(viewModel.canNavigateToNextState.value)
        assertTrue(viewModel.canNavigateToPreviousState.value)
    }

    @Test
    fun `navigation shows fallback for previous article when URL is not eligible for reader mode`() = runTest {
        val feedItems = seedFeedItems(item1Url = "https://www.youtube.com/watch?v=abc")
        val previousItem = feedItems[0]
        val middleItem = feedItems[1]

        viewModel.getReaderModeHtml(middleItem.toUrlInfo())
        assertTrue(viewModel.canNavigateToPreviousState.value)

        viewModel.navigateToPreviousArticle()
        advanceUntilIdle()

        val state = viewModel.readerModeState.value
        assertIs<ReaderModeState.HtmlNotAvailable>(state)
        assertEquals(previousItem.id, state.id)
        assertEquals(previousItem.url, state.url)
        assertEquals(previousItem.id, viewModel.currentArticleState.value?.id)
        assertFalse(viewModel.canNavigateToPreviousState.value)
        assertTrue(viewModel.canNavigateToNextState.value)
    }

    @Test
    fun `navigation fails gracefully when feed list changes externally`() = runTest {
        val feedItems = seedFeedItems()
        val middleItem = feedItems[1]

        viewModel.getReaderModeHtml(middleItem.toUrlInfo())
        assertTrue(viewModel.canNavigateToNextState.value)

        feedStateRepository.updateFeedFilter(FeedFilter.Bookmarks)
        assertTrue(feedStateRepository.feedState.value.isEmpty())

        viewModel.navigateToNextArticle()
        advanceUntilIdle()

        assertFalse(viewModel.canNavigateToNextState.value)
        // Reader state is unchanged — still showing the article that was open
        assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
    }

    @Test
    fun `feed content preference shows stored feed content`() = runTest {
        val item = seedItemWithContent("feed-happy", "https://example.com/a/feed-happy", SUBSTANTIAL_CONTENT)

        viewModel.readerModeState.test {
            assertEquals(ReaderModeState.Loading, awaitItem())

            viewModel.getReaderModeHtml(item.toUrlInfo(ArticleOpenMode.FEED_CONTENT))

            val state = awaitItem() as ReaderModeState.Success
            assertEquals(SUBSTANTIAL_CONTENT, state.readerModeData.content)
            assertEquals("https://example.com/a/feed-happy", state.readerModeData.baseUrl)
            assertEquals(ShownContentSource.FEED, state.readerModeData.shownContentSource)
            assertEquals("Content Feed feed-happy", state.readerModeData.siteName)
            assertTrue(state.readerModeData.canToggleContentSource)
        }
    }

    @Test
    fun `feed content preference accepts short feed content`() = runTest {
        val content = "<p>short</p>"
        val item = seedItemWithContent("feed-short", "https://example.com/a/feed-short", content)

        viewModel.readerModeState.test {
            assertEquals(ReaderModeState.Loading, awaitItem())

            viewModel.getReaderModeHtml(item.toUrlInfo(ArticleOpenMode.FEED_CONTENT))

            val state = awaitItem() as ReaderModeState.Success
            assertEquals(content, state.readerModeData.content)
            assertEquals(ShownContentSource.FEED, state.readerModeData.shownContentSource)
            assertTrue(state.readerModeData.canToggleContentSource)
        }
    }

    @Test
    fun `feed content preference accepts image-only feed content`() = runTest {
        val content = """<img src="https://imgs.xkcd.com/comics/example.png" alt="Comic">"""
        val item = seedItemWithContent("feed-image", "https://xkcd.com/1/", content)

        viewModel.readerModeState.test {
            assertEquals(ReaderModeState.Loading, awaitItem())

            viewModel.getReaderModeHtml(item.toUrlInfo(ArticleOpenMode.FEED_CONTENT))

            val state = awaitItem() as ReaderModeState.Success
            assertEquals(content, state.readerModeData.content)
            assertEquals(ShownContentSource.FEED, state.readerModeData.shownContentSource)
            assertTrue(state.readerModeData.canToggleContentSource)
        }
    }

    @Test
    fun `web content cannot toggle source when feed content is missing`() = runTest {
        val item = seedItemWithContent("web-no-feed", "https://example.com/a/web-no-feed", content = null)

        viewModel.readerModeState.test {
            assertEquals(ReaderModeState.Loading, awaitItem())

            viewModel.getReaderModeHtml(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))

            val state = awaitItem() as ReaderModeState.Success
            assertEquals(ShownContentSource.WEB, state.readerModeData.shownContentSource)
            assertFalse(state.readerModeData.canToggleContentSource)
        }
    }

    @Test
    fun `web preference falls back to feed content when parsing fails`() = runTest {
        parserBehavior = ParserBehavior.Error
        val item = seedItemWithContent("web-fallback", "https://example.com/a/web-fallback", SUBSTANTIAL_CONTENT)

        viewModel.getReaderModeHtml(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(SUBSTANTIAL_CONTENT, state.readerModeData.content)
        assertEquals(ShownContentSource.FEED, state.readerModeData.shownContentSource)
        assertTrue(state.readerModeData.canToggleContentSource)
    }

    @Test
    fun `default source uses global feed preference`() = runTest {
        settingsRepository.setArticleOpenMode(ArticleOpenMode.FEED_CONTENT)
        val item = seedItemWithContent("global-feed", "https://example.com/a/global-feed", SUBSTANTIAL_CONTENT)

        viewModel.getReaderModeHtml(item.toUrlInfo(ArticleOpenMode.DEFAULT))
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(ShownContentSource.FEED, state.readerModeData.shownContentSource)
    }

    @Test
    fun `reopening the same article reloads after global source changes`() = runTest {
        val item = seedItemWithContent("source-change", "https://example.com/a/source-change", SUBSTANTIAL_CONTENT)
        val urlInfo = item.toUrlInfo(ArticleOpenMode.DEFAULT)

        settingsRepository.setArticleOpenMode(ArticleOpenMode.FULL_ARTICLE)
        viewModel.getReaderModeHtml(urlInfo)
        advanceUntilIdle()
        assertEquals(
            ShownContentSource.WEB,
            assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData.shownContentSource,
        )

        settingsRepository.setArticleOpenMode(ArticleOpenMode.FEED_CONTENT)
        viewModel.getReaderModeHtml(urlInfo)
        advanceUntilIdle()
        assertEquals(
            ShownContentSource.FEED,
            assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData.shownContentSource,
        )
    }

    @Test
    fun `blank cached web content falls back to feed content`() = runTest {
        parserBehavior = ParserBehavior.Error
        val item = seedItemWithContent("blank-cache", "https://example.com/a/blank-cache", SUBSTANTIAL_CONTENT)
        feedItemContentFileHandler.saveFeedItemContentToFile(item.id, "   ")

        viewModel.getReaderModeHtml(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(ShownContentSource.FEED, state.readerModeData.shownContentSource)
    }

    @Test
    fun `blank parsed web content falls back to feed content`() = runTest {
        parserBehavior = ParserBehavior.HtmlBlank
        val item = seedItemWithContent("blank-parser", "https://example.com/a/blank-parser", SUBSTANTIAL_CONTENT)

        viewModel.getReaderModeHtml(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(ShownContentSource.FEED, state.readerModeData.shownContentSource)
    }

    @Test
    fun `web preference falls back to feed content when parsing times out`() = runTest {
        val item = seedItemWithContent("web-timeout", "https://example.com/a/web-timeout", SUBSTANTIAL_CONTENT)
        parserBehavior = ParserBehavior.DelayedSuccessById(
            delaysByArticleId = mapOf(item.id to 21_000),
        )

        viewModel.getReaderModeHtml(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(SUBSTANTIAL_CONTENT, state.readerModeData.content)
        assertEquals(ShownContentSource.FEED, state.readerModeData.shownContentSource)
        assertTrue(state.readerModeData.canToggleContentSource)
    }

    @Test
    fun `sets HtmlNotAvailable when neither web nor feed content is available`() = runTest {
        parserBehavior = ParserBehavior.Error
        val item = seedItemWithContent("nothing", "https://example.com/a/nothing", content = null)

        viewModel.getReaderModeHtml(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
        advanceUntilIdle()

        val state = viewModel.readerModeState.value
        assertIs<ReaderModeState.HtmlNotAvailable>(state)
        assertEquals(item.id, state.id)
    }

    @Test
    fun `toggleContentSource switches from web to feed and back`() = runTest {
        val item = seedItemWithContent("toggle", "https://example.com/a/toggle", SUBSTANTIAL_CONTENT)

        viewModel.readerModeState.test {
            assertEquals(ReaderModeState.Loading, awaitItem())

            viewModel.getReaderModeHtml(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
            val initialState = awaitItem() as ReaderModeState.Success
            assertEquals(ShownContentSource.WEB, initialState.readerModeData.shownContentSource)
            assertTrue(initialState.readerModeData.canToggleContentSource)

            viewModel.toggleContentSource()
            assertEquals(ReaderModeState.Loading, awaitItem())
            val feedState = awaitItem() as ReaderModeState.Success
            assertEquals(ShownContentSource.FEED, feedState.readerModeData.shownContentSource)
            assertEquals(SUBSTANTIAL_CONTENT, feedState.readerModeData.content)
            assertTrue(feedState.readerModeData.canToggleContentSource)

            viewModel.getReaderModeHtml(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
            assertEquals(
                ShownContentSource.FEED,
                assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData.shownContentSource,
            )

            viewModel.toggleContentSource()
            assertEquals(ReaderModeState.Loading, awaitItem())
            val webState = awaitItem() as ReaderModeState.Success
            assertEquals(ShownContentSource.WEB, webState.readerModeData.shownContentSource)
        }
    }

    @Test
    fun `toggleContentSource keeps the web article when feed content is missing`() = runTest {
        val item = seedItemWithContent("toggle-missing", "https://example.com/a/toggle-missing", content = null)

        viewModel.getReaderModeHtml(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
        advanceUntilIdle()
        val initialState = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(ShownContentSource.WEB, initialState.readerModeData.shownContentSource)
        // Without feed content there is nothing to switch to, so the toggle is not offered
        assertFalse(initialState.readerModeData.canToggleContentSource)

        viewModel.toggleContentSource()
        advanceUntilIdle()

        // State is unchanged — still showing the web article
        val restoredState = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(ShownContentSource.WEB, restoredState.readerModeData.shownContentSource)
    }

    @Test
    fun `toggleContentSource falls back to the website when full article parsing fails`() = runTest {
        val url = "https://example.com/a/toggle-web-failure"
        val item = seedItemWithContent("toggle-web-failure", url, content = SUBSTANTIAL_CONTENT)

        viewModel.getReaderModeHtml(item.toUrlInfo(ArticleOpenMode.FEED_CONTENT))
        advanceUntilIdle()
        val feedState = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(ShownContentSource.FEED, feedState.readerModeData.shownContentSource)
        assertTrue(feedState.readerModeData.canToggleContentSource)

        parserBehavior = ParserBehavior.Error
        viewModel.toggleContentSource()
        advanceUntilIdle()

        val fallbackState = assertIs<ReaderModeState.HtmlNotAvailable>(viewModel.readerModeState.value)
        assertEquals(url, fallbackState.url)
    }

    @Test
    fun `blank url item is shown from feed content`() = runTest {
        val item = seedItemWithContent("blank-url", url = "", content = SUBSTANTIAL_CONTENT)

        viewModel.readerModeState.test {
            assertEquals(ReaderModeState.Loading, awaitItem())

            viewModel.getReaderModeHtml(item.toUrlInfo())

            val state = awaitItem() as ReaderModeState.Success
            assertEquals(SUBSTANTIAL_CONTENT, state.readerModeData.content)
            assertEquals(ShownContentSource.FEED, state.readerModeData.shownContentSource)
            assertFalse(state.readerModeData.canToggleContentSource)
        }
    }

    @Test
    fun `blank url item uses feed source as relative content base`() = runTest {
        val item = seedItemWithContent("blank-url-base", url = "", content = "<p>Short post</p>")
        val urlInfo = item.toUrlInfo().copy(feedSourceBaseUrl = "https://example.com/source/")

        viewModel.getReaderModeHtml(urlInfo)
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals("https://example.com/source/", state.readerModeData.baseUrl)
    }

    @Test
    fun `blank url item falls back to the stored feed base url`() = runTest {
        val item = seedItemWithContent("blank-url-stored-base", url = "", content = "<p>Short post</p>")
        // The Compose feed list and the desktop reader route build FeedItemUrlInfo without it.
        val urlInfo = item.toUrlInfo().copy(feedSourceBaseUrl = null)

        viewModel.getReaderModeHtml(urlInfo)
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals("https://example.com", state.readerModeData.baseUrl)
    }

    @Test
    fun `feed content for ineligible url cannot toggle to web parsing`() = runTest {
        val item = seedItemWithContent("pdf", "https://example.com/file.pdf", SUBSTANTIAL_CONTENT)

        viewModel.getReaderModeHtml(item.toUrlInfo(ArticleOpenMode.FEED_CONTENT))
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(ShownContentSource.FEED, state.readerModeData.shownContentSource)
        assertFalse(state.readerModeData.canToggleContentSource)
    }

    @Test
    fun `blank url item accepts short feed content`() = runTest {
        val item = seedItemWithContent("blank-url-short", url = "", content = "<p>Short post</p>")

        viewModel.getReaderModeHtml(item.toUrlInfo())
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(ShownContentSource.FEED, state.readerModeData.shownContentSource)
    }

    @Test
    fun `navigation loads URL-less next and previous articles from feed content`() = runTest {
        val feedItems = seedFeedItems(
            item1Url = "",
            item1Content = "<p>Previous short post</p>",
            item3Url = "",
            item3Content = "<p>Next short post</p>",
        )

        viewModel.getReaderModeHtml(feedItems[1].toUrlInfo())
        viewModel.navigateToNextArticle()
        advanceUntilIdle()
        assertEquals(
            feedItems[2].id,
            assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData.id.id,
        )

        viewModel.navigateToPreviousArticle()
        advanceUntilIdle()
        viewModel.navigateToPreviousArticle()
        advanceUntilIdle()
        assertEquals(
            feedItems[0].id,
            assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData.id.id,
        )
    }

    @Test
    fun `blank url item with no content is HtmlNotAvailable`() = runTest {
        val item = seedItemWithContent("blank-url-empty", url = "", content = null)

        viewModel.getReaderModeHtml(item.toUrlInfo())
        advanceUntilIdle()

        assertIs<ReaderModeState.HtmlNotAvailable>(viewModel.readerModeState.value)
    }

    private suspend fun seedFeedItems(
        item1Url: String = "https://example.com/articles/1",
        item2Url: String = "https://example.com/articles/2",
        item3Url: String = "https://example.com/articles/3",
        item1Content: String? = null,
        item3Content: String? = null,
    ): List<FeedItem> {
        val feedSource = FeedSource(
            id = "source-1",
            url = "https://example.com/feed.xml",
            title = "Example Feed",
            category = null,
            lastSyncTimestamp = null,
            logoUrl = null,
            websiteUrl = "https://example.com",
            fetchFailed = false,
            articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
            isHiddenFromTimeline = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
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
                url = item1Url,
                title = "Article 1",
                pubDateMillis = 3000,
                feedSource = feedSource,
            ).copy(content = item1Content),
            createFeedItem(
                id = "item-2",
                url = item2Url,
                title = "Article 2",
                pubDateMillis = 2000,
                feedSource = feedSource,
            ),
            createFeedItem(
                id = "item-3",
                url = item3Url,
                title = "Article 3",
                pubDateMillis = 1000,
                feedSource = feedSource,
            ).copy(content = item3Content),
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

    private fun FeedItem.toUrlInfo(
        articleOpenMode: ArticleOpenMode = ArticleOpenMode.DEFAULT,
    ) = FeedItemUrlInfo(
        id = id,
        url = url,
        title = title,
        isBookmarked = isBookmarked,
        articleOpenMode = articleOpenMode,
        commentsUrl = commentsUrl,
        feedSourceTitle = feedSource.title,
        feedSourceBaseUrl = feedSource.websiteUrlFallback(),
    )

    private suspend fun seedItemWithContent(
        id: String,
        url: String,
        content: String?,
    ): FeedItem {
        val feedSource = FeedSource(
            id = "content-source-$id",
            url = "https://example.com/$id/feed.xml",
            title = "Content Feed $id",
            category = null,
            lastSyncTimestamp = null,
            logoUrl = null,
            websiteUrl = "https://example.com",
            fetchFailed = false,
            articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
            isHiddenFromTimeline = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
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
        val item = createFeedItem(
            id = id,
            url = url,
            title = "Title $id",
            pubDateMillis = 1000,
            feedSource = feedSource,
        ).copy(content = content)
        databaseHelper.insertFeedItems(listOf(item), lastSyncTimestamp = 0)
        feedStateRepository.getFeeds()
        return item
    }

    private sealed interface ParserBehavior {
        data object Success : ParserBehavior
        data object HtmlNull : ParserBehavior
        data object HtmlBlank : ParserBehavior
        data object Error : ParserBehavior
        data class DelayedSuccessById(
            val delaysByArticleId: Map<String, Long>,
        ) : ParserBehavior
    }

    private companion object {
        // Longer than the ViewModel's ~200 char "substantial text" threshold.
        private val SUBSTANTIAL_CONTENT = "<p>${"This is a full feed article body. ".repeat(10)}</p>"
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
            articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
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
