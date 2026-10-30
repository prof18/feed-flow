package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
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
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class ArticleListMutationTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `near-tail request is deduplicated until a new page extends the list`() {
        val items = mutableStateOf(createItems(INITIAL_ITEM_COUNT).toImmutableList())
        val listState = LazyListState()
        val requestCount = java.util.concurrent.atomic.AtomicInteger()
        val width = mutableStateOf(LIST_WIDTH_DP.dp)

        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.size(width = width.value, height = LIST_HEIGHT_DP.dp)) {
                        DesktopArticleList(
                            feedItems = items.value,
                            nextFeedState = NextFeedDisplayState.NextFeedDisplayDisabledState,
                            feedFontSize = FeedFontSizes(),
                            feedLayout = FeedLayout.LIST,
                            currentFeedFilter = FeedFilter.Timeline,
                            shareMenuLabel = EnFeedFlowStrings.menuCopyLink,
                            shareCommentsMenuLabel = EnFeedFlowStrings.menuCopyLinkComments,
                            swipeActions = SwipeActions(SwipeActionType.NONE, SwipeActionType.NONE),
                            onVisibleFeedItemsChanged = {},
                            requestMoreItems = { requestCount.incrementAndGet() },
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
                            isGridLayoutEnabled = false,
                            showFooter = false,
                        )
                    }
                }
            }
        }

        rule.onNode(hasScrollToIndexAction()).performScrollToIndex(INITIAL_ITEM_COUNT - 1)
        rule.waitUntil(timeoutMillis = PAGING_TIMEOUT_MS) { requestCount.get() == 1 }

        repeat(LAYOUT_INVALIDATION_COUNT) { iteration ->
            rule.runOnIdle { width.value = (LIST_WIDTH_DP + iteration + 1).dp }
            rule.waitForIdle()
        }
        assertEquals(1, requestCount.get(), "Relayouts at the same loaded tail must not request duplicate pages")

        rule.runOnIdle {
            val nextPage = createItems(PAGE_ITEM_COUNT, startIndex = INITIAL_ITEM_COUNT)
            items.value = (items.value + nextPage).toImmutableList()
        }
        rule.waitUntil(timeoutMillis = PAGING_TIMEOUT_MS) { requestCount.get() == 2 }
        assertEquals(2, requestCount.get(), "The new loaded tail should permit exactly one further page request")
    }

    @Test
    fun `list stays at the top when newer articles are prepended or the filter changes`() {
        val items = mutableStateOf(createItems(MUTATION_ITEM_COUNT).toImmutableList())
        val listState = LazyListState()
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.size(width = LIST_WIDTH_DP.dp, height = LIST_HEIGHT_DP.dp)) {
                        DesktopArticleList(
                            feedItems = items.value,
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
                            isGridLayoutEnabled = false,
                            showFooter = false,
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(rowTag(createItems(1).single().id)).assertIsDisplayed()

        // Startup refresh: newer articles land above the ones already shown.
        val newer = createItems(PREPENDED_COUNT, startIndex = MUTATION_ITEM_COUNT)
        rule.runOnIdle { items.value = (newer + items.value).toImmutableList() }
        rule.waitForIdle()
        rule.onNodeWithTag(rowTag(newer.first().id)).assertIsDisplayed()
        rule.runOnIdle { assertEquals(0, listState.firstVisibleItemIndex) }

        // Filter change: the previous top article is now deep in the new list.
        val previousTop = items.value.first()
        val otherFilter = createItems(MUTATION_ITEM_COUNT, startIndex = OTHER_FILTER_START) + previousTop
        rule.runOnIdle { items.value = otherFilter.toImmutableList() }
        rule.waitForIdle()
        rule.onNodeWithTag(rowTag(otherFilter.first().id)).assertIsDisplayed()
        rule.runOnIdle { assertEquals(0, listState.firstVisibleItemIndex) }
    }

    private companion object {
        const val INITIAL_ITEM_COUNT = 30
        const val PAGE_ITEM_COUNT = 10
        const val MUTATION_ITEM_COUNT = 30
        const val PREPENDED_COUNT = 5
        const val OTHER_FILTER_START = 100
        const val LIST_WIDTH_DP = 720
        const val LIST_HEIGHT_DP = 460
        const val LAYOUT_INVALIDATION_COUNT = 3
        const val PAGING_TIMEOUT_MS = 5_000L
    }
}

private fun rowTag(id: String): String = buildString {
    append("article_row_")
    id.forEach { append(if (it.isLetterOrDigit() || it == '_') it else '_') }
}

private fun createItems(count: Int, startIndex: Int = 0): List<FeedItem> =
    (startIndex until startIndex + count).map { index ->
        FeedItem(
            id = "article-$index",
            url = "https://example.com/article-$index",
            title = "Article $index",
            subtitle = "Summary for article $index.",
            content = null,
            imageUrl = null,
            feedSource = FeedSource(
                id = "test-source",
                url = "https://example.com/feed.xml",
                title = "Example Feed",
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
            dateString = "Today",
            commentsUrl = null,
            isBookmarked = false,
        )
    }
