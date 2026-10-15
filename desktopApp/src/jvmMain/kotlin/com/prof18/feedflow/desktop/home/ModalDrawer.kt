package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.jewel.foundation.theme.JewelTheme

internal class DrawerState(initiallyOpen: Boolean = false) {
    var isOpen by mutableStateOf(initiallyOpen)
        private set

    suspend fun open() { isOpen = true }
    suspend fun close() { isOpen = false }
}

@Composable
internal fun rememberDrawerState(): DrawerState = rememberSaveable(
    saver = Saver(save = { it.isOpen }, restore = { DrawerState(it) }),
) { DrawerState() }

@Composable
internal fun ModalDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    drawerContent: @Composable () -> Unit,
    paneContent: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val popupWidth = maxWidth
        val popupHeight = maxHeight
        paneContent()
        if (isOpen) {
            val focusRequester = remember { FocusRequester() }
            val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
            var dragOffset by remember { mutableFloatStateOf(0f) }
            var drawerWidth by remember { mutableFloatStateOf(0f) }
            Popup(
                popupPositionProvider = DrawerPopupPosition,
                onDismissRequest = onDismiss,
                properties = PopupProperties(focusable = true),
            ) {
                Box(
                    Modifier.size(popupWidth, popupHeight)
                        .focusRequester(focusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                                onDismiss()
                                true
                            } else {
                                false
                            }
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.32f))
                            .clickable(onClick = onDismiss),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(drawerPaneWidth.coerceAtMost(popupWidth))
                            .fillMaxHeight()
                            .onSizeChanged { drawerWidth = it.width.toFloat() }
                            .graphicsLayer { translationX = dragOffset * direction }
                            .pointerInput(Unit) { detectTapGestures { /* Keep drawer gaps above the scrim. */ } }
                            .pointerInput(direction) {
                                detectHorizontalDragGestures(
                                    onDragCancel = { dragOffset = 0f },
                                    onDragEnd = {
                                        if (-dragOffset > drawerWidth * DrawerDismissFraction) onDismiss()
                                        dragOffset = 0f
                                    },
                                    onHorizontalDrag = { change, amount ->
                                        change.consume()
                                        dragOffset = (dragOffset + amount * direction).coerceIn(-drawerWidth, 0f)
                                    },
                                )
                            }
                            .background(JewelTheme.globalColors.panelBackground),
                    ) {
                        drawerContent()
                    }
                }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }
        }
    }
}

private const val DrawerDismissFraction = 0.35f

private object DrawerPopupPosition : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(anchorBounds.left, anchorBounds.top)
}
