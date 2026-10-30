package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.testing.NativeDesktopWindowFixture
import com.prof18.feedflow.desktop.testing.NativeDesktopWindowFixtureRule
import com.prof18.feedflow.desktop.testing.awaitVisibleNode
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import dev.sebastiano.spectre.core.ComposeAutomator
import dev.sebastiano.spectre.core.RobotDriver
import dev.sebastiano.spectre.testing.ComposeAutomatorRule
import dev.sebastiano.spectre.testing.runSpectreTest
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class ModalDrawerSpectreTest {
    private val isOpen = mutableStateOf(true)
    private val drawerOpen = AtomicBoolean(true)
    private val paneClicks = AtomicInteger()
    private val fixture = NativeDesktopWindowFixture {
        CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
            FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                Box(Modifier.size(width = 560.dp, height = 640.dp)) {
                    ModalDrawer(
                        isOpen = isOpen.value,
                        onDismiss = {
                            isOpen.value = false
                            drawerOpen.set(false)
                        },
                        drawerContent = {
                            Box(Modifier.fillMaxSize().testTag(DRAWER_BODY_TAG))
                        },
                        paneContent = {
                            OutlinedButton(
                                onClick = { paneClicks.incrementAndGet() },
                                modifier = Modifier.testTag(PANE_ACTION_TAG),
                            ) {
                                Text(EnFeedFlowStrings.addFeed)
                            }
                        },
                    )
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
    fun `native swipe closes drawer while a short drag and pane interaction do not`() = runSpectreTest {
        val automator = spectre.automator
        val drawerBody = automator.awaitVisibleNode(DRAWER_BODY_TAG)
        automator.focusWindow(drawerBody)
        val center = drawerBody.centerOnScreen

        automator.swipe(
            startX = center.x,
            startY = center.y,
            endX = center.x - SHORT_DRAG_PX,
            endY = center.y,
            duration = SHORT_DRAG_DURATION_MS.milliseconds,
        )
        automator.waitForIdle()
        assertTrue(drawerOpen.get(), "A short drag should not dismiss the modal drawer")
        assertEquals(0, paneClicks.get(), "The short drag must not activate pane content")

        val currentDrawerBody = assertNotNull(automator.findOneByTestTag(DRAWER_BODY_TAG))
        val currentCenter = currentDrawerBody.centerOnScreen
        automator.swipe(
            startX = currentCenter.x + FULL_SWIPE_PX / 2,
            startY = currentCenter.y,
            endX = currentCenter.x - FULL_SWIPE_PX / 2,
            endY = currentCenter.y,
            duration = FULL_SWIPE_DURATION_MS.milliseconds,
        )
        automator.waitForIdle()

        assertTrue(!drawerOpen.get(), "A full left swipe should dismiss the modal drawer")
        assertEquals(0, paneClicks.get(), "A drawer swipe must not activate pane content")
    }

    private companion object {
        const val DRAWER_BODY_TAG = "native-modal-drawer-body"
        const val PANE_ACTION_TAG = "native-modal-pane-action"
        const val SHORT_DRAG_PX = 12
        const val SHORT_DRAG_DURATION_MS = 150L
        const val FULL_SWIPE_PX = 200
        const val FULL_SWIPE_DURATION_MS = 350L
    }
}
