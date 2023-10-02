package com.prof18.feedflow.ui.addfeed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun CategoriesSelector(
    modifier: Modifier = Modifier,
    categoriesState: CategoriesState,
    onExpandClick: () -> Unit,
    onAddCategoryClick: (CategoryName) -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        @Suppress("MagicNumber")
        val degrees by animateFloatAsState(
            if (categoriesState.isExpanded) {
                -90f
            } else {
                90f
            },
            label = "Expand Icon Animation",
        )
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable {
                    onExpandClick()
                }
                .fillMaxWidth()
                .padding(vertical = Spacing.regular),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val header = if (categoriesState.header == null) {
                stringResource(MR.strings.no_category_selected_header)
            } else {
                requireNotNull(categoriesState.header)
            }

            Text(header)

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.rotate(degrees),
            )
        }
        CategoriesList(
            categoriesState = categoriesState,
            onAddCategoryClick = onAddCategoryClick,
        )
    }
}

@Composable
private fun CategoriesList(
    categoriesState: CategoriesState,
    onAddCategoryClick: (CategoryName) -> Unit,
) {
    AnimatedVisibility(
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
                            .padding(end = Spacing.small),
                        selected = category.isSelected,
                        onClick = null,
                    )
                    Text(
                        text = category.name,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.weight(1f))
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
fun NewCategoryComposer(
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
        TextField(
            modifier = Modifier
                .weight(1f),
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
                    text = stringResource(MR.strings.new_category_hint),
                )
            },
        )
        IconButton(
            onClick = {
                onAddClick(CategoryName(name = categoryName))
            },
            enabled = isAddAllowed,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
            )
        }
    }
}
