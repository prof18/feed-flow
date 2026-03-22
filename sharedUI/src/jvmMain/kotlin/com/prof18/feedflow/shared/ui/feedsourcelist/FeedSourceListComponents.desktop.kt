package com.prof18.feedflow.shared.ui.feedsourcelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

actual fun Modifier.singleAndLongClickModifier(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onLongClickPositioned: ((Offset) -> Unit)?,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnLongClickPositioned by rememberUpdatedState(onLongClickPositioned)

    return@composed this
        .onGloballyPositioned { coordinates = it }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type != PointerEventType.Press || !event.buttons.isSecondaryPressed) {
                        continue
                    }

                    val position = event.changes.firstOrNull()?.position
                    if (position != null) {
                        val windowPosition = coordinates?.localToWindow(position)
                        if (windowPosition != null && currentOnLongClickPositioned != null) {
                            currentOnLongClickPositioned?.invoke(windowPosition)
                        } else {
                            currentOnLongClick?.invoke()
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }
        .clickable(
            interactionSource = interactionSource,
            onClick = onClick,
        )
}
