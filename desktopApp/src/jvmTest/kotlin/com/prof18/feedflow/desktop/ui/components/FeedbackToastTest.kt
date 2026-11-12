package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.DesktopFeedbackTone
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test

class FeedbackToastTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `a short message has no close button`() {
        show(EnFeedFlowStrings.linkCopiedSuccess, DesktopFeedbackDuration.Short, DesktopFeedbackTone.Success)
        rule.onNodeWithText(EnFeedFlowStrings.linkCopiedSuccess).assertExists()
        rule.onNodeWithContentDescription(EnFeedFlowStrings.closeButtonContentDescription).assertDoesNotExist()
        rule.onNodeWithText(EnFeedFlowStrings.closeButtonContentDescription).assertDoesNotExist()
    }

    @Test
    fun `a message with an action shows the action and a close button`() {
        show(
            message = EnFeedFlowStrings.browserLaunchError,
            duration = DesktopFeedbackDuration.Long,
            tone = DesktopFeedbackTone.Error,
            actionLabel = EnFeedFlowStrings.retryButton,
        )
        rule.onNodeWithText(EnFeedFlowStrings.retryButton).assertExists()
        rule.onNodeWithContentDescription(EnFeedFlowStrings.closeButtonContentDescription).assertExists()
    }

    @Test
    fun `a long-lived message shows a close button`() {
        show(EnFeedFlowStrings.databaseRecoveredFromCorruption, DesktopFeedbackDuration.Long, DesktopFeedbackTone.Info)
        rule.onNodeWithContentDescription(EnFeedFlowStrings.closeButtonContentDescription).assertExists()
    }

    @Test
    fun `work in progress has no close button`() {
        show(EnFeedFlowStrings.articleReportSending, DesktopFeedbackDuration.Indefinite, DesktopFeedbackTone.Progress)
        rule.onNodeWithText(EnFeedFlowStrings.articleReportSending).assertExists()
        rule.onNodeWithContentDescription(EnFeedFlowStrings.closeButtonContentDescription).assertDoesNotExist()
    }

    private fun show(
        message: String,
        duration: DesktopFeedbackDuration,
        tone: DesktopFeedbackTone,
        actionLabel: String? = null,
    ) {
        val state = DesktopFeedbackState()
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    LaunchedEffect(state) {
                        state.showFeedback(message, actionLabel = actionLabel, duration = duration, tone = tone)
                    }
                    SnackbarHost(state)
                }
            }
        }
        rule.mainClock.advanceTimeBy(ENTER_ANIMATION_MILLIS)
    }

    private companion object {
        const val ENTER_ANIMATION_MILLIS = 500L
    }
}
