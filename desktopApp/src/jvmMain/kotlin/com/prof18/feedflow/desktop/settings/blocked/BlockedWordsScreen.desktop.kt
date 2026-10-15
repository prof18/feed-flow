package com.prof18.feedflow.desktop.settings.blocked

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.composeunstyled.rememberScrollbarState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.components.Dialog
import com.prof18.feedflow.desktop.ui.components.FeedFlowVerticalScrollbar
import com.prof18.feedflow.shared.presentation.BlockedWordsViewModel
import com.prof18.feedflow.shared.presentation.model.BlockedWordAction
import com.prof18.feedflow.shared.presentation.model.BlockedWordOperationState
import com.prof18.feedflow.shared.presentation.model.BlockedWordOperationStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun BlockedWordsScreen(onCloseRequest: () -> Unit) {
    val viewModel = koinViewModel<BlockedWordsViewModel>()
    val words by viewModel.wordsState.collectAsState()
    val operations by viewModel.operationStates.collectAsState()
    BlockedWordsWindow(onCloseRequest) {
        BlockedWordsContent(
            words,
            viewModel::onAddWord,
            viewModel::onRemoveWord,
            onCloseRequest,
            operationStates = operations,
            onRetryOperation = viewModel::retryOperation,
        )
    }
}

@Composable
internal fun BlockedWordsWindow(onCloseRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        title = LocalFeedFlowStrings.current.settingsBlockedWords,
        size = DpSize(500.dp, 700.dp),
        onCloseRequest = onCloseRequest,
    ) { content() }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun BlockedWordsContent(
    words: ImmutableList<String>,
    onAddWord: (String) -> Unit,
    onRemoveWord: (String) -> Unit,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
    operationStates: ImmutableMap<String, BlockedWordOperationState>? = null,
    onRetryOperation: (String) -> Unit = {},
) {
    val strings = LocalFeedFlowStrings.current
    val inputFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp
    val currentOperations by rememberUpdatedState(operationStates)
    val currentWords by rememberUpdatedState(words)
    val removeWord by rememberUpdatedState(onRemoveWord)
    Column(
        modifier = modifier.fillMaxSize().padding(DesktopDesignTokens.spacePanel),
        verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSection),
    ) {
        BlockedWordInput(onAddWord, inputFocus, operationStates)
        Box(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(end = DesktopDesignTokens.spaceSection),
            ) {
                item {
                    Text(strings.blockedWordsDescription, Modifier.padding(bottom = DesktopDesignTokens.spaceSection))
                }
                operationStates?.values?.maxByOrNull { it.generation }?.let { operation ->
                    item {
                        BlockedWordOperationFeedback(
                            operation = operation,
                            hasPendingOperations = operationStates.hasPendingOperations(),
                            onRetry = onRetryOperation,
                            modifier = Modifier.padding(bottom = DesktopDesignTokens.spaceSection),
                        )
                    }
                }
                if (words.isEmpty()) {
                    item { Text(strings.blockedWordsEmptyState) }
                } else {
                    items(words, key = { it }) { word ->
                        Row(
                            modifier = Modifier.fillMaxWidth().testTag(BlockedWordsE2eIds.row(word))
                                .padding(vertical = DesktopDesignTokens.spaceMedium),
                            horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Tooltip(tooltip = { Text(word) }, modifier = Modifier.weight(1f)) {
                                Text(word, Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Tooltip(tooltip = { Text(strings.deleteFeed) }) {
                                IconButton(
                                    onClick = {
                                        if (!currentOperations.hasPendingOperations() && word in currentWords) {
                                            removeWord(word)
                                            inputFocus.requestFocus()
                                        }
                                    },
                                    enabled = !operationStates.hasPendingOperations(),
                                    modifier = Modifier.size(actionHeight)
                                        .testTag(BlockedWordsE2eIds.deleteButton(word))
                                        .semantics { contentDescription = strings.deleteFeed },
                                ) {
                                    Icon(AllIconsKeys.General.Delete, null)
                                }
                            }
                        }
                    }
                }
            }
            FeedFlowVerticalScrollbar(rememberScrollbarState(listState))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            DefaultButton(onClick = onCloseRequest, modifier = Modifier.heightIn(min = actionHeight)) {
                Text(strings.actionDone)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalJewelApi::class)
private fun BlockedWordInput(
    onAddWord: (String) -> Unit,
    focus: FocusRequester,
    operationStates: ImmutableMap<String, BlockedWordOperationState>?,
) {
    val strings = LocalFeedFlowStrings.current
    val field = rememberTextFieldState()
    var submitted by remember { mutableStateOf<SubmittedBlockedWord?>(null) }
    val submittedOperation = submitted?.operationFrom(operationStates)
    val awaitingResult = submitted?.isAwaiting(operationStates) == true
    val currentBusy by rememberUpdatedState(operationStates.hasPendingOperations() || awaitingResult)
    val currentOperations by rememberUpdatedState(operationStates)
    val addWord by rememberUpdatedState(onAddWord)
    val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp
    val add: () -> Unit = {
        if (!currentBusy && submitted?.isAwaiting(currentOperations) != true) {
            val rawDraft = field.text.toString()
            rawDraft.trim().takeIf(String::isNotEmpty)?.let { word ->
                if (currentOperations == null) {
                    field.setTextAndPlaceCursorAtEnd("")
                } else {
                    submitted = SubmittedBlockedWord(rawDraft, word, currentOperations?.get(word)?.generation ?: 0)
                }
                addWord(word)
                focus.requestFocus()
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(submitted, submittedOperation) {
        val submission = submitted ?: return@LaunchedEffect
        val operation = submittedOperation ?: return@LaunchedEffect
        confirmBlockedWordResult(submission, operation, field, focus) { submitted = null }
    }
    Column(verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium)) {
        Text(strings.addWordPlaceholder)
        Row(
            horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                state = field,
                modifier = Modifier.weight(1f).heightIn(min = actionHeight).focusRequester(focus)
                    .testTag(BlockedWordsE2eIds.INPUT).semantics { contentDescription = strings.addWordPlaceholder },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onKeyboardAction = KeyboardActionHandler { add() },
            )
            DefaultButton(
                onClick = add,
                enabled = field.text.isNotBlank() && !currentBusy,
                modifier = Modifier.heightIn(min = actionHeight).testTag(BlockedWordsE2eIds.ADD_BUTTON),
            ) { Text(strings.addWordPlaceholder) }
        }
    }
}

private fun confirmBlockedWordResult(
    submission: SubmittedBlockedWord,
    operation: BlockedWordOperationState,
    field: TextFieldState,
    focus: FocusRequester,
    onRelease: () -> Unit,
) {
    if (operation.action != BlockedWordAction.Add) {
        onRelease()
        return
    }
    if (operation.status != BlockedWordOperationStatus.Writing &&
        operation.status != BlockedWordOperationStatus.Failed
    ) {
        if (field.text.toString() == submission.rawDraft) field.setTextAndPlaceCursorAtEnd("")
        if (!operation.status.isPending()) onRelease()
    }
    if (!operation.status.isPending()) focus.requestFocus()
}

private data class SubmittedBlockedWord(val rawDraft: String, val word: String, val previousGeneration: Long)

private fun SubmittedBlockedWord.operationFrom(operations: ImmutableMap<String, BlockedWordOperationState>?) =
    operations?.get(word)?.takeIf {
        it.generation > previousGeneration
    }

private fun SubmittedBlockedWord.isAwaiting(operations: ImmutableMap<String, BlockedWordOperationState>?): Boolean {
    val operation = operationFrom(operations)
    return operation == null || (operation.action == BlockedWordAction.Add && operation.status.isPending())
}
