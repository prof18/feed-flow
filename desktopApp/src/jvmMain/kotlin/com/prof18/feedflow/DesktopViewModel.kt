package com.prof18.feedflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.prof18.feedflow.presentation.BaseViewModel

@Composable
fun <T : BaseViewModel> desktopViewModel(createViewModel: () -> T): T {
    val viewModel = remember { createViewModel() }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clear()
        }
    }
    return viewModel
}
