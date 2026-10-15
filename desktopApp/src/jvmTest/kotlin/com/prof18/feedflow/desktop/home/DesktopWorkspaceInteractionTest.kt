package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import kotlinx.coroutines.runBlocking
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopWorkspaceInteractionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `resize retains reader composition and list anchor across compact and wide panes`() {
        val width = mutableStateOf(1024.dp)
        val state = DesktopWorkspaceState(listWidth = 380.dp)
        val list = LazyListState(firstVisibleItemIndex = 12, firstVisibleItemScrollOffset = 34)
        var readerCreations = 0
        var readerScroll: ScrollState? = null
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.requiredSize(width = width.value, height = 600.dp)) {
                        DesktopWorkspace(
                            layout = state.layout(width.value, multiPane = true, readerSelected = true),
                            onSidebarResize = { _, _ -> 0.dp },
                            onListResize = { _, _ -> 0.dp },
                            sidebarContent = { Text("Feeds") },
                            listContent = {
                                LazyColumn(state = list) {
                                    items((0..49).toList()) {
                                        Text("Article $it", modifier = Modifier.heightIn(min = 64.dp))
                                    }
                                }
                            },
                            readerContent = {
                                remember { readerCreations++ }
                                val scroll = rememberScrollState()
                                readerScroll = scroll
                                Column(Modifier.verticalScroll(scroll).testTag("reader")) {
                                    Text("article-42")
                                    repeat(times = 50) { Text("Reader paragraph $it") }
                                }
                            },
                        )
                    }
                }
            }
        }
        rule.onNodeWithTag("reader").assertExists()
        rule.runOnIdle { runBlocking { requireNotNull(readerScroll).scrollTo(value = 80) } }
        for (next in listOf(500.dp, 741.dp, 1463.dp, 1024.dp)) {
            rule.runOnIdle { width.value = next }
            rule.onNodeWithTag("reader").assertExists()
            rule.onNodeWithText("article-42").assertExists()
            rule.runOnIdle {
                assertEquals(1, readerCreations)
                assertEquals(expected = 80, actual = readerScroll?.value)
                assertEquals(expected = 12, actual = list.firstVisibleItemIndex)
                assertEquals(expected = 34, actual = list.firstVisibleItemScrollOffset)
            }
        }
        rule.runOnIdle { state.toggleReaderFullscreen() }
        rule.onNodeWithTag("reader").assertExists()
        rule.runOnIdle { state.toggleReaderFullscreen() }
        rule.runOnIdle { assertEquals(1, readerCreations) }
    }

    @Test
    fun `F6 moves focus between visible panes without activating an article`() {
        val state = DesktopWorkspaceState(listWidth = 380.dp)
        var activations = 0
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.requiredSize(width = 1024.dp, height = 600.dp)) {
                        DesktopWorkspace(
                            layout = state.layout(1024.dp, multiPane = true, readerSelected = true),
                            onSidebarResize = { _, _ -> 0.dp },
                            onListResize = { _, _ -> 0.dp },
                            sidebarContent = {
                                OutlinedButton(onClick = {}, modifier = Modifier.testTag("sidebar")) { Text("Feeds") }
                            },
                            listContent = {
                                OutlinedButton(onClick = { activations++ }, modifier = Modifier.testTag("list")) {
                                    Text("Article")
                                }
                            },
                            readerContent = {
                                OutlinedButton(onClick = {}, modifier = Modifier.testTag("reader")) { Text("Reader") }
                            },
                        )
                    }
                }
            }
        }
        rule.onNodeWithTag("list").requestFocus()
        rule.onNodeWithTag("list").performKeyInput { pressKey(Key.F6) }
        rule.onNodeWithTag("reader").assertIsFocused()
        rule.onNodeWithTag("reader").performKeyInput { pressKey(Key.F6) }
        rule.onNodeWithTag("sidebar").assertIsFocused()
        assertEquals(0, activations)
    }

    @Test
    fun `divider arrows resize and clamp the article list`() {
        val writes = mutableListOf<Float>()
        val state = DesktopWorkspaceState(listWidth = 380.dp, onListWidthChanged = { writes.add(it.value) })
        setWorkspace(state, width = 1024)
        val divider = rule.onNodeWithContentDescription(EnFeedFlowStrings.desktopResizeArticleList)
        divider.requestFocus()
        divider.performKeyInput { pressKey(Key.DirectionRight) }
        rule.runOnIdle { assertEquals(396.dp, state.requestedListWidth) }
        divider.performKeyInput { repeat(times = 3) { pressKey(Key.DirectionRight) } }
        rule.runOnIdle { assertEquals(404.dp, state.requestedListWidth) }
        assertEquals(expected = 404f, actual = writes.last())
        divider.assertIsFocused()
    }

    @Test
    fun `pointer click focuses a separator for keyboard adjustment`() {
        val state = DesktopWorkspaceState(listWidth = 380.dp)
        setWorkspace(state, width = 1024)
        val divider = rule.onNodeWithContentDescription(EnFeedFlowStrings.desktopResizeArticleList)
        divider.performMouseInput { click() }
        divider.assertIsFocused()
        divider.performKeyInput { pressKey(Key.DirectionLeft) }
        rule.runOnIdle { assertEquals(364.dp, state.requestedListWidth) }
    }

    @Test
    fun `pointer divider drag changes width and persists only the finished request`() {
        val writes = mutableListOf<Float>()
        val state = DesktopWorkspaceState(listWidth = 380.dp, onListWidthChanged = { writes.add(it.value) })
        setWorkspace(state, width = 1463)
        val divider = rule.onNodeWithContentDescription(EnFeedFlowStrings.desktopResizeArticleList)
        divider.performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(x = 60f, y = 0f))
            moveBy(Offset(x = 40f, y = 0f))
            release()
        }
        rule.runOnIdle {
            assertTrue(requireNotNull(state.requestedListWidth) > 380.dp)
            assertEquals(listOf(state.requestedListWidth?.value), writes)
        }
    }

    @Test
    fun `reversing a divider drag responds immediately after hitting its limit`() {
        val state = DesktopWorkspaceState(listWidth = 380.dp)
        setWorkspace(state, width = 1024)
        val divider = rule.onNodeWithContentDescription(EnFeedFlowStrings.desktopResizeArticleList)
        divider.performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(x = 200f, y = 0f))
        }
        rule.runOnIdle { assertEquals(404.dp, state.requestedListWidth) }
        rule.onRoot().performMouseInput {
            moveBy(Offset(x = -24f, y = 0f))
            release()
        }
        rule.runOnIdle {
            assertTrue(
                requireNotNull(state.requestedListWidth) < 404.dp,
                "Width after reversing: ${state.requestedListWidth}",
            )
        }
    }

    private fun setWorkspace(state: DesktopWorkspaceState, width: Int) {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.requiredSize(width = width.dp, height = 600.dp)) {
                        DesktopWorkspace(
                            layout = state.layout(width.dp, multiPane = true, readerSelected = true),
                            onSidebarResize = { value, persist ->
                                state.resizeSidebar(value, persist)
                                state.layout(width.dp, multiPane = true).sidebarWidth
                            },
                            onListResize = { value, persist ->
                                state.resizeList(value, width.dp, true, persist)
                                state.layout(width.dp, multiPane = true).listWidth
                            },
                            sidebarContent = { Text("Feeds") },
                            listContent = { Text("Article") },
                            readerContent = { Text("Reader") },
                        )
                    }
                }
            }
        }
    }
}
