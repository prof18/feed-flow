package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/**
 * A resizable sidebar layout that allows the user to drag a divider to adjust the sidebar width.
 * The width is stored as a fraction of the total available width (0.0 to 1.0).
 *
 * @param sidebarWidthFraction Current sidebar width as a fraction of total width (e.g., 0.25 = 25%)
 * @param onSidebarWidthChanged Callback when the user drags to change the width
 * @param minWidthFraction Minimum allowed width fraction
 * @param maxWidthFraction Maximum allowed width fraction
 * @param sidebarContent Content to display in the sidebar
 * @param mainContent Content to display in the main area
 */
@Composable
fun ResizableSidebar(
    sidebarWidthFraction: Float,
    onSidebarWidthChanged: (Float) -> Unit,
    minWidthFraction: Float,
    maxWidthFraction: Float,
    sidebarContent: @Composable () -> Unit,
    mainContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    resizePointerIcon: PointerIcon = PointerIcon.Default,
) {
    var totalWidth by remember { mutableFloatStateOf(0f) }
    var currentFraction by remember(sidebarWidthFraction) { mutableFloatStateOf(sidebarWidthFraction) }

    Layout(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                totalWidth = size.width.toFloat()
            },
        content = {
            Box(Modifier.layoutId("sidebar")) { sidebarContent() }
            DragHandle(
                onDrag = { dragAmount ->
                    if (totalWidth > 0) {
                        val deltaFraction = dragAmount / totalWidth
                        val newFraction = (currentFraction + deltaFraction)
                            .coerceIn(minWidthFraction, maxWidthFraction)
                        currentFraction = newFraction
                    }
                },
                onDragEnd = {
                    onSidebarWidthChanged(currentFraction)
                },
                resizePointerIcon = resizePointerIcon,
                modifier = Modifier.layoutId("handle")
            )
            Box(Modifier.layoutId("main")) { mainContent() }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight

        val handlePlaceable = measurables.first { it.layoutId == "handle" }.measure(constraints.copy(minWidth = 0))
        val handleWidth = handlePlaceable.width

        // Calculate sidebar width based on fraction
        val sidebarWidth = (width * currentFraction).toInt().coerceAtLeast(0)

        val sidebarPlaceable = measurables.first { it.layoutId == "sidebar" }.measure(
            Constraints.fixed(width = sidebarWidth, height = height)
        )

        val mainWidth = (width - sidebarWidth - handleWidth).coerceAtLeast(0)
        val mainPlaceable = measurables.first { it.layoutId == "main" }.measure(
            Constraints.fixed(width = mainWidth, height = height)
        )

        layout(width, height) {
            sidebarPlaceable.place(0, 0)
            handlePlaceable.place(sidebarWidth, 0)
            mainPlaceable.place(sidebarWidth + handleWidth, 0)
        }
    }
}

@Composable
private fun DragHandle(
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    resizePointerIcon: PointerIcon,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .width(10.dp) // Thick touch target
            .fillMaxHeight()
            .hoverable(interactionSource = interactionSource)
            .pointerHoverIcon(resizePointerIcon)
            .pointerInput(onDrag, onDragEnd) {
                detectDragGestures(
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x)
                }
            },
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        // Thin visual line
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}
