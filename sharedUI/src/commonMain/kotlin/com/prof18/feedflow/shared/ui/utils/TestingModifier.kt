package com.prof18.feedflow.shared.ui.utils

import androidx.compose.ui.Modifier

expect fun Modifier.tagForTesting(
    tag: String,
    mergeDescendants: Boolean = false,
): Modifier
