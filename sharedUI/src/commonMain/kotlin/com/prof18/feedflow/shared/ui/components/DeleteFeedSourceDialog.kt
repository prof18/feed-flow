package com.prof18.feedflow.shared.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun DeleteFeedSourceDialog(
    showDialog: Boolean,
    feedSource: FeedSource,
    onDismiss: () -> Unit,
    onDeleteFeedSource: (FeedSource) -> Unit,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(LocalFeedFlowStrings.current.deleteFeedConfirmationTitle) },
            text = { Text(LocalFeedFlowStrings.current.deleteFeedConfirmationMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFeedSource(feedSource)
                        onDismiss()
                    },
                ) {
                    Text(
                        text = LocalFeedFlowStrings.current.deleteFeedButton,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(LocalFeedFlowStrings.current.cancelButton)
                }
            },
        )
    }
}
