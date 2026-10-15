package com.prof18.feedflow.desktop.accounts.miniflux

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.DataNotFound
import com.prof18.feedflow.core.model.NetworkFailure
import com.prof18.feedflow.desktop.accounts.AccountLoading
import com.prof18.feedflow.desktop.accounts.AccountScreen
import com.prof18.feedflow.desktop.accounts.CloudAccountContent
import com.prof18.feedflow.desktop.accounts.CredentialsForm
import com.prof18.feedflow.desktop.accounts.CredentialsState
import com.prof18.feedflow.desktop.accounts.ObserveAccountConnection
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.shared.presentation.MinifluxSyncViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun MinifluxSyncScreen(
    credentialsState: CredentialsState? = null,
) {
    val viewModel = koinViewModel<MinifluxSyncViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val isLoginLoading by viewModel.loginLoading.collectAsState()
    val hasLocalSubscriptions by viewModel.hasLocalSubscriptions.collectAsState(initial = false)
    val snackbarHostState = remember { DesktopFeedbackState() }
    val scope = rememberCoroutineScope()
    val strings = LocalFeedFlowStrings.current
    val fallbackDraft = remember { CredentialsState() }
    val draft = credentialsState ?: fallbackDraft

    ObserveAccountConnection(
        draft = draft,
        uiState = viewModel.uiState,
        messages = viewModel.errorState,
        loginLoading = viewModel.loginLoading,
    ) { failure ->
        val message = when (failure) {
            is NetworkFailure.Unauthorised -> strings.wrongCredentialsErrorMessage
            NetworkFailure.BadToken -> strings.minifluxBadTokenErrorMessage
            is DataNotFound -> strings.wrongUrlErrorMessage
            else -> strings.genericErrorMessage
        }
        draft.connectionErrorRevision++
        draft.connectionError = message
        draft.submissionPending = false
        scope.launch { snackbarHostState.showFeedback(message, duration = DesktopFeedbackDuration.Long) }
    }

    AccountScreen(snackbarHostState) {
        MinifluxAccountContent(
            uiState = uiState,
            credentialsState = draft,
            isLoginLoading = isLoginLoading,
            hasLocalSubscriptions = hasLocalSubscriptions,
            onLogin = { serverUrl, username, password -> viewModel.login(username, password, serverUrl.orEmpty()) },
            onDisconnect = viewModel::disconnect,
        )
    }
}

@Composable
internal fun MinifluxAccountContent(
    uiState: AccountConnectionUiState,
    credentialsState: CredentialsState,
    isLoginLoading: Boolean,
    hasLocalSubscriptions: Boolean,
    onLogin: (String?, String, String) -> Unit,
    onDisconnect: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    when (uiState) {
        AccountConnectionUiState.Loading -> AccountLoading()
        else -> CloudAccountContent(
            description = "",
            successMessage = strings.minifluxAccountConnected,
            noSyncMessage = strings.noMinifluxSyncYet,
            uiState = uiState,
            credentialsState = credentialsState,
            onBackupClick = null,
            onDisconnectClick = onDisconnect,
            lastDownloadMessage = strings.minifluxLastSync,
            connectContent = {
                CredentialsForm(
                    serverUrlHint = strings.minifluxUrlHint("https://demo.miniflux.app"),
                    usernameHint = null,
                    passwordHint = strings.minifluxPasswordHint,
                    isLoginLoading = isLoginLoading,
                    hasLocalSubscriptions = hasLocalSubscriptions,
                    errorMessage = credentialsState.connectionError,
                    errorRevision = credentialsState.connectionErrorRevision,
                    credentialsState = credentialsState,
                    onLogin = { serverUrl, username, password ->
                        credentialsState.connectionError = null
                        onLogin(serverUrl, username, password)
                    },
                )
            },
        )
    }
}
