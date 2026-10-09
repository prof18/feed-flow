package com.prof18.feedflow.android.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.widget.WidgetInstanceSettingsViewModel
import com.prof18.feedflow.android.widget.WidgetSettingsScaffold
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun WidgetInstanceSettingsScreen(
    appWidgetId: Int,
    navigateBack: () -> Unit,
    navigateToSyncSettings: () -> Unit,
) {
    val viewModel = koinViewModel<WidgetInstanceSettingsViewModel>(
        key = "widget-instance-$appWidgetId",
    ) {
        parametersOf(appWidgetId)
    }
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    LaunchedEffect(appWidgetId) {
        viewModel.reloadWidgetConfiguration()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.reloadWidgetConfiguration()
    }

    WidgetSettingsScaffold(
        title = LocalFeedFlowStrings.current.widgetConfigurationTitle,
        settingsState = settingsState,
        onFeedLayoutSelected = viewModel::updateFeedLayout,
        onShowHeaderSelected = viewModel::updateShowHeader,
        onShowRefreshButtonSelected = viewModel::updateShowRefreshButton,
        onFontScaleSelected = viewModel::updateFontScale,
        onBackgroundColorSelected = viewModel::updateBackgroundColor,
        onBackgroundOpacitySelected = viewModel::updateBackgroundOpacityPercent,
        onTextColorModeSelected = viewModel::updateTextColorMode,
        onHideImagesSelected = viewModel::updateHideImages,
        onContentFilterSelected = viewModel::updateContentFilter,
        onManageSync = navigateToSyncSettings,
        showConfirmButton = false,
        onConfirm = {},
        onNavigateBack = navigateBack,
    )
}
