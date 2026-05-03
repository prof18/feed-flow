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
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun EditCategoryNameDialog(
    showDialog: Boolean,
    categoryId: CategoryId,
    initialCategoryName: String,
    onDismiss: () -> Unit,
    validateCategoryName: (CategoryId?, CategoryName) -> CategoryNameValidationResult,
    onEditCategory: (CategoryId, CategoryName) -> Unit,
) {
    if (showDialog) {
        var editedCategoryName by remember(initialCategoryName) { mutableStateOf(initialCategoryName) }
        val categoryName = CategoryName(editedCategoryName)
        val validationResult = validateCategoryName(categoryId, categoryName)
        val hasDuplicateName = validationResult == CategoryNameValidationResult.DUPLICATE
        val canSave = validationResult == CategoryNameValidationResult.VALID
        val strings = LocalFeedFlowStrings.current

        AlertDialog(
            onDismissRequest = {
                onDismiss()
            },
            title = { Text(strings.editCategory) },
            text = {
                OutlinedTextField(
                    value = editedCategoryName,
                    onValueChange = { editedCategoryName = it },
                    label = { Text(strings.categoryName) },
                    singleLine = true,
                    isError = hasDuplicateName,
                    supportingText = if (hasDuplicateName) {
                        {
                            Text(strings.categoryNameAlreadyExists)
                        }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = canSave,
                    onClick = {
                        if (canSave) {
                            onEditCategory(categoryId, categoryName)
                        }
                        onDismiss()
                    },
                ) {
                    Text(strings.actionSave)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                    },
                ) {
                    Text(strings.deleteCategoryCloseButton)
                }
            },
        )
    }
}
