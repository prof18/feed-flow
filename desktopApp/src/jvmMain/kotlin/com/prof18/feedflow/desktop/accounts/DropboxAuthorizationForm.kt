package com.prof18.feedflow.desktop.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.ProgressButtonLabel
import org.jetbrains.jewel.ui.Outline
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

/** Two-step browser/code authorization. The code draft remains in memory only. */
@Composable
internal fun DropboxAuthorizationForm(
    state: CredentialsState,
    isLoading: Boolean,
    isCodeEntryReady: Boolean,
    errorRevision: Int,
    onStartAuth: () -> Unit,
    onConfirmCode: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    val strings = LocalFeedFlowStrings.current
    val previousLoading = remember { mutableStateOf(isLoading) }
    val previousErrorRevision = remember { mutableIntStateOf(errorRevision) }
    SideEffect(isLoading, errorRevision) {
        if ((previousLoading.value && !isLoading) || previousErrorRevision.value != errorRevision) {
            state.authCodePending = false
        }
        previousLoading.value = isLoading
        previousErrorRevision.value = errorRevision
    }
    Column(
        modifier = modifier.widthIn(max = 560.dp).fillMaxWidth()
            .verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!isCodeEntryReady) {
            AccountConnectButton(
                label = strings.accountConnectButton,
                isLoading = isLoading,
                errorRevision = errorRevision,
                onConnect = onStartAuth,
                state = state,
                tag = "dropbox-start-auth",
            )
        } else {
            DropboxCodeEntry(state, isLoading, onStartAuth, onConfirmCode)
        }
        errorMessage?.let { message -> Text(message, Modifier.semantics { error(message) }) }
    }
}

@Composable
private fun DropboxCodeEntry(
    state: CredentialsState,
    loading: Boolean,
    onStartAuth: () -> Unit,
    onConfirmCode: (String) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val latestLoading by rememberUpdatedState(loading)
    val latestConfirm by rememberUpdatedState(onConfirmCode)
    val latestStart by rememberUpdatedState(onStartAuth)
    var inputFocused by remember { mutableStateOf(false) }
    val codeError = strings.desktopDropboxCodeRequired.takeIf {
        state.validationRequested && state.authCode.text.isBlank()
    }
    val busy = loading || state.authCodePending || state.submissionPending
    fun confirm() = state.submitAuthCode(latestLoading, latestConfirm)

    Column(
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (inputFocused && event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                confirm()
                true
            } else {
                false
            }
        },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(strings.dropboxAuthCodeHint)
        TextField(
            value = state.authCode,
            onValueChange = { state.authCode = it },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag("dropbox-auth-code")
                .onFocusChanged { inputFocused = it.isFocused }.semantics {
                    contentDescription = strings.dropboxAuthCodeHint
                    codeError?.let { error(it) }
                },
            visualTransformation = PasswordVisualTransformation(),
            outline = if (codeError != null) Outline.Error else Outline.None,
        )
        codeError?.let { Text(it) }
        DropboxAuthorizationActions(
            state = state,
            busy = busy,
            onConfirm = ::confirm,
            onRestart = {
                if (!latestLoading && !state.submissionPending && !state.authCodePending) {
                    state.authCodeReady = false
                    state.authCode = TextFieldValue()
                    state.submissionPending = true
                    latestStart()
                }
            },
        )
    }
}

@Composable
private fun DropboxAuthorizationActions(
    state: CredentialsState,
    busy: Boolean,
    onConfirm: () -> Unit,
    onRestart: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onRestart,
            enabled = !busy,
            modifier = Modifier.testTag("dropbox-restart-auth"),
        ) { Text(strings.desktopDropboxRestartAuthorization) }
        DefaultButton(
            onClick = onConfirm,
            enabled = !busy && state.authCode.text.isNotBlank(),
            modifier = Modifier.testTag("dropbox-confirm-auth"),
        ) {
            ProgressButtonLabel(strings.dropboxConfirmButton, busy)
        }
    }
}
