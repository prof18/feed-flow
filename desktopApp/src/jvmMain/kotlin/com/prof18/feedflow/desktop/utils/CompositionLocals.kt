package com.prof18.feedflow.desktop.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.awt.ComposeWindow

val LocalComposeWindow = compositionLocalOf<ComposeWindow> {
    error("ComposeWindow not provided")
}
