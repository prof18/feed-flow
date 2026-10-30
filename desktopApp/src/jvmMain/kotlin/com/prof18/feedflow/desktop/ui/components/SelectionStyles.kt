package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import org.jetbrains.jewel.ui.component.styling.LocalMenuStyle
import org.jetbrains.jewel.ui.component.styling.LocalSimpleListItemStyleStyle
import org.jetbrains.jewel.ui.component.styling.MenuItemMetrics
import org.jetbrains.jewel.ui.component.styling.MenuMetrics
import org.jetbrains.jewel.ui.component.styling.MenuStyle
import org.jetbrains.jewel.ui.component.styling.SimpleListItemMetrics
import org.jetbrains.jewel.ui.component.styling.SimpleListItemStyle

@Composable
internal fun ProvideSelectionStyles(content: @Composable () -> Unit) {
    val listStyle = LocalSimpleListItemStyleStyle.current
    val menuStyle = LocalMenuStyle.current
    CompositionLocalProvider(
        LocalSimpleListItemStyleStyle provides remember(listStyle) { listStyle.withRoundedSelectionCorners() },
        LocalMenuStyle provides remember(menuStyle) { menuStyle.withRoundedSelectionCorners() },
        content = content,
    )
}

internal fun SimpleListItemStyle.withRoundedSelectionCorners(): SimpleListItemStyle = SimpleListItemStyle(
    colors = colors,
    metrics = SimpleListItemMetrics(
        innerPadding = metrics.innerPadding,
        outerPadding = metrics.outerPadding,
        selectionBackgroundCornerSize = CornerSize(DesktopDesignTokens.selectionCornerSize),
        iconTextGap = metrics.iconTextGap,
    ),
)

private fun MenuStyle.withRoundedSelectionCorners(): MenuStyle = MenuStyle(
    isDark = isDark,
    colors = colors,
    icons = icons,
    metrics = MenuMetrics(
        cornerSize = metrics.cornerSize,
        menuMargin = metrics.menuMargin,
        contentPadding = metrics.contentPadding,
        offset = metrics.offset,
        shadowSize = metrics.shadowSize,
        borderWidth = metrics.borderWidth,
        submenuMetrics = metrics.submenuMetrics,
        itemMetrics = MenuItemMetrics(
            selectionCornerSize = CornerSize(DesktopDesignTokens.selectionCornerSize),
            outerPadding = metrics.itemMetrics.outerPadding,
            contentPadding = metrics.itemMetrics.contentPadding,
            separatorPadding = metrics.itemMetrics.separatorPadding,
            keybindingsPadding = metrics.itemMetrics.keybindingsPadding,
            separatorThickness = metrics.itemMetrics.separatorThickness,
            separatorHeight = metrics.itemMetrics.separatorHeight,
            iconSize = metrics.itemMetrics.iconSize,
            minHeight = metrics.itemMetrics.minHeight,
        ),
    ),
)
