package com.prof18.feedflow.shared.ui.feed.addfeed

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.ui.feed.CategoriesSelector
import com.prof18.feedflow.shared.ui.feed.FeedUrlTextField
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.tagForTesting

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
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
    topAppBar: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = topAppBar,
        snackbarHost = snackbarHost,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
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
                        .fillMaxWidth()
                        .tagForTesting(TestingTag.FEED_URL_INPUT),
                    feedUrl = feedUrl,
                    showError = showError,
                    errorMessage = errorMessage,
                    onFeedUrlUpdated = onFeedUrlUpdated,
                )
            }

            item {
                CategoriesSelector(
                    modifier = Modifier
                        .padding(vertical = Spacing.regular)
                        .tagForTesting(TestingTag.CATEGORY_SELECTOR),
                    categoriesState = categoriesState,
                    onExpandClick = onExpandClick,
                    onAddCategoryClick = onAddCategoryClick,
                    onDeleteCategoryClick = onDeleteCategoryClick,
                    onEditCategoryClick = onEditCategoryClick,
                )
            }

            item {
                Button(
                    modifier = Modifier
                        .padding(top = Spacing.small)
                        .padding(bottom = Spacing.regular)
                        .fillMaxWidth()
                        .tagForTesting(TestingTag.ADD_FEED_BUTTON),
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
        }
    }
}
