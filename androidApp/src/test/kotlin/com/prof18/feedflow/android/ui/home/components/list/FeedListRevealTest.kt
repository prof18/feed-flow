package com.prof18.feedflow.android.ui.home.components.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.ui.preview.feedItemsForPreview
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.LocalReduceMotion
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.rememberFeedFlowStrings
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.compose.runtime.CompositionLocalProvider as ProvideLocals

@RunWith(RobolectricTestRunner::class)
class FeedListRevealTest {

    @get:Rule
    val compose = createComposeRule()

    private val items = (0 until ITEM_COUNT).map { index ->
        feedItemsForPreview.first().copy(id = "reveal-$index", title = "Reveal article $index")
    }.toImmutableList()

    private val listState = LazyListState()
    private var revealArticleId by mutableStateOf<String?>(null)
    private var revealedCount = 0

    @Test
    fun `an off-screen article is scrolled into view and acknowledged`() {
        setList()

        revealArticleId = "reveal-30"
        compose.waitForIdle()

        compose.runOnIdle {
            assertEquals(30, listState.firstVisibleItemIndex)
            assertEquals(1, revealedCount)
        }
    }

    @Test
    fun `a visible article does not move the list but is acknowledged`() {
        setList()

        revealArticleId = "reveal-0"
        compose.waitForIdle()

        compose.runOnIdle {
            assertEquals(0, listState.firstVisibleItemIndex)
            assertEquals(1, revealedCount)
        }
    }

    @Test
    fun `an article missing from the list is acknowledged without scrolling`() {
        setList()

        revealArticleId = "missing"
        compose.waitForIdle()

        compose.runOnIdle {
            assertEquals(0, listState.firstVisibleItemIndex)
            assertEquals(1, revealedCount)
        }
    }

    private fun setList() {
        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) {
                    ProvideLocals(LocalReduceMotion provides true) {
                        Box(Modifier.size(width = 400.dp, height = 600.dp)) {
                            FeedList(
                                feedItems = items,
                                nextFeedState = NextFeedDisplayState.NextFeedDisplayDisabledState,
                                feedFontSize = FeedFontSizes(),
                                feedLayout = FeedLayout.LIST,
                                currentFeedFilter = FeedFilter.Timeline,
                                shareMenuLabel = "Share",
                                shareCommentsMenuLabel = "Share with comments",
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
                                listState = listState,
                                isGridLayoutEnabled = false,
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
        }
        compose.waitForIdle()
    }

    private companion object {
        const val ITEM_COUNT = 40
    }
}
