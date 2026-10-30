package com.prof18.feedflow.desktop.accounts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.testing.AccountE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.ConfirmationDialog
import com.prof18.feedflow.desktop.ui.components.ProgressButtonLabel
import org.jetbrains.jewel.ui.Outline
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/** Provider credentials with one guarded default action and an ephemeral draft. */
@Composable
internal fun CredentialsForm(
    serverUrlHint: String?,
    usernameHint: String?,
    passwordHint: String?,
    isLoginLoading: Boolean,
    hasLocalSubscriptions: Boolean,
    onLogin: (serverUrl: String?, username: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    errorRevision: Int = 0,
    credentialsState: CredentialsState? = null,
) {
    val strings = LocalFeedFlowStrings.current
    val fallbackState = remember { CredentialsState() }
    val state = credentialsState ?: fallbackState
    val focus = remember { CredentialFocus() }
    var confirmReplacement by remember { mutableStateOf(false) }
    val latestLoading by rememberUpdatedState(isLoginLoading)
    val latestLogin by rememberUpdatedState(onLogin)
    ObserveCredentialSubmissionCompletion(state, isLoginLoading, errorRevision)
    val submit = { state.submit(serverUrlHint != null, latestLoading, latestLogin) }
    fun requestConnect() {
        if (latestLoading || state.submissionPending || confirmReplacement) return
        state.validationRequested = true
        val missingField = state.firstMissingField(serverUrlHint != null)
        when {
            missingField != null -> focus.request(missingField)
            hasLocalSubscriptions -> confirmReplacement = true
            else -> submit()
        }
    }
    Column(
        modifier = modifier.widthIn(max = 560.dp).fillMaxWidth()
            .verticalScroll(rememberScrollState()).padding(vertical = 8.dp)
            .credentialDefaultAction(focus, ::requestConnect),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CredentialFields(state, focus, serverUrlHint, usernameHint, passwordHint, !isLoginLoading)
        errorMessage?.let { Text(it, Modifier.testTag("account-login-error").semantics { error(it) }) }
        CredentialConnectButton(
            loading = isLoginLoading,
            enabled = !isLoginLoading && !state.submissionPending &&
                state.firstMissingField(serverUrlHint != null) == null,
            onClick = ::requestConnect,
        )
    }
    if (confirmReplacement) {
        ConfirmationDialog(
            title = strings.serverAccountReplaceLocalTitle,
            message = strings.serverAccountReplaceLocalMessage,
            onConfirm = {
                if (confirmReplacement) {
                    confirmReplacement = false
                    submit()
                }
            },
            onDismiss = { confirmReplacement = false },
            confirmLabel = strings.serverAccountReplaceAndConnectButton,
        )
    }
}

@Composable
private fun ObserveCredentialSubmissionCompletion(
    state: CredentialsState,
    isLoginLoading: Boolean,
    errorRevision: Int,
) {
    val previousLoading = remember { mutableStateOf(isLoginLoading) }
    val previousErrorRevision = remember { mutableIntStateOf(errorRevision) }
    SideEffect(isLoginLoading, errorRevision) {
        if ((previousLoading.value && !isLoginLoading) || previousErrorRevision.value != errorRevision) {
            state.submissionPending = false
        }
        previousLoading.value = isLoginLoading
        previousErrorRevision.value = errorRevision
    }
}

@Composable
private fun CredentialFields(
    state: CredentialsState,
    focus: CredentialFocus,
    serverHint: String?,
    usernameHint: String?,
    passwordHint: String?,
    enabled: Boolean,
) {
    val strings = LocalFeedFlowStrings.current
    var passwordVisible by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (serverHint != null) {
            CredentialField(
                label = strings.accountTextFieldServerUrl,
                value = state.serverUrl,
                onValueChange = { state.serverUrl = it },
                hint = serverHint,
                errorMessage = strings.desktopAccountServerRequired.takeIf {
                    state.validationRequested && state.serverUrl.text.isBlank()
                },
                enabled = enabled,
                tag = AccountE2eIds.SERVER_URL_INPUT,
                errorTag = "account-server-error",
                fieldType = CredentialFieldType.SERVER,
                focus = focus,
            )
        }
        CredentialField(
            label = strings.accountTextFieldUsername,
            value = state.username,
            onValueChange = { state.username = it },
            hint = usernameHint,
            errorMessage = strings.desktopAccountUsernameRequired.takeIf {
                state.validationRequested && state.username.text.isBlank()
            },
            enabled = enabled,
            tag = AccountE2eIds.USERNAME_INPUT,
            errorTag = "account-username-error",
            fieldType = CredentialFieldType.USERNAME,
            focus = focus,
        )
        CredentialField(
            label = strings.accountTextFieldPassword,
            value = state.password,
            onValueChange = { state.password = it },
            hint = passwordHint,
            errorMessage = strings.desktopAccountPasswordRequired.takeIf {
                state.validationRequested && state.password.text.isBlank()
            },
            enabled = enabled,
            tag = AccountE2eIds.PASSWORD_INPUT,
            errorTag = "account-password-error",
            fieldType = CredentialFieldType.PASSWORD,
            focus = focus,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                PasswordVisibilityButton(passwordVisible, enabled) { passwordVisible = !passwordVisible }
            },
        )
    }
}

@Composable
private fun CredentialField(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    hint: String?,
    errorMessage: String?,
    enabled: Boolean,
    tag: String,
    errorTag: String,
    fieldType: CredentialFieldType,
    focus: CredentialFocus,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label)
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().testTag(tag).focusRequester(focus.requester(fieldType))
                .onFocusChanged { focus.changed(fieldType, it.isFocused) }.semantics {
                    contentDescription = label
                    errorMessage?.let { error(it) }
                },
            enabled = enabled,
            outline = if (errorMessage != null) Outline.Error else Outline.None,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
        )
        if (errorMessage != null) Text(errorMessage, Modifier.testTag(errorTag)) else hint?.let { Text(it) }
    }
}

@Composable
private fun CredentialConnectButton(loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val strings = LocalFeedFlowStrings.current
    DefaultButton(
        onClick = onClick,
        modifier = Modifier.testTag(AccountE2eIds.CONNECT_BUTTON).semantics {
            if (loading) stateDescription = strings.desktopAccountConnecting
        },
        enabled = enabled,
    ) {
        ProgressButtonLabel(strings.accountConnectButton, loading)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PasswordVisibilityButton(visible: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val strings = LocalFeedFlowStrings.current
    val description = if (visible) strings.desktopAccountHidePassword else strings.desktopAccountShowPassword
    Tooltip(tooltip = { Text(description) }) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.testTag(AccountE2eIds.PASSWORD_VISIBILITY)
                .semantics { contentDescription = description },
        ) {
            Icon(AllIconsKeys.Actions.ToggleVisibility, contentDescription = null)
        }
    }
}

private class CredentialFocus {
    private val server = FocusRequester()
    private val username = FocusRequester()
    private val password = FocusRequester()
    var active by mutableStateOf<CredentialFieldType?>(null)

    fun requester(field: CredentialFieldType): FocusRequester = when (field) {
        CredentialFieldType.SERVER -> server
        CredentialFieldType.USERNAME -> username
        CredentialFieldType.PASSWORD -> password
    }

    fun request(field: CredentialFieldType) = requester(field).requestFocus()

    fun changed(field: CredentialFieldType, focused: Boolean) {
        if (focused) {
            active = field
        } else if (active == field) {
            active = null
        }
    }
}

private fun Modifier.credentialDefaultAction(focus: CredentialFocus, onConnect: () -> Unit): Modifier =
    onPreviewKeyEvent { event ->
        if (focus.active != null && event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
            onConnect()
            true
        } else {
            false
        }
    }
