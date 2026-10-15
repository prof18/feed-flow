package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class DesktopNoFeedsLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `enlarged setup actions stay readable at compact width in all themes`() {
        assertActionsReadable(LayoutDirection.Ltr)
    }

    @Test
    fun `enlarged RTL setup actions stay readable at compact width in all themes`() {
        assertActionsReadable(LayoutDirection.Rtl)
    }

    private fun assertActionsReadable(direction: LayoutDirection) {
        val theme = mutableStateOf(ThemeMode.LIGHT)
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalLayoutDirection provides direction,
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                FeedFlowTheme(theme.value != ThemeMode.LIGHT, theme.value == ThemeMode.OLED) {
                    Box(Modifier.size(width = 300.dp, height = 240.dp).testTag(CONTENT_TAG)) {
                        NoFeedsContent(
                            onDismissRequest = {},
                            onAddFeedClick = {},
                            onImportExportClick = {},
                            onAccountsClick = {},
                            onFeedSuggestionsClick = {},
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        val labels = listOf(
            EnFeedFlowStrings.addFeed,
            EnFeedFlowStrings.importExportLabel,
            EnFeedFlowStrings.settingsAccounts,
            EnFeedFlowStrings.feedSuggestionsTitle,
        )
        listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.OLED).forEach { mode ->
            rule.runOnIdle { theme.value = mode }
            labels.forEach { label ->
                val node = rule.onNodeWithText(label)
                node.performScrollTo().assertIsDisplayed()
                val layouts = mutableListOf<TextLayoutResult>()
                node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertTrue(!layouts.single().hasVisualOverflow, "$mode $direction $label must not be clipped")
                val content = rule.onNodeWithTag(CONTENT_TAG).fetchSemanticsNode().boundsInRoot
                val bounds = node.fetchSemanticsNode().boundsInRoot
                assertTrue(
                    bounds.left >= content.left && bounds.right <= content.right &&
                        bounds.top >= content.top && bounds.bottom <= content.bottom,
                    "$mode $direction $label: $bounds in $content",
                )
            }
        }
    }

    private companion object {
        const val CONTENT_TAG = "no-feeds-layout-fixture"
    }
}
