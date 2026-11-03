package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.jewel.foundation.lazy.SingleSelectionLazyColumn
import org.jetbrains.jewel.foundation.lazy.items
import org.jetbrains.jewel.foundation.lazy.rememberSingleSelectionLazyListState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.ListItemState
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.theme.simpleListItemStyle

/**
 * The inline counterpart of [DesktopChoiceDropdown]: the options stay on screen and the list scrolls, so a dialog
 * whose whole job is picking one of them never has to open a popup taller than its own window.
 */
@Composable
internal fun <T : Any> DesktopChoiceList(
    title: String,
    choices: ImmutableList<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberSingleSelectionLazyListState(initialSelectedKey = selected)
    val latestOnSelected by rememberUpdatedState(onSelected)
    SideEffect(selected) {
        if (listState.selectedKeys.singleOrNull() != selected) {
            listState.selectedKeys = setOf(selected)
        }
    }
    // Arrow keys move the list's own selection, so the caller learns about them from here rather than from a click.
    SideEffect(listState.selectedKeys) {
        val navigated = listState.selectedKeys.firstOrNull()
        choices.firstOrNull { it.first == navigated }?.first?.let { choice ->
            if (choice != selected) latestOnSelected(choice)
        }
    }
    val shape = remember { RoundedCornerShape(DesktopDesignTokens.selectionCornerSize) }
    val border = JewelTheme.globalColors.text.normal
        .copy(alpha = if (JewelTheme.isDark) BorderAlphaDark else BorderAlphaLight)
        .compositeOver(JewelTheme.globalColors.panelBackground)
    SingleSelectionLazyColumn(
        state = listState,
        modifier = modifier
            .border(width = 1.dp, color = border, shape = shape)
            .clip(shape)
            .semantics { contentDescription = title }
            .padding(DesktopDesignTokens.spaceSmall),
    ) {
        items(items = choices, key = { it.first }) { (key, label) ->
            ChoiceListRow(
                label = label,
                state = ListItemState(isSelected = isSelected, isActive = isActive),
                selected = isSelected,
                modifier = Modifier.fillMaxWidth().clickable { latestOnSelected(key) },
            )
        }
    }
}

@Composable
private fun ChoiceListRow(
    label: String,
    state: ListItemState,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val itemStyle = JewelTheme.simpleListItemStyle
    val color = itemStyle.colors.contentFor(state).value.takeOrElse { JewelTheme.globalColors.text.normal }
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) { this.selected = selected }
            .heightIn(min = DesktopDesignTokens.settingsRowMinHeight)
            .background(
                itemStyle.colors.backgroundFor(state).value,
                RoundedCornerShape(DesktopDesignTokens.buttonCornerSize),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceLarge),
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(Modifier.size(16.dp)) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = color)
            }
        }
    }
}

private const val BorderAlphaDark = 0.12f
private const val BorderAlphaLight = 0.09f
