package com.prof18.feedflow.shared.ui.feedsourcelist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
internal actual fun Modifier.feedSourceMenuClickModifier(onLongClick: () -> Unit): Modifier =
    this.combinedClickable(
        onClick = {
            // TODO: open edit feed
        },
        onLongClick = onLongClick,
    )
