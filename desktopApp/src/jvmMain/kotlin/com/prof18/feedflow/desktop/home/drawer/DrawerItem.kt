package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

@Composable
internal fun DrawerItem(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    visualStyle: DrawerItemVisualStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val background = sidebarRowBackground(
        selected = selected,
        highlighted = isHovered || isFocused,
        selectedColor = visualStyle.selectedContainerColor,
    )
    val content = if (selected) {
        visualStyle.selectedContentColor ?: JewelTheme.globalColors.text.normal
    } else {
        JewelTheme.globalColors.text.normal
    }
    Row(
        modifier = modifier
            .padding(vertical = visualStyle.itemVerticalPadding)
            .heightIn(min = visualStyle.itemMinHeight)
            .fillMaxWidth()
            .clip(visualStyle.itemShape)
            .background(background)
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
            androidx.compose.runtime.CompositionLocalProvider(
                org.jetbrains.jewel.foundation.theme.LocalContentColor provides content,
            ) { icon() }
        }
        Text(label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (badge != null) {
            Text(badge, style = DesktopDesignTokens.metadataText, color = JewelTheme.globalColors.text.info)
        }
    }
}
