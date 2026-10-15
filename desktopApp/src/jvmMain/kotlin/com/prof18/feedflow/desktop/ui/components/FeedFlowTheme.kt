package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.prof18.feedflow.desktop.ui.rememberDesktopDarkTheme
import org.jetbrains.jewel.foundation.GlobalColors
import org.jetbrains.jewel.foundation.TextColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.dark
import org.jetbrains.jewel.intui.standalone.theme.darkThemeDefinition
import org.jetbrains.jewel.intui.standalone.theme.light
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import org.jetbrains.jewel.ui.ComponentStyling

@Composable
internal fun FeedFlowTheme(
    darkTheme: Boolean = rememberDesktopDarkTheme(),
    useOledTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val effectiveDarkTheme = darkTheme || useOledTheme
    val background = desktopPanelBackground(darkTheme, useOledTheme)
    val sidebarBackground = when {
        useOledTheme -> Color(color = 0xFF101114)
        effectiveDarkTheme -> Color(color = 0xFF2B2D30)
        else -> Color(color = 0xFFF7F8FA)
    }
    val foreground = if (effectiveDarkTheme) Color(color = 0xFFDFE1E5) else Color(color = 0xFF27282E)
    val secondary = if (effectiveDarkTheme) Color(color = 0xFF9DA0A8) else Color(color = 0xFF6C707E)
    val textStyle = DesktopDesignTokens.chromeText.copy(color = foreground)
    val globalColors = if (effectiveDarkTheme) {
        GlobalColors.dark(
            text = TextColors.dark(normal = foreground, info = secondary),
            panelBackground = background,
            toolwindowBackground = sidebarBackground,
        )
    } else {
        GlobalColors.light(
            text = TextColors.light(normal = foreground, info = secondary),
            panelBackground = background,
            toolwindowBackground = sidebarBackground,
        )
    }
    val theme = if (effectiveDarkTheme) {
        JewelTheme.darkThemeDefinition(colors = globalColors, defaultTextStyle = textStyle)
    } else {
        JewelTheme.lightThemeDefinition(colors = globalColors, defaultTextStyle = textStyle)
    }
    IntUiTheme(theme = theme, styling = ComponentStyling) {
        ProvideButtonStyles {
            ProvideSelectionStyles(content)
        }
    }
}

internal fun desktopPanelBackground(darkTheme: Boolean, useOledTheme: Boolean): Color = when {
    useOledTheme -> Color.Black
    darkTheme -> Color(color = 0xFF1E1F22)
    else -> Color.White
}
