package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
internal actual fun isMultiSelectModifierPressed(): Boolean {
    val modifiers = LocalWindowInfo.current.keyboardModifiers
    return modifiers.isCtrlPressed || modifiers.isMetaPressed
}
