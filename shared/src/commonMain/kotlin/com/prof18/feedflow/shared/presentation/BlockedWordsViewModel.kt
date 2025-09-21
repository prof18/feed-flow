package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlockedWordsViewModel internal constructor(
    private val databaseHelper: DatabaseHelper,
    private val feedStateRepository: FeedStateRepository,
) : ViewModel() {

    val wordsState: StateFlow<ImmutableList<String>> = databaseHelper
        .observeBlockedWords()
        .map { list -> list.distinct().toImmutableList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = persistentListOf(),
        )

    fun onAddWord(input: String) {
        viewModelScope.launch {
            databaseHelper.addBlockedWord(input.trim())
            feedStateRepository.getFeeds()
        }
    }

    fun onRemoveWord(word: String) {
        viewModelScope.launch {
            databaseHelper.removeBlockedWord(word)
            feedStateRepository.getFeeds()
        }
    }
}
