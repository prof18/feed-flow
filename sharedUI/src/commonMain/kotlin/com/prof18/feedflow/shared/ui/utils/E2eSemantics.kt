package com.prof18.feedflow.shared.ui.utils

import androidx.compose.ui.Modifier

/**
 * Popups and dialogs own their semantics tree, so the `testTagsAsResourceId` flag set on the
 * activity content does not reach them and their test tags stay invisible to E2E drivers.
 * Apply this to popup content whose children need to be addressable by id.
 */
expect fun Modifier.exposeTestTagsAsResourceIds(): Modifier
