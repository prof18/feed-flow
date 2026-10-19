package com.prof18.feedflow.desktop.accounts

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import com.prof18.feedflow.core.model.AccountConnectionUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/** An in-memory form draft. It is never saved to preferences or serialized. */
@Stable
internal class CredentialsState(internal val ownerScope: CoroutineScope? = null) {
    var serverUrl by mutableStateOf(TextFieldValue())
    var username by mutableStateOf(TextFieldValue())
    var password by mutableStateOf(TextFieldValue())
    var submissionPending by mutableStateOf(false)
    var validationRequested by mutableStateOf(false)
    var authCode by mutableStateOf(TextFieldValue())
    var authCodePending by mutableStateOf(false)
    var authCodeReady by mutableStateOf(false)
    var connectionError by mutableStateOf<String?>(null)
    var connectionErrorRevision by mutableIntStateOf(0)
    internal var connectionObservationJob: Job? = null
    internal var observedConnectionState: AccountConnectionUiState? = null
    internal var observedLoginLoading: Boolean? = null

    fun firstMissingField(hasServer: Boolean): CredentialFieldType? = when {
        hasServer && serverUrl.text.isBlank() -> CredentialFieldType.SERVER
        username.text.isBlank() -> CredentialFieldType.USERNAME
        password.text.isBlank() -> CredentialFieldType.PASSWORD
        else -> null
    }

    fun submit(hasServer: Boolean, loading: Boolean, onLogin: (String?, String, String) -> Unit) {
        if (loading || submissionPending || firstMissingField(hasServer) != null) return
        submissionPending = true
        onLogin(serverUrl.text.takeIf { hasServer }, username.text, password.text)
    }

    fun submitAuthCode(loading: Boolean, onConfirm: (String) -> Unit) {
        if (loading || authCodePending || submissionPending) return
        validationRequested = true
        if (authCode.text.isBlank()) return
        authCodePending = true
        onConfirm(authCode.text)
    }

    fun clear() {
        serverUrl = TextFieldValue()
        username = TextFieldValue()
        password = TextFieldValue()
        submissionPending = false
        validationRequested = false
        authCode = TextFieldValue()
        authCodePending = false
        authCodeReady = false
        connectionError = null
    }
}

internal enum class CredentialFieldType { SERVER, USERNAME, PASSWORD }
