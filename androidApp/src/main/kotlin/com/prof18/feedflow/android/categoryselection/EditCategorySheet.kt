package com.prof18.feedflow.android.categoryselection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.shared.ui.components.AddCategoryButton
import com.prof18.feedflow.shared.ui.components.CategoryFlowRow
import com.prof18.feedflow.shared.ui.components.CategoryLoadingOverlay
import com.prof18.feedflow.shared.ui.components.EditCategoryDialogs
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun EditCategorySheet(
    sheetState: SheetState,
    categoryState: CategoriesState,
    onCategorySelected: (CategoryId) -> Unit,
    onAddCategory: (CategoryName) -> Unit,
    onDeleteCategory: (CategoryId) -> Unit,
    onEditCategory: (CategoryId, CategoryName) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        EditCategorySheetContent(
            categoryState = categoryState,
            onCategorySelected = onCategorySelected,
            onAddCategory = onAddCategory,
            onDeleteCategory = onDeleteCategory,
            onEditCategory = onEditCategory,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun EditCategorySheetContent(
    categoryState: CategoriesState,
    onCategorySelected: (CategoryId) -> Unit,
    onAddCategory: (CategoryName) -> Unit,
    onDeleteCategory: (CategoryId) -> Unit,
    onEditCategory: (CategoryId, CategoryName) -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryToDelete by remember { mutableStateOf<CategoryId?>(null) }
    var categoryToEdit by remember { mutableStateOf<CategoriesState.CategoryItem?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.regular)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.regular),
                text = LocalFeedFlowStrings.current.addFeedCategoriesTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            CategoryFlowRow(
                categories = categoryState.categories,
                onCategorySelected = onCategorySelected,
                onEditCategory = { categoryToEdit = it },
                onDeleteCategory = { categoryToDelete = it },
            )

            Spacer(modifier = Modifier.height(Spacing.regular))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                AddCategoryButton(
                    onClick = { showAddCategoryDialog = true },
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = LocalFeedFlowStrings.current.actionSave,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))
        }

        if (categoryState.isLoading) {
            CategoryLoadingOverlay(
                modifier = Modifier.matchParentSize(),
            )
        }
    }

    EditCategoryDialogs(
        showAddCategoryDialog = showAddCategoryDialog,
        categoryToDelete = categoryToDelete,
        categoryToEdit = categoryToEdit,
        onDismissAddDialog = { showAddCategoryDialog = false },
        onDismissDeleteDialog = { categoryToDelete = null },
        onDismissEditDialog = { categoryToEdit = null },
        onAddCategory = onAddCategory,
        onDeleteCategory = onDeleteCategory,
        onEditCategory = onEditCategory,
    )
}
