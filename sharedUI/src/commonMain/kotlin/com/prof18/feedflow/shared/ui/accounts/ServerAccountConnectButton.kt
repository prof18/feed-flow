package com.prof18.feedflow.shared.ui.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.prof18.feedflow.shared.ui.components.ConfirmationDialog
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun ServerAccountConnectButton(
    isLoginLoading: Boolean,
    isEnabled: Boolean,
    hasLocalSubscriptions: Boolean,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirmation by remember { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current

    Column(modifier = modifier.fillMaxWidth()) {
        if (hasLocalSubscriptions) {
            Text(
                text = strings.serverAccountSubscriptionsWarning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.small),
            )
        }

        Button(
            onClick = {
                if (hasLocalSubscriptions) {
                    showConfirmation = true
                } else {
                    onConnectClick()
                }
            },
            modifier = Modifier
                .testTag(AccountE2eIds.CONNECT_BUTTON)
                .fillMaxWidth(),
            enabled = !isLoginLoading && isEnabled,
        ) {
            if (isLoginLoading) {
                CircularProgressIndicator()
            } else {
                Text(strings.accountConnectButton)
            }
        }

        if (showConfirmation && hasLocalSubscriptions) {
            ConfirmationDialog(
                title = strings.serverAccountReplaceLocalTitle,
                message = strings.serverAccountReplaceLocalMessage,
                confirmButtonText = strings.serverAccountReplaceAndConnectButton,
                onConfirm = onConnectClick,
                onDismiss = { showConfirmation = false },
                isDestructive = true,
            )
        }
    }
}
