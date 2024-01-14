package com.prof18.feedflow.android.addfeed

import FeedFlowTheme
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.presentation.AddFeedViewModel
import com.prof18.feedflow.shared.presentation.preview.categoriesExpandedState
import com.prof18.feedflow.shared.ui.addfeed.AddFeedContent
import com.prof18.feedflow.shared.ui.preview.FeedFlowPhonePreview
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

    AddFeedContent(
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
        onExpandClick = {
            viewModel.onExpandCategoryClick()
        },
        onAddCategoryClick = { categoryName ->
            viewModel.addNewCategory(categoryName)
        },
        onDeleteCategoryClick = { categoryId ->
            viewModel.deleteCategory(categoryId.value)
        },
        topAppBar = {
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
    )
}

@FeedFlowPhonePreview
@Composable
private fun AddScreenContentPreview() {
    FeedFlowTheme {
        AddFeedContent(
            feedUrl = "https://www.ablog.com/feed",
            showError = false,
            errorMessage = "",
            categoriesState = categoriesExpandedState,
            onFeedUrlUpdated = {},
            addFeed = { },
            onExpandClick = {},
            onAddCategoryClick = {},
            onDeleteCategoryClick = {},
            topAppBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(resource = MR.strings.add_feed))
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {},
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                )
            },
        )
    }
}
