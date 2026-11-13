package com.prof18.feedflow.android.settings.storage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.shared.presentation.SyncAndStorageSettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StorageScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<SyncAndStorageSettingsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    StorageScreenContent(
        navigateBack = navigateBack,
        autoDeletePeriod = state.autoDeletePeriod,
        onAutoDeletePeriodSelected = { period ->
            viewModel.updateAutoDeletePeriod(period)
        },
        onClearDownloadedArticles = {
            viewModel.clearDownloadedArticleContent()
        },
    )
}
