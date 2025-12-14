package com.prof18.feedflow.desktop.categoryselection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.components.EditCategoryNameDialog as SharedEditCategoryDialog

// TODO: find shared code with EditCategorySheet
@Composable
fun EditCategoryDialog(
    categoryState: CategoriesState,
    onCategorySelected: (CategoryId) -> Unit,
    onAddCategory: (CategoryName) -> Unit,
    onDeleteCategory: (CategoryId) -> Unit,
    onEditCategory: (CategoryId, CategoryName) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = LocalFeedFlowStrings.current.addFeedCategoriesTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            EditCategoryDialogContent(
                categoryState = categoryState,
                onCategorySelected = onCategorySelected,
                onAddCategory = onAddCategory,
                onDeleteCategory = onDeleteCategory,
                onEditCategory = onEditCategory,
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(
                    text = LocalFeedFlowStrings.current.actionSave,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditCategoryDialogContent(
    categoryState: CategoriesState,
    onCategorySelected: (CategoryId) -> Unit,
    onAddCategory: (CategoryName) -> Unit,
    onDeleteCategory: (CategoryId) -> Unit,
    onEditCategory: (CategoryId, CategoryName) -> Unit,
) {
    var categoryToDelete by remember { mutableStateOf<CategoryId?>(null) }
    var categoryToEdit by remember { mutableStateOf<CategoriesState.CategoryItem?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                categoryState.categories.forEach { category ->
                    CategoryChipItem(
                        label = category.name ?: LocalFeedFlowStrings.current.noCategorySelectedHeader,
                        isSelected = category.isSelected,
                        onCategoryClick = { onCategorySelected(CategoryId(category.id)) },
                        onEditClick = if (category.name != null) {
                            { categoryToEdit = category }
                        } else {
                            null
                        },
                        onDeleteClick = if (category.name != null) {
                            { categoryToDelete = CategoryId(category.id) }
                        } else {
                            null
                        },
                        usePrimaryColor = category.name != null,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.regular))

            TextButton(
                onClick = { showAddCategoryDialog = true },
                modifier = Modifier.fillMaxWidth(),
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

        if (categoryState.isLoading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onAddClick = { categoryName ->
                onAddCategory(categoryName)
                showAddCategoryDialog = false
            },
        )
    }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Text(LocalFeedFlowStrings.current.deleteCategoryConfirmationTitle)
            },
            text = {
                Text(LocalFeedFlowStrings.current.deleteCategoryConfirmationMessage)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryToDelete?.let { onDeleteCategory(it) }
                        categoryToDelete = null
                    },
                ) {
                    Text(
                        LocalFeedFlowStrings.current.confirmButton,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { categoryToDelete = null },
                ) {
                    Text(LocalFeedFlowStrings.current.deleteCategoryCloseButton)
                }
            },
        )
    }

    SharedEditCategoryDialog(
        showDialog = categoryToEdit != null,
        categoryId = CategoryId(categoryToEdit?.id ?: ""),
        initialCategoryName = categoryToEdit?.name ?: "",
        onDismiss = { categoryToEdit = null },
        onEditCategory = { categoryId, newName ->
            onEditCategory(categoryId, newName)
            categoryToEdit = null
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryChipItem(
    label: String,
    isSelected: Boolean,
    onCategoryClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    usePrimaryColor: Boolean = true,
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val hasContextMenu = onEditClick != null || onDeleteClick != null

    val (selectedContainer, selectedContent, selectedBorder) = if (usePrimaryColor) {
        Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            MaterialTheme.colorScheme.primary,
        )
    } else {
        Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            MaterialTheme.colorScheme.secondary,
        )
    }

    Box {
        FilterChip(
            selected = isSelected,
            onClick = onCategoryClick,
            modifier = if (hasContextMenu) {
                Modifier.onClick(
                    matcher = PointerMatcher.mouse(PointerButton.Secondary),
                    onClick = { showContextMenu = true },
                )
            } else {
                Modifier
            },
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            },
            leadingIcon = if (isSelected) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                null
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                labelColor = MaterialTheme.colorScheme.onSurface,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedContainerColor = selectedContainer,
                selectedLabelColor = selectedContent,
                selectedLeadingIconColor = selectedContent,
            ),
            border = FilterChipDefaults.filterChipBorder(
                borderColor = MaterialTheme.colorScheme.outline,
                selectedBorderColor = selectedBorder,
                borderWidth = 1.dp,
                selectedBorderWidth = 1.5.dp,
                enabled = true,
                selected = isSelected,
            ),
        )

        if (hasContextMenu) {
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
            ) {
                if (onEditClick != null) {
                    DropdownMenuItem(
                        text = { Text(LocalFeedFlowStrings.current.editCategory) },
                        onClick = {
                            onEditClick()
                            showContextMenu = false
                        },
                    )
                }
                if (onDeleteClick != null) {
                    DropdownMenuItem(
                        text = { Text(LocalFeedFlowStrings.current.deleteFeed) },
                        onClick = {
                            onDeleteClick()
                            showContextMenu = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAddClick: (CategoryName) -> Unit,
) {
    var categoryName by remember { mutableStateOf("") }
    val isAddAllowed = categoryName.isNotBlank()

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
                            onAddClick(CategoryName(name = categoryName.trim()))
                        }
                    },
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isAddAllowed) {
                        onAddClick(CategoryName(name = categoryName.trim()))
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
