package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedItemUrlTitle
import com.prof18.feedflow.core.model.SearchFilter
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import com.prof18.feedflow.shared.presentation.model.SearchRequestState
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.RadioButtonRow
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

@Composable
internal fun DesktopSearchScreenContent(
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
    onOpenFeedWebsite: (String) -> Unit,
    modifier: Modifier = Modifier,
    isGridLayoutEnabled: Boolean = true,
    feedItemDisplaySettings: FeedItemDisplaySettings = FeedItemDisplaySettings(),
    searchRequestState: SearchRequestState? = null,
    onRetrySearch: () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {
    },
) {
    val strings = LocalFeedFlowStrings.current
    val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp
    val listState = key(searchQuery, searchFilter) { rememberLazyListState() }
    val gridState = key(searchQuery, searchFilter) { rememberLazyStaggeredGridState() }
    var selectedArticleId by key(searchQuery, searchFilter) { rememberSaveable { mutableStateOf<String?>(null) } }
    val queryFocusRequester = remember { FocusRequester() }
    val visibleResult = when (searchRequestState) {
        null -> searchState
        is SearchRequestState.Results -> searchRequestState.result.takeIf {
            searchRequestState.request.query == searchQuery && searchRequestState.request.filter == searchFilter
        }
        else -> null
    }
    Box(
        modifier = modifier.fillMaxSize().desktopSearchShortcuts(
            onSearch = { queryFocusRequester.requestFocus() },
            onBack = navigateBack,
        ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchControls(searchQuery, updateSearchQuery, navigateBack, queryFocusRequester)
            SearchFilters(searchFilter, currentFeedFilter, onSearchFilterSelected)
            when (searchRequestState) {
                SearchRequestState.Idle -> Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 72.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = JewelTheme.globalColors.text.info,
                        modifier = Modifier.padding(bottom = 6.dp).size(32.dp),
                    )
                    Text(strings.searchHintTitle, fontWeight = FontWeight.Medium)
                    Text(
                        strings.searchHintSubtitle,
                        color = JewelTheme.globalColors.text.info,
                        textAlign = TextAlign.Center,
                    )
                }
                is SearchRequestState.Loading -> Row(
                    modifier = Modifier.padding(16.dp).testTag("search-loading").semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                    },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text(strings.desktopSearchLoading(searchQuery))
                }
                is SearchRequestState.Error -> Column(
                    modifier = Modifier.padding(16.dp).testTag("search-error"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(strings.desktopSearchError)
                    OutlinedButton(onClick = onRetrySearch, modifier = Modifier.heightIn(min = actionHeight)) {
                        Text(strings.retryButton)
                    }
                }
                else -> Unit
            }
            when (visibleResult) {
                null, SearchState.EmptyState -> Unit
                is SearchState.NoDataFound -> Text(
                    strings.searchNoData(visibleResult.searchQuery),
                    modifier = Modifier.padding(16.dp),
                )
                is SearchState.DataFound -> {
                    Text(
                        strings.desktopSearchResultContext(searchQuery),
                        modifier = Modifier.padding(16.dp).semantics { heading() },
                    )
                    DesktopArticleList(
                        feedItems = visibleResult.items,
                        nextFeedState = NextFeedDisplayState.NextFeedDisplayDisabledState,
                        feedFontSize = feedFontSizes,
                        feedLayout = visibleResult.feedLayout,
                        currentFeedFilter = currentFeedFilter ?: FeedFilter.Timeline,
                        shareMenuLabel = shareMenuLabel,
                        shareCommentsMenuLabel = shareCommentsMenuLabel,
                        swipeActions = SwipeActions(SwipeActionType.NONE, SwipeActionType.NONE),
                        onVisibleFeedItemsChanged = {},
                        requestMoreItems = {},
                        onFeedItemClick = {
                            selectedArticleId = it.id
                            onFeedItemClick(it)
                        },
                        onOpenInBrowser = {
                            selectedArticleId = it.id
                            onFeedItemClick(it.copy(articleOpenMode = ArticleOpenMode.PREFERRED_BROWSER))
                        },
                        onBookmarkClick = onBookmarkClick,
                        onReadStatusClick = onReadStatusClick,
                        onCommentClick = onCommentClick,
                        markAllAsRead = {},
                        onShareClick = onShareClick,
                        onOpenFeedSettings = onOpenFeedSettings,
                        onOpenFeedWebsite = onOpenFeedWebsite,
                        onNavigateNext = {},
                        onMarkAllAboveAsRead = onMarkAllAboveAsRead,
                        onMarkAllBelowAsRead = onMarkAllBelowAsRead,
                        isGridLayoutEnabled = visibleResult.isGridLayoutEnabled && isGridLayoutEnabled,
                        feedItemDisplaySettings = feedItemDisplaySettings,
                        listState = listState,
                        gridState = gridState,
                        selectedArticleId = selectedArticleId,
                        showFooter = false,
                        modifier = Modifier.weight(1f).padding(top = 8.dp),
                    )
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) { snackbarHost() }
    }
}

@Composable
private fun SearchControls(
    query: String,
    updateQuery: (String) -> Unit,
    navigateBack: () -> Unit,
    focusRequester: FocusRequester,
) {
    val strings = LocalFeedFlowStrings.current
    val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp
    val fieldState = rememberTextFieldState(initialText = query)
    val focusManager = LocalFocusManager.current
    val latestUpdate by rememberUpdatedState(updateQuery)
    LaunchedEffect(fieldState) {
        snapshotFlow { fieldState.text.toString() }.distinctUntilChanged().collect { latestUpdate(it) }
    }
    LaunchedEffect(query) {
        if (fieldState.text.toString() != query) fieldState.setTextAndPlaceCursorAtEnd(query)
    }
    LaunchedEffect(focusRequester) {
        withFrameNanos { }
        focusRequester.requestFocus()
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            state = fieldState,
            modifier = Modifier.weight(1f).heightIn(min = actionHeight)
                .testTag("search-query").focusRequester(focusRequester),
            placeholder = { Text(strings.searchPlaceholder) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = JewelTheme.globalColors.text.info,
                    modifier = Modifier.padding(end = 6.dp).size(16.dp),
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onKeyboardAction = KeyboardActionHandler { focusManager.clearFocus() },
        )
        if (fieldState.text.isNotEmpty()) {
            OutlinedButton(
                onClick = {
                    fieldState.setTextAndPlaceCursorAtEnd("")
                    latestUpdate("")
                    focusRequester.requestFocus()
                },
                modifier = Modifier.heightIn(min = actionHeight).testTag("search-clear"),
            ) { Text(strings.desktopSearchClearButton) }
        }
        OutlinedButton(onClick = navigateBack, modifier = Modifier.heightIn(min = actionHeight)) {
            Text(strings.desktopSearchBackButton)
        }
    }
}

internal fun Modifier.desktopSearchShortcuts(onSearch: () -> Unit, onBack: (() -> Unit)? = null): Modifier =
    onKeyEvent { event ->
        when {
            event.type != KeyEventType.KeyDown -> false
            event.key == Key.F && (event.isCtrlPressed || event.isMetaPressed) -> {
                onSearch()
                true
            }
            event.key == Key.Escape && onBack != null -> {
                onBack()
                true
            }
            else -> false
        }
    }

@Composable
private fun SearchFilters(selected: SearchFilter, currentFeedFilter: FeedFilter?, select: (SearchFilter) -> Unit) {
    val strings = LocalFeedFlowStrings.current
    val currentLabel = currentFeedFilter?.let { filter ->
        when (filter) {
            is FeedFilter.Source -> filter.feedSource.title
            is FeedFilter.Category -> filter.feedCategory.title
            FeedFilter.Uncategorized -> strings.noCategory
            FeedFilter.Timeline -> strings.searchFilterAll
            FeedFilter.Read -> strings.searchFilterRead
            FeedFilter.Bookmarks -> strings.searchFilterBookmarks
        }
    }
    val choices = buildList {
        if (currentLabel != null) add(SearchFilter.CurrentFeed to currentLabel)
        add(SearchFilter.All to strings.searchFilterAll)
        add(SearchFilter.Read to strings.searchFilterRead)
        add(SearchFilter.Bookmarks to strings.searchFilterBookmarks)
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(choices, key = { it.first }) { (filter, label) ->
            RadioButtonRow(text = label, selected = selected == filter, onClick = { select(filter) })
        }
    }
}
