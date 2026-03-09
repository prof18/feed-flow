package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import com.prof18.feedflow.core.model.CategoryId

@Composable
internal expect fun CategoryContextMenu(
    showMenu: Boolean,
    menuPositionInWindow: Offset?,
    hideMenu: () -> Unit,
    categoryId: CategoryId,
    onEditCategoryClick: () -> Unit,
    onDeleteCategoryClick: (CategoryId) -> Unit,
)
