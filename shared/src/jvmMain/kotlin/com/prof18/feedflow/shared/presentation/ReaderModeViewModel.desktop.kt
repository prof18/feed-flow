package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.ReaderModeExtractor
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReaderModeViewModel internal constructor(
    private val readerModeExtractor: ReaderModeExtractor,
    private val markdownToHtmlConverter: MarkdownToHtmlConverter,
    private val settingsRepository: SettingsRepository,
    private val feedActionsRepository: FeedActionsRepository,
) : ViewModel() {

    private val readerModeMutableState: MutableStateFlow<ReaderModeState> = MutableStateFlow(
        ReaderModeState.Loading,
    )
    val readerModeState = readerModeMutableState.asStateFlow()

    private val readerFontSizeMutableState: MutableStateFlow<Int> = MutableStateFlow(
        settingsRepository.getReaderModeFontSize(),
    )
    val readerFontSizeState = readerFontSizeMutableState.asStateFlow()

    fun getReaderModeHtml(urlInfo: FeedItemUrlInfo) {
        viewModelScope.launch {
            readerModeMutableState.value = ReaderModeState.Loading
            val readerModeData = readerModeExtractor.extractReaderContent(urlInfo)
            if (readerModeData != null) {
                val tmpTitle = readerModeData.title
                val title = if (tmpTitle != null && readerModeData.content.contains(tmpTitle)) {
                    null
                } else {
                    tmpTitle
                }
                val html = getReaderModeStyledHtml(
                    colors = null,
                    content = readerModeData.content,
                    title = title,
                    fontSize = settingsRepository.getReaderModeFontSize(),
                )
                val markdown = markdownToHtmlConverter.convertToMarkdown(html)
                readerModeMutableState.value = ReaderModeState.Success(
                    readerModeData.copy(
                        content = markdown,
                    ),
                )
            } else {
                readerModeMutableState.value = ReaderModeState.HtmlNotAvailable(
                    url = urlInfo.url,
                    id = urlInfo.id,
                    isBookmarked = urlInfo.isBookmarked,
                )
            }
        }
    }

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
