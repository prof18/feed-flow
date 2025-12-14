package com.prof18.feedflow.shared.ui.feedsourcelist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.singleAndLongClickModifier(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
): Modifier =
    this.combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
    )
