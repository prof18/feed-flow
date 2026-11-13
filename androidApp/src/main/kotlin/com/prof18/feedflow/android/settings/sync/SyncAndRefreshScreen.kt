package com.prof18.feedflow.android.settings.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.shared.domain.FeedDownloadWorkerEnqueuer
import com.prof18.feedflow.shared.presentation.SyncAndStorageSettingsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SyncAndRefreshScreen(
    navigateBack: () -> Unit,
    navigateToAccounts: (() -> Unit)? = null,
) {
    val viewModel = koinViewModel<SyncAndStorageSettingsViewModel>()
    val feedDownloadWorkerEnqueuer = koinInject<FeedDownloadWorkerEnqueuer>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SyncAndRefreshScreenContent(
        navigateBack = navigateBack,
        navigateToAccounts = navigateToAccounts,
        syncPeriod = state.syncPeriod,
        backgroundSyncRestrictions = state.backgroundSyncRestrictions,
        refreshFeedsOnLaunch = state.refreshFeedsOnLaunch,
        onSyncPeriodSelected = { period ->
            viewModel.updateSyncPeriod(period)
            feedDownloadWorkerEnqueuer.updateWorker(period)
        },
        onSyncOnlyOnWifiToggle = { enabled ->
            viewModel.updateSyncOnlyOnWifi(enabled)
        },
        onSyncOnlyWhenChargingToggle = { enabled ->
            viewModel.updateSyncOnlyWhenCharging(enabled)
        },
        onRefreshFeedsOnLaunchToggle = { enabled ->
            viewModel.updateRefreshFeedsOnLaunch(enabled)
        },
    )
}
