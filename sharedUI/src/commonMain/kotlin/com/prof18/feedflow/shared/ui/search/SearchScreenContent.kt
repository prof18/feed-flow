package com.prof18.feedflow.shared.ui.search

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedItemUrlTitle
import com.prof18.feedflow.core.model.SearchFilter
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.shared.ui.home.components.list.FeedItemContainer
import com.prof18.feedflow.shared.ui.home.components.list.FeedItemView
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.ConditionalAnimatedVisibility
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.delay

@Composable
fun SearchScreenContent(
    searchState: SearchState,
    searchQuery: String,
    searchFilter: SearchFilter,
    currentFeedFilter: FeedFilter?,
    feedFontSizes: FeedFontSizes,
    shareMenuLabel: String,
    shareCommentsMenuLabel: String,
    updateSearchQuery: (String) -> Unit,
    onSearchFilterSelected: (SearchFilter) -> Unit,
    navigateBack: () -> Unit,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onMarkAllAboveAsRead: (String) -> Unit,
    onMarkAllBelowAsRead: (String) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    onShareClick: (FeedItemUrlTitle) -> Unit,
    onOpenFeedSettings: (com.prof18.feedflow.core.model.FeedSource) -> Unit,
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
        val layoutDir = LocalLayoutDirection.current
        Column(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .padding(start = padding.calculateLeftPadding(layoutDir))
                .padding(end = padding.calculateRightPadding(layoutDir)),
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

            SearchFilterChipsRow(
                selectedFilter = searchFilter,
                currentFeedFilter = currentFeedFilter,
                onFilterSelected = onSearchFilterSelected,
            )

            LazyColumn(
                modifier = Modifier
                    .padding(top = Spacing.regular)
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
                                    onOpenFeedSettings = onOpenFeedSettings,
                                    feedLayout = searchState.feedLayout,
                                    onShareClick = onShareClick,
                                    onMarkAllAboveAsRead = onMarkAllAboveAsRead,
                                    onMarkAllBelowAsRead = onMarkAllBelowAsRead,
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(padding.calculateBottomPadding()))
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
            ConditionalAnimatedVisibility(
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

@Composable
private fun SearchFilterChipsRow(
    selectedFilter: SearchFilter,
    currentFeedFilter: FeedFilter?,
    onFilterSelected: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val currentFeedLabel = remember(currentFeedFilter, strings) {
        currentFeedFilter?.getLabel(strings)
    }
    val filters = remember(currentFeedLabel) {
        buildList {
            if (currentFeedLabel != null) {
                add(SearchFilter.CurrentFeed)
            }
            add(SearchFilter.All)
            add(SearchFilter.Read)
            add(SearchFilter.Bookmarks)
        }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        items(
            items = filters,
            key = { it.name },
        ) { filter ->
            SearchFilterChip(
                label = filter.getLabel(strings, currentFeedLabel),
                isSelected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
private fun SearchFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xsmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

private fun SearchFilter.getLabel(strings: FeedFlowStrings, currentFeedLabel: String?): String {
    return when (this) {
        SearchFilter.CurrentFeed -> currentFeedLabel ?: strings.searchFilterAll
        SearchFilter.All -> strings.searchFilterAll
        SearchFilter.Read -> strings.searchFilterRead
        SearchFilter.Bookmarks -> strings.searchFilterBookmarks
    }
}

private fun FeedFilter.getLabel(strings: FeedFlowStrings): String {
    return when (this) {
        is FeedFilter.Category -> feedCategory.title
        is FeedFilter.Source -> feedSource.title
        FeedFilter.Uncategorized -> strings.noCategory
        FeedFilter.Timeline -> strings.searchFilterAll
        FeedFilter.Read -> strings.searchFilterRead
        FeedFilter.Bookmarks -> strings.searchFilterBookmarks
    }
}
