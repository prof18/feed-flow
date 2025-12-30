package com.prof18.feedflow.desktop.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object NativeWebViewState {
    var isEnabled by mutableStateOf(false)
        internal set
}
