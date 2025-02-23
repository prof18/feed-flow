package com.prof18.feedflow.desktop.addfeed

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.presentation.AddFeedViewModel
import com.prof18.feedflow.shared.presentation.preview.categoriesExpandedState
import com.prof18.feedflow.shared.ui.feed.addfeed.AddFeedContent
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun AddFeedScreenContent(
    onFeedAdded: () -> Unit,
    modifier: Modifier = Modifier,
    topAppBar: @Composable () -> Unit = {},
) {
    var feedUrl by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val viewModel = desktopViewModel { DI.koin.get<AddFeedViewModel>() }

    val snackbarHostState = remember { SnackbarHostState() }

    val latestOnFeedAdded by rememberUpdatedState(onFeedAdded)
    val strings = LocalFeedFlowStrings.current
    LaunchedEffect(Unit) {
        viewModel.feedAddedState.collect { feedAddedState ->
            when (feedAddedState) {
                is FeedAddedState.Error -> {
                    showLoading = false
                    showError = true
                    errorMessage = when (feedAddedState) {
                        FeedAddedState.Error.InvalidUrl -> strings.invalidRssUrl
                        FeedAddedState.Error.InvalidTitleLink -> strings.missingTitleAndLink
                        FeedAddedState.Error.GenericError -> strings.addFeedGenericError
                    }
                }

                is FeedAddedState.FeedAdded -> {
                    val feedName = feedAddedState.feedName
                    val message = if (feedName != null) {
                        strings.feedAddedMessage(feedName)
                    } else {
                        strings.feedAddedMessageWithoutName
                    }

                    snackbarHostState.showSnackbar(
                        message,
                        duration = SnackbarDuration.Short,
                    )

                    showLoading = false
                    feedUrl = ""
                    latestOnFeedAdded()
                }

                FeedAddedState.FeedNotAdded -> {
                    showLoading = false
                    showError = false
                    errorMessage = ""
                }

                FeedAddedState.Loading -> {
                    showLoading = true
                }
            }
        }
    }

    val categoriesState by viewModel.categoriesState.collectAsState()

    AddFeedContent(
        modifier = modifier,
        feedUrl = feedUrl,
        showError = showError,
        showLoading = showLoading,
        errorMessage = errorMessage,
        onFeedUrlUpdated = { url ->
            feedUrl = url
            viewModel.updateFeedUrlTextFieldValue(url)
        },
        addFeed = {
            viewModel.addFeed()
        },
        categoriesState = categoriesState,
        onExpandClick = {
            viewModel.onExpandCategoryClick()
        },
        onAddCategoryClick = { categoryName ->
            viewModel.addNewCategory(categoryName)
        },
        onDeleteCategoryClick = { categoryId ->
            viewModel.deleteCategory(categoryId.value)
        },
        onEditCategoryClick = { categoryId, newName ->
            viewModel.editCategory(categoryId, newName)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topAppBar = topAppBar,
    )
}

@Preview
@Composable
private fun AddScreenContentPreview() {
    FeedFlowTheme {
        AddFeedContent(
            feedUrl = "https://www.ablog.com/feed",
            showError = false,
            showLoading = false,
            errorMessage = "",
            snackbarHost = { SnackbarHost(SnackbarHostState()) },
            categoriesState = categoriesExpandedState,
            onFeedUrlUpdated = {},
            addFeed = { },
            onExpandClick = {},
            onAddCategoryClick = {},
            onDeleteCategoryClick = {},
            onEditCategoryClick = { _, _ -> },
        )
    }
}
