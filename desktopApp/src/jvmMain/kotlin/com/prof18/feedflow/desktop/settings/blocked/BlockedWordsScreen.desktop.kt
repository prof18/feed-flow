package com.prof18.feedflow.desktop.settings.blocked

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.settings.SettingsBlock
import com.prof18.feedflow.desktop.settings.SettingsPage
import com.prof18.feedflow.desktop.settings.SettingsSection
import com.prof18.feedflow.desktop.settings.settingsRowSeparator
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
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
internal fun BlockedWordsScreen(
    draft: BlockedWordDraft,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<BlockedWordsViewModel>()
    val words by viewModel.wordsState.collectAsState()
    val operations by viewModel.operationStates.collectAsState()
    Column(modifier.fillMaxSize()) {
        BlockedWordsContent(
            words,
            viewModel::onAddWord,
            viewModel::onRemoveWord,
            onCloseRequest = {},
            modifier = Modifier.weight(1f),
            draft = draft,
            showCloseButton = false,
            operationStates = operations,
            onRetryOperation = viewModel::retryOperation,
        )
    }
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
    draft: BlockedWordDraft = rememberBlockedWordDraft(),
    showCloseButton: Boolean = true,
) {
    val strings = LocalFeedFlowStrings.current
    val inputFocus = remember { FocusRequester() }
    val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp
    val currentOperations by rememberUpdatedState(operationStates)
    val currentWords by rememberUpdatedState(words)
    val removeWord by rememberUpdatedState(onRemoveWord)
    val onRemove: (String) -> Unit = { word ->
        if (!currentOperations.hasPendingOperations() && word in currentWords) {
            removeWord(word)
            inputFocus.requestFocus()
        }
    }
    Column(modifier = modifier.fillMaxSize()) {
        SettingsPage(title = strings.settingsFilters, modifier = Modifier.weight(1f)) {
            SettingsSection(title = strings.addWordPlaceholder, footer = strings.blockedWordsDescription) {
                SettingsBlock {
                    BlockedWordInput(onAddWord, inputFocus, operationStates, draft)
                }
            }
            operationStates?.values?.maxByOrNull { it.generation }?.let { operation ->
                BlockedWordOperationFeedback(
                    operation = operation,
                    hasPendingOperations = operationStates.hasPendingOperations(),
                    onRetry = onRetryOperation,
                )
            }
            SettingsSection(title = strings.settingsBlockedWords) {
                if (words.isEmpty()) {
                    SettingsBlock {
                        Text(strings.blockedWordsEmptyState, color = JewelTheme.globalColors.text.info)
                    }
                } else {
                    words.forEach { word ->
                        key(word) {
                            BlockedWordRow(
                                word = word,
                                deleteLabel = strings.deleteFeed,
                                enabled = !operationStates.hasPendingOperations(),
                                actionSize = actionHeight,
                                onRemove = { onRemove(word) },
                            )
                        }
                    }
                }
            }
        }
        if (showCloseButton) {
            Row(
                Modifier.fillMaxWidth().padding(DesktopDesignTokens.spaceSection),
                horizontalArrangement = Arrangement.End,
            ) {
                DefaultButton(onClick = onCloseRequest, modifier = Modifier.heightIn(min = actionHeight)) {
                    Text(strings.actionDone)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun BlockedWordRow(
    word: String,
    deleteLabel: String,
    enabled: Boolean,
    actionSize: Dp,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .settingsRowSeparator()
            .testTag(BlockedWordsE2eIds.row(word))
            .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tooltip(tooltip = { Text(word) }, modifier = Modifier.weight(1f)) {
            Text(word, Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Tooltip(tooltip = { Text(deleteLabel) }) {
            IconButton(
                onClick = onRemove,
                enabled = enabled,
                modifier = Modifier.size(actionSize)
                    .testTag(BlockedWordsE2eIds.deleteButton(word))
                    .semantics { contentDescription = deleteLabel },
            ) {
                Icon(AllIconsKeys.General.Delete, null)
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
    draft: BlockedWordDraft,
) {
    val strings = LocalFeedFlowStrings.current
    val field = draft.field
    var submitted by draft.submittedState
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

internal class BlockedWordDraft(val field: TextFieldState) {
    internal val submittedState = mutableStateOf<SubmittedBlockedWord?>(null)
}

@Composable
internal fun rememberBlockedWordDraft(): BlockedWordDraft {
    val field = rememberTextFieldState()
    return remember(field) { BlockedWordDraft(field) }
}

internal data class SubmittedBlockedWord(val rawDraft: String, val word: String, val previousGeneration: Long)

private fun SubmittedBlockedWord.operationFrom(operations: ImmutableMap<String, BlockedWordOperationState>?) =
    operations?.get(word)?.takeIf {
        it.generation > previousGeneration
    }

private fun SubmittedBlockedWord.isAwaiting(operations: ImmutableMap<String, BlockedWordOperationState>?): Boolean {
    val operation = operationFrom(operations)
    return operation == null || (operation.action == BlockedWordAction.Add && operation.status.isPending())
}
