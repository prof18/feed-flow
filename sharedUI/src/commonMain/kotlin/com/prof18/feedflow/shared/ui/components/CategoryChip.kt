package com.prof18.feedflow.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.shared.ui.feedsourcelist.singleAndLongClickModifier
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onCategoryClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    usePrimaryColor: Boolean = true,
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val hasContextMenu = onEditClick != null || onDeleteClick != null

    val (containerColor, contentColor, borderColor) = if (isSelected) {
        if (usePrimaryColor) {
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
    } else {
        Triple(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.onSurface,
            MaterialTheme.colorScheme.outline,
        )
    }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.singleAndLongClickModifier(
                onClick = onCategoryClick,
                onLongClick = if (hasContextMenu) {
                    { showContextMenu = true }
                } else {
                    null
                },
            ),
            shape = MaterialTheme.shapes.small,
            color = containerColor,
            border = BorderStroke(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = contentColor,
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = contentColor,
                )
            }
        }

        if (hasContextMenu) {
            CategoryChipContextMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick,
            )
        }
    }
}

@Composable
private fun CategoryChipContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onEditClick: (() -> Unit)?,
    onDeleteClick: (() -> Unit)?,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        if (onEditClick != null) {
            DropdownMenuItem(
                text = { Text(LocalFeedFlowStrings.current.editCategory) },
                onClick = {
                    onEditClick()
                    onDismissRequest()
                },
            )
        }
        if (onDeleteClick != null) {
            DropdownMenuItem(
                text = { Text(LocalFeedFlowStrings.current.deleteFeed) },
                onClick = {
                    onDeleteClick()
                    onDismissRequest()
                },
            )
        }
    }
}
