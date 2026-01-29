package com.prof18.feedflow.android.settings.syncstorage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.shared.domain.FeedDownloadWorkerEnqueuer
import com.prof18.feedflow.shared.presentation.SyncAndStorageSettingsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SyncAndStorageScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<SyncAndStorageSettingsViewModel>()
    val feedDownloadWorkerEnqueuer = koinInject<FeedDownloadWorkerEnqueuer>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SyncAndStorageScreenContent(
        navigateBack = navigateBack,
        syncPeriod = state.syncPeriod,
        autoDeletePeriod = state.autoDeletePeriod,
        refreshFeedsOnLaunch = state.refreshFeedsOnLaunch,
        showRssParsingErrors = state.showRssParsingErrors,
        onSyncPeriodSelected = { period ->
            viewModel.updateSyncPeriod(period)
            feedDownloadWorkerEnqueuer.updateWorker(period)
        },
        onAutoDeletePeriodSelected = { period ->
            viewModel.updateAutoDeletePeriod(period)
        },
        onRefreshFeedsOnLaunchToggle = { enabled ->
            viewModel.updateRefreshFeedsOnLaunch(enabled)
        },
        onShowRssParsingErrorsToggle = { enabled ->
            viewModel.updateShowRssParsingErrors(enabled)
        },
        onClearDownloadedArticles = {
            viewModel.clearDownloadedArticleContent()
        },
    )
}
