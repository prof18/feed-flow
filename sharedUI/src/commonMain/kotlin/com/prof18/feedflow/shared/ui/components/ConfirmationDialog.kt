package com.prof18.feedflow.shared.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.PreviewTheme

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmButtonText: String = LocalFeedFlowStrings.current.confirmButton,
    dismissButtonText: String = LocalFeedFlowStrings.current.cancelButton,
    isDestructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = if (isDestructive) {
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    )
                } else {
                    ButtonDefaults.textButtonColors()
                },
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(dismissButtonText)
            }
        },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun ConfirmationDialogPreview() {
    PreviewTheme {
        ConfirmationDialog(
            title = "Delete Article",
            message = "Are you sure you want to delete this article? This action cannot be undone.",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun ConfirmationDialogDestructivePreview() {
    PreviewTheme {
        ConfirmationDialog(
            title = "Clear All Data",
            message = "This will permanently delete all downloaded articles and cached data.",
            onConfirm = {},
            onDismiss = {},
            isDestructive = true,
        )
    }
}
