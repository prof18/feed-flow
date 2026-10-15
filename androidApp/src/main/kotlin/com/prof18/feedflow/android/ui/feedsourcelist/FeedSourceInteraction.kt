package com.prof18.feedflow.android.ui.feedsourcelist

import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier

fun Modifier.singleAndLongClickModifier(
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)?,
): Modifier =
    this.combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
    )
