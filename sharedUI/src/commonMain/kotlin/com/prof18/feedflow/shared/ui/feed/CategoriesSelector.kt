package com.prof18.feedflow.shared.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun CategoriesSelector(
    categoriesState: CategoriesState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCategory = categoriesState.categories.find { it.isSelected }

    CategorySelectorChip(
        selectedCategory = selectedCategory,
        onClick = onClick,
        modifier = modifier.padding(bottom = Spacing.regular),
    )
}

@Composable
private fun CategorySelectorChip(
    selectedCategory: CategoriesState.CategoryItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoryText = selectedCategory?.name ?: LocalFeedFlowStrings.current.noCategorySelectedHeader
    val hasCategory = selectedCategory?.name != null

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = LocalFeedFlowStrings.current.addFeedCategoryTitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (hasCategory) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Label,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (hasCategory) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )

                Text(
                    text = categoryText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (hasCategory) FontWeight.Medium else FontWeight.Normal,
                    color = if (hasCategory) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = LocalFeedFlowStrings.current.editCategory,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
