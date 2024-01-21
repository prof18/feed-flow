package com.prof18.feedflow.shared.ui.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics

actual fun Modifier.tagForTesting(
    tag: String,
    mergeDescendants: Boolean,
): Modifier =
    this.semantics {}.testTag(tag)
