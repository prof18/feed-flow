package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.desktop.home.drawer.DesktopFeedManagementToolbar
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.i18n.FeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopFeedManagementToolbarLayoutTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `large labels keep feed management actions readable and usable in compact themes and directions`() {
        val width = mutableStateOf(DIALOG_CONTENT_WIDTH)
        val theme = mutableStateOf(ThemeMode.LIGHT)
        val direction = mutableStateOf(LayoutDirection.Ltr)
        val selectedCount = mutableIntStateOf(0)
        val actions = mutableListOf<String>()

        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides LongEnglishFeedFlowStrings,
                LocalDensity provides Density(density.density, fontScale = 2f),
                LocalLayoutDirection provides direction.value,
            ) {
                FeedFlowTheme(
                    darkTheme = theme.value != ThemeMode.LIGHT,
                    useOledTheme = theme.value == ThemeMode.OLED,
                ) {
                    Box(
                        Modifier
                            .requiredSize(width = width.value, height = DIALOG_CONTENT_HEIGHT)
                            .clipToBounds()
                            .testTag(CONTENT_TAG),
                    ) {
                        DesktopFeedManagementToolbar(
                            selectedCount = selectedCount.intValue,
                            onClear = { actions += "clear" },
                            onMove = { actions += "move" },
                            onDelete = { actions += "delete" },
                        )
                    }
                }
            }
        }

        rule.onNodeWithText(LongEnglishFeedFlowStrings.desktopExpandAllFeeds).assertDoesNotExist()
        rule.onNodeWithText(LongEnglishFeedFlowStrings.desktopCollapseAllFeeds).assertDoesNotExist()
        listOf(
            LongEnglishFeedFlowStrings.desktopClearFeedSelection,
            LongEnglishFeedFlowStrings.desktopMoveSelectedFeeds,
            LongEnglishFeedFlowStrings.desktopDeleteSelectedFeeds,
        ).forEach { rule.onNodeWithText(it).assertIsNotEnabled() }

        rule.runOnIdle { selectedCount.intValue = 2 }
        listOf(
            LongEnglishFeedFlowStrings.desktopClearFeedSelection to "clear",
            LongEnglishFeedFlowStrings.desktopMoveSelectedFeeds to "move",
            LongEnglishFeedFlowStrings.desktopDeleteSelectedFeeds to "delete",
        ).forEach { (label, callback) ->
            rule.onNodeWithText(label).assertIsDisplayed().assertIsEnabled().performClick()
            assertEquals(callback, actions.last())
        }

        listOf(DIALOG_CONTENT_WIDTH, COMPACT_CONTENT_WIDTH).forEach { availableWidth ->
            rule.runOnIdle { width.value = availableWidth }
            listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { layoutDirection ->
                rule.runOnIdle { direction.value = layoutDirection }
                listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.OLED).forEach { themeMode ->
                    rule.runOnIdle { theme.value = themeMode }
                    assertActionLabelsFit(
                        availableWidth = availableWidth,
                        direction = layoutDirection,
                        theme = themeMode,
                    )
                }
            }
        }

        val keyboardActions = listOf(
            LongEnglishFeedFlowStrings.desktopClearFeedSelection to "clear",
            LongEnglishFeedFlowStrings.desktopMoveSelectedFeeds to "move",
            LongEnglishFeedFlowStrings.desktopDeleteSelectedFeeds to "delete",
        )
        rule.runOnIdle { actions.clear() }
        val first = rule.onNodeWithText(keyboardActions.first().first)
        first.requestFocus()
        keyboardActions.forEach { (label, _) ->
            val action = rule.onNodeWithText(label)
            action.assertIsDisplayed().assertIsFocused().performKeyInput { pressKey(Key.Enter) }
            action.performKeyInput { pressKey(Key.Tab) }
        }
        first.assertIsFocused()
        rule.runOnIdle { assertEquals(keyboardActions.map { it.second }, actions) }

        rule.runOnIdle { selectedCount.intValue = 0 }
        keyboardActions.forEach { (label, _) -> rule.onNodeWithText(label).assertIsNotEnabled() }
    }

    private fun assertActionLabelsFit(
        availableWidth: androidx.compose.ui.unit.Dp,
        direction: LayoutDirection,
        theme: ThemeMode,
    ) {
        val contentBounds = rule.onNodeWithTag(CONTENT_TAG).fetchSemanticsNode().boundsInRoot
        listOf(
            LongEnglishFeedFlowStrings.desktopClearFeedSelection,
            LongEnglishFeedFlowStrings.desktopMoveSelectedFeeds,
            LongEnglishFeedFlowStrings.desktopDeleteSelectedFeeds,
        ).forEach { label ->
            val node = rule.onNodeWithText(label)
            node.assertIsDisplayed()
            val bounds = node.fetchSemanticsNode().boundsInRoot
            assertTrue(
                bounds.left >= contentBounds.left && bounds.right <= contentBounds.right &&
                    bounds.top >= contentBounds.top && bounds.bottom <= contentBounds.bottom,
                "$theme $direction at $availableWidth: $label bounds $bounds exceed $contentBounds",
            )
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(
                !layouts.single().hasVisualOverflow,
                "$theme $direction at $availableWidth: $label is clipped",
            )
        }
    }

    private companion object {
        const val CONTENT_TAG = "feed-management-toolbar-content"
        val DIALOG_CONTENT_WIDTH = 448.dp
        val COMPACT_CONTENT_WIDTH = 320.dp
        val DIALOG_CONTENT_HEIGHT = 608.dp
    }
}

private object LongEnglishFeedFlowStrings : FeedFlowStrings by EnFeedFlowStrings {
    override val desktopExpandAllFeeds = "Expand every feed category"
    override val desktopCollapseAllFeeds = "Collapse every feed category"
    override val desktopClearFeedSelection = "Clear the selected feed subscriptions"
    override val desktopMoveSelectedFeeds = "Move selected feeds to another category"
    override val desktopDeleteSelectedFeeds = "Delete selected feed subscriptions"
}
