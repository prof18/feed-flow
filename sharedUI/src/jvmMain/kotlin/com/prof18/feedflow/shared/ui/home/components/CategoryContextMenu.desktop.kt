package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.shared.ui.components.DeleteCategoryDialog
import com.prof18.feedflow.shared.ui.components.menu.DesktopPopupMenu
import com.prof18.feedflow.shared.ui.components.menu.DesktopPopupMenuEntry
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf

@Composable
internal actual fun CategoryContextMenu(
    showMenu: Boolean,
    menuPositionInWindow: Offset?,
    hideMenu: () -> Unit,
    categoryId: CategoryId,
    onEditCategoryClick: () -> Unit,
    onDeleteCategoryClick: (CategoryId) -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current
    val menuEntries = persistentListOf(
        DesktopPopupMenuEntry.Action(
            text = strings.editFeedSourceNameButton,
            onClick = {
                hideMenu()
                onEditCategoryClick()
            },
        ),
        DesktopPopupMenuEntry.Action(
            text = strings.deleteFeed,
            onClick = {
                hideMenu()
                showDeleteDialog = true
            },
        ),
    )

    DesktopPopupMenu(
        showMenu = showMenu,
        menuPositionInWindow = menuPositionInWindow,
        menuEntries = menuEntries,
        closeMenu = hideMenu,
    )

    DeleteCategoryDialog(
        showDialog = showDeleteDialog,
        categoryId = categoryId,
        onDismiss = {
            showDeleteDialog = false
        },
        onDeleteCategory = { id ->
            onDeleteCategoryClick(id)
            showDeleteDialog = false
        },
    )
}
