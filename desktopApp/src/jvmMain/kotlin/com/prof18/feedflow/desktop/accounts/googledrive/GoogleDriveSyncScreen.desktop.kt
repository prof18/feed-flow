package com.prof18.feedflow.desktop.accounts.googledrive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.GoogleDriveSynMessages
import com.prof18.feedflow.desktop.accounts.AccountConnectButton
import com.prof18.feedflow.desktop.accounts.AccountScreen
import com.prof18.feedflow.desktop.accounts.CloudAccountContent
import com.prof18.feedflow.desktop.accounts.CredentialsState
import com.prof18.feedflow.desktop.accounts.ObserveAccountConnection
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.shared.presentation.GoogleDriveSyncViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun GoogleDriveSyncScreen(
    credentialsState: CredentialsState? = null,
) {
    val viewModel = koinViewModel<GoogleDriveSyncViewModel>()
    val uiState by viewModel.googleDriveConnectionUiState.collectAsState()
    val strings = LocalFeedFlowStrings.current
    val fallbackDraft = remember { CredentialsState() }
    val draft = credentialsState ?: fallbackDraft

    ObserveAccountConnection(
        draft = draft,
        uiState = viewModel.googleDriveConnectionUiState,
        messages = viewModel.googleDriveSyncMessageState,
    ) { event ->
        if (event == GoogleDriveSynMessages.Error) {
            draft.connectionErrorRevision++
            draft.connectionError = strings.googleDriveSyncError
            draft.syncError = strings.googleDriveSyncError
            draft.submissionPending = false
        }
    }

    AccountScreen {
        GoogleDriveAccountContent(
            uiState = uiState,
            credentialsState = draft,
            onConnect = viewModel::startGoogleDriveAuthFlow,
            onBackup = viewModel::triggerBackup,
            onDisconnect = viewModel::disconnect,
        )
    }
}

@Composable
internal fun GoogleDriveAccountContent(
    uiState: AccountConnectionUiState,
    credentialsState: CredentialsState,
    onConnect: () -> Unit,
    onBackup: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    CloudAccountContent(
        description = strings.googleDriveDesktopSyncDescription,
        successMessage = strings.googleDriveSyncSuccess,
        noSyncMessage = strings.noGoogleDriveSyncYet,
        disconnectMessage = strings.accountDisconnectBackupMessage,
        uiState = uiState,
        credentialsState = credentialsState,
        onBackupClick = onBackup,
        onDisconnectClick = onDisconnect,
        lastUploadMessage = strings.lastUpload,
        lastDownloadMessage = strings.lastDownload,
        connectContent = {
            AccountConnectButton(
                label = strings.googleDriveConnectButton,
                isLoading = uiState == AccountConnectionUiState.Loading,
                errorRevision = credentialsState.connectionErrorRevision,
                errorMessage = credentialsState.connectionError,
                onConnect = {
                    credentialsState.connectionError = null
                    credentialsState.syncError = null
                    onConnect()
                },
                state = credentialsState,
            )
        },
    )
}
