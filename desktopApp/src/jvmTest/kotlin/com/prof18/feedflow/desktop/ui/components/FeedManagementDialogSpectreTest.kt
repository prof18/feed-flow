package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
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
import com.prof18.feedflow.desktop.main.DesktopDialogWindowDestination
import com.prof18.feedflow.desktop.main.DesktopDialogWindowNavigator
import com.prof18.feedflow.desktop.main.LocalDesktopDialogWindowNavigator
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
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.awt.Component
import java.awt.Container
import java.awt.Dialog
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.awt.Window as AwtWindow

class FeedManagementDialogSpectreTest {

    private val fixture = FeedManagementWindowFixture()

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
    fun `Escape closes the native feed management window and keeps its owner alive`(): Unit = runSpectreTest {
        val automator = spectre.automator
        automator.waitForNode(tag = HOME_TAG)
        assertNotNull(automator.findOneByTestTag(HOME_TAG))

        openManagement(automator)
        assertTrue(fixture.didLaunchManagement(), fixture.diagnostic(automator))
        assertTrue(fixture.isManagementOpen(), fixture.diagnostic(automator))
        awaitManagementContent()
        assertTrue(fixture.hasDialogContentDescription(EnFeedFlowStrings.addFeed))

        fixture.pressManagementEscape()
        awaitManagementClosed()
        automator.waitForIdle()

        assertFalse(fixture.isManagementOpen())
        automator.waitForNode(tag = HOME_TAG)
        assertFalse(fixture.hasDialogContentDescription(EnFeedFlowStrings.addFeed))
        assertNotNull(automator.findOneByTestTag(HOME_TAG))
        assertTrue(fixture.isOwnerWindowShowing())

        openManagement(automator)
        awaitManagementContent()
        assertTrue(fixture.hasDialogContentDescription(EnFeedFlowStrings.addFeed))
        fixture.pressManagementEscape()
        awaitManagementClosed()
        assertFalse(fixture.isManagementOpen())
        assertTrue(fixture.isOwnerWindowShowing())
    }

    // Spectre 0.7.1 discovers ComposeWindow/ComposePanel but misses ComposeDialog. Keep
    // these assertions against the real dialog using Compose tooling and its AWT input listener.
    private suspend fun awaitManagementContent() {
        awaitNativeCondition {
            fixture.hasDialogText(EnFeedFlowStrings.drawerTitleTimeline) &&
                fixture.hasDialogContentDescription(EnFeedFlowStrings.addFeed)
        }
    }

    private suspend fun awaitManagementClosed() {
        awaitNativeCondition {
            !fixture.isManagementOpen() && !fixture.hasDialogContentDescription(EnFeedFlowStrings.addFeed)
        }
    }

    private suspend fun awaitNativeCondition(condition: () -> Boolean) {
        val deadline = System.nanoTime() + NATIVE_DIALOG_TIMEOUT_NANOS
        while (!condition()) {
            check(System.nanoTime() < deadline) { fixture.diagnostic(spectre.automator) }
            delay(WINDOW_POLL_INTERVAL_MS)
        }
    }

    private suspend fun openManagement(automator: ComposeAutomator) {
        val openButton = automator.findOneByTestTag(OPEN_MANAGEMENT_TAG)
            ?: error("Feed management launcher was not exposed in the owner window")
        automator.focusWindow(openButton)
        automator.click(openButton)
        automator.waitForIdle()
    }

    private class FeedManagementWindowFixture {
        val navigator = DesktopDialogWindowNavigator()
        private val windowRef = AtomicReference<AwtWindow?>()
        private val exitRef = AtomicReference<(() -> Unit)?>(null)
        private val launcherClicks = java.util.concurrent.atomic.AtomicInteger()
        private lateinit var appThread: Thread

        fun start() {
            appThread = Thread({
                application(exitProcessOnExit = false) {
                    exitRef.set(::exitApplication)
                    Window(
                        onCloseRequest = ::exitApplication,
                        title = "FeedFlow Spectre fixture",
                    ) {
                        windowRef.compareAndSet(null, window)
                        CompositionLocalProvider(
                            LocalDesktopDialogWindowNavigator provides navigator,
                            LocalFeedFlowStrings provides EnFeedFlowStrings,
                        ) {
                            FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                                Column {
                                    Text("Home remains open", Modifier.testTag(HOME_TAG))
                                    OutlinedButton(
                                        onClick = {
                                            launcherClicks.incrementAndGet()
                                            navigator.open(DesktopDialogWindowDestination.FeedSources)
                                        },
                                        modifier = Modifier.testTag(OPEN_MANAGEMENT_TAG),
                                    ) {
                                        Text("Open feed management")
                                    }
                                }
                                FeedManagementDialog(
                                    displayState = emptyHomeDisplayState(),
                                    feedManagementActions = emptyManagementActions(),
                                    onFeedSuggestionsClick = {},
                                    onImportExportClick = {},
                                )
                            }
                        }
                    }
                }
            }, "feedflow-spectre-fixture").apply {
                isDaemon = true
                start()
            }
        }

        val ownerWindow: AwtWindow
            get() = awaitWindow()

        fun isManagementOpen(): Boolean {
            var isOpen = false
            SwingUtilities.invokeAndWait {
                isOpen = navigator.isOpen(DesktopDialogWindowDestination.FeedSources)
            }
            return isOpen
        }

        fun didLaunchManagement(): Boolean = launcherClicks.get() == 1

        @OptIn(ExperimentalComposeUiApi::class, ComposeToolingApi::class)
        fun diagnostic(automator: ComposeAutomator): String {
            val windows = AtomicReference("")
            SwingUtilities.invokeAndWait {
                windows.set(
                    AwtWindow.getWindows().joinToString(separator = "\n") { awtWindow ->
                        val title = (awtWindow as? Dialog)?.title.orEmpty()
                        val panels = descendants(awtWindow)
                            .filterIsInstance<ComposePanel>()
                            .joinToString { panel ->
                                "${panel.javaClass.simpleName}(semantics=${panel.semanticsOwners.size})"
                            }
                        "${awtWindow.javaClass.name}(title=$title, showing=${awtWindow.isShowing}, " +
                            "displayable=${awtWindow.isDisplayable}, " +
                            "owner=${awtWindow.owner?.javaClass?.simpleName}, " +
                            "panels=[$panels])"
                    },
                )
            }
            automator.refreshWindows()
            return "navigatorOpen=${isManagementOpen()}, launcherClicks=${launcherClicks.get()}, " +
                "spectreSurfaces=${automator.surfaceIds()}\nAWT windows:\n${windows.get()}"
        }

        @OptIn(ExperimentalComposeUiApi::class, ComposeToolingApi::class)
        private fun dialogMatches(predicate: (SemanticsNode) -> Boolean): Boolean {
            var found = false
            SwingUtilities.invokeAndWait {
                found = AwtWindow.getWindows().filterIsInstance<ComposeDialog>()
                    .filter { it.isShowing && it.title == EnFeedFlowStrings.drawerTitleFeedSources }
                    .flatMap { it.semanticsOwners }
                    .any { owner -> semanticDescendants(owner.rootSemanticsNode).any(predicate) }
            }
            return found
        }

        fun hasDialogText(text: String): Boolean = dialogMatches { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == text } == true
        }

        fun hasDialogContentDescription(description: String): Boolean = dialogMatches { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(description) == true
        }

        fun pressManagementEscape() {
            SwingUtilities.invokeAndWait {
                val window = AwtWindow.getWindows().filterIsInstance<ComposeDialog>().single {
                    it.isShowing && it.title == EnFeedFlowStrings.drawerTitleFeedSources
                }
                val input = descendants(window).single { component ->
                    component.keyListeners.any { it.javaClass.name.contains("ComposeSceneMediator") }
                }
                window.toFront()
                input.requestFocusInWindow()
                val escape = java.awt.event.KeyEvent(
                    input,
                    java.awt.event.KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(),
                    0,
                    java.awt.event.KeyEvent.VK_ESCAPE,
                )
                input.keyListeners.forEach { it.keyPressed(escape) }
                assertTrue(escape.isConsumed)
            }
        }

        private fun semanticDescendants(node: SemanticsNode): Sequence<SemanticsNode> = sequence {
            yield(node)
            node.children.forEach { yieldAll(semanticDescendants(it)) }
        }

        fun isOwnerWindowShowing(): Boolean {
            var isShowing = false
            SwingUtilities.invokeAndWait { isShowing = ownerWindow.isShowing }
            return isShowing
        }

        fun awaitWindow(timeoutMillis: Long = WINDOW_START_TIMEOUT_MS): AwtWindow {
            val deadline = System.currentTimeMillis() + timeoutMillis
            while (System.currentTimeMillis() < deadline) {
                windowRef.get()?.let { return it }
                Thread.sleep(WINDOW_POLL_INTERVAL_MS)
            }
            error("FeedFlow Spectre owner window did not start within $timeoutMillis ms")
        }

        fun stop() {
            SwingUtilities.invokeAndWait { exitRef.get()?.invoke() }
            appThread.join(WINDOW_STOP_TIMEOUT_MS)
        }

        private fun descendants(component: Component): Sequence<Component> = sequence {
            yield(component)
            if (component is Container) {
                component.components.forEach { yieldAll(descendants(it)) }
            }
        }
    }

    private class FixtureLifecycleRule(
        private val fixture: FeedManagementWindowFixture,
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
        const val HOME_TAG = "spectre-feed-management-owner"
        const val OPEN_MANAGEMENT_TAG = "spectre-open-feed-management"
        const val NATIVE_DIALOG_TIMEOUT_NANOS = 5_000_000_000L
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
    onChangeFeedCategoryClick = {},
    onOpenWebsite = {},
)
