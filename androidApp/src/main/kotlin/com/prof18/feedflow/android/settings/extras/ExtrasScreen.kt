package com.prof18.feedflow.android.settings.extras

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.shared.presentation.ExtrasSettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ExtrasScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<ExtrasSettingsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    ExtrasScreenContent(
        navigateBack = navigateBack,
        state = state,
        onReduceMotionToggle = viewModel::updateReduceMotionEnabled,
    )
}
