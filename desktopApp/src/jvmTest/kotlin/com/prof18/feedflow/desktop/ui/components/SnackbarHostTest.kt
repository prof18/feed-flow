package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.testing.NativeDesktopWindowFixture
import com.prof18.feedflow.desktop.testing.NativeDesktopWindowFixtureRule
import com.prof18.feedflow.desktop.testing.awaitVisibleNode
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import dev.sebastiano.spectre.core.ComposeAutomator
import dev.sebastiano.spectre.core.RobotDriver
import dev.sebastiano.spectre.testing.ComposeAutomatorRule
import dev.sebastiano.spectre.testing.runSpectreTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import javax.swing.SwingUtilities
import kotlin.test.assertNull

class SnackbarHostTest {
    private lateinit var state: DesktopFeedbackState
    private val fixture = NativeDesktopWindowFixture {
        CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
            FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                state = remember { DesktopFeedbackState() }
                LaunchedEffect(state) {
                    state.showFeedback(
                        message = EnFeedFlowStrings.feedAddedMessageWithoutName,
                        duration = DesktopFeedbackDuration.Indefinite,
                    )
                }
                Box(Modifier.width(500.dp)) {
                    SnackbarHost(state, Modifier.testTag(FEEDBACK_TAG))
                }
            }
        }
    }
    private val spectre = ComposeAutomatorRule(
        factory = {
            ComposeAutomator.inProcess(
                robotDriver = RobotDriver.synthetic(rootWindow = fixture.awaitWindow()),
            )
        },
    )

    @get:Rule
    val rules = RuleChain.outerRule(NativeDesktopWindowFixtureRule(fixture)).around(spectre)

    @Test
    fun `swiping feedback dismisses it and releases the pending message`() = runSpectreTest {
        val automator = spectre.automator
        val feedback = automator.awaitVisibleNode(FEEDBACK_TAG)
        automator.focusWindow(feedback)
        val center = feedback.centerOnScreen
        val halfWidth = feedback.boundsOnScreen.width / SWIPE_WIDTH_DIVISOR
        automator.swipe(
            startX = center.x - halfWidth,
            startY = center.y,
            endX = center.x + halfWidth,
            endY = center.y,
        )
        automator.waitUntilGone(tag = FEEDBACK_TAG)
        SwingUtilities.invokeAndWait { assertNull(state.currentFeedback) }
    }

    private companion object {
        const val FEEDBACK_TAG = "feedback"
        const val SWIPE_WIDTH_DIVISOR = 3
    }
}
