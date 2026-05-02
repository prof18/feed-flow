package com.prof18.feedflow.android.settings.appearance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.shared.presentation.ExtrasSettingsViewModel
import com.prof18.feedflow.shared.presentation.MainSettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AppearanceScreen(
    navigateBack: () -> Unit,
) {
    val mainSettingsViewModel = koinViewModel<MainSettingsViewModel>()
    val extrasSettingsViewModel = koinViewModel<ExtrasSettingsViewModel>()

    val settingsState by mainSettingsViewModel.settingsState.collectAsStateWithLifecycle()
    val extrasState by extrasSettingsViewModel.state.collectAsStateWithLifecycle()

    AppearanceScreenContent(
        navigateBack = navigateBack,
        themeMode = settingsState.themeMode,
        isReduceMotionEnabled = extrasState.isReduceMotionEnabled,
        onThemeModeSelected = mainSettingsViewModel::updateThemeMode,
        onReduceMotionToggled = extrasSettingsViewModel::updateReduceMotionEnabled,
    )
}
