package com.prof18.feedflow.desktop.accounts.dropbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalUriHandler
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.DropboxSynMessages
import com.prof18.feedflow.desktop.accounts.AccountScreen
import com.prof18.feedflow.desktop.accounts.CloudAccountContent
import com.prof18.feedflow.desktop.accounts.CredentialsState
import com.prof18.feedflow.desktop.accounts.DropboxAuthorizationForm
import com.prof18.feedflow.desktop.accounts.ObserveAccountConnection
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.utils.openUriSafely
import com.prof18.feedflow.shared.presentation.DropboxSyncViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DropboxSyncScreen(
    credentialsState: CredentialsState? = null,
) {
    val viewModel = koinViewModel<DropboxSyncViewModel>()
    val uiState by viewModel.dropboxConnectionUiState.collectAsState()
    val snackbarHostState = remember { DesktopFeedbackState() }
    val uriHandler = LocalUriHandler.current
    val strings = LocalFeedFlowStrings.current
    val scope = rememberCoroutineScope()
    val fallbackDraft = remember { CredentialsState() }
    val draft = credentialsState ?: fallbackDraft

    fun showError(message: String) {
        draft.connectionError = message
        draft.connectionErrorRevision++
        draft.submissionPending = false
        draft.authCodePending = false
        scope.launch { snackbarHostState.showFeedback(message, duration = DesktopFeedbackDuration.Short) }
    }

    ObserveAccountConnection(
        draft = draft,
        uiState = viewModel.dropboxConnectionUiState,
        messages = viewModel.dropboxSyncMessageState,
    ) { event ->
        when (event) {
            DropboxSynMessages.Error -> showError(strings.dropboxSyncError)
            DropboxSynMessages.CodeExpired -> showError(strings.dropboxAuthCodeExpired)
            is DropboxSynMessages.ProceedToAuth -> {
                draft.submissionPending = false
                draft.authCodeReady = true
                if (!uriHandler.openUriSafely(event.authorizeUrl)) showError(strings.browserLaunchError)
            }
        }
    }

    AccountScreen(snackbarHostState) {
        DropboxAccountContent(
            uiState = uiState,
            credentialsState = draft,
            onStartAuth = viewModel::startDropboxAuthFlow,
            onConfirmCode = viewModel::handleDropboxAuthResponse,
            onBackup = viewModel::triggerBackup,
            onDisconnect = viewModel::disconnect,
        )
    }
}

@Composable
internal fun DropboxAccountContent(
    uiState: AccountConnectionUiState,
    credentialsState: CredentialsState,
    onStartAuth: () -> Unit,
    onConfirmCode: (String) -> Unit,
    onBackup: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    CloudAccountContent(
        description = strings.dropboxSyncDesktopDescription,
        successMessage = strings.dropboxSyncSuccess,
        noSyncMessage = strings.noDropboxSyncYet,
        uiState = uiState,
        credentialsState = credentialsState,
        onBackupClick = onBackup,
        onDisconnectClick = onDisconnect,
        lastUploadMessage = strings.lastUpload,
        lastDownloadMessage = strings.lastDownload,
        connectContent = {
            DropboxAuthorizationForm(
                state = credentialsState,
                isLoading = uiState == AccountConnectionUiState.Loading,
                isCodeEntryReady = credentialsState.authCodeReady,
                errorRevision = credentialsState.connectionErrorRevision,
                errorMessage = credentialsState.connectionError,
                onStartAuth = {
                    credentialsState.connectionError = null
                    onStartAuth()
                },
                onConfirmCode = { code ->
                    credentialsState.connectionError = null
                    onConfirmCode(code)
                },
            )
        },
    )
}
