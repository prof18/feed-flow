package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.PopupProperties
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.shared.ui.components.DeleteCategoryDialog
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun CategoryContextMenu(
    showMenu: Boolean,
    hideMenu: () -> Unit,
    categoryId: CategoryId,
    onEditCategoryClick: () -> Unit,
    onDeleteCategoryClick: (CategoryId) -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = hideMenu,
        properties = PopupProperties(),
    ) {
        DropdownMenuItem(
            text = {
                Text(LocalFeedFlowStrings.current.editFeedSourceNameButton)
            },
            onClick = {
                hideMenu()
                onEditCategoryClick()
            },
        )

        DropdownMenuItem(
            text = {
                Text(LocalFeedFlowStrings.current.deleteFeed)
            },
            onClick = {
                hideMenu()
                showDeleteDialog = true
            },
        )
    }

    DeleteCategoryDialog(
        showDialog = showDeleteDialog,
        categoryId = categoryId,
        onDismiss = {
            showDeleteDialog = false
        },
        onDeleteCategory = { id ->
            onDeleteCategoryClick(id)
            showDeleteDialog = false
        }
    )
}
