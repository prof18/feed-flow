package com.prof18.feedflow.android.ui.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.prof18.feedflow.android.ui.home.components.list.NEXT_FEED_BUTTON_TAG
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.rememberFeedFlowStrings
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState.NextFeedDisplayDisabledState
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState.NextFeedDisplayEnabledState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EmptyFeedViewTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `enabled next feed shows a button that navigates`() {
        var navigateCount = 0
        setEmptyView(
            nextFeedState = NextFeedDisplayEnabledState(title = "Next"),
            onNavigateNext = { navigateCount++ },
        )

        compose.onNodeWithTag(NEXT_FEED_BUTTON_TAG).performClick()

        assertEquals(1, navigateCount)
    }

    @Test
    fun `disabled next feed shows no button`() {
        setEmptyView(nextFeedState = NextFeedDisplayDisabledState)

        compose.onNodeWithTag(NEXT_FEED_BUTTON_TAG).assertDoesNotExist()
    }

    private fun setEmptyView(
        nextFeedState: NextFeedDisplayState,
        onNavigateNext: () -> Unit = {},
    ) {
        setContent {
            EmptyFeedView(
                currentFeedFilter = FeedFilter.Timeline,
                isDrawerVisible = false,
                nextFeedState = nextFeedState,
                onReloadClick = {},
                onBackToTimelineClick = {},
                onOpenDrawerClick = {},
                onNavigateNext = onNavigateNext,
            )
        }
    }

    private fun setContent(content: @Composable () -> Unit) {
        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) { content() }
            }
        }
    }
}
