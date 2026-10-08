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
import com.prof18.feedflow.core.model.ReaderModeDefaults
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.core.model.ShownContentSource
import com.prof18.feedflow.core.model.resolveArticleOpenMode
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.data.SettingsRepository.Companion.DEFAULT_READER_MODE_FONT_SIZE
import com.prof18.feedflow.shared.domain.feed.ArticleNavigationRepository
import com.prof18.feedflow.shared.domain.feed.ArticlePosition
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feeditem.ArticleContentParser
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.collections.immutable.toImmutableList
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
import org.koin.test.get
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class ReaderModeViewModelTest : KoinTestBase() {

    private val viewModel: ReaderModeViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val articleNavigationRepository: ArticleNavigationRepository by inject()
    private val feedItemContentFileHandler: FeedItemContentFileHandler by inject()
    private val settingsRepository: SettingsRepository by inject()
    private var parserBehavior: ParserBehavior = ParserBehavior.Success

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<ArticleContentParser> {
            object : ArticleContentParser {
                override suspend fun parse(url: String): String? =
                    when (val currentParserBehavior = parserBehavior) {
                        ParserBehavior.Success -> "Content"
                        ParserBehavior.HtmlBlank -> "   "
                        ParserBehavior.Error -> null
                        ParserBehavior.DelayedError -> {
                            delay(100)
                            null
                        }
                        is ParserBehavior.DelayedSuccessByUrlPath -> {
                            val pathSegment = url.substringAfterLast('/')
                            delay(currentParserBehavior.delaysByUrlPathSegment[pathSegment] ?: 0)
                            "Content-$pathSegment"
                        }
                    }

                override suspend fun prepareFeedContent(html: String, baseUrl: String?): String = html
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
    fun `loadReaderContent updates selected article`() = runTest {
        val urlInfo = FeedItemUrlInfo(
            id = "open-1",
            url = "https://example.com/articles/open-1",
            title = "Open Article",
            isBookmarked = false,
            articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
            commentsUrl = null,
        )

        viewModel.loadReaderContent(urlInfo)

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

        viewModel.loadReaderContent(urlInfo)
        assertEquals(urlInfo.id, viewModel.currentArticleState.value?.id)

        viewModel.clearSelection()

        assertNull(viewModel.currentArticleState.value)
    }

    @Test
    fun `resetState clears selected article and navigation flags`() = runTest {
        val feedItems = seedFeedItems()
        viewModel.loadReaderContent(feedItems[1].toUrlInfo())

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
    fun `loadReaderContent uses cached content when available`() = runTest {
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

            viewModel.loadReaderContent(urlInfo)

            val successState = awaitItem() as ReaderModeState.Success
            assertEquals("Cached content", successState.readerModeData.content)
            assertEquals("Cached Article", successState.readerModeData.title)
            assertEquals("https://example.com", successState.readerModeData.baseUrl)
        }
    }

    @Test
    fun `loadReaderContent uses parser result when cache missing`() = runTest {
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

            viewModel.loadReaderContent(urlInfo)

            val successState = awaitItem() as ReaderModeState.Success
            assertEquals("Content", successState.readerModeData.content)
            assertEquals("Original title", successState.readerModeData.title)
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

        viewModel.loadReaderContent(urlInfo)
        assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)

        viewModel.setLoading()
        assertEquals(ReaderModeState.Loading, viewModel.readerModeState.value)
    }

    @Test
    fun `loadReaderContent sets ContentNotAvailable when parser returns error`() = runTest {
        parserBehavior = ParserBehavior.Error
        val urlInfo = FeedItemUrlInfo(
            id = "error-1",
            url = "https://example.com/articles/error",
            title = "Error Article",
            isBookmarked = false,
            articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
            commentsUrl = null,
        )

        viewModel.loadReaderContent(urlInfo)

        val state = viewModel.readerModeState.value
        assertIs<ReaderModeState.ContentNotAvailable>(state)
        assertEquals(urlInfo.url, state.url)
        assertEquals(urlInfo.id, state.id)
    }

    @Test
    fun `loadReaderContent keeps latest requested article when previous request finishes later`() = runTest {
        parserBehavior = ParserBehavior.DelayedSuccessByUrlPath(
            delaysByUrlPathSegment = mapOf(
                "slow" to 300,
                "fast" to 10,
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

        viewModel.loadReaderContent(slowArticle)
        viewModel.loadReaderContent(fastArticle)
        advanceUntilIdle()

        val state = viewModel.readerModeState.value
        assertIs<ReaderModeState.Success>(state)
        assertEquals("fast-article", state.readerModeData.id.id)
    }

    @Test
    fun `loadReaderContent saves parsed content when save on open is enabled`() = runTest {
        settingsRepository.setSaveItemContentOnOpen(true)
        val urlInfo = webArticle(id = "save-enabled")

        viewModel.loadReaderContent(urlInfo)
        advanceUntilIdle()

        assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals("Content", feedItemContentFileHandler.loadFeedItemContent(urlInfo.id))
    }

    @Test
    fun `loadReaderContent does not save parsed content when save on open is disabled`() = runTest {
        settingsRepository.setSaveItemContentOnOpen(false)
        val urlInfo = webArticle(id = "save-disabled")

        viewModel.loadReaderContent(urlInfo)
        advanceUntilIdle()

        assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertFalse(feedItemContentFileHandler.isContentAvailable(urlInfo.id))
    }

    @Test
    fun `only the latest requested article is saved when requests overlap`() = runTest {
        settingsRepository.setSaveItemContentOnOpen(true)
        parserBehavior = ParserBehavior.DelayedSuccessByUrlPath(
            delaysByUrlPathSegment = mapOf(
                "slow-article" to 300,
                "fast-article" to 10,
            ),
        )

        viewModel.loadReaderContent(webArticle(id = "slow-article"))
        viewModel.loadReaderContent(webArticle(id = "fast-article"))
        advanceUntilIdle()

        assertFalse(feedItemContentFileHandler.isContentAvailable("slow-article"))
        assertEquals("Content-fast-article", feedItemContentFileHandler.loadFeedItemContent("fast-article"))
    }

    private fun webArticle(id: String) = FeedItemUrlInfo(
        id = id,
        url = "https://example.com/articles/$id",
        title = "Article $id",
        isBookmarked = false,
        articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
        commentsUrl = null,
    )

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
        viewModel.loadReaderContent(middleItem.toUrlInfo())

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

        viewModel.loadReaderContent(middleItem.toUrlInfo())
        assertTrue(viewModel.canNavigateToNextState.value)

        viewModel.navigateToNextArticle()
        advanceUntilIdle()

        val state = viewModel.readerModeState.value
        assertIs<ReaderModeState.ContentNotAvailable>(state)
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

        viewModel.loadReaderContent(middleItem.toUrlInfo())
        assertTrue(viewModel.canNavigateToPreviousState.value)

        viewModel.navigateToPreviousArticle()
        advanceUntilIdle()

        val state = viewModel.readerModeState.value
        assertIs<ReaderModeState.ContentNotAvailable>(state)
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

        viewModel.loadReaderContent(middleItem.toUrlInfo())
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

            viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FEED_CONTENT))

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

            viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FEED_CONTENT))

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

            viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FEED_CONTENT))

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

            viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))

            val state = awaitItem() as ReaderModeState.Success
            assertEquals(ShownContentSource.WEB, state.readerModeData.shownContentSource)
            assertFalse(state.readerModeData.canToggleContentSource)
        }
    }

    @Test
    fun `web reader does not reinsert an inline feed thumbnail removed by extraction`() = runTest {
        val imageUrl = "https://example.com/masthead.png?width=600&format=png"
        val item = seedItemWithContent(
            "inline-thumbnail",
            "https://example.com/article",
            """<img src="${imageUrl.replace("&", "&amp;")}">$SUBSTANTIAL_CONTENT""",
        )
        val urlInfo = item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE).copy(imageUrl = imageUrl)

        viewModel.loadReaderContent(urlInfo)
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertNull(state.readerModeData.imageUrl)
        assertEquals("Content", state.readerModeData.content)

        viewModel.toggleContentSource()
        advanceUntilIdle()

        val feedState = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(ShownContentSource.FEED, feedState.readerModeData.shownContentSource)
        assertEquals(imageUrl, feedState.readerModeData.imageUrl)
    }

    @Test
    fun `web reader retains independently supplied feed hero metadata`() = runTest {
        val imageUrl = "https://example.com/hero.jpg"
        val item = seedItemWithContent("metadata-hero", "https://example.com/article", SUBSTANTIAL_CONTENT)

        viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE).copy(imageUrl = imageUrl))
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(imageUrl, state.readerModeData.imageUrl)
    }

    @Test
    fun `web preference falls back to feed content when parsing fails`() = runTest {
        parserBehavior = ParserBehavior.Error
        val item = seedItemWithContent("web-fallback", "https://example.com/a/web-fallback", SUBSTANTIAL_CONTENT)

        viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
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

        viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.DEFAULT))
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(ShownContentSource.FEED, state.readerModeData.shownContentSource)
    }

    @Test
    fun `reopening the same article reloads after global source changes`() = runTest {
        val item = seedItemWithContent("source-change", "https://example.com/a/source-change", SUBSTANTIAL_CONTENT)
        val urlInfo = item.toUrlInfo(ArticleOpenMode.DEFAULT)

        settingsRepository.setArticleOpenMode(ArticleOpenMode.FULL_ARTICLE)
        viewModel.loadReaderContent(urlInfo)
        advanceUntilIdle()
        assertEquals(
            ShownContentSource.WEB,
            assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData.shownContentSource,
        )

        settingsRepository.setArticleOpenMode(ArticleOpenMode.FEED_CONTENT)
        viewModel.loadReaderContent(urlInfo)
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

        viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(ShownContentSource.FEED, state.readerModeData.shownContentSource)
    }

    @Test
    fun `blank parsed web content falls back to feed content`() = runTest {
        parserBehavior = ParserBehavior.HtmlBlank
        val item = seedItemWithContent("blank-parser", "https://example.com/a/blank-parser", SUBSTANTIAL_CONTENT)

        viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(ShownContentSource.FEED, state.readerModeData.shownContentSource)
    }

    @Test
    fun `web content shows the feed item title and feed source name`() = runTest {
        val item = seedItemWithContent("web-meta", "https://example.com/a/web-meta", SUBSTANTIAL_CONTENT)

        viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(ShownContentSource.WEB, state.readerModeData.shownContentSource)
        assertEquals("Title web-meta", state.readerModeData.title)
        assertEquals("Content Feed web-meta", state.readerModeData.siteName)
        assertEquals("Content", state.readerModeData.content)
    }

    @Test
    fun `web preference falls back to feed content when parsing times out`() = runTest {
        val item = seedItemWithContent("web-timeout", "https://example.com/a/web-timeout", SUBSTANTIAL_CONTENT)
        parserBehavior = ParserBehavior.DelayedSuccessByUrlPath(
            delaysByUrlPathSegment = mapOf(item.id to 21_000),
        )

        viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(SUBSTANTIAL_CONTENT, state.readerModeData.content)
        assertEquals(ShownContentSource.FEED, state.readerModeData.shownContentSource)
        assertTrue(state.readerModeData.canToggleContentSource)
    }

    @Test
    fun `sets ContentNotAvailable when neither web nor feed content is available`() = runTest {
        parserBehavior = ParserBehavior.Error
        val item = seedItemWithContent("nothing", "https://example.com/a/nothing", content = null)

        viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
        advanceUntilIdle()

        val state = viewModel.readerModeState.value
        assertIs<ReaderModeState.ContentNotAvailable>(state)
        assertEquals(item.id, state.id)
    }

    @Test
    fun `toggleContentSource switches from web to feed and back`() = runTest {
        val item = seedItemWithContent("toggle", "https://example.com/a/toggle", SUBSTANTIAL_CONTENT)

        viewModel.readerModeState.test {
            assertEquals(ReaderModeState.Loading, awaitItem())

            viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
            val initialState = awaitItem() as ReaderModeState.Success
            assertEquals(ShownContentSource.WEB, initialState.readerModeData.shownContentSource)
            assertTrue(initialState.readerModeData.canToggleContentSource)

            viewModel.toggleContentSource()
            assertEquals(ReaderModeState.Loading, awaitItem())
            val feedState = awaitItem() as ReaderModeState.Success
            assertEquals(ShownContentSource.FEED, feedState.readerModeData.shownContentSource)
            assertEquals(SUBSTANTIAL_CONTENT, feedState.readerModeData.content)
            assertTrue(feedState.readerModeData.canToggleContentSource)

            viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
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

        viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
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

        viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FEED_CONTENT))
        advanceUntilIdle()
        val feedState = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(ShownContentSource.FEED, feedState.readerModeData.shownContentSource)
        assertTrue(feedState.readerModeData.canToggleContentSource)

        parserBehavior = ParserBehavior.Error
        viewModel.toggleContentSource()
        advanceUntilIdle()

        val fallbackState = assertIs<ReaderModeState.ContentNotAvailable>(viewModel.readerModeState.value)
        assertEquals(url, fallbackState.url)
    }

    @Test
    fun `blank url item is shown from feed content`() = runTest {
        val item = seedItemWithContent("blank-url", url = "", content = SUBSTANTIAL_CONTENT)

        viewModel.readerModeState.test {
            assertEquals(ReaderModeState.Loading, awaitItem())

            viewModel.loadReaderContent(item.toUrlInfo())

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

        viewModel.loadReaderContent(urlInfo)
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals("https://example.com/source/", state.readerModeData.baseUrl)
    }

    @Test
    fun `blank url item falls back to the stored feed base url`() = runTest {
        val item = seedItemWithContent("blank-url-stored-base", url = "", content = "<p>Short post</p>")
        // The Compose feed list and the desktop reader route build FeedItemUrlInfo without it.
        val urlInfo = item.toUrlInfo().copy(feedSourceBaseUrl = null)

        viewModel.loadReaderContent(urlInfo)
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals("https://example.com", state.readerModeData.baseUrl)
    }

    @Test
    fun `feed content for ineligible url cannot toggle to web parsing`() = runTest {
        val item = seedItemWithContent("pdf", "https://example.com/file.pdf", SUBSTANTIAL_CONTENT)

        viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FEED_CONTENT))
        advanceUntilIdle()

        val state = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
        assertEquals(ShownContentSource.FEED, state.readerModeData.shownContentSource)
        assertFalse(state.readerModeData.canToggleContentSource)
    }

    @Test
    fun `blank url item accepts short feed content`() = runTest {
        val item = seedItemWithContent("blank-url-short", url = "", content = "<p>Short post</p>")

        viewModel.loadReaderContent(item.toUrlInfo())
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

        viewModel.loadReaderContent(feedItems[1].toUrlInfo())
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
    fun `blank url item with no content is ContentNotAvailable`() = runTest {
        val item = seedItemWithContent("blank-url-empty", url = "", content = null)

        viewModel.loadReaderContent(item.toUrlInfo())
        advanceUntilIdle()

        assertIs<ReaderModeState.ContentNotAvailable>(viewModel.readerModeState.value)
    }

    @Test
    fun `reader follows overlapping search results in both directions`() = runTest(testDispatcher) {
        keepReadNavigationItems()
        val items = seedFeedItems()
        articleNavigationRepository.setSearchResults(listOf(items[2], items[0]).toImmutableList())

        viewModel.loadReaderContent(items[2].toUrlInfo())
        advanceUntilIdle()
        assertReaderSelection(items[2], previous = false, next = true)
        viewModel.navigateToNextArticle()
        advanceUntilIdle()
        assertReaderSelection(items[0], previous = true, next = false)
        viewModel.navigateToPreviousArticle()
        advanceUntilIdle()
        assertReaderSelection(items[2], previous = false, next = true)
    }

    @Test
    fun `reader navigates search results absent from the loaded home list`() = runTest(testDispatcher) {
        keepReadNavigationItems()
        val home = seedFeedItems()
        val source = home.first().feedSource
        val results = listOf(
            createFeedItem("search-x", "https://example.com/search-x", "Search X", 5000, source),
            createFeedItem("search-y", "https://example.com/search-y", "Search Y", 4000, source),
        )
        databaseHelper.insertFeedItems(results, lastSyncTimestamp = 0)
        articleNavigationRepository.setSearchResults(results.toImmutableList())

        viewModel.loadReaderContent(results[0].toUrlInfo())
        advanceUntilIdle()
        assertReaderSelection(results[0], previous = false, next = true)
        viewModel.navigateToNextArticle()
        advanceUntilIdle()
        assertReaderSelection(results[1], previous = true, next = false)
        viewModel.navigateToPreviousArticle()
        advanceUntilIdle()
        assertReaderSelection(results[0], previous = false, next = true)
        assertEquals(home.map { it.id }, feedStateRepository.feedState.value.map { it.id })
    }

    @Test
    fun `clearing search restores reader home navigation`() = runTest(testDispatcher) {
        keepReadNavigationItems()
        val items = seedFeedItems()
        articleNavigationRepository.setSearchResults(listOf(items[2], items[0]).toImmutableList())
        articleNavigationRepository.clearSearchResults()

        viewModel.loadReaderContent(items[1].toUrlInfo())
        advanceUntilIdle()
        assertReaderSelection(items[1], previous = true, next = true)
        viewModel.navigateToNextArticle()
        advanceUntilIdle()
        assertReaderSelection(items[2], previous = true, next = false)
        viewModel.navigateToPreviousArticle()
        advanceUntilIdle()
        assertReaderSelection(items[1], previous = true, next = true)
    }

    @Test
    fun `reader uses home neighbors when current item is not a search result`() = runTest(testDispatcher) {
        keepReadNavigationItems()
        val items = seedFeedItems()
        articleNavigationRepository.setSearchResults(listOf(items[2], items[0]).toImmutableList())

        viewModel.loadReaderContent(items[1].toUrlInfo())
        advanceUntilIdle()
        assertReaderSelection(items[1], previous = true, next = true)
        viewModel.navigateToNextArticle()
        advanceUntilIdle()
        assertReaderSelection(items[2], previous = false, next = true)
        viewModel.loadReaderContent(items[1].toUrlInfo())
        advanceUntilIdle()
        viewModel.navigateToPreviousArticle()
        advanceUntilIdle()
        assertReaderSelection(items[0], previous = true, next = false)
    }

    @Test
    fun `published search navigation survives reader mark-read emissions`() = runTest(testDispatcher) {
        keepReadNavigationItems()
        val items = seedFeedItems(item1Title = "NavigationMatch A", item3Title = "NavigationMatch C")
        val searchViewModel = get<SearchViewModel>()
        searchViewModel.updateSearchQuery("NavigationMatch")
        advanceTimeBy(500.milliseconds)
        advanceUntilIdle()
        assertEquals(
            listOf(items[0].id, items[2].id),
            assertIs<SearchState.DataFound>(searchViewModel.searchState.value).items.map { it.id },
        )

        viewModel.loadReaderContent(items[0].toUrlInfo())
        advanceUntilIdle()
        assertReaderSelection(items[0], previous = false, next = true)
        viewModel.navigateToNextArticle()
        advanceUntilIdle()
        assertReaderSelection(items[2], previous = true, next = false)
        viewModel.navigateToPreviousArticle()
        advanceUntilIdle()
        assertReaderSelection(items[0], previous = false, next = true)
        assertEquals(ArticlePosition(1, 2), articleNavigationRepository.getArticlePosition(items[0].id))
        assertEquals(ArticlePosition(2, 2), articleNavigationRepository.getArticlePosition(items[2].id))
        val dbItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        ).associateBy { it.url_hash }
        assertTrue(dbItems.getValue(items[0].id).is_read)
        assertTrue(dbItems.getValue(items[2].id).is_read)
    }

    private fun keepReadNavigationItems() {
        settingsRepository.setShowReadArticlesTimeline(true)
        settingsRepository.setHideReadItems(false)
    }

    private fun assertReaderSelection(item: FeedItem, previous: Boolean, next: Boolean) {
        assertEquals(item.id, viewModel.currentArticleState.value?.id)
        assertEquals(item.id, assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData.id.id)
        assertEquals(previous, viewModel.canNavigateToPreviousState.value)
        assertEquals(next, viewModel.canNavigateToNextState.value)
    }

    @Test
    fun `feed reader retrieves audio by item id without navigation metadata`() = runTest {
        val item = seedItemWithContent(
            id = "audio-feed",
            url = "https://example.com/audio-notes",
            content = SUBSTANTIAL_CONTENT,
            audioUrl = AUDIO_URL,
        )
        viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FEED_CONTENT))
        advanceUntilIdle()
        val data = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData
        assertEquals(AUDIO_URL, data.audioUrl)
        assertEquals(item.imageUrl ?: item.feedSource.logoUrl, data.audioImageUrl)
        assertEquals(ShownContentSource.FEED, data.shownContentSource)
    }

    @Test
    fun `audio return retrieves stored reader metadata without changing selection or read state`() = runTest {
        val item = seedItemWithContent("audio-return", "", SUBSTANTIAL_CONTENT, audioUrl = AUDIO_URL)
        settingsRepository.setArticleOpenMode(ArticleOpenMode.PREFERRED_BROWSER)
        val info = viewModel.getAudioEpisodeReaderInfo(FeedItemId(item.id))
        assertEquals(item.id, info?.id)
        assertEquals(ArticleOpenMode.DEFAULT, info?.articleOpenMode)
        assertEquals(
            ArticleOpenMode.FEED_CONTENT,
            requireNotNull(info).resolveArticleOpenMode(settingsRepository.getArticleOpenMode()),
        )
        assertEquals(item.isBookmarked, info?.isBookmarked)
        assertNull(viewModel.currentArticleState.value)
        assertEquals(item.isRead, databaseHelper.getAllFeedItemFlagsForCloud().single { it.id == item.id }.isRead)

        viewModel.loadReaderContent(requireNotNull(info))
        advanceUntilIdle()
        val data = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData
        assertEquals(item.id, data.id.id)
        assertEquals(AUDIO_URL, data.audioUrl)
        assertEquals(ShownContentSource.FEED, data.shownContentSource)
    }

    @Test
    fun `audio return respects the global mode and per-feed overrides`() = runTest {
        val item = seedItemWithContent("audio-return-preferences", "https://example.com/audio", SUBSTANTIAL_CONTENT)
        settingsRepository.setArticleOpenMode(ArticleOpenMode.PREFERRED_BROWSER)
        val defaultInfo = requireNotNull(viewModel.getAudioEpisodeReaderInfo(FeedItemId(item.id)))
        assertEquals(
            ArticleOpenMode.PREFERRED_BROWSER,
            defaultInfo.resolveArticleOpenMode(settingsRepository.getArticleOpenMode()),
        )

        ArticleOpenMode.entries.forEach { mode ->
            databaseHelper.insertFeedSourcePreference(
                feedSourceId = item.feedSource.id,
                articleOpenMode = mode,
                isHidden = false,
                isPinned = false,
                isNotificationEnabled = false,
                isHideImagesEnabled = false,
            )
            val info = requireNotNull(viewModel.getAudioEpisodeReaderInfo(FeedItemId(item.id)))
            assertEquals(mode, info.articleOpenMode)
            val expected = if (mode == ArticleOpenMode.DEFAULT) ArticleOpenMode.PREFERRED_BROWSER else mode
            assertEquals(expected, info.resolveArticleOpenMode(settingsRepository.getArticleOpenMode()))
        }
    }

    @Test
    fun `audio return for a removed episode keeps the current article`() = runTest {
        val item = seedItemWithContent("other-reader", "https://example.com/notes", SUBSTANTIAL_CONTENT)
        viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FEED_CONTENT))
        advanceUntilIdle()
        val currentState = viewModel.readerModeState.value
        assertNull(viewModel.getAudioEpisodeReaderInfo(FeedItemId("removed-audio")))
        assertEquals(currentState, viewModel.readerModeState.value)
        assertEquals(item.id, viewModel.currentArticleState.value?.id)
    }

    @Test
    fun `cached web content and source toggles retain stored audio`() = runTest {
        val item = seedItemWithContent(
            id = "audio-cache",
            url = "https://example.com/audio-notes",
            content = SUBSTANTIAL_CONTENT,
            audioUrl = AUDIO_URL,
        )
        feedItemContentFileHandler.saveFeedItemContentToFile(item.id, "Cached show notes")
        viewModel.loadReaderContent(item.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
        advanceUntilIdle()
        val cached = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData
        assertEquals("Cached show notes", cached.content)
        assertEquals(AUDIO_URL, cached.audioUrl)
        for (expectedSource in listOf(ShownContentSource.FEED, ShownContentSource.WEB)) {
            viewModel.toggleContentSource()
            advanceUntilIdle()
            val data = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData
            assertEquals(expectedSource, data.shownContentSource)
            assertEquals(AUDIO_URL, data.audioUrl)
        }
    }

    @Test
    fun `paging to plain article clears audio metadata`() = runTest {
        val items = seedFeedItems(item2AudioUrl = AUDIO_URL)
        viewModel.loadReaderContent(items[1].toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
        advanceUntilIdle()
        assertEquals(
            AUDIO_URL,
            assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData.audioUrl,
        )
        viewModel.navigateToNextArticle()
        advanceUntilIdle()
        val next = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData
        assertEquals(items[2].id, next.id.id)
        assertNull(next.audioUrl)
    }

    @Test
    fun `failed and stale loads cannot expose previous audio`() = runTest {
        val audio = seedItemWithContent(
            id = "audio-slow",
            url = "https://example.com/a/slow",
            content = SUBSTANTIAL_CONTENT,
            audioUrl = AUDIO_URL,
        )
        val plain = seedItemWithContent("audio-plain", "https://example.com/a/fast", content = null)
        viewModel.loadReaderContent(audio.toUrlInfo(ArticleOpenMode.FEED_CONTENT))
        advanceUntilIdle()
        assertEquals(
            AUDIO_URL,
            assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData.audioUrl,
        )
        parserBehavior = ParserBehavior.DelayedError
        viewModel.loadReaderContent(plain.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
        assertEquals(ReaderModeState.Loading, viewModel.readerModeState.value)
        advanceUntilIdle()
        assertIs<ReaderModeState.ContentNotAvailable>(viewModel.readerModeState.value)

        parserBehavior = ParserBehavior.DelayedSuccessByUrlPath(mapOf("slow" to 300, "fast" to 10))
        viewModel.loadReaderContent(audio.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
        viewModel.loadReaderContent(plain.toUrlInfo(ArticleOpenMode.FULL_ARTICLE))
        advanceUntilIdle()
        val latest = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData
        assertEquals(plain.id, latest.id.id)
        assertNull(latest.audioUrl)
    }

    private suspend fun seedFeedItems(
        item1Url: String = "https://example.com/articles/1",
        item2Url: String = "https://example.com/articles/2",
        item3Url: String = "https://example.com/articles/3",
        item1Content: String? = null,
        item3Content: String? = null,
        item1Title: String = "Article 1",
        item3Title: String = "Article 3",
        item2AudioUrl: String? = null,
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
                title = item1Title,
                pubDateMillis = 3000,
                feedSource = feedSource,
            ).copy(content = item1Content),
            createFeedItem(
                id = "item-2",
                url = item2Url,
                title = "Article 2",
                pubDateMillis = 2000,
                feedSource = feedSource,
            ).copy(audioUrl = item2AudioUrl),
            createFeedItem(
                id = "item-3",
                url = item3Url,
                title = item3Title,
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
        audioUrl: String? = null,
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
        ).copy(content = content, audioUrl = audioUrl)
        databaseHelper.insertFeedItems(listOf(item), lastSyncTimestamp = 0)
        feedStateRepository.getFeeds()
        return item
    }

    private sealed interface ParserBehavior {
        data object Success : ParserBehavior
        data object HtmlBlank : ParserBehavior
        data object Error : ParserBehavior
        data object DelayedError : ParserBehavior
        data class DelayedSuccessByUrlPath(
            val delaysByUrlPathSegment: Map<String, Long>,
        ) : ParserBehavior
    }

    private companion object {
        const val AUDIO_URL = "https://example.com/episode.mp3?token=a%2Bb&part=1"

        // Longer than the ViewModel's ~200 char "substantial text" threshold.
        private val SUBSTANTIAL_CONTENT = "<p>${"This is a full feed article body. ".repeat(10)}</p>"
    }
}

class ReaderModeViewModelTimeoutTest : KoinTestBase() {

    private val viewModel: ReaderModeViewModel by inject()

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<ArticleContentParser> {
            object : ArticleContentParser {
                override suspend fun parse(url: String): String? {
                    delay(2.minutes)
                    return "Content"
                }

                override suspend fun prepareFeedContent(html: String, baseUrl: String?): String = html
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
    fun `loadReaderContent sets ContentNotAvailable when parser returns null html`() = runTest {
        val urlInfo = FeedItemUrlInfo(
            id = "null-html-1",
            url = "https://example.com/articles/null-html",
            title = "Null Html Article",
            isBookmarked = false,
            articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
            commentsUrl = null,
        )

        viewModel.loadReaderContent(urlInfo)
        advanceTimeBy(1.minutes)

        val state = viewModel.readerModeState.value
        assertIs<ReaderModeState.ContentNotAvailable>(state)
        assertEquals(urlInfo.url, state.url)
        assertEquals(urlInfo.id, state.id)
    }
}
