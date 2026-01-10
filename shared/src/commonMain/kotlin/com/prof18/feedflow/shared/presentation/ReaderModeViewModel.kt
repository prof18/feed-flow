package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.model.ReaderModeData
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class ReaderModeViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val feedActionsRepository: FeedActionsRepository,
    private val feedItemParserWorker: FeedItemParserWorker,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val feedStateRepository: FeedStateRepository,
) : ViewModel() {

    private val readerModeMutableState: MutableStateFlow<ReaderModeState> = MutableStateFlow(
        ReaderModeState.Loading,
    )
    val readerModeState = readerModeMutableState.asStateFlow()

    private val readerFontSizeMutableState: MutableStateFlow<Int> = MutableStateFlow(
        settingsRepository.getReaderModeFontSize(),
    )
    val readerFontSizeState = readerFontSizeMutableState.asStateFlow()

    private var currentArticleId: String? = null

    private val canNavigateToPreviousMutableState = MutableStateFlow(false)
    val canNavigateToPreviousState = canNavigateToPreviousMutableState.asStateFlow()

    private val canNavigateToNextMutableState = MutableStateFlow(false)
    val canNavigateToNextState = canNavigateToNextMutableState.asStateFlow()

    fun setLoading() {
        readerModeMutableState.value = ReaderModeState.Loading
    }

    private fun setCurrentArticle(articleId: String) {
        currentArticleId = articleId

        val position = feedStateRepository.getArticlePosition(articleId)
        if (position == null) {
            canNavigateToPreviousMutableState.value = false
            canNavigateToNextMutableState.value = false
            return
        }

        canNavigateToPreviousMutableState.value = position.currentPosition > 1
        canNavigateToNextMutableState.value = position.currentPosition < position.totalArticles
    }

    fun getReaderModeHtml(urlInfo: FeedItemUrlInfo) {
        viewModelScope.launch {
            readerModeMutableState.value = ReaderModeState.Loading
            setCurrentArticle(urlInfo.id)

            // Use feed item ID directly as filename
            val feedItemId = urlInfo.id

            // Check if content is cached
            val cachedContent = feedItemContentFileHandler.loadFeedItemContent(feedItemId)
            if (cachedContent != null) {
                readerModeMutableState.value = ReaderModeState.Success(
                    ReaderModeData(
                        id = FeedItemId(urlInfo.id),
                        title = urlInfo.title,
                        content = cachedContent,
                        url = urlInfo.url,
                        fontSize = settingsRepository.getReaderModeFontSize(),
                        isBookmarked = urlInfo.isBookmarked,
                        commentsUrl = urlInfo.commentsUrl,
                    ),
                )
                return@launch
            }

            // Content not cached, trigger parsing
            val result = withTimeoutOrNull(20.seconds) {
                feedItemParserWorker.parse(feedItemId, urlInfo.url)
            }

            when (result) {
                null -> readerModeMutableState.value = ReaderModeState.HtmlNotAvailable(
                    url = urlInfo.url,
                    id = urlInfo.id,
                    isBookmarked = urlInfo.isBookmarked,
                )
                is ParsingResult.Success -> {
                    val htmlContent = result.htmlContent
                    if (htmlContent != null) {
                        readerModeMutableState.value = ReaderModeState.Success(
                            ReaderModeData(
                                id = FeedItemId(urlInfo.id),
                                title = result.title ?: urlInfo.title,
                                content = htmlContent,
                                url = urlInfo.url,
                                fontSize = settingsRepository.getReaderModeFontSize(),
                                isBookmarked = urlInfo.isBookmarked,
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

    fun navigateToNextArticle() {
        viewModelScope.launch {
            val articleId = currentArticleId ?: run {
                canNavigateToNextMutableState.update { false }
                return@launch
            }
            val nextArticle = feedStateRepository.getNextArticle(articleId)
            if (nextArticle != null) {
                val urlInfo = nextArticle.toFeedItemUrlInfo()
                feedActionsRepository.markAsRead(hashSetOf(FeedItemId(urlInfo.id)))
                getReaderModeHtml(urlInfo)
            }
        }
    }

    fun navigateToPreviousArticle() {
        viewModelScope.launch {
            val articleId = currentArticleId ?: run {
                canNavigateToPreviousMutableState.update { false }
                return@launch
            }
            val previousArticle = feedStateRepository.getPreviousArticle(articleId)
            if (previousArticle != null) {
                val urlInfo = previousArticle.toFeedItemUrlInfo()
                feedActionsRepository.markAsRead(hashSetOf(FeedItemId(urlInfo.id)))
                getReaderModeHtml(urlInfo)
            }
        }
    }

    private fun FeedItem.toFeedItemUrlInfo() = FeedItemUrlInfo(
        id = id,
        url = url,
        title = title,
        isBookmarked = isBookmarked,
        commentsUrl = commentsUrl,
        linkOpeningPreference = LinkOpeningPreference.READER_MODE,
    )
}
