package com.prof18.feedflow.desktop.addfeed

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.ui.style.FeedFlowTheme
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.presentation.AddFeedViewModel
import com.prof18.feedflow.shared.presentation.preview.categoriesState
import com.prof18.feedflow.shared.ui.addfeed.AddFeedsContent

@Composable
fun AddFeedScreen(
    onFeedAdded: () -> Unit,
) {
    var feedUrl by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val viewModel = desktopViewModel { DI.koin.get<AddFeedViewModel>() }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.feedAddedState.collect { feedAddedState ->
            when (feedAddedState) {
                is FeedAddedState.Error -> {
                    showError = true
                    errorMessage = feedAddedState.errorMessage.localized()
                }

                is FeedAddedState.FeedAdded -> {
                    val message = feedAddedState.message.localized()

                    snackbarHostState.showSnackbar(
                        message,
                        duration = SnackbarDuration.Short,
                    )

                    feedUrl = ""
                    onFeedAdded()
                }

                FeedAddedState.FeedNotAdded -> {
                    showError = false
                    errorMessage = ""
                }
            }
        }
    }

    val categoriesState by viewModel.categoriesState.collectAsState()

    AddFeedScreenContent(
        feedUrl = feedUrl,
        isInvalidUrl = showError,
        errorMessage = errorMessage,
        snackbarHostState = snackbarHostState,
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
    )
}

@Suppress("LongParameterList")
@Composable
private fun AddFeedScreenContent(
    feedUrl: String,
    isInvalidUrl: Boolean,
    errorMessage: String,
    snackbarHostState: SnackbarHostState,
    categoriesState: CategoriesState,
    onFeedUrlUpdated: (String) -> Unit,
    addFeed: () -> Unit,
    onExpandClick: () -> Unit,
    onAddCategoryClick: (CategoryName) -> Unit,
    onDeleteCategoryClick: (CategoryId) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        AddFeedsContent(
            paddingValues = paddingValues,
            feedUrl = feedUrl,
            showError = isInvalidUrl,
            errorMessage = errorMessage,
            categoriesState = categoriesState,
            onFeedUrlUpdated = onFeedUrlUpdated,
            addFeed = addFeed,
            onExpandClick = onExpandClick,
            onAddCategoryClick = onAddCategoryClick,
            onDeleteCategoryClick = onDeleteCategoryClick,
        )
    }
}

@Preview
@Composable
private fun AddScreenContentPreview() {
    FeedFlowTheme {
        AddFeedScreenContent(
            feedUrl = "https://www.ablog.com/feed",
            isInvalidUrl = false,
            errorMessage = "",
            snackbarHostState = SnackbarHostState(),
            categoriesState = categoriesState,
            onFeedUrlUpdated = {},
            addFeed = { },
            onExpandClick = {},
            onAddCategoryClick = {},
            onDeleteCategoryClick = {},
        )
    }
}

@Preview
@Composable
private fun AddScreenContentDarkPreview() {
    FeedFlowTheme(
        darkTheme = true,
    ) {
        AddFeedScreenContent(
            feedUrl = "https://www.ablog.com/feed",
            isInvalidUrl = false,
            errorMessage = "",
            snackbarHostState = SnackbarHostState(),
            categoriesState = categoriesState,
            onFeedUrlUpdated = {},
            addFeed = { },
            onExpandClick = {},
            onAddCategoryClick = {},
            onDeleteCategoryClick = {},
        )
    }
}

@Preview
@Composable
private fun AddScreenContentInvalidUrlPreview() {
    FeedFlowTheme {
        AddFeedScreenContent(
            feedUrl = "https://www.ablog.com/feed",
            isInvalidUrl = true,
            errorMessage = "The link you provided is not a valid RSS feed",
            snackbarHostState = SnackbarHostState(),
            categoriesState = categoriesState,
            onFeedUrlUpdated = {},
            addFeed = { },
            onExpandClick = {},
            onAddCategoryClick = {},
            onDeleteCategoryClick = {},
        )
    }
}
