package com.prof18.feedflow.android.ui.home.components.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.rememberFeedFlowStrings
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState.NextFeedDisplayDisabledState
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState.NextFeedDisplayEnabledState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeedFooterButtonsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `enabled next feed shows a button that navigates`() {
        var navigateCount = 0
        setFooter(
            feedFilter = FeedFilter.Timeline,
            nextFeedState = NextFeedDisplayEnabledState(title = "Next"),
            onNavigateNext = { navigateCount++ },
        )

        compose.onNodeWithTag(NEXT_FEED_BUTTON_TAG).assertTextEquals("Next")
        compose.onNodeWithTag(NEXT_FEED_BUTTON_TAG).performClick()

        assertEquals(1, navigateCount)
        compose.onNodeWithText(EnFeedFlowStrings.markAllReadButton).assertExists()
    }

    @Test
    fun `disabled next feed shows no button`() {
        setFooter(
            feedFilter = FeedFilter.Timeline,
            nextFeedState = NextFeedDisplayDisabledState,
        )

        compose.onNodeWithTag(NEXT_FEED_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `read filter keeps the next feed button without mark all as read`() {
        setFooter(
            feedFilter = FeedFilter.Read,
            nextFeedState = NextFeedDisplayEnabledState(title = "Next"),
        )

        compose.onNodeWithTag(NEXT_FEED_BUTTON_TAG).assertExists()
        compose.onNodeWithText(EnFeedFlowStrings.markAllReadButton).assertDoesNotExist()
    }

    private fun setFooter(
        feedFilter: FeedFilter,
        nextFeedState: NextFeedDisplayState,
        onNavigateNext: () -> Unit = {},
    ) {
        setContent {
            FeedFooterButtons(
                currentFeedFilter = feedFilter,
                nextFeedState = nextFeedState,
                markAllAsRead = {},
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
