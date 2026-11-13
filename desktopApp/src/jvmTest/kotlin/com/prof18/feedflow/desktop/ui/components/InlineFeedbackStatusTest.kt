package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackResult
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.DesktopFeedbackTone
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InlineFeedbackStatusTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `status shows the message without a close button and dismisses itself`() {
        val state = DesktopFeedbackState()
        var result: DesktopFeedbackResult? = null
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    LaunchedEffect(state) {
                        result = state.showFeedback(
                            message = MESSAGE,
                            duration = DesktopFeedbackDuration.Short,
                            tone = DesktopFeedbackTone.Success,
                        )
                    }
                    InlineFeedbackStatus(state)
                }
            }
        }

        rule.mainClock.advanceTimeBy(ENTER_ANIMATION_MILLIS)
        rule.onNodeWithText(MESSAGE).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.closeButtonContentDescription).assertDoesNotExist()

        rule.mainClock.advanceTimeBy(SHORT_DURATION_PLUS_EXIT_MILLIS)
        rule.onNodeWithTag(InlineFeedbackStatusTag).assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(DesktopFeedbackResult.Dismissed, result)
            assertNull(state.currentFeedback)
        }
    }

    private companion object {
        const val MESSAGE = "Feed Il Post added"
        const val ENTER_ANIMATION_MILLIS = 500L
        const val SHORT_DURATION_PLUS_EXIT_MILLIS = 5_000L
    }
}
