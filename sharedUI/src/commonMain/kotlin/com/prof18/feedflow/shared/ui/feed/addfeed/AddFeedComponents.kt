package com.prof18.feedflow.shared.ui.feed.addfeed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.shared.ui.feed.CategoriesSelector
import com.prof18.feedflow.shared.ui.feed.FeedUrlTextField
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun AddFeedContent(
    feedUrl: String,
    showError: Boolean,
    showLoading: Boolean,
    errorMessage: String,
    categoriesState: CategoriesState,
    onFeedUrlUpdated: (String) -> Unit,
    addFeed: () -> Unit,
    onExpandClick: () -> Unit,
    onAddCategoryClick: (CategoryName) -> Unit,
    onDeleteCategoryClick: (CategoryId) -> Unit,
    onEditCategoryClick: (CategoryId, CategoryName) -> Unit,
    isNotificationEnabled: Boolean,
    modifier: Modifier = Modifier,
    showNotificationToggle: Boolean = false,
    onNotificationToggleChanged: (Boolean) -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    topAppBar: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = topAppBar,
        snackbarHost = snackbarHost,
    ) { paddingValues ->
        val layoutDir = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .padding(start = paddingValues.calculateLeftPadding(layoutDir))
                .padding(end = paddingValues.calculateRightPadding(layoutDir))
                .padding(horizontal = Spacing.regular),
        ) {
            item {
                Text(
                    text = LocalFeedFlowStrings.current.feedUrlHelpText,
                    modifier = Modifier
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FeedUrlTextField(
                    modifier = Modifier
                        .padding(top = Spacing.regular)
                        .fillMaxWidth(),
                    feedUrl = feedUrl,
                    showError = showError,
                    errorMessage = errorMessage,
                    onFeedUrlUpdated = onFeedUrlUpdated,
                )
            }

            item {
                CategoriesSelector(
                    modifier = Modifier
                        .padding(vertical = Spacing.regular),
                    categoriesState = categoriesState,
                    onExpandClick = onExpandClick,
                    onAddCategoryClick = onAddCategoryClick,
                    onDeleteCategoryClick = onDeleteCategoryClick,
                    onEditCategoryClick = onEditCategoryClick,
                )
            }

            item {
                if (showNotificationToggle) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.small),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = LocalFeedFlowStrings.current.enableNotificationsForFeed,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Switch(
                            checked = isNotificationEnabled,
                            onCheckedChange = onNotificationToggleChanged,
                        )
                    }
                }
            }

            item {
                Button(
                    modifier = Modifier
                        .padding(top = Spacing.small)
                        .padding(bottom = Spacing.regular)
                        .fillMaxWidth(),
                    enabled = feedUrl.isNotBlank() && !showLoading,
                    onClick = addFeed,
                ) {
                    if (showLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    } else {
                        Text(LocalFeedFlowStrings.current.addFeed)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}
