package com.prof18.feedflow.desktop.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.desktop.settings.SettingsActionRow
import com.prof18.feedflow.desktop.settings.SettingsRow
import com.prof18.feedflow.desktop.settings.SettingsSection
import com.prof18.feedflow.desktop.testing.AccountE2eIds
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.ConfirmationDialog
import com.prof18.feedflow.desktop.ui.components.SnackbarHost
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text

@Composable
internal fun AccountScreen(
    snackbarHostState: DesktopFeedbackState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // The Accounts detail page draws the service header and back navigation.
    Column(modifier.fillMaxSize()) {
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
            Column(modifier.fillMaxSize().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    val strings = LocalFeedFlowStrings.current
    val idle = uiState.syncState != AccountSyncUIState.Loading
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SettingsSection {
            SettingsRow(
                title = successMessage,
                modifier = Modifier.testTag(AccountE2eIds.CONNECTED_MESSAGE),
                leading = {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = ConnectedColor,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            SyncStatus(uiState.syncState, noSyncMessage, lastUploadMessage, lastDownloadMessage)
        }
        SettingsSection {
            if (onBackupClick != null) {
                SettingsActionRow(
                    title = strings.backupButton,
                    onClick = onBackupClick,
                    enabled = idle,
                    modifier = Modifier.testTag("account_backup_button"),
                )
            }
            SettingsActionRow(
                title = strings.accountDisconnectButton,
                onClick = onRequestDisconnect,
                enabled = idle,
                destructive = true,
                modifier = Modifier.testTag(AccountE2eIds.DISCONNECT_BUTTON),
            )
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
    // One emitter: the rows stay inside the parent card, which draws separators between them.
    Column {
        when (syncState) {
            AccountSyncUIState.Loading -> SettingsRow(
                title = LocalFeedFlowStrings.current.accountRefreshProgress,
                trailing = { CircularProgressIndicator() },
            )
            AccountSyncUIState.None -> SettingsRow(title = noSyncMessage)
            is AccountSyncUIState.Synced -> {
                syncState.lastUploadDate?.let { date ->
                    lastUploadMessage?.let {
                        SettingsRow(title = it(date), modifier = Modifier.testTag(AccountE2eIds.LAST_SYNC_LABEL))
                    }
                }
                syncState.lastDownloadDate?.let { date ->
                    lastDownloadMessage?.let {
                        SettingsRow(title = it(date), modifier = Modifier.testTag(AccountE2eIds.LAST_SYNC_LABEL))
                    }
                }
            }
        }
    }
}

private val ConnectedColor = Color(color = 0xFF2F9E6B)

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
