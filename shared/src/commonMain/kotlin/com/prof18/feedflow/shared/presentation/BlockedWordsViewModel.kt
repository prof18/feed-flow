package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.repo.BlockedWordRepository
import com.prof18.feedflow.core.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlockedWordsUiState(
    val blockedWords: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val newWordText: String = ""
)

// Attempting to follow the pattern from HomeViewModel for scope management.
// If a common BaseViewModel exists and is discoverable, that would be preferable.
// For now, implementing scope directly.
open class BlockedWordsViewModel(
    private val blockedWordRepository: BlockedWordRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    // Mimic viewModelScope from potential BaseViewModel structure
    protected val viewModelScope: CoroutineScope = MainScope()

    private val _uiState = MutableStateFlow(BlockedWordsUiState())
    val uiState: StateFlow<BlockedWordsUiState> = _uiState.asStateFlow()

    init {
        loadBlockedWords()
    }

    private fun loadBlockedWords() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        blockedWordRepository.getBlockedWords()
            .onEach { words ->
                _uiState.update {
                    it.copy(
                        blockedWords = words,
                        isLoading = false
                    )
                }
            }
            .catch { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load blocked words: ${throwable.message}"
                    )
                }
            }
            .launchIn(viewModelScope) // Use viewModelScope
    }

    fun updateNewWordText(text: String) {
        _uiState.update { it.copy(newWordText = text) }
    }

    fun addBlockedWord() {
        val newWord = _uiState.value.newWordText.trim()
        if (newWord.isEmpty()) {
            _uiState.update { it.copy(error = "Blocked word cannot be empty.") }
            return
        }

        viewModelScope.launch(dispatcherProvider.io) {
            try {
                blockedWordRepository.insertBlockedWord(newWord)
                // State will be updated by the collector in loadBlockedWords()
                // Clear the text input field
                _uiState.update { it.copy(newWordText = "", error = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to add blocked word: ${e.message}")
                }
            }
        }
    }

    fun deleteBlockedWord(word: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            try {
                blockedWordRepository.deleteBlockedWord(word)
                // State will be updated by the collector in loadBlockedWords()
                 _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to delete blocked word: ${e.message}")
                }
            }
        }
    }

    // Call this when the ViewModel is no longer needed to cancel viewModelScope
    fun onCleared() {
        viewModelScope.coroutineContext.cancel()
    }
}
