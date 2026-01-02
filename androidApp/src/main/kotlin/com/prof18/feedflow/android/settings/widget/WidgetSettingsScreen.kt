package com.prof18.feedflow.android.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.widget.WidgetSettingsScaffold
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WidgetSettingsScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<WidgetSettingsViewModel>()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()

    WidgetSettingsScaffold(
        title = LocalFeedFlowStrings.current.widgetConfigurationTitle,
        settingsState = settingsState,
        onSyncPeriodSelected = viewModel::updateSyncPeriod,
        onFeedLayoutSelected = viewModel::updateFeedLayout,
        onShowHeaderSelected = viewModel::updateShowHeader,
        onFontScaleSelected = viewModel::updateFontScale,
        onBackgroundColorSelected = viewModel::updateBackgroundColor,
        onBackgroundOpacitySelected = viewModel::updateBackgroundOpacityPercent,
        showConfirmButton = false,
        onConfirm = {},
        onNavigateBack = navigateBack,
    )
}
