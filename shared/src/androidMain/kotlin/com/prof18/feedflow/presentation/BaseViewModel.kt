package com.prof18.feedflow.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope

actual abstract class BaseViewModel : ViewModel() {

    actual val scope: CoroutineScope = viewModelScope

    actual fun clear() {
        // no-op. AAC ViewModel is taking care of that
    }
}
