package com.prof18.feedflow.desktop.editfeed

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import com.prof18.feedflow.shared.presentation.EditFeedViewModel
import com.prof18.feedflow.shared.presentation.preview.categoriesExpandedState
import com.prof18.feedflow.shared.ui.feed.editfeed.EditFeedContent
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.tagForTesting

internal data class EditFeedScreen(
    private val feedSource: FeedSource,
) : Screen {

    @Composable
    override fun Content() {
        val viewModel = desktopViewModel { DI.koin.get<EditFeedViewModel>() }

        LaunchedEffect(feedSource) {
            viewModel.loadFeedToEdit(feedSource)
        }

        val feedUrl by viewModel.feedUrlState.collectAsState()
        val feedName by viewModel.feedNameState.collectAsState()
        val linkOpeningPreference by viewModel.linkOpeningPreferenceState.collectAsState()
        val isHidden by viewModel.isHiddenFromTimelineState.collectAsState()
        var showLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        var showError by remember { mutableStateOf(false) }

        val categoriesState by viewModel.categoriesState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        val strings = LocalFeedFlowStrings.current
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(Unit) {
            viewModel.feedEditedState.collect { feedAddedState ->
                when (feedAddedState) {
                    is FeedEditedState.Error -> {
                        showError = true
                        showLoading = false
                        errorMessage = when (feedAddedState) {
                            FeedEditedState.Error.InvalidUrl -> strings.invalidRssUrl
                            FeedEditedState.Error.InvalidTitleLink -> strings.missingTitleAndLink
                            FeedEditedState.Error.GenericError -> strings.editFeedGenericError
                        }
                    }

                    is FeedEditedState.FeedEdited -> {
                        showLoading = false
                        val message = strings.feedEditedMessage(feedAddedState.feedName)
                        snackbarHostState.showSnackbar(
                            message,
                            duration = SnackbarDuration.Short,
                        )
                        navigator.pop()
                    }

                    FeedEditedState.Idle -> {
                        showLoading = false
                        showError = false
                        errorMessage = ""
                    }

                    FeedEditedState.Loading -> {
                        showLoading = true
                    }
                }
            }
        }

        EditFeedContent(
            feedUrl = feedUrl,
            feedName = feedName,
            showError = showError,
            errorMessage = errorMessage,
            showLoading = showLoading,
            categoriesState = categoriesState,
            canEditUrl = viewModel.canEditUrl(),
            linkOpeningPreference = linkOpeningPreference,
            onFeedUrlUpdated = { url ->
                viewModel.updateFeedUrlTextFieldValue(url)
            },
            onFeedNameUpdated = { name ->
                viewModel.updateFeedNameTextFieldValue(name)
            },
            onLinkOpeningPreferenceSelected = { preference ->
                viewModel.updateLinkOpeningPreference(preference)
            },
            isHidden = isHidden,
            onHiddenToggled = { hidden ->
                viewModel.updateIsHiddenFromTimeline(hidden)
            },
            editFeed = {
                viewModel.editFeed()
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topAppBar = {
                TopAppBar(
                    title = {
                        Text(LocalFeedFlowStrings.current.editFeed)
                    },
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier
                                .tagForTesting(TestingTag.BACK_BUTTON),
                            onClick = {
                                navigator.pop()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                )
            },
        )
    }
}

@Preview
@Composable
private fun EditScreenPreview() {
    FeedFlowTheme {
        EditFeedContent(
            feedName = "Feed Name",
            feedUrl = "https://www.ablog.com/feed",
            showError = false,
            showLoading = false,
            canEditUrl = true,
            errorMessage = "",
            categoriesState = categoriesExpandedState,
            linkOpeningPreference = LinkOpeningPreference.DEFAULT,
            isHidden = false,
            onHiddenToggled = {},
            onFeedUrlUpdated = {},
            onFeedNameUpdated = {},
            onLinkOpeningPreferenceSelected = {},
            editFeed = { },
            onExpandClick = {},
            onAddCategoryClick = {},
            onDeleteCategoryClick = {},
            topAppBar = {
                TopAppBar(
                    title = {
                        Text(LocalFeedFlowStrings.current.editFeed)
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {},
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                )
            },
        )
    }
}
