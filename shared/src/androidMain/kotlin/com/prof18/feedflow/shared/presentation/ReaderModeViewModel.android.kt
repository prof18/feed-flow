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
import java.security.MessageDigest

class ReaderModeViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val feedActionsRepository: FeedActionsRepository,
    private val feedItemParserWorker: FeedItemParserWorker,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
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

            val feedItemId = urlInfo.url.toFeedItemId()
            val cachedContent = feedItemContentFileHandler.loadFeedItemContent(feedItemId)

            if (cachedContent != null) {
                readerModeMutableState.value = ReaderModeState.Success(
                    ReaderModeData(
                        id = FeedItemId(urlInfo.id),
                        content = cachedContent,
                        url = urlInfo.url,
                        fontSize = settingsRepository.getReaderModeFontSize(),
                        isBookmarked = urlInfo.isBookmarked,
                        title = urlInfo.title,
                        commentsUrl = urlInfo.commentsUrl,
                    ),
                )
                return@launch
            }

            when (val result = feedItemParserWorker.triggerImmediateParsing(urlInfo.url)) {
                is ParsingResult.Success -> {
                    val content = result.htmlContent
                    if (content != null) {
                        readerModeMutableState.value = ReaderModeState.Success(
                            ReaderModeData(
                                id = FeedItemId(urlInfo.id),
                                content = content,
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

    private fun String.toFeedItemId(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(this.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
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
