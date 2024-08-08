package com.prof18.feedflow.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel

@Composable
inline fun <T : ViewModel> desktopViewModel(crossinline createViewModel: () -> T): T {
    // TODO: Move to Androidx ViewModel constructor when ViewModelStoreOwner will be scoped outside Navigation
    val viewModel = remember { createViewModel() }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.viewModelScope.cancel()
        }
    }
    return viewModel
}
