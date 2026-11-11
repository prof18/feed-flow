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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory
import com.prof18.feedflow.desktop.testing.FeedSuggestionsE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionOperationState
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionOperationStatus
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionUndoOffer
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
            operation = FeedSuggestionOperationStatus.AddFailed,
            onAdd = { feed, category -> additions += feed to category },
        )

        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionAddError).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.retryButton).performSemanticsAction(SemanticsActions.OnClick) { it() }

        rule.runOnIdle { assertEquals(listOf(feed to category.name), additions) }
    }

    @Test
    fun `queued add callback is ignored while the add is running`() {
        val operation = mutableStateOf<FeedSuggestionOperationStatus?>(null)
        var additions = 0
        content(body = { Suggestions(operation.value, onAdd = { _, _ -> additions++ }) })
        val captured = rule.onNodeWithTag(FeedSuggestionsE2eIds.addButton(feed.url))
            .fetchSemanticsNode().config[SemanticsActions.OnClick].action!!

        rule.runOnIdle { operation.value = FeedSuggestionOperationStatus.Adding }
        rule.runOnIdle {
            captured()
            assertEquals(0, additions)
        }
    }

    @Test
    fun `successful add clears the earlier failure and disables the action`() {
        val operation = mutableStateOf<FeedSuggestionOperationStatus?>(FeedSuggestionOperationStatus.AddFailed)
        content(body = { Suggestions(operation.value) })
        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionAddError).assertIsDisplayed()

        rule.runOnIdle { operation.value = FeedSuggestionOperationStatus.Added }

        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionAdded).assertIsDisplayed()
        rule.onNodeWithTag(FeedSuggestionsE2eIds.addButton(feed.url)).assertIsNotEnabled()
        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionAddError).assertDoesNotExist()
    }

    @Test
    fun `add error remains readable in compact enlarged RTL layout`() {
        compact { Suggestions(FeedSuggestionOperationStatus.AddFailed) }

        val layoutResults = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionAddError)
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layoutResults) }
        rule.runOnIdle {
            assertTrue(layoutResults.isNotEmpty())
            assertFalse(layoutResults.single().hasVisualOverflow)
        }
    }

    @Test
    fun `added row names its category only when it is not the selected tab`() {
        content(
            operationState = FeedSuggestionOperationState(
                FeedSuggestionOperationStatus.Added,
                addedToCategory = "Business",
            ),
        )
        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionAddedTo("Business")).assertIsDisplayed()
    }

    @Test
    fun `added row has no category note in the tab it was added from`() {
        content(
            operationState = FeedSuggestionOperationState(
                FeedSuggestionOperationStatus.Added,
                addedToCategory = category.name,
            ),
        )
        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionAddedTo(category.name)).assertDoesNotExist()
    }

    @Test
    fun `undo confirmation names the category and passes the offer generation back`() {
        val offer = FeedSuggestionUndoOffer(feed.name, feed.url, "Business", generation = 91L)
        var undoneGeneration: Long? = null
        content(undoOffer = offer, onUndo = { undoneGeneration = it })

        val message = EnFeedFlowStrings.feedSuggestionAddedMessage(feed.name, "Business")
        rule.onNodeWithText(message).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionUndo).performSemanticsAction(
            SemanticsActions.OnClick,
        ) { it() }
        rule.runOnIdle { assertEquals(91L, undoneGeneration) }
        rule.onNodeWithText(message).assertDoesNotExist()
    }

    private val feed = SuggestedFeed(
        name = "Fixture source",
        url = "https://fixture.example.invalid/rss",
        description = "A deterministic suggestion fixture with no remote image",
    )
    private val category = SuggestedFeedCategory("fixture", "Fixture category", "", listOf(feed))

    @Composable
    private fun Suggestions(
        operationStatus: FeedSuggestionOperationStatus?,
        operationState: FeedSuggestionOperationState? = operationStatus?.let(::FeedSuggestionOperationState),
        undoOffer: FeedSuggestionUndoOffer? = null,
        onUndo: (Long) -> Unit = {},
        onAdd: (SuggestedFeed, String) -> Unit = { _, _ -> },
    ) {
        FeedSuggestionsContent(
            categories = persistentListOf(category),
            selectedCategoryId = category.id,
            isLoading = false,
            onCategorySelected = {},
            onAddFeed = onAdd,
            onCloseRequest = {},
            operationStates = operationState?.let { persistentMapOf(feed.url to it) } ?: persistentMapOf(),
            undoOfferState = undoOffer,
            onUndo = onUndo,
        )
    }

    private fun compact(body: @Composable () -> Unit) {
        content(fontScale = 2f, direction = LayoutDirection.Rtl) {
            Box(Modifier.size(width = 360.dp, height = 500.dp)) { body() }
        }
    }

    private fun content(
        operation: FeedSuggestionOperationStatus? = null,
        operationState: FeedSuggestionOperationState? = operation?.let(::FeedSuggestionOperationState),
        undoOffer: FeedSuggestionUndoOffer? = null,
        onUndo: (Long) -> Unit = {},
        onAdd: (SuggestedFeed, String) -> Unit = { _, _ -> },
        fontScale: Float = 1f,
        direction: LayoutDirection = LayoutDirection.Ltr,
        body: @Composable () -> Unit = {
            Suggestions(operation, operationState, undoOffer, onUndo, onAdd)
        },
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
