package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class SettingsCategoryLayoutTest(
    private val direction: LayoutDirection,
    private val fontScale: Float,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `category labels fit and remain selectable at minimum settings width`() {
        compose.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides direction,
                LocalDensity provides Density(LocalDensity.current.density, fontScale),
                LocalFeedFlowStrings provides EnFeedFlowStrings,
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.requiredSize(width = SETTINGS_WIDTH, height = SETTINGS_HEIGHT)) {
                        SettingsContent(
                            initialCategory = DesktopSettingsCategory.GENERAL,
                            modifier = Modifier,
                        ) { category ->
                            Text(
                                text = category.name,
                                modifier = Modifier.testTag(DETAIL_TAG),
                            )
                        }
                    }
                }
            }
        }

        val categories = listOf(
            DesktopSettingsCategory.GENERAL to EnFeedFlowStrings.settingsAppTitle,
            DesktopSettingsCategory.ACCOUNTS to EnFeedFlowStrings.settingsAccounts,
            DesktopSettingsCategory.FEED_LIST to EnFeedFlowStrings.settingsFeedListTitle,
            DesktopSettingsCategory.READING to EnFeedFlowStrings.settingsReadingBehavior,
            DesktopSettingsCategory.FILTERS to EnFeedFlowStrings.settingsFilters,
            DesktopSettingsCategory.SYNC_STORAGE to EnFeedFlowStrings.settingsSyncAndStorage,
            DesktopSettingsCategory.ABOUT to EnFeedFlowStrings.settingsAboutAndSupport,
        )

        categories.forEach { (category, label) ->
            compose.onNodeWithTag(SETTINGS_CATEGORIES_TAG).performScrollToNode(hasText(label))
            val node = compose.onNodeWithText(label, useUnmergedTree = true).assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            assertFalse(layout.hasVisualOverflow, "The full category label '$label' should be visible")
            assertTrue(
                layout.size.height <= node.fetchSemanticsNode().boundsInRoot.height,
                "Category label '$label' should fit vertically inside its text node",
            )

            compose.onNodeWithText(label).performClick()
            compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(category.name)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}, font {1}")
        fun parameters(): List<Array<Any>> = LayoutDirection.entries.flatMap { direction ->
            listOf(1f, LARGE_FONT_SCALE).map { scale -> arrayOf(direction, scale) }
        }

        private const val DETAIL_TAG = "settings-category-detail"
        private const val LARGE_FONT_SCALE = 2f
        private val SETTINGS_WIDTH = 680.dp
        private val SETTINGS_HEIGHT = 520.dp
    }
}
