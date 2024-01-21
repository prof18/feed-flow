package com.prof18.feedflow.shared.ui.addfeed

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.tagForTesting
import dev.icerock.moko.resources.compose.stringResource
import java.awt.SystemColor.text

@Composable
fun AddFeedContent(
    feedUrl: String,
    showError: Boolean,
    errorMessage: String,
    categoriesState: CategoriesState,
    onFeedUrlUpdated: (String) -> Unit,
    addFeed: () -> Unit,
    onExpandClick: () -> Unit,
    onAddCategoryClick: (CategoryName) -> Unit,
    onDeleteCategoryClick: (CategoryId) -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    topAppBar: @Composable () -> Unit = {},
) {
    Scaffold(
        topBar = topAppBar,
        snackbarHost = snackbarHost,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = Spacing.regular),
        ) {
            item {
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
                        .padding(top = Spacing.regular)
                        .tagForTesting(TestingTag.CATEGORY_SELECTOR),
                    categoriesState = categoriesState,
                    onExpandClick = onExpandClick,
                    onAddCategoryClick = onAddCategoryClick,
                    onDeleteCategoryClick = onDeleteCategoryClick,
                )
            }

            item {
                Button(
                    modifier = Modifier
                        .padding(top = Spacing.small)
                        .padding(bottom = Spacing.regular)
                        .fillMaxWidth()
                        .tagForTesting(TestingTag.ADD_FEED_BUTTON),
                    enabled = feedUrl.isNotBlank(),
                    onClick = addFeed,
                ) {
                    Text(stringResource(resource = MR.strings.add_feed))
                }
            }
        }
    }
}

@Composable
private fun FeedUrlTextField(
    modifier: Modifier = Modifier,
    feedUrl: String,
    showError: Boolean,
    errorMessage: String,
    onFeedUrlUpdated: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier,
        label = {
            Text(text = stringResource(resource = MR.strings.feed_url))
        },
        isError = showError,
        supportingText = if (showError) {
            {
                Text(
                    modifier = Modifier
                        .tagForTesting(TestingTag.INVALID_URL_ERROR_MESSAGE),
                    text = errorMessage,
                )
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Uri,
            autoCorrect = false,
        ),
        value = feedUrl,
        onValueChange = {
            onFeedUrlUpdated(it)
        },
        placeholder = {
            Text(
                stringResource(resource = MR.strings.feed_url_placeholder),
                maxLines = 1,
            )
        },
        maxLines = 1,
    )
}
