package com.prof18.feedflow.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.lifecycle.ScreenDisposable
import cafe.adriel.voyager.core.lifecycle.ScreenLifecycleStore
import cafe.adriel.voyager.core.screen.Screen
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

@Composable
internal inline fun <reified T : ViewModel> screenViewModel(
    screen: Screen,
    crossinline createViewModel: () -> T,
): T {
    val holder = ScreenLifecycleStore.get<ScreenViewModelHolder<T>>(screen) {
        ScreenViewModelHolder(createViewModel())
    }
    return holder.viewModel
}

internal class ScreenViewModelHolder<VM : ViewModel>(
    val viewModel: VM,
) : ScreenDisposable {
    override fun onDispose(screen: Screen) {
        viewModel.viewModelScope.cancel()
    }
}
