package com.prof18.feedflow.shared.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun DeleteAllFeedsInCategoryDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onDeleteAllFeeds: () -> Unit,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(LocalFeedFlowStrings.current.deleteAllFeedsConfirmationTitle) },
            text = { Text(LocalFeedFlowStrings.current.deleteAllFeedsConfirmationMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAllFeeds()
                        onDismiss()
                    },
                ) {
                    Text(
                        text = LocalFeedFlowStrings.current.confirmButton,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(LocalFeedFlowStrings.current.deleteCategoryCloseButton)
                }
            },
        )
    }
}
