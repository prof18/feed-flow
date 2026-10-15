package com.prof18.feedflow.desktop.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.testing.AccountE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text

@Composable
internal fun AccountConnectButton(
    label: String,
    isLoading: Boolean,
    errorRevision: Int,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
    state: CredentialsState? = null,
    tag: String = AccountE2eIds.CONNECT_BUTTON,
    errorMessage: String? = null,
) {
    var localSubmissionPending by remember { mutableStateOf(false) }
    val previousLoading = remember { mutableStateOf(isLoading) }
    val previousErrorRevision = remember { mutableStateOf(errorRevision) }
    val latestIsLoading by rememberUpdatedState(isLoading)
    val latestOnConnect by rememberUpdatedState(onConnect)
    val latestState by rememberUpdatedState(state)
    val strings = LocalFeedFlowStrings.current

    LaunchedEffect(isLoading, errorRevision, state) {
        val loadingCompleted = previousLoading.value && !isLoading
        val receivedNewError = previousErrorRevision.value != errorRevision
        if (loadingCompleted || receivedNewError) {
            state?.submissionPending = false
            localSubmissionPending = false
        }
        previousLoading.value = isLoading
        previousErrorRevision.value = errorRevision
    }

    val isSubmissionPending = state?.submissionPending ?: localSubmissionPending
    val isBusy = isLoading || isSubmissionPending
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DefaultButton(
            onClick = {
                val currentState = latestState
                val currentIsPending = currentState?.submissionPending ?: localSubmissionPending
                if (!latestIsLoading && !currentIsPending) {
                    if (currentState != null) {
                        currentState.submissionPending = true
                    } else {
                        localSubmissionPending = true
                    }
                    latestOnConnect()
                }
            },
            modifier = Modifier
                .testTag(tag)
                .semantics {
                    if (isBusy) stateDescription = strings.desktopAccountConnecting
                },
            enabled = !isBusy,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(label)
                Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                    if (isBusy) CircularProgressIndicator(Modifier.size(12.dp))
                }
            }
        }
        errorMessage?.let { message -> Text(message, Modifier.semantics { error(message) }) }
    }
}
