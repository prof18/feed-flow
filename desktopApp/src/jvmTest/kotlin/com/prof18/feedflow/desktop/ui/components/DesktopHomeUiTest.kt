package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class DesktopHomeUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val entries = listOf(
        SidebarEntry(SidebarKey.Timeline, label = "Timeline"),
        SidebarEntry.feed(id = "a", title = "Feed A"),
        SidebarEntry.feed(id = "b", title = "Feed B"),
        SidebarEntry.feed(id = "c", title = "Feed C"),
    )

    @Test
    fun `arrows change selected row and Enter activates once`() {
        val activations = mutableListOf<SidebarKey>()
        setHost(activeKey = SidebarKey.Feed("a"), onActivate = activations::add)

        rule.onNodeWithTag(SIDEBAR_TAG).requestFocus()
        rule.onNodeWithTag(SIDEBAR_TAG).performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.DirectionDown)
            pressKey(Key.DirectionUp)
        }

        rule.onNodeWithTag(rowTag(SidebarKey.Feed("b"))).assertIsSelected()
        assertEquals(emptyList<SidebarKey>(), activations)

        rule.onNodeWithTag(SIDEBAR_TAG).performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf<SidebarKey>(SidebarKey.Feed("b")), activations)
    }

    @Test
    fun `plain pointer activates and modified pointer selects without activation`() {
        val activations = mutableListOf<SidebarKey>()
        var selection = emptySet<String>()
        setHost(
            activeKey = SidebarKey.Feed("a"),
            onActivate = activations::add,
            onSelectionChanged = { selection = it },
        )

        rule.onNodeWithTag(rowTag(SidebarKey.Feed("c"))).performMouseInput { click() }
        rule.waitForIdle()
        assertEquals(listOf<SidebarKey>(SidebarKey.Feed("c")), activations)
        rule.onNodeWithTag(rowTag(SidebarKey.Feed("c"))).assertIsSelected()

        val modifiedFeed = rule.onNodeWithTag(rowTag(SidebarKey.Feed("b")))
        modifiedFeed.performKeyInput { keyDown(Key.ShiftLeft) }
        try {
            modifiedFeed.performMouseInput { click() }
        } finally {
            modifiedFeed.performKeyInput { keyUp(Key.ShiftLeft) }
        }
        rule.waitForIdle()
        assertEquals(listOf<SidebarKey>(SidebarKey.Feed("c")), activations)
        assertEquals(setOf("b", "c"), selection)
    }

    @Test
    fun `Shift pointer and Shift Enter select the full visible source range`() {
        val activations = mutableListOf<SidebarKey>()
        var selection = emptySet<String>()
        setHost(
            activeKey = SidebarKey.Feed("a"),
            onActivate = activations::add,
            onSelectionChanged = { selection = it },
        )
        val last = rule.onNodeWithTag(rowTag(SidebarKey.Feed("c")))
        last.performKeyInput { keyDown(Key.ShiftLeft) }
        last.performMouseInput { click() }
        last.performKeyInput { keyUp(Key.ShiftLeft) }
        assertEquals(setOf("a", "b", "c"), selection)
        rule.onNodeWithTag(SIDEBAR_TAG).requestFocus()
        rule.onNodeWithTag(SIDEBAR_TAG).performKeyInput {
            pressKey(Key.DirectionUp)
            keyDown(Key.ShiftLeft)
            pressKey(Key.Enter)
            keyUp(Key.ShiftLeft)
        }
        assertEquals(setOf("a", "b"), selection)
        assertEquals(emptyList<SidebarKey>(), activations)
    }

    @Test
    fun `Shift arrows extend the sidebar source range without activation`() {
        var selection = emptySet<String>()
        val activations = mutableListOf<SidebarKey>()
        setHost(
            activeKey = SidebarKey.Feed("a"),
            onActivate = activations::add,
            onSelectionChanged = { selection = it },
        )
        rule.onNodeWithTag(SIDEBAR_TAG).requestFocus()
        rule.onNodeWithTag(SIDEBAR_TAG).performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.DirectionDown)
            keyUp(Key.ShiftLeft)
        }
        assertEquals(setOf("a", "b"), selection)
        assertEquals(emptyList<SidebarKey>(), activations)
    }

    @Test
    fun `category trailing action consumes pointer and Enter without activating category`() {
        val categoryKey = SidebarKey.Category("news")
        val activations = mutableListOf<SidebarKey>()
        var expansionCount = 0
        setHost(
            activeKey = SidebarKey.Timeline,
            onActivate = activations::add,
            sidebarEntries = listOf(
                SidebarEntry.category("news", "News", childCount = 1),
                SidebarEntry.feed("news-feed", "News Feed"),
            ),
            trailingContent = { key ->
                if (key == categoryKey) {
                    IconButton(
                        onClick = { expansionCount += 1 },
                        modifier = Modifier.testTag(EXPAND_SECTION_TAG),
                    ) {
                        Text("Expand")
                    }
                }
            },
            onExpansionChange = { key, _ ->
                if (key == categoryKey) expansionCount += 1
            },
        )

        val expandButton = rule.onNodeWithTag(EXPAND_SECTION_TAG)
        expandButton.performMouseInput { click() }
        rule.waitForIdle()
        assertEquals(1, expansionCount)
        assertEquals(emptyList<SidebarKey>(), activations)

        rule.onNodeWithTag(SIDEBAR_TAG).requestFocus()
        rule.onNodeWithTag(SIDEBAR_TAG).performKeyInput { pressKey(Key.DirectionRight) }
        rule.waitForIdle()
        assertEquals(2, expansionCount)
        assertEquals(emptyList<SidebarKey>(), activations)

        rule.onNodeWithTag(SIDEBAR_TAG).performKeyInput { pressKey(Key.DirectionLeft) }
        rule.waitForIdle()
        assertEquals(expected = 3, actual = expansionCount)
        assertEquals(emptyList<SidebarKey>(), activations)
    }

    @Test
    fun `Enter activates the selected category`() {
        val categoryKey = SidebarKey.Category("news")
        val activations = mutableListOf<SidebarKey>()
        setHost(
            activeKey = categoryKey,
            onActivate = activations::add,
            sidebarEntries = listOf(
                SidebarEntry.category("news", "News", childCount = 1),
                SidebarEntry.feed("news-feed", "News Feed"),
            ),
        )

        rule.onNodeWithTag(SIDEBAR_TAG).requestFocus()
        rule.onNodeWithTag(SIDEBAR_TAG).performKeyInput { pressKey(Key.Enter) }

        assertEquals(listOf<SidebarKey>(categoryKey), activations)
    }

    @Test
    fun `Tab traversal enters and exits the Jewel sidebar`() {
        setHost(activeKey = SidebarKey.Feed("a"), onActivate = {})

        rule.onNodeWithTag(BEFORE_TAG).requestFocus()
        rule.onNodeWithTag(BEFORE_TAG).performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag(SIDEBAR_TAG).assertIsFocused()

        rule.onNodeWithTag(SIDEBAR_TAG).performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag(AFTER_TAG).assertIsFocused()

        rule.onNodeWithTag(AFTER_TAG).performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.Tab)
            keyUp(Key.ShiftLeft)
        }
        rule.onNodeWithTag(SIDEBAR_TAG).assertIsFocused()
    }

    @Test
    fun `Jewel theme changes preserve sidebar selection and activation`() {
        val activations = mutableListOf<SidebarKey>()
        var darkTheme by mutableStateOf(false)
        setHost(
            activeKey = SidebarKey.Feed("a"),
            onActivate = activations::add,
            isDark = { darkTheme },
            showThemeProbe = true,
        )

        rule.onNodeWithTag(SIDEBAR_TAG).requestFocus()
        rule.onNodeWithTag(SIDEBAR_TAG).performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag(rowTag(SidebarKey.Feed("b"))).assertIsSelected()

        rule.runOnIdle { darkTheme = true }
        rule.onNodeWithTag(THEME_TAG).assertTextEquals("true")
        rule.onNodeWithTag(rowTag(SidebarKey.Feed("b"))).assertIsSelected()
        rule.onNodeWithTag(SIDEBAR_TAG).performKeyInput { pressKey(Key.Enter) }

        assertEquals(listOf<SidebarKey>(SidebarKey.Feed("b")), activations)
    }

    private fun setHost(
        activeKey: SidebarKey,
        onActivate: (SidebarKey) -> Unit,
        sidebarEntries: List<SidebarEntry> = entries,
        onSelectionChanged: (Set<String>) -> Unit = {},
        isDark: () -> Boolean = { false },
        showThemeProbe: Boolean = false,
        trailingContent: @Composable (SidebarKey) -> Unit = {},
        onExpansionChange: (SidebarKey, Boolean) -> Unit = { _, _ -> },
    ) {
        rule.setContent {
            FeedFlowTheme(darkTheme = isDark(), useOledTheme = false) {
                Column {
                    DefaultButton(
                        onClick = {},
                        modifier = Modifier.testTag(BEFORE_TAG),
                    ) {
                        Text("Before")
                    }
                    SidebarList(
                        entries = sidebarEntries.toImmutableList(),
                        activeKey = activeKey,
                        onActivate = onActivate,
                        modifier = Modifier.testTag(SIDEBAR_TAG),
                        onSelectionChanged = onSelectionChanged,
                        onExpansionChange = onExpansionChange,
                        trailingContent = trailingContent,
                    )
                    DefaultButton(
                        onClick = {},
                        modifier = Modifier.testTag(AFTER_TAG),
                    ) {
                        Text("After")
                    }
                    if (showThemeProbe) {
                        Text(JewelTheme.isDark.toString(), Modifier.testTag(THEME_TAG))
                    }
                }
            }
        }
    }

    private fun rowTag(key: SidebarKey): String = "sidebar-$key"

    private companion object {
        const val SIDEBAR_TAG = "sidebar-list"
        const val BEFORE_TAG = "sidebar-before"
        const val AFTER_TAG = "sidebar-after"
        const val THEME_TAG = "theme-is-dark"
        const val EXPAND_SECTION_TAG = "expand-section"
    }
}
