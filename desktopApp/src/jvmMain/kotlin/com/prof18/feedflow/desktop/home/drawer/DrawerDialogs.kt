package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.runtime.Composable
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.desktop.feedforms.CategoryNameDialog
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.ConfirmationDialog

@Composable
internal fun DrawerDeleteAllFeedsDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onDeleteAllFeeds: () -> Unit,
) {
    if (!showDialog) return
    val strings = LocalFeedFlowStrings.current
    ConfirmationDialog(
        title = strings.deleteAllFeedsConfirmationTitle,
        message = strings.deleteAllFeedsConfirmationMessage,
        confirmLabel = strings.deleteAllFeedsInCategory,
        onConfirm = onDeleteAllFeeds,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun DrawerDeleteCategoryDialog(
    showDialog: Boolean,
    categoryId: CategoryId,
    onDismiss: () -> Unit,
    onDeleteCategory: (CategoryId) -> Unit,
) {
    if (!showDialog) return
    val strings = LocalFeedFlowStrings.current
    ConfirmationDialog(
        title = strings.deleteCategoryConfirmationTitle,
        message = strings.deleteCategoryConfirmationMessage,
        confirmLabel = strings.deleteCategory,
        onConfirm = { onDeleteCategory(categoryId) },
        onDismiss = onDismiss,
    )
}

@Composable
internal fun DrawerEditCategoryNameDialog(
    showDialog: Boolean,
    categoryId: CategoryId,
    initialCategoryName: String,
    onDismiss: () -> Unit,
    validateCategoryName: (CategoryId?, CategoryName) -> CategoryNameValidationResult,
    onEditCategory: (CategoryId, CategoryName) -> Unit,
) {
    CategoryNameDialog(
        visible = showDialog,
        title = LocalFeedFlowStrings.current.editCategory,
        initialName = initialCategoryName,
        categoryId = categoryId,
        validateCategoryName = validateCategoryName,
        onDismiss = onDismiss,
        onSave = { name ->
            onEditCategory(categoryId, name)
            onDismiss()
        },
    )
}
