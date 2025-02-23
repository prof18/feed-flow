package com.prof18.feedflow.shared.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun EditCategoryDialog(
    showDialog: Boolean,
    categoryId: CategoryId,
    initialCategoryName: String,
    onDismiss: () -> Unit,
    onEditCategory: (CategoryId, CategoryName) -> Unit,
) {
    if (showDialog) {
        var editedCategoryName by remember { mutableStateOf(initialCategoryName) }

        AlertDialog(
            onDismissRequest = {
                onDismiss()
            },
            title = { Text(LocalFeedFlowStrings.current.editCategory) },
            text = {
                OutlinedTextField(
                    value = editedCategoryName,
                    onValueChange = { editedCategoryName = it },
                    label = { Text(LocalFeedFlowStrings.current.categoryName) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = editedCategoryName.isNotBlank(),
                    onClick = {
                        if (editedCategoryName.isNotBlank()) {
                            onEditCategory(categoryId, CategoryName(editedCategoryName))
                        }
                        onDismiss()
                    }
                ) {
                    Text(LocalFeedFlowStrings.current.actionSave)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                    }
                ) {
                    Text(LocalFeedFlowStrings.current.deleteCategoryCloseButton)
                }
            }
        )
    }
}
