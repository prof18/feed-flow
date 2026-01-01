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
    val syncPeriod by viewModel.syncPeriodState.collectAsStateWithLifecycle()
    val feedLayout by viewModel.feedLayoutState.collectAsStateWithLifecycle()

    WidgetSettingsScaffold(
        title = LocalFeedFlowStrings.current.widgetConfigurationTitle,
        syncPeriod = syncPeriod,
        feedLayout = feedLayout,
        headerText = LocalFeedFlowStrings.current.widgetSettingsHeader,
        onSyncPeriodSelected = viewModel::updateSyncPeriod,
        onFeedLayoutSelected = viewModel::updateFeedLayout,
        showConfirmButton = false,
        onConfirm = {},
        onNavigateBack = navigateBack,
    )
}
