package com.prof18.feedflow.android.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun SupportPurchaseThankYouDialog(
    onDismiss: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.supportPurchaseThankYouTitle) },
        text = { Text(strings.supportPurchaseThankYouMessage) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionDone)
            }
        },
    )
}
