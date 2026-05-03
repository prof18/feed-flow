package com.prof18.feedflow.shared.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun AddCategoryDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    validateCategoryName: (CategoryId?, CategoryName) -> CategoryNameValidationResult,
    onAddCategory: (CategoryName) -> Unit,
) {
    if (showDialog) {
        var categoryName by remember { mutableStateOf("") }
        val validationResult = validateCategoryName(null, CategoryName(name = categoryName))
        val hasDuplicateName = validationResult == CategoryNameValidationResult.DUPLICATE
        val isAddAllowed = validationResult == CategoryNameValidationResult.VALID

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = LocalFeedFlowStrings.current.addFeedCategoryTitle,
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    singleLine = true,
                    isError = hasDuplicateName,
                    supportingText = if (hasDuplicateName) {
                        {
                            Text(LocalFeedFlowStrings.current.categoryNameAlreadyExists)
                        }
                    } else {
                        null
                    },
                    placeholder = {
                        Text(LocalFeedFlowStrings.current.newCategoryHint)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isAddAllowed) {
                                onAddCategory(CategoryName(name = categoryName.trim()))
                            }
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isAddAllowed) {
                            onAddCategory(CategoryName(name = categoryName.trim()))
                        }
                    },
                    enabled = isAddAllowed,
                ) {
                    Text(LocalFeedFlowStrings.current.confirmButton)
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
