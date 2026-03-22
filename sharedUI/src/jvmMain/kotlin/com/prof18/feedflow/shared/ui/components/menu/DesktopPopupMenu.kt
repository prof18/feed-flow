package com.prof18.feedflow.shared.ui.components.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

@Composable
fun DesktopPopupMenu(
    showMenu: Boolean,
    menuPositionInWindow: Offset?,
    menuEntries: ImmutableList<DesktopPopupMenuEntry>,
    closeMenu: () -> Unit,
) {
    if (!showMenu || menuPositionInWindow == null) return

    val menuShape = MaterialTheme.shapes.medium
    val menuBackgroundColor = MaterialTheme.colorScheme.surface
    val menuBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    val actionEntries = menuEntries.filterIsInstance<DesktopPopupMenuEntry.Action>()
    val focusRequesters = remember(actionEntries.size) {
        List(actionEntries.size) { FocusRequester() }
    }
    val menuFocusRequester = remember { FocusRequester() }
    var selectedIndex by remember(actionEntries.size) { mutableIntStateOf(NO_SELECTED_INDEX) }

    Popup(
        popupPositionProvider = CursorPopupPositionProvider(menuPositionInWindow),
        onDismissRequest = closeMenu,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 240.dp, max = 280.dp),
            shape = menuShape,
            color = menuBackgroundColor,
            shadowElevation = 16.dp,
            border = BorderStroke(
                width = 1.dp,
                color = menuBorderColor,
            ),
        ) {
            LaunchedEffect(showMenu, actionEntries.size) {
                if (showMenu) {
                    selectedIndex = NO_SELECTED_INDEX
                    menuFocusRequester.requestFocus()
                }
            }

            Column(
                modifier = Modifier
                    .focusRequester(menuFocusRequester)
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        handleDesktopPopupMenuNavigation(
                            event = event,
                            actionEntriesCount = actionEntries.size,
                            selectedIndex = selectedIndex,
                            onSelectedIndexChange = { selectedIndex = it },
                            focusRequesters = focusRequesters,
                            closeMenu = closeMenu,
                        )
                    },
            ) {
                var actionIndex = 0
                menuEntries.forEach { entry ->
                    when (entry) {
                        DesktopPopupMenuEntry.Divider -> DesktopPopupMenuDivider()
                        is DesktopPopupMenuEntry.Action -> {
                            val currentIndex = actionIndex
                            DesktopPopupMenuItem(
                                text = entry.text,
                                icon = entry.icon,
                                onClick = entry.onClick,
                                focusRequester = focusRequesters[currentIndex],
                                isSelected = selectedIndex == currentIndex,
                                onSelected = { selectedIndex = currentIndex },
                            )
                            actionIndex += 1
                        }
                    }
                }
            }
        }
    }
}

sealed interface DesktopPopupMenuEntry {
    data class Action(
        val text: String,
        val onClick: () -> Unit,
        val icon: ImageVector? = null,
    ) : DesktopPopupMenuEntry

    data object Divider : DesktopPopupMenuEntry
}

private class CursorPopupPositionProvider(
    private val windowPosition: Offset,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(
            x = windowPosition.x.roundToInt().coerceIn(0, maxX),
            y = windowPosition.y.roundToInt().coerceIn(0, maxY),
        )
    }
}

@Composable
private fun DesktopPopupMenuItem(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    isSelected: Boolean,
    onSelected: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val latestOnSelected by rememberUpdatedState(onSelected)
    val itemShape = MaterialTheme.shapes.small

    LaunchedEffect(isHovered) {
        if (isHovered) {
            latestOnSelected()
        }
    }

    val itemBackground = if (isSelected) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .clip(itemShape)
            .background(itemBackground)
            .onFocusChanged {
                if (it.isFocused) {
                    onSelected()
                }
            }
            .hoverable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                when (event.key) {
                    Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                        onClick()
                        true
                    }

                    else -> false
                }
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DesktopPopupMenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
    )
}

private fun handleDesktopPopupMenuNavigation(
    event: KeyEvent,
    actionEntriesCount: Int,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    focusRequesters: List<FocusRequester>,
    closeMenu: () -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false

    return when (event.key) {
        Key.DirectionDown -> {
            if (actionEntriesCount == 0) return true
            val nextIndex = when (selectedIndex) {
                NO_SELECTED_INDEX -> 0
                else -> (selectedIndex + 1).coerceAtMost(actionEntriesCount - 1)
            }
            onSelectedIndexChange(nextIndex)
            focusRequesters[nextIndex].requestFocus()
            true
        }

        Key.DirectionUp -> {
            if (actionEntriesCount == 0) return true
            val previousIndex = when (selectedIndex) {
                NO_SELECTED_INDEX -> 0
                else -> (selectedIndex - 1).coerceAtLeast(0)
            }
            onSelectedIndexChange(previousIndex)
            focusRequesters[previousIndex].requestFocus()
            true
        }

        Key.Escape -> {
            closeMenu()
            true
        }

        else -> false
    }
}

private const val NO_SELECTED_INDEX = -1
