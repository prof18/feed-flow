package com.prof18.feedflow.desktop.accounts.icloud

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.desktop.accounts.AccountConnectButton
import com.prof18.feedflow.desktop.accounts.AccountLoading
import com.prof18.feedflow.desktop.accounts.AccountScreen
import com.prof18.feedflow.desktop.accounts.CloudAccountContent
import com.prof18.feedflow.desktop.accounts.CredentialsState
import com.prof18.feedflow.desktop.accounts.ObserveAccountConnection
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.shared.presentation.ICloudSyncViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ICloudSyncScreen(
    credentialsState: CredentialsState? = null,
) {
    val viewModel = koinViewModel<ICloudSyncViewModel>()
    val uiState by viewModel.iCloudConnectionUiState.collectAsState()
    val snackbarHostState = remember { DesktopFeedbackState() }
    val strings = LocalFeedFlowStrings.current
    val scope = rememberCoroutineScope()
    val fallbackDraft = remember { CredentialsState() }
    val draft = credentialsState ?: fallbackDraft

    ObserveAccountConnection(
        draft = draft,
        uiState = viewModel.iCloudConnectionUiState,
        messages = viewModel.syncMessageQueue,
    ) { event ->
        if (event is SyncResult.Error) {
            draft.connectionErrorRevision++
            draft.connectionError = strings.errorAccountSync(event.errorCode.code)
            draft.submissionPending = false
            scope.launch {
                snackbarHostState.showFeedback(
                    strings.errorAccountSync(event.errorCode.code),
                    duration = DesktopFeedbackDuration.Short,
                )
            }
        }
    }

    AccountScreen(snackbarHostState) {
        ICloudAccountContent(
            uiState = uiState,
            credentialsState = draft,
            onConnect = viewModel::setICloudAuth,
            onBackup = viewModel::triggerBackup,
            onDisconnect = viewModel::unlink,
        )
    }
}

@Composable
internal fun ICloudAccountContent(
    uiState: AccountConnectionUiState,
    credentialsState: CredentialsState,
    onConnect: () -> Unit,
    onBackup: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    when (uiState) {
        AccountConnectionUiState.Loading -> AccountLoading()
        else -> CloudAccountContent(
            description = strings.icloudSyncDescription,
            successMessage = strings.icloudSyncSuccess,
            noSyncMessage = strings.noIcloudSyncYet,
            uiState = uiState,
            credentialsState = credentialsState,
            onBackupClick = onBackup,
            onDisconnectClick = onDisconnect,
            lastUploadMessage = strings.lastUpload,
            lastDownloadMessage = strings.lastDownload,
            connectContent = {
                AccountConnectButton(
                    label = strings.accountConnectButton,
                    isLoading = uiState == AccountConnectionUiState.Loading,
                    errorRevision = credentialsState.connectionErrorRevision,
                    errorMessage = credentialsState.connectionError,
                    onConnect = {
                        credentialsState.connectionError = null
                        onConnect()
                    },
                    state = credentialsState,
                )
            },
        )
    }
}
