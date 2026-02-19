package com.prof18.feedflow.shared.ui.feedsourcelist

import androidx.compose.ui.Modifier

expect fun Modifier.singleAndLongClickModifier(
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)?,
): Modifier
