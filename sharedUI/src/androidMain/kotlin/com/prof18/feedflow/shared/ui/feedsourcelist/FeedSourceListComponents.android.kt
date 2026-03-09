package com.prof18.feedflow.shared.ui.feedsourcelist

import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

actual fun Modifier.singleAndLongClickModifier(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onLongClickPositioned: ((Offset) -> Unit)?,
): Modifier =
    this.combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
    )
