package com.prof18.feedflow.desktop.accounts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.prof18.feedflow.core.model.AccountConnectionUiState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
internal fun <T> ObserveAccountConnection(
    draft: CredentialsState,
    uiState: StateFlow<AccountConnectionUiState>,
    messages: Flow<T>,
    loginLoading: StateFlow<Boolean>? = null,
    onMessage: (T) -> Unit,
) {
    val detailScope = rememberCoroutineScope()
    val ownerScope = draft.ownerScope ?: detailScope
    val latestOnMessage = rememberUpdatedState(onMessage)
    SideEffect(draft, ownerScope, uiState, messages, loginLoading) {
        draft.connectionObservationJob?.cancel()
        // Provider/page navigation removes the detail; requests remain owned by the Settings session.
        draft.connectionObservationJob = ownerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            launch(start = CoroutineStart.UNDISPATCHED) {
                uiState.collect { current ->
                    val previous = draft.observedConnectionState
                    draft.observedConnectionState = current
                    if (current is AccountConnectionUiState.Linked) draft.clear()
                    if (previous == AccountConnectionUiState.Loading && current == AccountConnectionUiState.Unlinked) {
                        draft.submissionPending = false
                        draft.authCodePending = false
                    }
                }
            }
            loginLoading?.let { loading ->
                launch(start = CoroutineStart.UNDISPATCHED) {
                    loading.collect { current ->
                        if (draft.observedLoginLoading == true && !current) draft.submissionPending = false
                        draft.observedLoginLoading = current
                    }
                }
            }
            messages.collect { latestOnMessage.value(it) }
        }
    }
}
