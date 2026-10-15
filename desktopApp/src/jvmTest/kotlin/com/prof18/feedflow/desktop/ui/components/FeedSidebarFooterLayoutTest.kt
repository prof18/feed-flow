package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
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
    fun `sidebar options expand and collapse categories without a management window`() {
        val library = DesktopLibraryFixture.load()
        val source = library.sources.first { !it.feedSource.isPinned && it.feedSource.category != null }.feedSource
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false) {
                    Box(Modifier.requiredSize(width = 300.dp, height = 600.dp)) {
                        FeedSidebar(
                            displayState = displayState(library.drawerState()),
                            feedManagementActions = emptyManagementActions {},
                            onFeedFilterSelected = {},
                        )
                    }
                }
            }
        }
        val heading = hasText(EnFeedFlowStrings.drawerTitleFeedSources)
        rule.onNode(hasScrollAction()).performScrollToNode(heading)
        rule.onNode(heading and hasClickAction()).assertDoesNotExist()
        rule.onNodeWithContentDescription(EnFeedFlowStrings.moreOptionsButtonContentDescription).performClick()
        rule.onNodeWithText(EnFeedFlowStrings.desktopCollapseAllFeeds).performClick()
        rule.onNodeWithText(source.title).assertDoesNotExist()
        rule.onNodeWithContentDescription(EnFeedFlowStrings.moreOptionsButtonContentDescription).performClick()
        rule.onNodeWithText(EnFeedFlowStrings.desktopExpandAllFeeds).performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(source.title))
        rule.onNodeWithText(source.title).assertIsDisplayed()
    }

    @Test
    fun `add button opens the add options menu like mobile and routes every option`() {
        val homeDisplayState = displayState(DesktopLibraryFixture.load().drawerState())
        val theme = mutableStateOf(ThemeMode.LIGHT)
        val direction = mutableStateOf(LayoutDirection.Ltr)
        val opened = mutableListOf<String>()
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
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
                            feedManagementActions = emptyManagementActions { opened += "add" }.copy(
                                onFeedSuggestionsClick = { opened += "suggestions" },
                                onImportFeedsClick = { opened += "import" },
                            ),
                            onFeedFilterSelected = {},
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

        assertEquals(1, rule.onAllNodesWithText(EnFeedFlowStrings.drawerTitleFeedSources).fetchSemanticsNodes().size)
        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionsTitle).assertDoesNotExist()
        rule.onNodeWithText(EnFeedFlowStrings.importExportLabel).assertDoesNotExist()

        listOf(
            EnFeedFlowStrings.addFeed to "add",
            EnFeedFlowStrings.feedSuggestionsTitle to "suggestions",
            EnFeedFlowStrings.importFeedButton to "import",
        ).forEach { (label, expected) ->
            rule.onNodeWithContentDescription(EnFeedFlowStrings.addFeed).performClick()
            rule.onNodeWithText(label).assertIsDisplayed().performClick()
            rule.runOnIdle { assertEquals(expected, opened.last()) }
            rule.onNodeWithText(EnFeedFlowStrings.importFeedButton).assertDoesNotExist()
        }
        rule.runOnIdle { assertEquals(listOf("add", "suggestions", "import"), opened) }
    }

    private fun assertLabelsFit(description: String) {
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(EnFeedFlowStrings.drawerTitleFeedSources))
        val sidebar = rule.onNodeWithTag(SIDEBAR_TAG).fetchSemanticsNode().boundsInRoot
        listOf(
            EnFeedFlowStrings.drawerTitleFeedSources,
        ).forEach { label ->
            val matches = rule.onAllNodesWithText(label)
            val nodes = if (label == EnFeedFlowStrings.drawerTitleFeedSources) {
                assertEquals(1, matches.fetchSemanticsNodes().size, "Expected one Feed Sources header")
                listOf(rule.onNodeWithText(label))
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
        onOpenWebsite = {},
    )

    private companion object {
        const val SIDEBAR_TAG = "compact-sidebar"
        val SIDEBAR_WIDTH = 200.dp
        val SIDEBAR_HEIGHT = 600.dp
    }
}
