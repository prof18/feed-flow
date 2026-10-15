@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.prof18.feedflow.desktop.settings.blocked

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.BlockedWordAction
import com.prof18.feedflow.shared.presentation.model.BlockedWordOperationState
import com.prof18.feedflow.shared.presentation.model.BlockedWordOperationStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlockedWordOperationUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `add failure retains draft and retry targets that word`() {
        val operations = mutableStateOf<ImmutableMap<String, BlockedWordOperationState>>(persistentMapOf())
        val retries = mutableListOf<String>()
        val adds = mutableListOf<String>()
        var removes = 0
        content(
            onAdd = adds::add,
            onRemove = { removes++ },
            onRetry = retries::add,
            body = {
                Words(
                    words = persistentListOf("existing"),
                    operationStates = operations.value,
                    onAdd = adds::add,
                    onRemove = { removes++ },
                    onRetry = retries::add,
                )
            },
        )
        type("  fixture  ")
        clickAdd()
        rule.runOnIdle {
            operations.value = operationMap(
                "fixture",
                BlockedWordAction.Add,
                BlockedWordOperationStatus.Failed,
            )
        }

        rule.onNodeWithText(EnFeedFlowStrings.blockedWordOperationError).assertIsDisplayed()
        assertDraft("  fixture  ")
        rule.onNodeWithText(EnFeedFlowStrings.retryButton).performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle {
            assertEquals(listOf("fixture"), retries)
            assertEquals(listOf("fixture"), adds)
            assertEquals(0, removes)
        }
        assertDraft("  fixture  ")
    }

    @Test
    fun `successful add clears the submitted draft after operation is applied`() {
        val operations = mutableStateOf<ImmutableMap<String, BlockedWordOperationState>>(persistentMapOf())
        val adds = mutableListOf<String>()
        content(
            onAdd = adds::add,
            body = { Words(operationStates = operations.value, onAdd = adds::add) },
        )
        type("  fixture  ")
        clickAdd()
        assertDraft("  fixture  ")
        rule.runOnIdle {
            assertEquals(listOf("fixture"), adds)
            operations.value = operationMap(
                "fixture",
                BlockedWordAction.Add,
                BlockedWordOperationStatus.Applied,
            )
        }
        assertDraft("")
        rule.onNodeWithText(EnFeedFlowStrings.blockedWordAdded).assertIsDisplayed()
    }

    @Test
    fun `completion preserves a newer draft typed after the submitted word`() {
        val operations = mutableStateOf<ImmutableMap<String, BlockedWordOperationState>>(persistentMapOf())
        content(body = { Words(operationStates = operations.value) })
        type("submitted")
        clickAdd()
        type("newer draft")
        rule.runOnIdle {
            operations.value = operationMap(
                "submitted",
                BlockedWordAction.Add,
                BlockedWordOperationStatus.Applied,
            )
        }
        assertDraft("newer draft")
    }

    @Test
    fun `pending operation disables add and remove and ignores captured callbacks`() {
        val operations = mutableStateOf<ImmutableMap<String, BlockedWordOperationState>>(persistentMapOf())
        val adds = mutableListOf<String>()
        val removes = mutableListOf<String>()
        content(
            onAdd = adds::add,
            onRemove = removes::add,
            body = {
                Words(
                    words = persistentListOf("existing"),
                    operationStates = operations.value,
                    onAdd = adds::add,
                    onRemove = removes::add,
                )
            },
        )
        type("new word")
        val add = rule.onNodeWithTag(BlockedWordsE2eIds.ADD_BUTTON)
        val capturedAdd = add.fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        val remove = rule.onNodeWithTag(BlockedWordsE2eIds.deleteButton("existing"))
        val capturedRemove = remove.fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        rule.runOnIdle {
            operations.value = operationMap(
                "other word",
                BlockedWordAction.Add,
                BlockedWordOperationStatus.Writing,
            )
        }

        add.assertIsNotEnabled()
        remove.assertIsNotEnabled()
        rule.runOnIdle {
            capturedAdd()
            capturedRemove()
            assertTrue(adds.isEmpty())
            assertTrue(removes.isEmpty())
        }
        assertDraft("new word")
        rule.onNodeWithTag(BlockedWordsE2eIds.INPUT).assertIsEnabled()
    }

    @Test
    fun `refresh failure reports saved word and clears matching draft`() {
        val operations = mutableStateOf<ImmutableMap<String, BlockedWordOperationState>>(persistentMapOf())
        content(body = { Words(operationStates = operations.value) })
        type("fixture")
        clickAdd()
        rule.runOnIdle {
            operations.value = operationMap(
                "fixture",
                BlockedWordAction.Add,
                BlockedWordOperationStatus.RefreshFailed,
            )
        }

        assertDraft("")
        rule.onNodeWithText(EnFeedFlowStrings.blockedWordRefreshError).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.blockedWordOperationError).assertDoesNotExist()
    }

    @Test
    fun `failed removal leaves an unrelated unsent draft and retry never adds or removes directly`() {
        val operations = mutableStateOf(
            operationMap("saved word", BlockedWordAction.Remove, BlockedWordOperationStatus.Failed),
        )
        val retries = mutableListOf<String>()
        var additions = 0
        var removals = 0
        content(
            words = persistentListOf("saved word"),
            operationStates = operations.value,
            onAdd = { additions++ },
            onRemove = { removals++ },
            onRetry = retries::add,
        )
        type("unsent draft")
        rule.onNodeWithText(EnFeedFlowStrings.blockedWordOperationError).assertIsDisplayed()
        assertDraft("unsent draft")
        rule.onNodeWithText(EnFeedFlowStrings.retryButton).performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle {
            assertEquals(listOf("saved word"), retries)
            assertEquals(0, additions)
            assertEquals(0, removals)
        }
        assertDraft("unsent draft")
    }

    @Test
    fun `captured retry callback is ignored while an older operation is pending`() {
        val operations = mutableStateOf(
            operationMap("fixture", BlockedWordAction.Add, BlockedWordOperationStatus.Failed),
        )
        var retries = 0
        content(
            onRetry = { retries++ },
            body = { Words(operationStates = operations.value, onRetry = { retries++ }) },
        )
        val retry = rule.onNodeWithText(EnFeedFlowStrings.retryButton)
        val capturedRetry = retry.fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        rule.runOnIdle {
            operations.value = operationMap(
                "fixture",
                BlockedWordAction.Add,
                BlockedWordOperationStatus.Failed,
            ).put(
                "older",
                BlockedWordOperationState(
                    generation = 0,
                    word = "older",
                    action = BlockedWordAction.Add,
                    status = BlockedWordOperationStatus.Writing,
                ),
            )
        }
        rule.runOnIdle {
            capturedRetry()
            assertEquals(0, retries)
        }
        rule.onNodeWithText(EnFeedFlowStrings.retryButton).assertIsNotEnabled()
    }

    @Test
    fun `failed add followed by removal releases the form for another add`() {
        val operations = mutableStateOf<ImmutableMap<String, BlockedWordOperationState>>(persistentMapOf())
        val additions = mutableListOf<String>()
        content(body = {
            Words(
                words = persistentListOf("fixture"),
                operationStates = operations.value,
                onAdd = additions::add,
                onRemove = { word ->
                    operations.value = persistentMapOf(
                        word to BlockedWordOperationState(
                            generation = 2,
                            word = word,
                            action = BlockedWordAction.Remove,
                            status = BlockedWordOperationStatus.Applied,
                        ),
                    )
                },
            )
        })
        type("fixture")
        clickAdd()
        rule.runOnIdle {
            operations.value = operationMap("fixture", BlockedWordAction.Add, BlockedWordOperationStatus.Failed)
        }
        rule.onNodeWithTag(BlockedWordsE2eIds.deleteButton("fixture"))
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        assertDraft("fixture")
        type("another word")
        rule.onNodeWithTag(BlockedWordsE2eIds.ADD_BUTTON).assertIsEnabled()
        clickAdd()
        rule.runOnIdle { assertEquals(listOf("fixture", "another word"), additions) }
    }

    @Test
    fun `two immediate add callbacks submit only once before operation state arrives`() {
        val additions = mutableListOf<String>()
        content(operationStates = persistentMapOf(), onAdd = additions::add)
        type("fixture")
        val action = rule.onNodeWithTag(BlockedWordsE2eIds.ADD_BUTTON)
            .fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        rule.runOnIdle {
            action()
            action()
            assertEquals(listOf("fixture"), additions)
        }
        assertDraft("fixture")
    }

    @Test
    fun `compact enlarged RTL operation error has no visual overflow`() {
        val operations = operationMap(
            "fixture",
            BlockedWordAction.Add,
            BlockedWordOperationStatus.Failed,
        )
        compact {
            Words(operationStates = operations)
        }

        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(EnFeedFlowStrings.blockedWordOperationError)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        rule.runOnIdle {
            assertTrue(layouts.isNotEmpty())
            assertFalse(layouts.single().hasVisualOverflow)
        }
    }

    @Test
    fun `legacy synchronous callback mode still clears draft after submission`() {
        val adds = mutableListOf<String>()
        content(operationStates = null, onAdd = adds::add)
        type("  fixture  ")
        rule.onNodeWithTag(BlockedWordsE2eIds.INPUT).performKeyInput { pressKey(Key.Enter) }

        rule.runOnIdle { assertEquals(listOf("fixture"), adds) }
        assertDraft("")
    }

    private fun type(value: String) {
        rule.onNodeWithTag(BlockedWordsE2eIds.INPUT).performTextReplacement(value)
    }

    private fun clickAdd() {
        rule.onNodeWithTag(BlockedWordsE2eIds.ADD_BUTTON)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    private fun assertDraft(value: String) {
        val expected = androidx.compose.ui.test.SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.EditableText,
            AnnotatedString(value),
        )
        rule.onNodeWithTag(BlockedWordsE2eIds.INPUT)
            .assert(expected)
    }

    private fun operationMap(
        word: String,
        action: BlockedWordAction,
        status: BlockedWordOperationStatus,
    ) = persistentMapOf(
        word to BlockedWordOperationState(generation = 1, word = word, action = action, status = status),
    )

    private val initialWords = persistentListOf("existing")

    @Composable
    private fun Words(
        words: ImmutableList<String> = initialWords,
        operationStates: ImmutableMap<String, BlockedWordOperationState>? = null,
        onAdd: (String) -> Unit = {},
        onRemove: (String) -> Unit = {},
        onRetry: (String) -> Unit = {},
    ) {
        BlockedWordsContent(
            words = words,
            onAddWord = onAdd,
            onRemoveWord = onRemove,
            onCloseRequest = {},
            operationStates = operationStates,
            onRetryOperation = onRetry,
        )
    }

    private fun content(
        words: ImmutableList<String> = initialWords,
        operationStates: ImmutableMap<String, BlockedWordOperationState>? = null,
        onAdd: (String) -> Unit = {},
        onRemove: (String) -> Unit = {},
        onRetry: (String) -> Unit = {},
        body: @Composable () -> Unit = { Words(words, operationStates, onAdd, onRemove, onRetry) },
    ) {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false, content = body)
            }
        }
    }

    private fun compact(body: @Composable () -> Unit) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.size(width = 360.dp, height = 500.dp)) { body() }
                }
            }
        }
    }
}
