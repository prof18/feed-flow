package com.prof18.feedflow.desktop.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.prof18.feedflow.core.utils.FeatureFlags

object NativeWebViewState {
    private var isLibraryLoaded by mutableStateOf(false)

    val isEnabled: Boolean
        get() = FeatureFlags.USE_NATIVE_WEBVIEW_FOR_READER_MODE && isLibraryLoaded

    internal fun setLibraryLoaded(loaded: Boolean) {
        isLibraryLoaded = loaded
    }
}
