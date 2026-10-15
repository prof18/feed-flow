@file:OptIn(
    androidx.compose.ui.test.ExperimentalTestApi::class,
    org.jetbrains.jewel.foundation.ExperimentalJewelApi::class,
)

package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.input.TextFieldValue
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
import com.prof18.feedflow.desktop.addfeed.AddFeedWindow
import com.prof18.feedflow.desktop.editfeed.EditFeedWindow
import com.prof18.feedflow.desktop.main.DesktopDialogWindowDestination
import com.prof18.feedflow.desktop.main.DesktopDialogWindowNavigator
import com.prof18.feedflow.desktop.main.LocalDesktopDialogWindowNavigator
import com.prof18.feedflow.desktop.testing.DesktopLibraryFixture
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.junit.Rule
import org.junit.Test
import java.awt.Component
import java.awt.Container
import java.awt.Dialog
import java.awt.Window
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedSidebarContextMenuEscapeTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `native Escape and close dismiss add feed while its owner stays visible`() {
        assertFeedWindowDismissal(edit = false)
    }

    @Test
    fun `native Escape and close dismiss edit feed while its owner stays visible`() {
        assertFeedWindowDismissal(edit = true)
    }

    private fun assertFeedWindowDismissal(edit: Boolean) {
        val visible = mutableStateOf(true)
        val draft = mutableStateOf(TextFieldValue("Fixture draft"))
        var dismissals = 0
        val dismiss: () -> Unit = {
            visible.value = false
            dismissals++
        }
        val title = if (edit) EnFeedFlowStrings.openFeedSettings else EnFeedFlowStrings.addFeed
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Text("Home remains open", Modifier.testTag(HOME_TAG))
                    if (visible.value) {
                        val body: @androidx.compose.runtime.Composable () -> Unit = {
                            TextField(
                                value = draft.value,
                                onValueChange = { draft.value = it },
                                modifier = Modifier.testTag(FORM_INPUT_TAG),
                            )
                        }
                        if (edit) EditFeedWindow(dismiss, body) else AddFeedWindow(dismiss, body)
                    }
                }
            }
        }
        awaitFeedWindow(title)
        rule.onNodeWithTag(FORM_INPUT_TAG).requestFocus()
        SwingUtilities.invokeAndWait {
            val window = visibleDialog(title)
            val input = descendants(window).single { component ->
                component.keyListeners.any { it.javaClass.name.contains("ComposeSceneMediator") }
            }
            val escape = KeyEvent(input, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE)
            input.keyListeners.forEach { it.keyPressed(escape) }
            assertTrue(escape.isConsumed)
        }
        rule.waitUntil(timeoutMillis = WINDOW_RENDER_TIMEOUT_MS) { !visible.value }
        rule.runOnIdle { assertEquals(1, dismissals) }
        rule.onNodeWithTag(HOME_TAG).assertIsDisplayed()

        rule.runOnIdle { visible.value = true }
        awaitFeedWindow(title)
        SwingUtilities.invokeAndWait {
            val window = visibleDialog(title)
            window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
        }
        rule.waitUntil(timeoutMillis = WINDOW_RENDER_TIMEOUT_MS) { !visible.value }
        rule.runOnIdle {
            assertEquals(2, dismissals)
            assertEquals("Fixture draft", draft.value.text)
        }
        rule.onNodeWithTag(HOME_TAG).assertIsDisplayed()
    }

    private fun awaitFeedWindow(title: String) {
        rule.waitUntilExactlyOneExists(
            androidx.compose.ui.test.hasTestTag(FORM_INPUT_TAG),
            timeoutMillis = WINDOW_RENDER_TIMEOUT_MS,
        )
        rule.waitUntil(timeoutMillis = WINDOW_RENDER_TIMEOUT_MS) {
            var showing = false
            SwingUtilities.invokeAndWait {
                showing = Window.getWindows().filterIsInstance<Dialog>().any { it.isVisible && it.title == title }
            }
            showing
        }
    }

    private fun visibleDialog(title: String): Dialog = Window.getWindows().filterIsInstance<Dialog>().single {
        it.isVisible && it.title == title
    }

    @Test
    fun `Escape closes actual feed management window and leaves home visible`() {
        val navigator = DesktopDialogWindowNavigator()
        var feedFilterSelected = false
        val displayState = emptyHomeDisplayState()
        val managementActions = emptyManagementActions { feedFilterSelected = true }

        rule.setContent {
            CompositionLocalProvider(
                LocalDesktopDialogWindowNavigator provides navigator,
                LocalFeedFlowStrings provides EnFeedFlowStrings,
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Text("Home remains open", Modifier.testTag(HOME_TAG))
                    FeedManagementDialog(
                        displayState = displayState,
                        feedManagementActions = managementActions,
                        onFeedSuggestionsClick = {},
                        onImportExportClick = {},
                    )
                }
            }
        }

        rule.runOnIdle { navigator.open(DesktopDialogWindowDestination.FeedSources) }
        rule.waitUntilExactlyOneExists(
            hasContentDescription(EnFeedFlowStrings.addFeed),
            timeoutMillis = WINDOW_RENDER_TIMEOUT_MS,
        )
        assertTrue(navigator.isOpen(DesktopDialogWindowDestination.FeedSources))

        rule.onNodeWithContentDescription(EnFeedFlowStrings.addFeed)
            .requestFocus()
        SwingUtilities.invokeAndWait {
            val window = Window.getWindows().filterIsInstance<Dialog>().single {
                it.isVisible && it.title == EnFeedFlowStrings.drawerTitleFeedSources
            }
            val input = descendants(window).single { component ->
                component.keyListeners.any { it.javaClass.name.contains("ComposeSceneMediator") }
            }
            val escape = KeyEvent(input, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE)
            // Exercise the actual AWT input listener; scene injection bypasses window preview handlers.
            input.keyListeners.forEach { it.keyPressed(escape) }
            assertTrue(escape.isConsumed)
        }
        rule.waitUntil(timeoutMillis = WINDOW_RENDER_TIMEOUT_MS) {
            !navigator.isOpen(DesktopDialogWindowDestination.FeedSources)
        }
        rule.waitForIdle()

        assertFalse(navigator.isOpen(DesktopDialogWindowDestination.FeedSources))
        assertFalse(feedFilterSelected)
        rule.onNodeWithTag(HOME_TAG).assertIsDisplayed()
    }

    @Test
    fun `Escape dismisses a source menu before closing native management`() {
        val navigator = DesktopDialogWindowNavigator()
        val source = DesktopLibraryFixture.load().sources.first()
        val displayState = emptyHomeDisplayState().copy(
            navDrawerState = NavDrawerState(feedSourcesWithoutCategory = persistentListOf(source)),
        )
        rule.setContent {
            CompositionLocalProvider(
                LocalDesktopDialogWindowNavigator provides navigator,
                LocalFeedFlowStrings provides EnFeedFlowStrings,
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Text("Home remains open", Modifier.testTag(HOME_TAG))
                    FeedManagementDialog(
                        displayState = displayState,
                        feedManagementActions = emptyManagementActions {},
                        onFeedSuggestionsClick = {},
                        onImportExportClick = {},
                    )
                }
            }
        }
        rule.runOnIdle { navigator.open(DesktopDialogWindowDestination.FeedSources) }
        rule.waitUntilExactlyOneExists(
            hasText(source.feedSource.title),
            timeoutMillis = WINDOW_RENDER_TIMEOUT_MS,
        )
        rule.onNodeWithText(source.feedSource.title).requestFocus()
        sendNativeManagementKey(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)
        rule.waitUntilExactlyOneExists(
            hasText(EnFeedFlowStrings.openFeedSettings),
            timeoutMillis = WINDOW_RENDER_TIMEOUT_MS,
        )
        sendNativeManagementKey(KeyEvent.VK_ESCAPE)
        rule.waitUntilDoesNotExist(
            hasText(EnFeedFlowStrings.openFeedSettings),
            timeoutMillis = WINDOW_RENDER_TIMEOUT_MS,
        )
        assertTrue(navigator.isOpen(DesktopDialogWindowDestination.FeedSources))
        rule.onNodeWithText(EnFeedFlowStrings.openFeedSettings).assertDoesNotExist()
        sendNativeManagementKey(KeyEvent.VK_ESCAPE)
        rule.waitForIdle()
        assertFalse(navigator.isOpen(DesktopDialogWindowDestination.FeedSources))
    }

    private fun sendNativeManagementKey(keyCode: Int, modifiers: Int = 0) {
        SwingUtilities.invokeAndWait {
            val window = Window.getWindows().filterIsInstance<Dialog>().single {
                it.isVisible && it.title == EnFeedFlowStrings.drawerTitleFeedSources
            }
            val input = descendants(window).single { component ->
                component.keyListeners.any { it.javaClass.name.contains("ComposeSceneMediator") }
            }
            val event = KeyEvent(input, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifiers, keyCode)
            input.keyListeners.forEach { it.keyPressed(event) }
        }
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) {
            component.components.forEach { yieldAll(descendants(it)) }
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

    private fun emptyManagementActions(onFilterSelected: (FeedFilter) -> Unit) = FeedManagementActions(
        onAddFeedClick = {},
        onFeedFilterSelected = onFilterSelected,
        onEditFeedClick = {},
        onDeleteFeedSourceClick = {},
        onPinFeedClick = {},
        onEditCategoryClick = { _: CategoryId, _: CategoryName -> },
        validateCategoryName = { _: CategoryId?, _: CategoryName -> CategoryNameValidationResult.VALID },
        onDeleteCategoryClick = {},
        onChangeFeedCategoryClick = {},
        onOpenWebsite = {},
    )

    private companion object {
        const val HOME_TAG = "management-dialog-home-owner"
        const val FORM_INPUT_TAG = "native-feed-form-input"
        const val WINDOW_RENDER_TIMEOUT_MS = 5_000L
    }
}
