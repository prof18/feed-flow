package com.prof18.feedflow.shared.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ChangeFeedCategoryDialog(
    showDialog: Boolean,
    feedSource: FeedSource,
    categories: ImmutableList<FeedSourceCategory>,
    onDismiss: () -> Unit,
    onCategorySelected: (FeedSource, FeedSourceCategory?) -> Unit,
) {
    if (showDialog) {
        var selectedCategory by remember { mutableStateOf(feedSource.category) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(LocalFeedFlowStrings.current.changeCategory) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Option for no category
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategory = null }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                        )
                        Text(
                            text = LocalFeedFlowStrings.current.noCategory,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    // List of available categories
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategory = category }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedCategory?.id == category.id,
                                onClick = { selectedCategory = category },
                            )
                            Text(
                                text = category.title,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCategorySelected(feedSource, selectedCategory)
                        onDismiss()
                    },
                ) {
                    Text(LocalFeedFlowStrings.current.actionSave)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                ) {
                    Text(LocalFeedFlowStrings.current.deleteCategoryCloseButton)
                }
            },
        )
    }
}
