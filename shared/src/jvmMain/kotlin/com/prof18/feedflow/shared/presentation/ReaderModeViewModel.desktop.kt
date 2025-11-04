package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.model.ReaderModeData
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReaderModeViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val feedActionsRepository: FeedActionsRepository,
    private val feedItemParserWorker: FeedItemParserWorker,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val markdownToHtmlConverter: MarkdownToHtmlConverter,
) : ViewModel() {

    private val readerModeMutableState: MutableStateFlow<ReaderModeState> = MutableStateFlow(
        ReaderModeState.Loading,
    )
    val readerModeState = readerModeMutableState.asStateFlow()

    private val readerFontSizeMutableState: MutableStateFlow<Int> = MutableStateFlow(
        settingsRepository.getReaderModeFontSize(),
    )
    val readerFontSizeState = readerFontSizeMutableState.asStateFlow()

    fun setLoading() {
        readerModeMutableState.value = ReaderModeState.Loading
    }

    fun getReaderModeHtml(urlInfo: FeedItemUrlInfo) {
        viewModelScope.launch {
            readerModeMutableState.value = ReaderModeState.Loading

            // Use feed item ID directly as filename
            val feedItemId = urlInfo.id
            val cachedContent = feedItemContentFileHandler.loadFeedItemContent(feedItemId)

            if (cachedContent != null) {
                val tmpTitle = urlInfo.title
                val title = if (tmpTitle != null && cachedContent.contains(tmpTitle)) {
                    null
                } else {
                    tmpTitle
                }
                val html = getReaderModeStyledHtml(
                    colors = null,
                    content = cachedContent,
                    title = title,
                    fontSize = settingsRepository.getReaderModeFontSize(),
                )
                val markdown = markdownToHtmlConverter.convertToMarkdown(html)
                readerModeMutableState.value = ReaderModeState.Success(
                    ReaderModeData(
                        id = FeedItemId(urlInfo.id),
                        content = markdown,
                        url = urlInfo.url,
                        fontSize = settingsRepository.getReaderModeFontSize(),
                        isBookmarked = urlInfo.isBookmarked,
                        title = urlInfo.title,
                        commentsUrl = urlInfo.commentsUrl,
                    ),
                )
                return@launch
            }

            when (val result = feedItemParserWorker.triggerImmediateParsing(feedItemId, urlInfo.url)) {
                is ParsingResult.Success -> {
                    val content = result.htmlContent
                    if (content != null) {
                        val tmpTitle = urlInfo.title
                        val title = if (tmpTitle != null && content.contains(tmpTitle)) {
                            null
                        } else {
                            tmpTitle
                        }
                        val html = getReaderModeStyledHtml(
                            colors = null,
                            content = content,
                            title = title,
                            fontSize = settingsRepository.getReaderModeFontSize(),
                        )
                        val markdown = markdownToHtmlConverter.convertToMarkdown(html)
                        readerModeMutableState.value = ReaderModeState.Success(
                            ReaderModeData(
                                id = FeedItemId(urlInfo.id),
                                content = markdown,
                                url = urlInfo.url,
                                fontSize = settingsRepository.getReaderModeFontSize(),
                                isBookmarked = urlInfo.isBookmarked,
                                title = urlInfo.title,
                                commentsUrl = urlInfo.commentsUrl,
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
                is ParsingResult.Error -> {
                    readerModeMutableState.value = ReaderModeState.HtmlNotAvailable(
                        url = urlInfo.url,
                        id = urlInfo.id,
                        isBookmarked = urlInfo.isBookmarked,
                    )
                }
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
