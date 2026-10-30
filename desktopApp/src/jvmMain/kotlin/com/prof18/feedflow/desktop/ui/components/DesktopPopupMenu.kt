package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.jewel.ui.component.PopupMenu
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.separator
import kotlin.math.roundToInt

@Composable
internal fun DesktopPopupMenu(
    showMenu: Boolean,
    menuPositionInWindow: Offset?,
    menuEntries: ImmutableList<DesktopPopupMenuEntry>,
    closeMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!showMenu || menuPositionInWindow == null) return
    val positionProvider = remember(menuPositionInWindow) { CursorMenuPosition(menuPositionInWindow) }
    PopupMenu(
        popupPositionProvider = positionProvider,
        onDismissRequest = {
            closeMenu()
            true
        },
        modifier = modifier,
    ) {
        menuEntries.forEach { entry ->
            when (entry) {
                DesktopPopupMenuEntry.Divider -> separator()
                is DesktopPopupMenuEntry.Action -> selectableItem(
                    selected = false,
                    onClick = {
                        closeMenu()
                        entry.onClick()
                    },
                ) { Text(entry.text) }
            }
        }
    }
}

private class CursorMenuPosition(private val position: Offset) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        x = position.x.roundToInt().coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
        y = position.y.roundToInt().coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
    )
}

internal sealed interface DesktopPopupMenuEntry {
    data class Action(
        val text: String,
        val onClick: () -> Unit,
        val icon: ImageVector? = null,
    ) : DesktopPopupMenuEntry

    data object Divider : DesktopPopupMenuEntry
}
