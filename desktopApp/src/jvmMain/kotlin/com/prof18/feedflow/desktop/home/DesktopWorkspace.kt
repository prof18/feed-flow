package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import org.jetbrains.jewel.foundation.theme.JewelTheme
import java.awt.Cursor
import kotlin.math.roundToInt

/** Keeps pane content at stable composition sites while width and visibility change. */
@Composable
internal fun DesktopWorkspace(
    layout: DesktopWorkspaceLayout,
    onSidebarResize: (Dp, Boolean) -> Dp,
    onListResize: (Dp, Boolean) -> Dp,
    sidebarContent: @Composable () -> Unit,
    listContent: @Composable () -> Unit,
    readerContent: @Composable () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val paneRequesters = remember { List(WORKSPACE_PANE_COUNT) { FocusRequester() } }
    var focusedPane by remember { mutableIntStateOf(-1) }
    val visiblePanes = listOfNotNull(
        0.takeIf { layout.isSidebarDocked },
        1.takeIf { layout.listWidth > 0.dp },
        2.takeIf { layout.readerWidth > 0.dp },
    )
    fun paneFocus(index: Int): Modifier = Modifier
        .focusRequester(paneRequesters[index])
        .onFocusChanged { if (it.hasFocus) focusedPane = index }
        .focusRestorer()
        .focusGroup()

    Row(
        Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.F6 && visiblePanes.isNotEmpty()) {
                val step = if (event.isShiftPressed) -1 else 1
                val current = visiblePanes.indexOf(focusedPane)
                val next = if (current < 0) 0 else Math.floorMod(current + step, visiblePanes.size)
                paneRequesters[visiblePanes[next]].requestFocus()
                true
            } else {
                false
            }
        },
    ) {
        if (layout.isSidebarDocked) {
            Box(Modifier.width(layout.sidebarWidth).fillMaxHeight().then(paneFocus(0))) { sidebarContent() }
            WorkspaceDivider(
                description = strings.desktopResizeSidebar,
                width = layout.sidebarWidth,
                onResize = onSidebarResize,
            )
        }
        if (layout.listWidth > 0.dp) {
            val modifier = if (layout.readerWidth > 0.dp) Modifier.width(layout.listWidth) else Modifier.weight(1f)
            Box(modifier.fillMaxHeight().then(paneFocus(1))) { listContent() }
        }
        if (layout.listWidth > 0.dp && layout.readerWidth > 0.dp) {
            WorkspaceDivider(
                description = strings.desktopResizeArticleList,
                width = layout.listWidth,
                onResize = onListResize,
            )
        }
        if (layout.readerWidth > 0.dp) {
            Box(Modifier.weight(1f).fillMaxHeight().then(paneFocus(2))) { readerContent() }
        }
    }
}

@Composable
private fun WorkspaceDivider(
    description: String,
    width: Dp,
    onResize: (Dp, Boolean) -> Dp,
) {
    val density = LocalDensity.current
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    val currentWidth by rememberUpdatedState(width)
    val currentOnResize by rememberUpdatedState(onResize)
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    var dragWidth by remember { mutableStateOf(width) }
    val border = JewelTheme.globalColors.borders.normal
    val color = if (focused) JewelTheme.globalColors.borders.focused else border
    val widthDescription = LocalFeedFlowStrings.current.desktopPaneWidth(width.value.roundToInt().toString())
    val isHovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            // Takes no layout space: only a hairline shows, while the drag target overlaps both pane edges.
            .zIndex(1f)
            .layout { measurable, constraints ->
                val targetWidth = workspaceDividerWidth.roundToPx()
                val placeable = measurable.measure(constraints.copy(minWidth = targetWidth, maxWidth = targetWidth))
                layout(0, placeable.height) { placeable.place(-targetWidth / 2, 0) }
            }
            .fillMaxHeight()
            .semantics {
                contentDescription = description
                stateDescription = widthDescription
            }
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                val step = if (event.isShiftPressed) dividerLargeStep else dividerStep
                val delta = when (event.key) {
                    Key.DirectionLeft -> -step * direction
                    Key.DirectionRight -> step * direction
                    else -> null
                }
                if (event.type == KeyEventType.KeyDown && delta != null) {
                    currentOnResize(currentWidth + delta, true)
                    true
                } else {
                    false
                }
            }
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { focusRequester.requestFocus() }
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
            .draggable(
                state = rememberDraggableState { delta ->
                    dragWidth = currentOnResize(dragWidth + with(density) { delta.toDp() } * direction, false)
                },
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    focusRequester.requestFocus()
                    dragWidth = currentWidth
                },
                onDragStopped = { currentOnResize(dragWidth, true) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(1.dp).fillMaxHeight().background(border.copy(alpha = 0.5f)))
        if (focused || isHovered) {
            Box(Modifier.width(2.dp).height(32.dp).background(color))
        }
    }
}

private val dividerStep = 16.dp
private val dividerLargeStep = 32.dp
private const val WORKSPACE_PANE_COUNT = 3
