package com.prof18.feedflow.shared.ui.feedsourcelist

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

expect fun Modifier.singleAndLongClickModifier(
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)?,
    onLongClickPositioned: ((Offset) -> Unit)? = null,
): Modifier
