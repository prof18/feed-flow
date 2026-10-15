package com.prof18.feedflow.desktop.categoryselection

import androidx.compose.runtime.Composable
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.desktop.feedforms.CategoryEditorDialog

@Composable
fun EditCategoryDialog(
    categoryState: CategoriesState,
    onCategorySelected: (CategoryId) -> Unit,
    onAddCategory: (CategoryName) -> Unit,
    onDeleteCategory: (CategoryId) -> Unit,
    onEditCategory: (CategoryId, CategoryName) -> Unit,
    validateCategoryName: (CategoryId?, CategoryName) -> CategoryNameValidationResult,
    onDismiss: () -> Unit,
) = CategoryEditorDialog(
    categoryState,
    onCategorySelected,
    onAddCategory,
    onDeleteCategory,
    onEditCategory,
    validateCategoryName,
    onDismiss,
)
