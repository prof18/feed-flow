@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.prof18.feedflow.desktop.feedsuggestions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedAddState
import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory
import com.prof18.feedflow.desktop.testing.FeedSuggestionsE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionOperationState
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionOperationStatus
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedSuggestionOperationUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `add failure offers retry for the same feed and category`() {
        val additions = mutableListOf<Pair<SuggestedFeed, String>>()
        content(
            addState = FeedAddState.NotAdded,
            operation = FeedSuggestionOperationStatus.AddFailed,
            onAdd = { feed, category -> additions += feed to category },
        )

        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionAddError).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.retryButton).performSemanticsAction(SemanticsActions.OnClick) { it() }

        rule.runOnIdle { assertEquals(listOf(feed to category.name), additions) }
    }

    @Test
    fun `update failure keeps added action disabled and does not offer add retry`() {
        content(addState = FeedAddState.Added, operation = FeedSuggestionOperationStatus.UpdateFailed)

        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionUpdateError).assertIsDisplayed()
        rule.onNodeWithTag(FeedSuggestionsE2eIds.addButton(feed.url)).assertIsNotEnabled()
        rule.onNodeWithText(EnFeedFlowStrings.retryButton).assertDoesNotExist()
    }

    @Test
    fun `refreshing an added feed announces progress and blocks its add action`() {
        content(addState = FeedAddState.Added, operation = FeedSuggestionOperationStatus.Refreshing)

        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionRefreshing).assertIsDisplayed()
        rule.onNodeWithTag(FeedSuggestionsE2eIds.addButton(feed.url)).assertIsNotEnabled()
        rule.onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertCountEquals(1)
    }

    @Test
    fun `queued add callback is ignored while add or refresh operation is busy`() {
        val addState = mutableStateOf(FeedAddState.NotAdded)
        val operation = mutableStateOf<FeedSuggestionOperationStatus?>(null)
        var additions = 0
        content(
            onAdd = { _, _ -> additions++ },
            body = { Suggestions(addState.value, operation.value, onAdd = { _, _ -> additions++ }) },
        )
        val captured = rule.onNodeWithTag(FeedSuggestionsE2eIds.addButton(feed.url))
            .fetchSemanticsNode().config[SemanticsActions.OnClick].action!!

        listOf(FeedSuggestionOperationStatus.Adding, FeedSuggestionOperationStatus.Refreshing).forEach { busy ->
            rule.runOnIdle { operation.value = busy }
            rule.runOnIdle {
                captured()
                assertEquals(0, additions, "A queued click must not duplicate $busy")
            }
            rule.runOnIdle { operation.value = null }
        }
    }

    @Test
    fun `successful add clears prior operation feedback`() {
        val operation = mutableStateOf(FeedSuggestionOperationStatus.AddFailed)
        val addState = mutableStateOf(FeedAddState.NotAdded)
        content(body = { Suggestions(addState.value, operation.value) })
        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionAddError).assertIsDisplayed()

        rule.runOnIdle {
            addState.value = FeedAddState.Added
            operation.value = FeedSuggestionOperationStatus.Added
        }

        rule.onNodeWithText(EnFeedFlowStrings.feedAddedMessageWithoutName).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionAddError).assertDoesNotExist()
        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionUpdateError).assertDoesNotExist()
        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionRefreshing).assertDoesNotExist()
    }

    @Test
    fun `operation errors remain readable in compact enlarged RTL layout`() {
        compact {
            Suggestions(
                addState = FeedAddState.NotAdded,
                operationStatus = FeedSuggestionOperationStatus.AddFailed,
            )
        }

        val layoutResults = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionAddError)
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layoutResults) }
        rule.runOnIdle {
            assertTrue(layoutResults.isNotEmpty())
            assertFalse(layoutResults.single().hasVisualOverflow)
        }
    }

    private val feed = SuggestedFeed(
        name = "Fixture source",
        url = "https://fixture.example.invalid/rss",
        description = "A deterministic suggestion fixture with no remote image",
    )
    private val category = SuggestedFeedCategory("fixture", "Fixture category", "", listOf(feed))

    @Composable
    private fun Suggestions(
        addState: FeedAddState,
        operationStatus: FeedSuggestionOperationStatus?,
        onAdd: (SuggestedFeed, String) -> Unit = { _, _ -> },
    ) {
        FeedSuggestionsContent(
            categories = persistentListOf(category),
            selectedCategoryId = category.id,
            feedStates = persistentMapOf(feed.url to addState),
            isLoading = false,
            onCategorySelected = {},
            onAddFeed = onAdd,
            onCloseRequest = {},
            operationStates = operationStatus?.let {
                persistentMapOf(feed.url to FeedSuggestionOperationState(generation = 1, status = it))
            } ?: persistentMapOf(),
        )
    }

    private fun compact(body: @Composable () -> Unit) {
        content(fontScale = 2f, direction = LayoutDirection.Rtl) {
            Box(Modifier.size(width = 360.dp, height = 500.dp)) { body() }
        }
    }

    private fun content(
        addState: FeedAddState = FeedAddState.NotAdded,
        operation: FeedSuggestionOperationStatus? = null,
        onAdd: (SuggestedFeed, String) -> Unit = { _, _ -> },
        fontScale: Float = 1f,
        direction: LayoutDirection = LayoutDirection.Ltr,
        body: @Composable () -> Unit = { Suggestions(addState, operation, onAdd) },
    ) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalLayoutDirection provides direction,
                LocalDensity provides Density(density.density, fontScale),
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false, content = body)
            }
        }
    }
}
