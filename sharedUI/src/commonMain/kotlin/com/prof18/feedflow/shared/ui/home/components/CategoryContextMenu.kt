package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.PopupProperties
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun CategoryContextMenu(
    showMenu: Boolean,
    hideMenu: () -> Unit,
    onEditCategoryClick: () -> Unit,
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = hideMenu,
        properties = PopupProperties(),
    ) {
        DropdownMenuItem(
            text = {
                Text(LocalFeedFlowStrings.current.editCategory)
            },
            onClick = {
                onEditCategoryClick()
            },
        )
    }
}