package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.desktop.testing.FeedItemE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class ArticleLongFeedTest(private val layout: FeedLayout) {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `all paged articles remain reachable in every layout`() {
        val items = mutableStateOf(createLongFeedItems(PAGE_SIZE).toImmutableList())
        val listState = LazyListState()
        val gridState = LazyStaggeredGridState()
        val pageRequests = AtomicInteger()

        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.size(width = VIEWPORT_WIDTH.dp, height = VIEWPORT_HEIGHT.dp)) {
                        DesktopArticleList(
                            feedItems = items.value,
                            nextFeedState = NextFeedDisplayState.NextFeedDisplayDisabledState,
                            feedFontSize = FeedFontSizes(),
                            feedLayout = layout,
                            currentFeedFilter = FeedFilter.Timeline,
                            shareMenuLabel = EnFeedFlowStrings.menuCopyLink,
                            shareCommentsMenuLabel = EnFeedFlowStrings.menuCopyLinkComments,
                            swipeActions = SwipeActions(SwipeActionType.NONE, SwipeActionType.NONE),
                            onVisibleFeedItemsChanged = {},
                            requestMoreItems = { pageRequests.incrementAndGet() },
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
                            gridState = gridState,
                            isGridLayoutEnabled = true,
                            showFooter = false,
                        )
                    }
                }
            }
        }

        for (index in 0 until ARTICLE_COUNT) {
            if (index >= items.value.size) {
                rule.waitUntil(PAGING_TIMEOUT_MS) { pageRequests.get() >= items.value.size / PAGE_SIZE }
                rule.runOnIdle {
                    val start = items.value.size
                    items.value = (items.value + createLongFeedItems(PAGE_SIZE, start)).toImmutableList()
                }
                rule.waitForIdle()
            }

            rule.onNode(hasScrollToIndexAction()).performScrollToIndex(index)
            rule.waitForIdle()
            val expectedId = articleId(index)
            rule.onNodeWithTag(FeedItemE2eIds.row(expectedId)).assertIsDisplayed()

            rule.runOnIdle {
                val loadedIds = items.value.map { it.id }.toSet()
                val indexes = if (layout == FeedLayout.LIST) {
                    listState.layoutInfo.visibleItemsInfo.map { it.index }
                } else {
                    gridState.layoutInfo.visibleItemsInfo.map { it.index }
                }
                val visibleIds = indexes.mapNotNull { items.value.getOrNull(it)?.id }
                assertTrue(visibleIds.isNotEmpty(), "No articles observed for $layout")
                assertTrue(
                    visibleIds.all { it in loadedIds },
                    "Visible article key was absent from loaded data for $layout",
                )
                assertEquals(visibleIds.size, visibleIds.toSet().size, "Duplicate visible article key for $layout")
                assertEquals(items.value.size, loadedIds.size, "Duplicate article IDs in loaded data for $layout")
            }
        }

        assertEquals(ARTICLE_COUNT, items.value.size)
        rule.onNodeWithTag(FeedItemE2eIds.row(articleId(ARTICLE_COUNT - 1))).assertIsDisplayed()
    }

    companion object {
        private const val ARTICLE_COUNT = 120
        private const val PAGE_SIZE = 40
        private const val VIEWPORT_WIDTH = 1_000
        private const val VIEWPORT_HEIGHT = 600
        private const val PAGING_TIMEOUT_MS = 5_000L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): List<Array<Any>> = FeedLayout.entries.map { arrayOf(it) }
    }
}

private fun articleId(index: Int) = "long-article-$index"

private fun createLongFeedItems(count: Int, startIndex: Int = 0): List<FeedItem> =
    (startIndex until startIndex + count).map { index ->
        FeedItem(
            id = articleId(index),
            url = "https://example.com/article-$index",
            title = "Article $index",
            subtitle = "Summary for article $index.",
            content = null,
            imageUrl = null,
            feedSource = FeedSource(
                id = "long-feed-source",
                url = "https://example.com/feed.xml",
                title = "Long feed",
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
            isRead = index % 2 == 0,
            dateString = "Day ${index % 7}",
            commentsUrl = null,
            isBookmarked = index % 3 == 0,
        )
    }
