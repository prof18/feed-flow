package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReaderModeViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val feedActionsRepository: FeedActionsRepository,
) : ViewModel() {

    private val readerFontSizeMutableState: MutableStateFlow<Int> = MutableStateFlow(
        settingsRepository.getReaderModeFontSize(),
    )
    val readerFontSizeState = readerFontSizeMutableState.asStateFlow()

    fun updateFontSize(newFontSize: Int) {
        settingsRepository.setReaderModeFontSize(newFontSize)
        readerFontSizeMutableState.update { newFontSize }
    }

    fun updateBookmarkStatus(feedItemId: FeedItemId, bookmarked: Boolean) {
        viewModelScope.launch {
            feedActionsRepository.updateBookmarkStatus(feedItemId, bookmarked)
        }
    }
}
