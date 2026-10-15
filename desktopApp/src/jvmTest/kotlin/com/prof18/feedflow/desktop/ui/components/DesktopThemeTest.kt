package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.styling.SimpleListItemStyle
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DesktopThemeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `feed logo fallback remains visible on an OLED surface`() {
        rule.setContent {
            FeedFlowTheme(darkTheme = true, useOledTheme = true) {
                FeedSourceLogoImage(
                    imageUrl = "",
                    size = 24.dp,
                    modifier = Modifier.testTag("feed-logo-fixture").background(Color.Black),
                )
            }
        }
        val pixels = rule.onNodeWithTag("feed-logo-fixture").captureToImage().toPixelMap()
        val visible = (0 until pixels.width).any { x ->
            (0 until pixels.height).any { y ->
                val color = pixels[x, y]
                color.red > MIN_VISIBLE_CHANNEL && color.green > MIN_VISIBLE_CHANNEL && color.blue > MIN_VISIBLE_CHANNEL
            }
        }
        assertTrue(visible, "The decorative fallback must have contrast against the OLED surface")
    }

    @Test
    fun `mounted content updates text icon selection and focus styles across themes`() {
        val mode = mutableStateOf(ThemeMode.Light)
        var observed: ThemeObservation? = null
        rule.setContent {
            val current = mode.value
            FeedFlowTheme(darkTheme = current == ThemeMode.Dark, useOledTheme = current == ThemeMode.Oled) {
                val identity = remember { Any() }
                val sidebarStyle = rememberDesktopSidebarItemStyle()
                val text = JewelTheme.defaultTextStyle
                val colors = JewelTheme.globalColors
                val observation = ThemeObservation(
                    identity,
                    JewelTheme.isDark,
                    text,
                    colors.text.normal,
                    colors.panelBackground,
                    colors.outlines.focused,
                    sidebarStyle,
                )
                SideEffect { observed = observation }
                OutlinedButton(onClick = {}, modifier = Modifier.testTag("theme-focus-probe")) {
                    Text("Theme typography")
                }
            }
        }
        rule.onNodeWithTag("theme-focus-probe").requestFocus()
        rule.onNodeWithTag("theme-focus-probe").assertIsFocused()
        lateinit var light: ThemeObservation
        rule.runOnIdle {
            light = assertNotNull(observed)
            mode.value = ThemeMode.Dark
        }
        rule.waitForIdle()
        lateinit var dark: ThemeObservation
        rule.runOnIdle {
            dark = assertNotNull(observed)
            assertSame(light.identity, dark.identity)
            assertNotEquals(light.text.color, dark.text.color)
            assertNotEquals(light.iconTint, dark.iconTint)
            assertNotEquals(light.sidebarStyle.colors.backgroundSelected, dark.sidebarStyle.colors.backgroundSelected)
            assertTrue(dark.focusOutline != Color.Unspecified)
            mode.value = ThemeMode.Oled
        }
        rule.waitForIdle()
        rule.onNodeWithTag("theme-focus-probe").assertIsFocused()
        rule.runOnIdle {
            val oled = assertNotNull(observed)
            assertSame(light.identity, oled.identity)
            assertTrue(oled.isDark, "OLED must retain dark control styling even when requested independently")
            assertEquals(dark.text, oled.text)
            assertEquals(dark.iconTint, oled.iconTint)
            assertEquals(dark.focusOutline, oled.focusOutline)
            assertEquals(dark.sidebarStyle, oled.sidebarStyle)
            assertNotEquals(dark.panel, oled.panel)
        }
    }

    @Test
    fun `chrome font and its license are present for offline rendering`() {
        listOf("fonts/IBMPlexSans.ttf", "fonts/OFL.txt").forEach { path ->
            assertNotNull(javaClass.classLoader.getResource(path), "Missing packaged resource $path")
        }
    }

    private companion object {
        const val MIN_VISIBLE_CHANNEL = 0.2f
    }
}

private enum class ThemeMode { Light, Dark, Oled }

private data class ThemeObservation(
    val identity: Any,
    val isDark: Boolean,
    val text: TextStyle,
    val iconTint: Color,
    val panel: Color,
    val focusOutline: Color,
    val sidebarStyle: SimpleListItemStyle,
)
