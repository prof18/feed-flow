package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
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
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.core.model.ThemeMode
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
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedSidebarFooterLayoutTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `large footer labels remain readable and activate their actions in a compact sidebar`() {
        val navigator = DesktopDialogWindowNavigator()
        val homeDisplayState = displayState(DesktopLibraryFixture.load().drawerState())
        val theme = mutableStateOf(ThemeMode.LIGHT)
        val direction = mutableStateOf(LayoutDirection.Ltr)
        var suggestionsOpened = 0
        var importExportOpened = 0
        var addFeedOpened = 0
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalDesktopDialogWindowNavigator provides navigator,
                LocalDensity provides Density(density.density, fontScale = 2f),
                LocalLayoutDirection provides direction.value,
            ) {
                FeedFlowTheme(
                    darkTheme = theme.value != ThemeMode.LIGHT,
                    useOledTheme = theme.value == ThemeMode.OLED,
                ) {
                    Box(
                        Modifier
                            .requiredSize(width = SIDEBAR_WIDTH, height = SIDEBAR_HEIGHT)
                            .clipToBounds()
                            .testTag(SIDEBAR_TAG),
                    ) {
                        FeedSidebar(
                            displayState = homeDisplayState,
                            feedManagementActions = emptyManagementActions { addFeedOpened++ },
                            onFeedFilterSelected = {},
                            onFeedSuggestionsClick = { suggestionsOpened++ },
                            onImportExportClick = { importExportOpened++ },
                        )
                    }
                }
            }
        }

        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { layoutDirection ->
            rule.runOnIdle { direction.value = layoutDirection }
            listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.OLED).forEach { themeMode ->
                rule.runOnIdle { theme.value = themeMode }
                assertLabelsFit("$themeMode $layoutDirection")
            }
        }

        rule.onAllNodesWithText(EnFeedFlowStrings.drawerTitleFeedSources).onLast().performClick()
        assertTrue(navigator.isOpen(DesktopDialogWindowDestination.FeedSources))
        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionsTitle).performClick()
        rule.onNodeWithText(EnFeedFlowStrings.importExportLabel).performClick()
        rule.runOnIdle {
            assertTrue(suggestionsOpened == 1)
            assertTrue(importExportOpened == 1)
        }

        rule.runOnIdle { navigator.close(DesktopDialogWindowDestination.FeedSources) }
        val header = rule.onAllNodesWithText(EnFeedFlowStrings.drawerTitleFeedSources).onFirst()
        val footer = rule.onAllNodesWithText(EnFeedFlowStrings.drawerTitleFeedSources).onLast()
        val suggestions = rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionsTitle)
        val importExport = rule.onNodeWithText(EnFeedFlowStrings.importExportLabel)
        header.requestFocus()
        header.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertTrue(navigator.isOpen(DesktopDialogWindowDestination.FeedSources)) }
        rule.runOnIdle { navigator.close(DesktopDialogWindowDestination.FeedSources) }
        header.performKeyInput { pressKey(Key.Tab) }
        val add = rule.onNodeWithContentDescription(EnFeedFlowStrings.addFeed)
        add.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(1, addFeedOpened) }
        add.performKeyInput { pressKey(Key.Tab) }
        footer.assertIsFocused().performKeyInput { pressKey(Key.Spacebar) }
        rule.runOnIdle { assertTrue(navigator.isOpen(DesktopDialogWindowDestination.FeedSources)) }
        footer.performKeyInput { pressKey(Key.Tab) }
        suggestions.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        suggestions.performKeyInput { pressKey(Key.Tab) }
        importExport.assertIsFocused().performKeyInput { pressKey(Key.Spacebar) }
        importExport.performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.Tab)
            keyUp(Key.ShiftLeft)
        }
        suggestions.assertIsFocused()
        rule.runOnIdle {
            assertEquals(2, suggestionsOpened)
            assertEquals(2, importExportOpened)
        }
    }

    private fun assertLabelsFit(description: String) {
        val sidebar = rule.onNodeWithTag(SIDEBAR_TAG).fetchSemanticsNode().boundsInRoot
        listOf(
            EnFeedFlowStrings.drawerTitleFeedSources,
            EnFeedFlowStrings.feedSuggestionsTitle,
            EnFeedFlowStrings.importExportLabel,
        ).forEach { label ->
            val matches = rule.onAllNodesWithText(label)
            val nodes = if (label == EnFeedFlowStrings.drawerTitleFeedSources) {
                assertEquals(2, matches.fetchSemanticsNodes().size, "Expected header and footer label")
                listOf(matches.onFirst(), matches.onLast())
            } else {
                listOf(rule.onNodeWithText(label))
            }
            nodes.forEach { node ->
                node.assertIsDisplayed()
                val bounds = node.fetchSemanticsNode().boundsInRoot
                assertTrue(
                    bounds.left >= sidebar.left && bounds.right <= sidebar.right &&
                        bounds.top >= sidebar.top && bounds.bottom <= sidebar.bottom,
                    "$description $label bounds $bounds exceed sidebar $sidebar",
                )
                val layouts = mutableListOf<TextLayoutResult>()
                node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertTrue(
                    !layouts.single().hasVisualOverflow,
                    "$description $label is visually clipped at 2x text scale",
                )
            }
        }
    }

    private fun displayState(navDrawerState: com.prof18.feedflow.core.model.NavDrawerState) = HomeDisplayState(
        feedItems = persistentListOf(),
        navDrawerState = navDrawerState,
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

    private fun emptyManagementActions(onAddFeed: () -> Unit) = FeedManagementActions(
        onAddFeedClick = onAddFeed,
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

    private companion object {
        const val SIDEBAR_TAG = "compact-sidebar"
        val SIDEBAR_WIDTH = 200.dp
        val SIDEBAR_HEIGHT = 600.dp
    }
}
