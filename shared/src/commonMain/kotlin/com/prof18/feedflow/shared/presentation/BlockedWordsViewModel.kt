package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.presentation.model.BlockedWordAction
import com.prof18.feedflow.shared.presentation.model.BlockedWordOperationState
import com.prof18.feedflow.shared.presentation.model.BlockedWordOperationStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BlockedWordsViewModel internal constructor(
    private val databaseHelper: DatabaseHelper,
    private val feedStateRepository: FeedStateRepository,
    private val writeAddedWord: suspend (String) -> Unit = databaseHelper::addBlockedWord,
    private val writeRemovedWord: suspend (String) -> Unit = databaseHelper::removeBlockedWord,
    private val reloadFeeds: suspend () -> Boolean = feedStateRepository::reloadFeeds,
) : ViewModel() {

    private val operations = MutableStateFlow<ImmutableMap<String, BlockedWordOperationState>>(persistentMapOf())
    val operationStates = operations.asStateFlow()
    private val operationMutex = Mutex()
    private var operationGeneration = 0L

    val wordsState: StateFlow<ImmutableList<String>> = databaseHelper
        .observeBlockedWords()
        .map { list -> list.distinct().toImmutableList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = persistentListOf(),
        )

    fun onAddWord(input: String) {
        val word = input.trim()
        if (word.isNotEmpty()) startOperation(word, BlockedWordAction.Add)
    }

    fun onRemoveWord(word: String) {
        startOperation(word, BlockedWordAction.Remove)
    }

    fun retryOperation(word: String) {
        val operation = operations.value[word] ?: return
        when (operation.status) {
            BlockedWordOperationStatus.Failed -> startOperation(word, operation.action)
            BlockedWordOperationStatus.RefreshFailed -> startOperation(word, operation.action, reloadOnly = true)
            else -> Unit
        }
    }

    private fun startOperation(word: String, action: BlockedWordAction, reloadOnly: Boolean = false) {
        val current = operations.value[word]
        val pending = current?.status == BlockedWordOperationStatus.Writing ||
            current?.status == BlockedWordOperationStatus.Refreshing
        if (current?.action == action && pending) {
            return
        }
        val operation = BlockedWordOperationState(
            generation = ++operationGeneration,
            word = word,
            action = action,
            status = if (reloadOnly) BlockedWordOperationStatus.Refreshing else BlockedWordOperationStatus.Writing,
        )
        operations.update { (it + (word to operation)).toPersistentMap() }
        viewModelScope.launch {
            operationMutex.withLock { performOperation(operation, reloadOnly) }
        }
    }

    private suspend fun performOperation(operation: BlockedWordOperationState, reloadOnly: Boolean) {
        var saved = reloadOnly
        try {
            if (!reloadOnly) {
                when (operation.action) {
                    BlockedWordAction.Add -> writeAddedWord(operation.word)
                    BlockedWordAction.Remove -> writeRemovedWord(operation.word)
                }
                saved = true
                updateOperation(operation, BlockedWordOperationStatus.Refreshing)
            }
            updateOperation(
                operation,
                if (reloadFeeds()) BlockedWordOperationStatus.Applied else BlockedWordOperationStatus.RefreshFailed,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Logger.e(error) { "Unable to finish blocked word operation" }
            updateOperation(
                operation,
                if (saved) BlockedWordOperationStatus.RefreshFailed else BlockedWordOperationStatus.Failed,
            )
        }
    }

    private fun updateOperation(operation: BlockedWordOperationState, status: BlockedWordOperationStatus) {
        operations.update { states ->
            if (states[operation.word]?.generation == operation.generation) {
                (states + (operation.word to operation.copy(status = status))).toPersistentMap()
            } else {
                states
            }
        }
    }
}
