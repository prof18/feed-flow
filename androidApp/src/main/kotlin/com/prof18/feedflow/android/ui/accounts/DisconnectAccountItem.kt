package com.prof18.feedflow.android.ui.accounts

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.prof18.feedflow.android.ui.components.ConfirmationDialog
import com.prof18.feedflow.android.ui.settings.SettingItem
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings

@Composable
fun DisconnectAccountItem(
    kind: DisconnectAccountKind,
    isEnabled: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    var showConfirmation by remember { mutableStateOf(false) }

    // A sync that starts while the dialog is open closes it for good, instead of
    // hiding it and showing it again when the sync ends.
    SideEffect(isEnabled) {
        if (!isEnabled) showConfirmation = false
    }

    Box(modifier = modifier) {
        SettingItem(
            modifier = Modifier.testTag(AccountE2eIds.DISCONNECT_BUTTON),
            title = strings.accountDisconnectButton,
            isEnabled = isEnabled,
            isDestructive = true,
            icon = Icons.Default.LinkOff,
            onClick = { showConfirmation = true },
        )

        if (showConfirmation && isEnabled) {
            ConfirmationDialog(
                title = strings.accountDisconnectTitle,
                message = when (kind) {
                    DisconnectAccountKind.ReaderService -> strings.accountDisconnectServerMessage
                    DisconnectAccountKind.BackupService -> strings.accountDisconnectBackupMessage
                },
                confirmButtonText = strings.accountDisconnectButton,
                // ConfirmationDialog calls onConfirm then onDismiss with no guard of its own;
                // clearing the flag first makes a second click in the same frame a no-op.
                onConfirm = {
                    if (showConfirmation && isEnabled) {
                        showConfirmation = false
                        onConfirm()
                    }
                },
                onDismiss = { showConfirmation = false },
                isDestructive = true,
                confirmButtonTestTag = AccountE2eIds.DISCONNECT_CONFIRM_BUTTON,
            )
        }
    }
}

enum class DisconnectAccountKind {
    ReaderService,
    BackupService,
}
