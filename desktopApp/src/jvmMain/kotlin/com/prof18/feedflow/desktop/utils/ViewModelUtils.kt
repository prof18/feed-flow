package com.prof18.feedflow.desktop.utils

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.androidx.navigation3.viewModelStoreOwner
import org.koin.core.parameter.ParametersDefinition
import org.koin.mp.KoinPlatform

@Composable
inline fun <reified T : ViewModel> koinNavViewModel(
    noinline parameters: ParametersDefinition? = null,
): T {
    val koin = KoinPlatform.getKoin()
    return viewModel(
        viewModelStoreOwner = viewModelStoreOwner(),
        factory = { koin.get<T>(parameters = parameters) },
    )
}

@Composable
inline fun <reified T : ViewModel> desktopViewModel(
    noinline viewModelFactory: @Composable () -> T,
): T {
    return viewModel {
        viewModelFactory()
    }
}
