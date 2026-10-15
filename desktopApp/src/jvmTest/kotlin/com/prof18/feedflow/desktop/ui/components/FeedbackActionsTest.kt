package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackResult
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class FeedbackActionsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `retry completes feedback once and shows the next message`() {
        val state = DesktopFeedbackState()
        var result: DesktopFeedbackResult? = null
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    LaunchedEffect(state) {
                        result = state.showFeedback(
                            EnFeedFlowStrings.browserLaunchError,
                            actionLabel = EnFeedFlowStrings.retryButton,
                        )
                        state.showFeedback(
                            EnFeedFlowStrings.linkCopiedSuccess,
                            duration = DesktopFeedbackDuration.Indefinite,
                        )
                    }
                    SnackbarHost(state)
                }
            }
        }
        rule.onNodeWithText(EnFeedFlowStrings.retryButton).performClick()
        rule.onNodeWithText(EnFeedFlowStrings.linkCopiedSuccess).assertExists()
        rule.runOnIdle { assertEquals(DesktopFeedbackResult.ActionPerformed, result) }
    }

    @Test
    fun `close dismisses actionable feedback without performing its action`() {
        val state = DesktopFeedbackState()
        var result: DesktopFeedbackResult? = null
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = true, useOledTheme = true) {
                    LaunchedEffect(state) {
                        result = state.showFeedback(
                            EnFeedFlowStrings.browserLaunchError,
                            actionLabel = EnFeedFlowStrings.retryButton,
                        )
                    }
                    SnackbarHost(state)
                }
            }
        }
        rule.onNodeWithText(EnFeedFlowStrings.closeButtonContentDescription).performClick()
        rule.onNodeWithText(EnFeedFlowStrings.browserLaunchError).assertDoesNotExist()
        rule.runOnIdle { assertEquals(DesktopFeedbackResult.Dismissed, result) }
    }
}
