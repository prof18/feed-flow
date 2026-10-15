package com.prof18.feedflow.desktop.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.prof18.feedflow.desktop.ui.components.DesktopSnackbarHost
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopFeedbackHostTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `action completes feedback through the selected desktop renderer`() {
        val state = DesktopFeedbackState()
        var result: DesktopFeedbackResult? = null
        rule.setContent {
            DesktopUiEnvironment(darkTheme = false, currentLanguageTag = "en") {
                LaunchedEffect(state) {
                    result = state.showFeedback(
                        EnFeedFlowStrings.browserLaunchError,
                        actionLabel = EnFeedFlowStrings.retryButton,
                    )
                }
                DesktopSnackbarHost(state)
            }
        }
        rule.onNodeWithText(EnFeedFlowStrings.retryButton).performClick()
        rule.onNodeWithText(EnFeedFlowStrings.browserLaunchError).assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(DesktopFeedbackResult.ActionPerformed, result)
            assertNull(state.currentFeedback)
        }
    }

    @Test
    fun `temporarily removing the host keeps its callers message for return`() {
        val state = DesktopFeedbackState()
        val visible = mutableStateOf(true)
        var result: DesktopFeedbackResult? = null
        rule.setContent {
            DesktopUiEnvironment(darkTheme = true, currentLanguageTag = "en") {
                LaunchedEffect(state) {
                    result = state.showFeedback(
                        EnFeedFlowStrings.browserLaunchError,
                        actionLabel = EnFeedFlowStrings.retryButton,
                    )
                }
                if (visible.value) DesktopSnackbarHost(state)
            }
        }
        rule.onNodeWithText(EnFeedFlowStrings.browserLaunchError).assertExists()
        rule.runOnIdle { visible.value = false }
        rule.onNodeWithText(EnFeedFlowStrings.browserLaunchError).assertDoesNotExist()
        rule.runOnIdle {
            assertNull(result)
            assertEquals(EnFeedFlowStrings.browserLaunchError, state.currentFeedback?.message)
            visible.value = true
        }
        rule.onNodeWithText(EnFeedFlowStrings.retryButton).performClick()
        rule.runOnIdle { assertEquals(DesktopFeedbackResult.ActionPerformed, result) }
    }

    @Test
    fun `canceling the visible caller lets the renderer show the next message`() {
        val state = DesktopFeedbackState()
        var first: Job? = null
        rule.setContent {
            DesktopUiEnvironment(darkTheme = true, useOledTheme = true, currentLanguageTag = "en") {
                val scope = rememberCoroutineScope()
                LaunchedEffect(state) {
                    first = scope.launch {
                        state.showFeedback(
                            EnFeedFlowStrings.browserLaunchError,
                            duration = DesktopFeedbackDuration.Indefinite,
                        )
                    }
                    scope.launch {
                        state.showFeedback(
                            EnFeedFlowStrings.linkCopiedSuccess,
                            actionLabel = EnFeedFlowStrings.retryButton,
                        )
                    }
                }
                DesktopSnackbarHost(state)
            }
        }
        rule.onNodeWithText(EnFeedFlowStrings.browserLaunchError).assertExists()
        rule.runOnIdle { requireNotNull(first).cancel() }
        rule.onNodeWithText(EnFeedFlowStrings.linkCopiedSuccess).assertExists()
        rule.onNodeWithText(EnFeedFlowStrings.retryButton).performClick()
        rule.runOnIdle { assertNull(state.currentFeedback) }
    }
}
