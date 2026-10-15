package com.prof18.feedflow.desktop.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.testing.AccountE2eIds
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.ConfirmationDialog
import com.prof18.feedflow.desktop.ui.components.SnackbarHost
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text

@Composable
internal fun AccountScreen(
    account: SyncAccounts,
    onBackClick: () -> Unit,
    showNavigateBack: Boolean,
    snackbarHostState: DesktopFeedbackState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showNavigateBack) {
                OutlinedButton(onClick = onBackClick) { Text(strings.desktopSearchBackButton) }
            }
            Text(account.desktopTitle(strings))
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            content()
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
internal fun CloudAccountContent(
    description: String,
    successMessage: String,
    noSyncMessage: String,
    uiState: AccountConnectionUiState,
    onBackupClick: (() -> Unit)?,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier,
    lastUploadMessage: ((String) -> String)? = null,
    lastDownloadMessage: ((String) -> String)? = null,
    credentialsState: CredentialsState? = null,
    connectContent: @Composable () -> Unit,
) {
    var isDisconnectConfirmationVisible by remember { mutableStateOf(false) }
    var disconnectConfirmed by remember { mutableStateOf(false) }
    val latestUiState by rememberUpdatedState(uiState)
    val latestOnDisconnectClick by rememberUpdatedState(onDisconnectClick)
    val latestOnBackupClick by rememberUpdatedState(onBackupClick)
    val strings = LocalFeedFlowStrings.current

    LaunchedEffect(uiState is AccountConnectionUiState.Linked, credentialsState) {
        if (uiState is AccountConnectionUiState.Linked) credentialsState?.clear()
    }

    LaunchedEffect(uiState) {
        if (!uiState.canDisconnect()) {
            isDisconnectConfirmationVisible = false
        }
        if (uiState == AccountConnectionUiState.Unlinked) {
            disconnectConfirmed = false
        }
    }

    when (uiState) {
        AccountConnectionUiState.Loading -> AccountLoading(modifier.fillMaxSize())
        AccountConnectionUiState.Unlinked -> {
            Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (description.isNotBlank()) Text(description)
                connectContent()
            }
        }
        is AccountConnectionUiState.Linked -> {
            LinkedAccountDetails(
                modifier = modifier,
                uiState = uiState,
                successMessage = successMessage,
                noSyncMessage = noSyncMessage,
                onBackupClick = onBackupClick?.let {
                    {
                        if (latestUiState.canDisconnect()) latestOnBackupClick?.invoke()
                    }
                },
                onRequestDisconnect = {
                    if (!disconnectConfirmed && latestUiState.canDisconnect()) {
                        isDisconnectConfirmationVisible = true
                    }
                },
                lastUploadMessage = lastUploadMessage,
                lastDownloadMessage = lastDownloadMessage,
            )
        }
    }

    DisconnectConfirmationDialog(
        isVisible = isDisconnectConfirmationVisible,
        uiState = latestUiState,
        disconnectConfirmed = disconnectConfirmed,
        onConfirm = {
            if (!disconnectConfirmed && latestUiState.canDisconnect()) {
                disconnectConfirmed = true
                isDisconnectConfirmationVisible = false
                latestOnDisconnectClick()
            }
        },
        onDismiss = { isDisconnectConfirmationVisible = false },
    )
}

@Composable
private fun LinkedAccountDetails(
    uiState: AccountConnectionUiState.Linked,
    successMessage: String,
    noSyncMessage: String,
    onBackupClick: (() -> Unit)?,
    onRequestDisconnect: () -> Unit,
    lastUploadMessage: ((String) -> String)?,
    lastDownloadMessage: ((String) -> String)?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(successMessage, Modifier.testTag(AccountE2eIds.CONNECTED_MESSAGE))
            SyncStatus(uiState.syncState, noSyncMessage, lastUploadMessage, lastDownloadMessage)
            if (onBackupClick != null) {
                OutlinedButton(
                    onClick = onBackupClick,
                    enabled = uiState.syncState != AccountSyncUIState.Loading,
                    modifier = Modifier.testTag("account_backup_button"),
                ) { Text(LocalFeedFlowStrings.current.backupButton) }
            }
            OutlinedButton(
                onClick = onRequestDisconnect,
                modifier = Modifier.testTag(AccountE2eIds.DISCONNECT_BUTTON),
                enabled = uiState.syncState != AccountSyncUIState.Loading,
            ) { Text(LocalFeedFlowStrings.current.accountDisconnectButton) }
        }
    }
}

@Composable
private fun SyncStatus(
    syncState: AccountSyncUIState,
    noSyncMessage: String,
    lastUploadMessage: ((String) -> String)?,
    lastDownloadMessage: ((String) -> String)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (syncState) {
            AccountSyncUIState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()
                Text(
                    LocalFeedFlowStrings.current.accountRefreshProgress,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            AccountSyncUIState.None -> Text(noSyncMessage)
            is AccountSyncUIState.Synced -> {
                syncState.lastUploadDate?.let { date ->
                    lastUploadMessage?.let { Text(it(date), Modifier.testTag(AccountE2eIds.LAST_SYNC_LABEL)) }
                }
                syncState.lastDownloadDate?.let { date ->
                    lastDownloadMessage?.let { Text(it(date), Modifier.testTag(AccountE2eIds.LAST_SYNC_LABEL)) }
                }
            }
        }
    }
}

@Composable
private fun DisconnectConfirmationDialog(
    isVisible: Boolean,
    uiState: AccountConnectionUiState,
    disconnectConfirmed: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (isVisible && !disconnectConfirmed && uiState.canDisconnect()) {
        val strings = LocalFeedFlowStrings.current
        ConfirmationDialog(
            title = strings.desktopAccountDisconnectTitle,
            message = strings.desktopAccountDisconnectMessage,
            onConfirm = {
                if (!disconnectConfirmed && uiState.canDisconnect()) onConfirm()
            },
            onDismiss = onDismiss,
            confirmLabel = strings.accountDisconnectButton,
        )
    }
}

private fun AccountConnectionUiState.canDisconnect(): Boolean =
    this is AccountConnectionUiState.Linked && syncState != AccountSyncUIState.Loading

@Composable
internal fun AccountLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(LocalFeedFlowStrings.current.desktopAccountUpdating)
        }
    }
}
