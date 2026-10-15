package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.desktop.testing.DesktopLibraryFixture
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import dev.sebastiano.spectre.core.ComposeAutomator
import dev.sebastiano.spectre.core.RobotDriver
import dev.sebastiano.spectre.testing.ComposeAutomatorRule
import dev.sebastiano.spectre.testing.runSpectreTest
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.awt.Dialog
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.awt.Window as AwtWindow

class FeedSidebarNativeNavigationSpectreTest {

    private val fixture = FeedSidebarWindowFixture()

    private val spectre = ComposeAutomatorRule(
        factory = {
            ComposeAutomator.inProcess(
                robotDriver = RobotDriver.synthetic(rootWindow = fixture.awaitWindow()),
            )
        },
    )

    @get:Rule
    val rules = RuleChain.outerRule(FixtureLifecycleRule(fixture)).around(spectre)

    @Test
    fun `canonical sidebar navigation stays in the owner window`(): Unit = runSpectreTest {
        val automator = spectre.automator
        automator.waitForNode(tag = HOME_TAG)
        assertNotNull(automator.findOneByTestTag(HOME_TAG))

        val timeline = automator.findOneByText(EnFeedFlowStrings.drawerTitleTimeline)
            ?: error("Canonical FeedFlow sidebar was not exposed in the owner window")
        automator.focusWindow(timeline)
        automator.click(timeline)
        automator.waitForIdle()

        awaitNativeCondition { fixture.selectedFilter() == FeedFilter.Timeline }
        assertTrue(fixture.isOwnerWindowShowing())
        assertEquals(0, fixture.visibleFeedSourcesDialogs())
        automator.waitForNode(text = EnFeedFlowStrings.drawerTitleFeedSources)
        assertNotNull(automator.findOneByText(EnFeedFlowStrings.drawerTitleFeedSources))
    }

    private suspend fun awaitNativeCondition(condition: () -> Boolean) {
        val deadline = System.nanoTime() + NATIVE_WINDOW_TIMEOUT_NANOS
        while (!condition()) {
            check(System.nanoTime() < deadline) { "Sidebar navigation did not settle in the owner window" }
            delay(WINDOW_POLL_INTERVAL_MS)
        }
    }

    private class FeedSidebarWindowFixture {
        private val windowRef = AtomicReference<AwtWindow?>()
        private val exitRef = AtomicReference<(() -> Unit)?>(null)
        private val selectedFilterRef = AtomicReference<FeedFilter?>(null)
        private lateinit var appThread: Thread

        fun start() {
            appThread = Thread({
                application(exitProcessOnExit = false) {
                    exitRef.set(::exitApplication)
                    Window(
                        onCloseRequest = ::exitApplication,
                        title = "FeedFlow sidebar fixture",
                    ) {
                        windowRef.compareAndSet(null, window)
                        CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                            FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                                val displayState = remember {
                                    mutableStateOf(
                                        emptyHomeDisplayState().copy(
                                            navDrawerState = DesktopLibraryFixture.load().drawerState(),
                                        ),
                                    )
                                }
                                Column(Modifier.fillMaxSize()) {
                                    Text("Canonical FeedFlow home", Modifier.testTag(HOME_TAG))
                                    FeedSidebar(
                                        displayState = displayState.value,
                                        feedManagementActions = emptyManagementActions(),
                                        onFeedFilterSelected = { filter ->
                                            selectedFilterRef.set(filter)
                                            displayState.value = displayState.value.copy(currentFeedFilter = filter)
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }, "feedflow-sidebar-fixture").apply {
                isDaemon = true
                start()
            }
        }

        fun selectedFilter(): FeedFilter? = selectedFilterRef.get()

        fun visibleFeedSourcesDialogs(): Int {
            var count = 0
            SwingUtilities.invokeAndWait {
                count = AwtWindow.getWindows().filterIsInstance<Dialog>().count {
                    it.isShowing && it.title == EnFeedFlowStrings.drawerTitleFeedSources
                }
            }
            return count
        }

        fun isOwnerWindowShowing(): Boolean {
            var showing = false
            SwingUtilities.invokeAndWait { showing = awaitWindow().isShowing }
            return showing
        }

        fun awaitWindow(timeoutMillis: Long = WINDOW_START_TIMEOUT_MS): AwtWindow {
            val deadline = System.currentTimeMillis() + timeoutMillis
            while (System.currentTimeMillis() < deadline) {
                windowRef.get()?.let { return it }
                Thread.sleep(WINDOW_POLL_INTERVAL_MS)
            }
            error("FeedFlow sidebar owner window did not start within $timeoutMillis ms")
        }

        fun stop() {
            SwingUtilities.invokeAndWait { exitRef.get()?.invoke() }
            appThread.join(WINDOW_STOP_TIMEOUT_MS)
        }
    }

    private class FixtureLifecycleRule(
        private val fixture: FeedSidebarWindowFixture,
    ) : TestRule {
        override fun apply(base: Statement, description: Description): Statement = object : Statement() {
            override fun evaluate() {
                fixture.start()
                try {
                    base.evaluate()
                } finally {
                    fixture.stop()
                }
            }
        }
    }

    private companion object {
        const val HOME_TAG = "spectre-canonical-sidebar-home"
        const val NATIVE_WINDOW_TIMEOUT_NANOS = 5_000_000_000L
        const val WINDOW_START_TIMEOUT_MS = 10_000L
        const val WINDOW_POLL_INTERVAL_MS = 25L
        const val WINDOW_STOP_TIMEOUT_MS = 2_000L
    }
}

private fun emptyHomeDisplayState() = HomeDisplayState(
    feedItems = persistentListOf<FeedItem>(),
    navDrawerState = NavDrawerState(),
    unReadCount = 0,
    isUnreadCountHidden = false,
    feedUpdateStatus = NoFeedSourcesStatus,
    feedFontSizes = FeedFontSizes(),
    currentFeedFilter = FeedFilter.Timeline,
    nextFeedDisplayState = NextFeedDisplayState.NextFeedDisplayDisabledState,
    swipeActions = SwipeActions(SwipeActionType.NONE, SwipeActionType.NONE),
    feedLayout = FeedLayout.LIST,
    isGridLayoutEnabled = false,
)

private fun emptyManagementActions() = FeedManagementActions(
    onAddFeedClick = {},
    onFeedFilterSelected = {},
    onEditFeedClick = {},
    onDeleteFeedSourceClick = {},
    onPinFeedClick = {},
    onEditCategoryClick = { _: CategoryId, _: CategoryName -> },
    validateCategoryName = { _: CategoryId?, _: CategoryName -> CategoryNameValidationResult.VALID },
    onDeleteCategoryClick = {},
    onOpenWebsite = {},
)
