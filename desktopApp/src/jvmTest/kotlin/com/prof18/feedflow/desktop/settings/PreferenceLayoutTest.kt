package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class PreferenceLayoutTest(private val direction: LayoutDirection, private val fontScale: Float) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `long preference label keeps usable width alongside a longer selected value`() {
        val title = "The preferred background synchronization interval for downloaded articles"
        val value = "Every twelve hours while the application remains open on this computer"
        compose.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides direction,
                LocalDensity provides Density(LocalDensity.current.density, fontScale),
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.requiredSize(width = 280.dp, height = 480.dp).verticalScroll(rememberScrollState())) {
                        SettingsChoice(
                            title = title,
                            choices = persistentListOf(value, "Never", "Hourly", "Daily", "Weekly")
                                .mapIndexed { index, label -> index to label }.toPersistentList(),
                            selected = 0,
                            onSelected = {},
                        )
                    }
                }
            }
        }
        val label = compose.onNodeWithText(title, substring = true)
        assertTrue(label.fetchSemanticsNode().boundsInRoot.width > 0, "The preference label must have readable width")
        val layouts = mutableListOf<TextLayoutResult>()
        label.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse(layouts.single().hasVisualOverflow, "The complete preference label should wrap")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}, font {1}")
        fun parameters(): List<Array<Any>> = LayoutDirection.entries.flatMap { direction ->
            listOf(1f, LARGE_FONT_SCALE).map { scale -> arrayOf(direction, scale) }
        }
        private const val LARGE_FONT_SCALE = 1.5f
    }
}
