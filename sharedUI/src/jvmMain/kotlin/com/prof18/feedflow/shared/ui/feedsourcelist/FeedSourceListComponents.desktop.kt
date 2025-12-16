package com.prof18.feedflow.shared.ui.feedsourcelist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.onClick
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerButton

@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.singleAndLongClickModifier(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    return@composed this.onClick(
        enabled = true,
        interactionSource = interactionSource,
        matcher = PointerMatcher.mouse(PointerButton.Secondary), // Right Mouse Button
        onClick = onLongClick ?: {},
    ).clickable(
        interactionSource = interactionSource,
    ) { onClick() }
}
