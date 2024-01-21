package com.prof18.feedflow.shared.ui.utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.tagForTesting(tag: String, mergeDescendants: Boolean): Modifier =
    this.semantics(mergeDescendants = mergeDescendants) {
        testTagsAsResourceId = true
    }.testTag(tag)
