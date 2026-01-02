package com.prof18.feedflow.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList

@Composable
fun CategoryFlowRow(
    categories: ImmutableList<CategoriesState.CategoryItem>,
    onCategorySelected: (CategoryId) -> Unit,
    onEditCategory: (CategoriesState.CategoryItem) -> Unit,
    onDeleteCategory: (CategoryId) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        categories.forEach { category ->
            CategoryChip(
                label = category.name ?: LocalFeedFlowStrings.current.noCategorySelectedHeader,
                isSelected = category.isSelected,
                onCategoryClick = { onCategorySelected(CategoryId(category.id)) },
                onEditClick = if (category.name != null) {
                    { onEditCategory(category) }
                } else {
                    null
                },
                onDeleteClick = if (category.name != null) {
                    { onDeleteCategory(CategoryId(category.id)) }
                } else {
                    null
                },
                usePrimaryColor = category.name != null,
            )
        }
    }
}

@Composable
fun EditCategoryDialogs(
    showAddCategoryDialog: Boolean,
    categoryToDelete: CategoryId?,
    categoryToEdit: CategoriesState.CategoryItem?,
    onDismissAddDialog: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onDismissEditDialog: () -> Unit,
    onAddCategory: (CategoryName) -> Unit,
    onDeleteCategory: (CategoryId) -> Unit,
    onEditCategory: (CategoryId, CategoryName) -> Unit,
) {
    AddCategoryDialog(
        showDialog = showAddCategoryDialog,
        onDismiss = onDismissAddDialog,
        onAddCategory = { categoryName ->
            onAddCategory(categoryName)
            onDismissAddDialog()
        },
    )

    DeleteCategoryDialog(
        showDialog = categoryToDelete != null,
        categoryId = categoryToDelete ?: CategoryId(""),
        onDismiss = onDismissDeleteDialog,
        onDeleteCategory = { categoryId ->
            onDeleteCategory(categoryId)
            onDismissDeleteDialog()
        },
    )

    EditCategoryNameDialog(
        showDialog = categoryToEdit != null,
        categoryId = CategoryId(categoryToEdit?.id ?: ""),
        initialCategoryName = categoryToEdit?.name ?: "",
        onDismiss = onDismissEditDialog,
        onEditCategory = { categoryId, newName ->
            onEditCategory(categoryId, newName)
            onDismissEditDialog()
        },
    )
}

@Composable
fun AddCategoryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.small))
        Text(
            text = LocalFeedFlowStrings.current.addFeedCategoryTitle,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun CategoryLoadingOverlay(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
