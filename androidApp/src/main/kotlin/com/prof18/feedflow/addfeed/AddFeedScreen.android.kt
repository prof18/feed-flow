@file:OptIn(ExperimentalMaterial3Api::class)

package com.prof18.feedflow.addfeed

import FeedFlowTheme
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.domain.model.FeedAddedState
import com.prof18.feedflow.presentation.AddFeedViewModel
import com.prof18.feedflow.presentation.preview.categoriesState
import com.prof18.feedflow.ui.addfeed.AddFeedsContent
import com.prof18.feedflow.ui.preview.FeedFlowPreview
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddFeedScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<AddFeedViewModel>()
    var feedUrl by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.feedAddedState.collect { feedAddedState ->
            when (feedAddedState) {
                is FeedAddedState.Error -> {
                    showError = true
                    errorMessage = feedAddedState.errorMessage.toString(context)
                }

                is FeedAddedState.FeedAdded -> {
                    feedUrl = ""
                    val message = feedAddedState.message.toString(context)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT)
                        .show()
                }

                FeedAddedState.FeedNotAdded -> {
                    showError = false
                    errorMessage = ""
                }
            }
        }
    }

    val categoriesState by viewModel.categoriesState.collectAsStateWithLifecycle()

    AddFeedScreenContent(
        feedUrl = feedUrl,
        showError = showError,
        errorMessage = errorMessage,
        categoriesState = categoriesState,
        onFeedUrlUpdated = { url ->
            feedUrl = url
            viewModel.updateFeedUrlTextFieldValue(url)
        },
        addFeed = {
            viewModel.addFeed()
        },
        navigateBack = navigateBack,
        onExpandClick = {
            viewModel.onExpandCategoryClick()
        },
        onAddCategoryClick = { categoryName ->
            viewModel.addNewCategory(categoryName)
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AddFeedScreenContent(
    feedUrl: String,
    showError: Boolean,
    errorMessage: String,
    categoriesState: CategoriesState,
    onFeedUrlUpdated: (String) -> Unit,
    addFeed: () -> Unit,
    navigateBack: () -> Unit,
    onExpandClick: () -> Unit,
    onAddCategoryClick: (CategoryName) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(resource = MR.strings.add_feed))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navigateBack()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        AddFeedsContent(
            paddingValues = paddingValues,
            feedUrl = feedUrl,
            showError = showError,
            errorMessage = errorMessage,
            categoriesState = categoriesState,
            onFeedUrlUpdated = onFeedUrlUpdated,
            addFeed = addFeed,
            onExpandClick = onExpandClick,
            onAddCategoryClick = onAddCategoryClick,
        )
    }
}

@FeedFlowPreview
@Composable
private fun AddScreenContentPreview() {
    FeedFlowTheme {
        AddFeedScreenContent(
            feedUrl = "https://www.ablog.com/feed",
            showError = false,
            errorMessage = "",
            categoriesState = categoriesState,
            onFeedUrlUpdated = {},
            addFeed = { },
            navigateBack = {},
            onExpandClick = {},
            onAddCategoryClick = {},
        )
    }
}

@FeedFlowPreview
@Composable
private fun AddScreenContentInvalidUrlPreview() {
    FeedFlowTheme {
        AddFeedScreenContent(
            feedUrl = "https://www.ablog.com/feed",
            showError = true,
            errorMessage = "The link you provided is not a valid RSS feed",
            categoriesState = categoriesState,
            onFeedUrlUpdated = {},
            addFeed = { },
            navigateBack = {},
            onExpandClick = {},
            onAddCategoryClick = {},
        )
    }
}
