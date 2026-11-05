package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.LocalReduceMotion
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class ArticleRevealTest {
    @get:Rule
    val rule = createComposeRule()

    private val items = createRevealItems(ARTICLE_COUNT).toImmutableList()
    private val listState = LazyListState()
    private var revealArticleId by mutableStateOf<String?>(null)
    private var revealedCount = 0

    @Test
    fun `an off-screen article is scrolled into view and acknowledged`() {
        setList()

        revealArticleId = revealId(TARGET)
        rule.waitForIdle()

        rule.runOnIdle {
            assertEquals(TARGET, listState.firstVisibleItemIndex)
            assertEquals(1, revealedCount)
        }
    }

    @Test
    fun `the list snaps to the article instead of animating even with motion enabled`() {
        setList(reduceMotion = false)
        rule.mainClock.autoAdvance = false

        revealArticleId = revealId(TARGET)
        repeat(SNAP_FRAMES) { rule.mainClock.advanceTimeByFrame() }

        rule.runOnIdle { assertEquals(TARGET, listState.firstVisibleItemIndex) }
    }

    @Test
    fun `a visible article keeps the list in place and is acknowledged`() {
        setList()

        revealArticleId = revealId(1)
        rule.waitForIdle()

        rule.runOnIdle {
            assertEquals(0, listState.firstVisibleItemIndex)
            assertEquals(1, revealedCount)
        }
    }

    @Test
    fun `an article missing from the list is acknowledged without scrolling`() {
        setList()

        revealArticleId = "missing"
        rule.waitForIdle()

        rule.runOnIdle {
            assertEquals(0, listState.firstVisibleItemIndex)
            assertEquals(1, revealedCount)
        }
    }

    private fun setList(reduceMotion: Boolean = true) {
        rule.setContent {
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalReduceMotion provides reduceMotion,
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.size(width = VIEWPORT_WIDTH.dp, height = VIEWPORT_HEIGHT.dp)) {
                        DesktopArticleList(
                            feedItems = items,
                            nextFeedState = NextFeedDisplayState.NextFeedDisplayDisabledState,
                            feedFontSize = FeedFontSizes(),
                            feedLayout = FeedLayout.LIST,
                            currentFeedFilter = FeedFilter.Timeline,
                            shareMenuLabel = EnFeedFlowStrings.menuCopyLink,
                            shareCommentsMenuLabel = EnFeedFlowStrings.menuCopyLinkComments,
                            swipeActions = SwipeActions(SwipeActionType.NONE, SwipeActionType.NONE),
                            onVisibleFeedItemsChanged = {},
                            requestMoreItems = {},
                            onFeedItemClick = {},
                            onOpenInBrowser = {},
                            onBookmarkClick = { _, _ -> },
                            onReadStatusClick = { _, _ -> },
                            onCommentClick = {},
                            markAllAsRead = {},
                            onShareClick = {},
                            onOpenFeedSettings = {},
                            onOpenFeedWebsite = {},
                            onNavigateNext = {},
                            modifier = Modifier,
                            listState = listState,
                            gridState = LazyStaggeredGridState(),
                            isGridLayoutEnabled = false,
                            showFooter = false,
                            revealArticleId = revealArticleId,
                            onArticleRevealed = {
                                revealedCount++
                                revealArticleId = null
                            },
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private companion object {
        const val ARTICLE_COUNT = 60
        const val TARGET = 40
        const val SNAP_FRAMES = 3
        const val VIEWPORT_WIDTH = 1_000
        const val VIEWPORT_HEIGHT = 600
    }
}

internal fun revealId(index: Int) = "long-article-$index"

internal fun createRevealItems(count: Int): List<FeedItem> =
    (0 until count).map { index ->
        FeedItem(
            id = revealId(index),
            url = "https://example.com/article-$index",
            title = "Article $index",
            subtitle = "Summary for article $index.",
            content = null,
            imageUrl = null,
            feedSource = FeedSource(
                id = "reveal-feed-source",
                url = "https://example.com/feed.xml",
                title = "Reveal feed",
                category = null,
                logoUrl = null,
                websiteUrl = "https://example.com",
                lastSyncTimestamp = null,
                fetchFailed = false,
                articleOpenMode = ArticleOpenMode.DEFAULT,
                isHiddenFromTimeline = false,
                isPinned = false,
                isNotificationEnabled = false,
                isHideImagesEnabled = false,
            ),
            pubDateMillis = null,
            isRead = false,
            dateString = "Day ${index % 7}",
            commentsUrl = null,
            isBookmarked = false,
        )
    }
