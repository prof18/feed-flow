package com.prof18.feedflow.shared.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun DeleteCategoryDialog(
    showDialog: Boolean,
    categoryId: CategoryId,
    onDismiss: () -> Unit,
    onDeleteCategory: (CategoryId) -> Unit,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                onDismiss()
            },
            title = { Text(LocalFeedFlowStrings.current.deleteCategoryConfirmationTitle) },
            text = { Text(LocalFeedFlowStrings.current.deleteCategoryConfirmationMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(categoryId)
                        onDismiss()
                    },
                ) {
                    Text(LocalFeedFlowStrings.current.deleteFeed)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                    },
                ) {
                    Text(LocalFeedFlowStrings.current.deleteCategoryCloseButton)
                }
            },
        )
    }
}
