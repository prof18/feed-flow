package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.SearchFilter
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.desktop.main.MainWindowSurface
import com.prof18.feedflow.desktop.testing.FeedItemE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.SearchRequest
import com.prof18.feedflow.shared.presentation.model.SearchRequestState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DesktopSearchJourneyTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun `idle search gives the user a prompt`() {
        show(query = "", requestState = SearchRequestState.Idle)

        rule.onNodeWithText(EnFeedFlowStrings.searchHintTitle).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.searchHintSubtitle).assertIsDisplayed()
    }

    @Test
    fun `home shortcut works before any child has keyboard focus`() {
        var searches = 0
        rule.setContent {
            FeedFlowTheme(darkTheme = false) {
                MainWindowSurface(modifier = Modifier.testTag("search-shortcut-surface"), onSearch = { searches++ }) {
                    Box(Modifier.requiredSize(width = 500.dp, height = 480.dp))
                }
            }
        }
        rule.onNodeWithTag("search-shortcut-surface").performKeyInput {
            keyDown(Key.MetaLeft)
            pressKey(Key.F)
            keyUp(Key.MetaLeft)
        }
        rule.runOnIdle { assertEquals(1, searches) }
    }

    @Test
    fun `loading a new search hides an older article`() {
        show(
            query = "new",
            requestState = SearchRequestState.Loading(request("new")),
            previousState = resultWithArticle(),
        )

        rule.onNodeWithTag("search-loading").assertIsDisplayed()
        rule.onNodeWithText(ArticleRowTest.ARTICLE_TITLE).assertDoesNotExist()
    }

    @Test
    fun `failed search keeps query and offers retry`() {
        var retries = 0
        show(
            query = "failed",
            requestState = SearchRequestState.Error(request("failed")),
            previousState = resultWithArticle(),
            onRetry = { retries++ },
        )

        rule.onNodeWithTag("search-error").assertIsDisplayed()
        rule.onNodeWithText(ArticleRowTest.ARTICLE_TITLE).assertDoesNotExist()
        rule.onNodeWithText(EnFeedFlowStrings.retryButton).performClick()
        rule.runOnIdle { assertEquals(1, retries) }
        rule.onNodeWithTag("search-clear").assertIsDisplayed()
    }

    @Test
    fun `results for an older query never appear`() {
        val completed = SearchRequestState.Results(request("old"), resultWithArticle())
        show(query = "new", requestState = completed)
        rule.onNodeWithText(ArticleRowTest.ARTICLE_TITLE).assertDoesNotExist()
    }

    @Test
    fun `results for an older filter never appear`() {
        val completed = SearchRequestState.Results(request("old"), resultWithArticle())
        show(
            query = "old",
            filter = SearchFilter.Bookmarks,
            requestState = completed,
        )
        rule.onNodeWithText(ArticleRowTest.ARTICLE_TITLE).assertDoesNotExist()
    }

    @Test
    fun `matching result opens the focused article with Enter`() {
        val opened = mutableListOf<FeedItemUrlInfo>()
        show(
            query = "article",
            requestState = SearchRequestState.Results(request("article"), resultWithArticle()),
            onOpen = opened::add,
        )

        val row = rule.onNodeWithTag(ArticleRowTest.ARTICLE_ROW_TAG)
        row.requestFocus()
        row.performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(ArticleRowTest.ARTICLE_ID, opened.single().id) }
        row.assertIsSelected()
    }

    @Test
    fun `enlarged RTL search actions render complete labels at compact size`() {
        show(
            query = "failed",
            requestState = SearchRequestState.Error(request("failed")),
            fontScale = 2f,
            direction = LayoutDirection.Rtl,
        )
        listOf(
            EnFeedFlowStrings.desktopSearchBackButton,
            EnFeedFlowStrings.desktopSearchClearButton,
            EnFeedFlowStrings.retryButton,
        ).forEach { label ->
            val layouts = mutableListOf<TextLayoutResult>()
            rule.onNodeWithText(label).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertFalse(layouts.single().hasVisualOverflow, "Complete action label must fit: $label")
        }
    }

    @Test
    fun `no matches explain the query and clear it`() {
        val query = mutableStateOf("missing")
        show(
            query = query.value,
            requestState = SearchRequestState.Results(request("missing"), SearchState.NoDataFound("missing")),
            onQueryChange = { query.value = it },
        )

        rule.onNodeWithText(EnFeedFlowStrings.searchNoData("missing")).assertIsDisplayed()
        rule.onNodeWithTag("search-clear").performClick()
        rule.runOnIdle { assertEquals("", query.value) }
    }

    @Test
    fun `Control F returns focus from an article to the search query`() {
        assertFindShortcutReturnsToQuery(Key.CtrlLeft)
    }

    @Test
    fun `Meta F returns focus from an article to the search query`() {
        assertFindShortcutReturnsToQuery(Key.MetaLeft)
    }

    @Test
    fun `Escape on the search query navigates back once`() {
        var backCount = 0
        show(
            query = "article",
            requestState = SearchRequestState.Results(request("article"), resultWithArticle()),
            onNavigateBack = { backCount++ },
        )

        val query = rule.onNodeWithTag("search-query")
        query.assertIsFocused()
        query.performKeyInput { pressKey(Key.Escape) }
        rule.runOnIdle { assertEquals(1, backCount) }
    }

    @Test
    fun `clearing a query returns keyboard focus to the search field`() {
        show(query = "article", requestState = SearchRequestState.Idle)

        rule.onNodeWithTag("search-clear").performClick()
        rule.onNodeWithTag("search-query").assertIsFocused()
    }

    @Test
    fun `Escape dismisses result context menu before leaving search`() {
        var backs = 0
        show(
            query = "article",
            requestState = SearchRequestState.Results(request("article"), resultWithArticle()),
            onNavigateBack = { backs++ },
        )
        val row = rule.onNodeWithTag(ArticleRowTest.ARTICLE_ROW_TAG)
        row.requestFocus()
        row.performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.F10)
            keyUp(Key.ShiftLeft)
        }
        rule.onNodeWithText(EnFeedFlowStrings.menuAddToBookmark).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.openFeedSettings).performKeyInput { pressKey(Key.Escape) }
        rule.onNodeWithText(EnFeedFlowStrings.menuAddToBookmark).assertDoesNotExist()
        row.assertIsFocused()
        rule.runOnIdle { assertEquals(0, backs) }
        row.performKeyInput { pressKey(Key.Escape) }
        rule.runOnIdle { assertEquals(1, backs) }
    }

    @Test
    fun `opening a result marks its row selected`() {
        val opened = mutableListOf<FeedItemUrlInfo>()
        show(
            query = "article",
            requestState = SearchRequestState.Results(request("article"), resultWithArticle()),
            onOpen = opened::add,
        )

        val row = rule.onNodeWithTag(ArticleRowTest.ARTICLE_ROW_TAG)
        row.requestFocus()
        row.performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        row.assertIsSelected()
        assertEquals(ArticleRowTest.ARTICLE_ID, opened.single().id)
    }

    @Test
    fun `loading and finishing the same query retains result scroll position`() {
        val result = SearchState.DataFound(
            items = (0 until 80).map { index ->
                ArticleRowTest.article.copy(id = "search-result-$index", title = "Search result $index")
            }.toImmutableList(),
            feedLayout = FeedLayout.LIST,
            isGridLayoutEnabled = false,
        )
        val request = request("article")
        val status = mutableStateOf<SearchRequestState>(SearchRequestState.Results(request, result))
        show(
            query = "article",
            requestState = status.value,
            requestStateProvider = { status.value },
        )

        rule.onNode(
            hasScrollToIndexAction() and hasAnyDescendant(hasTestTag(FeedItemE2eIds.row("search-result-0"))),
        ).performScrollToIndex(RESULT_SCROLL_INDEX)
        rule.onNodeWithTag(FeedItemE2eIds.row("search-result-45")).assertIsDisplayed()
        rule.runOnIdle { status.value = SearchRequestState.Loading(request) }
        rule.onNodeWithTag(FeedItemE2eIds.row("search-result-45")).assertDoesNotExist()
        rule.runOnIdle { status.value = SearchRequestState.Results(request, result) }
        rule.onNodeWithTag(FeedItemE2eIds.row("search-result-45")).assertIsDisplayed()
    }

    @Test
    fun `returning from reader restores selected result and list position`() {
        val visible = mutableStateOf(true)
        val result = SearchState.DataFound(
            items = (0 until 80).map { index ->
                ArticleRowTest.article.copy(id = "search-result-$index", title = "Search result $index")
            }.toImmutableList(),
            feedLayout = FeedLayout.LIST,
            isGridLayoutEnabled = false,
        )
        show(
            query = "article",
            requestState = SearchRequestState.Results(request("article"), result),
            visibleProvider = { visible.value },
        )
        rule.onNode(
            hasScrollToIndexAction() and hasAnyDescendant(hasTestTag(FeedItemE2eIds.row("search-result-0"))),
        ).performScrollToIndex(RESULT_SCROLL_INDEX)
        val row = rule.onNodeWithTag(FeedItemE2eIds.row("search-result-45"))
        row.requestFocus()
        row.performKeyInput { pressKey(Key.Enter) }
        row.assertIsSelected()
        rule.runOnIdle { visible.value = false }
        row.assertDoesNotExist()
        rule.runOnIdle { visible.value = true }
        row.assertIsDisplayed().assertIsSelected()
        rule.onNodeWithTag("search-query").assertIsFocused()
    }

    private fun assertFindShortcutReturnsToQuery(modifier: Key) {
        val opened = mutableListOf<FeedItemUrlInfo>()
        show(
            query = "article",
            requestState = SearchRequestState.Results(request("article"), resultWithArticle()),
            onOpen = opened::add,
        )

        val row = rule.onNodeWithTag(ArticleRowTest.ARTICLE_ROW_TAG)
        row.requestFocus()
        row.assertIsFocused()
        row.performKeyInput {
            keyDown(modifier)
            pressKey(Key.F)
            keyUp(modifier)
        }
        rule.onNodeWithTag("search-query").assertIsFocused()
        rule.runOnIdle { assertEquals(emptyList(), opened) }
    }

    private fun show(
        query: String,
        requestState: SearchRequestState,
        filter: SearchFilter = SearchFilter.All,
        previousState: SearchState = SearchState.EmptyState,
        onRetry: () -> Unit = {},
        onOpen: (FeedItemUrlInfo) -> Unit = {},
        onQueryChange: (String) -> Unit = {},
        onNavigateBack: () -> Unit = {},
        fontScale: Float = 1f,
        direction: LayoutDirection = LayoutDirection.Ltr,
        requestStateProvider: () -> SearchRequestState = { requestState },
        visibleProvider: () -> Boolean = { true },
    ) {
        rule.setContent {
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalDensity provides Density(LocalDensity.current.density, fontScale),
                LocalLayoutDirection provides direction,
            ) {
                FeedFlowTheme(darkTheme = false) {
                    Box(Modifier.requiredSize(width = 500.dp, height = 480.dp)) {
                        val holder = rememberSaveableStateHolder()
                        if (visibleProvider()) {
                            holder.SaveableStateProvider("search") {
                                DesktopSearchScreenContent(
                                    searchState = previousState,
                                    searchQuery = query,
                                    searchFilter = filter,
                                    currentFeedFilter = FeedFilter.Timeline,
                                    searchRequestState = requestStateProvider(),
                                    onRetrySearch = onRetry,
                                    feedFontSizes = FeedFontSizes(),
                                    shareMenuLabel = EnFeedFlowStrings.menuCopyLink,
                                    shareCommentsMenuLabel = EnFeedFlowStrings.menuCopyLinkComments,
                                    updateSearchQuery = onQueryChange,
                                    onSearchFilterSelected = {},
                                    navigateBack = onNavigateBack,
                                    onFeedItemClick = onOpen,
                                    onBookmarkClick = { _, _ -> },
                                    onReadStatusClick = { _, _ -> },
                                    onMarkAllAboveAsRead = {},
                                    onMarkAllBelowAsRead = {},
                                    onCommentClick = {},
                                    onShareClick = {},
                                    onOpenFeedSettings = {},
                                    onOpenFeedWebsite = {},
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun request(query: String) = SearchRequest(
        generation = 1,
        query = query,
        filter = SearchFilter.All,
        feedFilter = FeedFilter.Timeline,
    )

    private fun resultWithArticle() = SearchState.DataFound(
        items = persistentListOf(ArticleRowTest.article),
        feedLayout = FeedLayout.LIST,
        isGridLayoutEnabled = false,
    )

    private companion object {
        const val RESULT_SCROLL_INDEX = 45
    }
}
