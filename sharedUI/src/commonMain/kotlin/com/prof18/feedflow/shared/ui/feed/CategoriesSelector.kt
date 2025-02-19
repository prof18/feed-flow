package com.prof18.feedflow.shared.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.tagForTesting

@Composable
internal fun CategoriesSelector(
    categoriesState: CategoriesState,
    onExpandClick: () -> Unit,
    onAddCategoryClick: (CategoryName) -> Unit,
    onDeleteCategoryClick: (CategoryId) -> Unit,
    modifier: Modifier = Modifier,
) {
    ExposedDropdownMenuBox(
        expanded = categoriesState.isExpanded,
        onExpandedChange = { onExpandClick() },
        modifier = modifier,
    ) {
        val header = if (categoriesState.header == null) {
            LocalFeedFlowStrings.current.noCategorySelectedHeader
        } else {
            requireNotNull(categoriesState.header)
        }

        Column {
            OutlinedTextField(
                value = header,
                onValueChange = {},
                readOnly = true,
                label = { Text(LocalFeedFlowStrings.current.addFeedCategoryTitle) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriesState.isExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
            )

            CategoriesList(
                categoriesState = categoriesState,
                onAddCategoryClick = onAddCategoryClick,
                onDeleteCategoryClick = onDeleteCategoryClick,
                modifier = Modifier
                    .padding(top = Spacing.small),
            )
        }
    }
}

@Composable
private fun CategoriesList(
    modifier: Modifier,
    categoriesState: CategoriesState,
    onAddCategoryClick: (CategoryName) -> Unit,
    onDeleteCategoryClick: (CategoryId) -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategoryId?>(null) }

    if (showDeleteDialog && categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                categoryToDelete = null
            },
            title = { Text(LocalFeedFlowStrings.current.deleteCategoryConfirmationTitle) },
            text = { Text(LocalFeedFlowStrings.current.deleteCategoryConfirmationMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryToDelete?.let { onDeleteCategoryClick(it) }
                        showDeleteDialog = false
                        categoryToDelete = null
                    }
                ) {
                    Text(LocalFeedFlowStrings.current.deleteFeed)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        categoryToDelete = null
                    }
                ) {
                    Text(LocalFeedFlowStrings.current.deleteCategoryCloseButton)
                }
            }
        )
    }
    AnimatedVisibility(
        modifier = modifier,
        visible = categoriesState.isExpanded,
        enter = expandVertically(
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntSize.VisibilityThreshold,
            ),
        ),
        exit = shrinkVertically(),
    ) {
        Column {
            categoriesState.categories.forEach { category ->
                Row(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            category.onClick(CategoryId(category.id))
                        }
                        .fillMaxWidth()
                        .padding(vertical = Spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        modifier = Modifier
                            .padding(vertical = Spacing.small)
                            .padding(end = Spacing.small)
                            .tagForTesting("${TestingTag.CATEGORY_RADIO_BUTTON}_${category.name}"),
                        selected = category.isSelected,
                        onClick = null,
                    )
                    Text(
                        text = category.name
                            ?: LocalFeedFlowStrings.current.noCategorySelectedHeader,
                    )

                    Spacer(Modifier.weight(1f))

                    if (category.name != null) {
                        IconButton(
                            modifier = Modifier
                                .tagForTesting("${TestingTag.DELETE_CATEGORY_BUTTON}_${category.name}"),
                            onClick = {
                                showDeleteDialog = true
                                categoryToDelete = CategoryId(category.id)
                            },
                        ) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }

            NewCategoryComposer(
                modifier = Modifier
                    .padding(bottom = Spacing.regular),
                onAddClick = { categoryName ->
                    onAddCategoryClick(categoryName)
                },
            )
        }
    }
}

@Composable
internal fun NewCategoryComposer(
    modifier: Modifier = Modifier,
    onAddClick: (CategoryName) -> Unit,
) {
    var categoryName by remember {
        mutableStateOf("")
    }
    val isAddAllowed = categoryName.isNotBlank()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .tagForTesting(TestingTag.CATEGORY_TEXT_INPUT),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (isAddAllowed) {
                        onAddClick(CategoryName(name = categoryName))
                    }
                },
            ),
            value = categoryName,
            onValueChange = { categoryName = it },
            placeholder = {
                Text(
                    text = LocalFeedFlowStrings.current.newCategoryHint,
                )
            },
            trailingIcon = {
                IconButton(
                    modifier = Modifier
                        .tagForTesting(TestingTag.ADD_CATEGORY_BUTTON),
                    onClick = {
                        onAddClick(CategoryName(name = categoryName))
                        categoryName = ""
                    },
                    enabled = isAddAllowed,
                ) {
                    Icon(
                        Icons.Outlined.AddCircleOutline,
                        contentDescription = null,
                    )
                }
            },
        )
    }
}
