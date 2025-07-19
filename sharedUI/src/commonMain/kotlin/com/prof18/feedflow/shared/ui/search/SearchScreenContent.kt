package com.prof18.feedflow.shared.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedItemUrlTitle
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.shared.ui.home.components.list.FeedItemContainer
import com.prof18.feedflow.shared.ui.home.components.list.FeedItemView
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.delay

@Composable
fun SearchScreenContent(
    searchState: SearchState,
    searchQuery: String,
    feedFontSizes: FeedFontSizes,
    shareMenuLabel: String,
    shareCommentsMenuLabel: String,
    updateSearchQuery: (String) -> Unit,
    navigateBack: () -> Unit,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    onShareClick: (FeedItemUrlTitle) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {
    },
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(searchQuery, TextRange(searchQuery.length))) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // Sometimes is crashing because the focusRequester is done too ealy
        delay(timeMillis = 100)
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = snackbarHost,
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
        ) {
            SearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.regular),
                searchQuery = textFieldValue,
                onSearchQueryChange = {
                    textFieldValue = it
                    updateSearchQuery(it.text)
                },
                focusRequester = focusRequester,
                clearFocus = {
                    focusManager.clearFocus()
                },
                navigateBack = navigateBack,
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                when (searchState) {
                    is SearchState.EmptyState -> {
                        // No-op
                    }

                    is SearchState.NoDataFound -> {
                        item {
                            NoDataFoundView(searchState)
                        }
                    }

                    is SearchState.DataFound -> {
                        itemsIndexed(searchState.items) { index, item ->
                            FeedItemContainer(feedLayout = searchState.feedLayout) {
                                FeedItemView(
                                    feedItem = item,
                                    feedFontSize = feedFontSizes,
                                    shareCommentsMenuLabel = shareCommentsMenuLabel,
                                    shareMenuLabel = shareMenuLabel,
                                    onFeedItemClick = onFeedItemClick,
                                    onBookmarkClick = onBookmarkClick,
                                    onReadStatusClick = onReadStatusClick,
                                    onCommentClick = onCommentClick,
                                    feedLayout = searchState.feedLayout,
                                    onShareClick = onShareClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoDataFoundView(
    state: SearchState.NoDataFound,
) {
    Text(
        modifier = Modifier
            .padding(horizontal = Spacing.regular),
        text = LocalFeedFlowStrings.current.searchNoData(state.searchQuery),
    )
}

@Composable
private fun SearchBar(
    searchQuery: TextFieldValue,
    focusRequester: FocusRequester,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    clearFocus: () -> Unit,
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
) {
    OutlinedTextField(
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        placeholder = {
            Text(LocalFeedFlowStrings.current.searchPlaceholder)
        },
        leadingIcon = {
            IconButton(
                onClick = {
                    clearFocus()
                    navigateBack()
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                )
            }
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .fillMaxWidth(),
        value = searchQuery,
        singleLine = true,
        keyboardActions = KeyboardActions(
            onDone = {
                clearFocus()
            },
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
        onValueChange = onSearchQueryChange,
        trailingIcon = {
            AnimatedVisibility(
                visible = searchQuery.text.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                IconButton(onClick = { onSearchQueryChange(TextFieldValue("")) }) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
    )
}
