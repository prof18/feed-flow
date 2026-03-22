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
import io.ktor.http.Url
import kotlinx.coroutines.Job
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

    private val canNavigateToPreviousMutableState = MutableStateFlow(false)
    val canNavigateToPreviousState = canNavigateToPreviousMutableState.asStateFlow()

    private val canNavigateToNextMutableState = MutableStateFlow(false)
    val canNavigateToNextState = canNavigateToNextMutableState.asStateFlow()

    private val currentArticleMutableState = MutableStateFlow<FeedItemUrlInfo?>(null)
    val currentArticleState = currentArticleMutableState.asStateFlow()

    private var loadReaderModeJob: Job? = null
    private var currentArticleId: String? = null

    fun setLoading() {
        loadReaderModeJob?.cancel()
        readerModeMutableState.value = ReaderModeState.Loading
    }

    fun getReaderModeHtml(urlInfo: FeedItemUrlInfo) {
        val isSameArticle = currentArticleId == urlInfo.id && readerModeMutableState.value.isForArticle(urlInfo.id)
        currentArticleId = urlInfo.id
        currentArticleMutableState.value = urlInfo
        updateNavigationFlags()
        if (!isSameArticle) {
            loadArticleContent(urlInfo)
        }
    }

    fun clearSelection() {
        currentArticleMutableState.value = null
    }

    fun resetState() {
        loadReaderModeJob?.cancel()
        currentArticleId = null
        clearSelection()
        canNavigateToPreviousMutableState.value = false
        canNavigateToNextMutableState.value = false
        readerModeMutableState.value = ReaderModeState.Loading
    }

    private fun updateNavigationFlags() {
        val position = currentArticleId?.let { feedStateRepository.getArticlePosition(it) }
        canNavigateToPreviousMutableState.value = position != null && position.currentPosition > 1
        canNavigateToNextMutableState.value = position != null && position.currentPosition < position.totalArticles
    }

    private fun loadArticleContent(urlInfo: FeedItemUrlInfo) {
        loadReaderModeJob?.cancel()
        loadReaderModeJob = viewModelScope.launch {
            readerModeMutableState.value = ReaderModeState.Loading

            val requestedArticleId = urlInfo.id
            val baseUrl = urlInfo.getBaseUrl()

            val cachedContent = feedItemContentFileHandler.loadFeedItemContent(requestedArticleId)
            if (cachedContent != null) {
                emitIfStillCurrent(
                    articleId = requestedArticleId,
                    state = ReaderModeState.Success(
                        ReaderModeData(
                            id = FeedItemId(urlInfo.id),
                            title = urlInfo.title,
                            content = cachedContent,
                            url = urlInfo.url,
                            baseUrl = baseUrl,
                            fontSize = settingsRepository.getReaderModeFontSize(),
                            isBookmarked = urlInfo.isBookmarked,
                            commentsUrl = urlInfo.commentsUrl,
                        ),
                    ),
                )
                return@launch
            }

            val result = withTimeoutOrNull(20.seconds) {
                feedItemParserWorker.parse(requestedArticleId, urlInfo.url, urlInfo.imageUrl)
            }

            if (currentArticleId != requestedArticleId) return@launch

            val successResult = result as? ParsingResult.Success
            val htmlContent = successResult?.htmlContent
            val state = if (htmlContent != null) {
                ReaderModeState.Success(
                    ReaderModeData(
                        id = FeedItemId(urlInfo.id),
                        title = successResult.title ?: urlInfo.title,
                        content = htmlContent,
                        url = urlInfo.url,
                        baseUrl = baseUrl,
                        fontSize = settingsRepository.getReaderModeFontSize(),
                        isBookmarked = urlInfo.isBookmarked,
                        commentsUrl = urlInfo.commentsUrl,
                    ),
                )
            } else {
                ReaderModeState.HtmlNotAvailable(
                    url = urlInfo.url,
                    id = urlInfo.id,
                    isBookmarked = urlInfo.isBookmarked,
                )
            }
            emitIfStillCurrent(requestedArticleId, state)
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

    private fun emitIfStillCurrent(articleId: String, state: ReaderModeState) {
        if (currentArticleId == articleId) {
            readerModeMutableState.value = state
        }
    }

    private fun FeedItemUrlInfo.getBaseUrl(): String = try {
        val url = Url(url)
        "${url.protocol.name}://${url.host}"
    } catch (_: Exception) {
        url
    }

    fun navigateToNextArticle() {
        viewModelScope.launch {
            val articleId = currentArticleId ?: run {
                canNavigateToNextMutableState.value = false
                return@launch
            }
            val nextArticle = feedStateRepository.getNextArticle(articleId)?.toFeedItemUrlInfo()
            if (nextArticle != null) {
                feedActionsRepository.markAsRead(hashSetOf(FeedItemId(nextArticle.id)))
                getReaderModeHtml(nextArticle)
            } else {
                canNavigateToNextMutableState.value = false
            }
        }
    }

    fun navigateToPreviousArticle() {
        val articleId = currentArticleId ?: run {
            canNavigateToPreviousMutableState.value = false
            return
        }
        val prevArticle = feedStateRepository.getPreviousArticle(articleId)?.toFeedItemUrlInfo()
        if (prevArticle != null) {
            viewModelScope.launch {
                feedActionsRepository.markAsRead(hashSetOf(FeedItemId(prevArticle.id)))
            }
            getReaderModeHtml(prevArticle)
        } else {
            canNavigateToPreviousMutableState.value = false
        }
    }

    private fun ReaderModeState.isForArticle(articleId: String): Boolean = when (this) {
        ReaderModeState.Loading -> false
        is ReaderModeState.Success -> readerModeData.id.id == articleId
        is ReaderModeState.HtmlNotAvailable -> id == articleId
    }

    private fun FeedItem.toFeedItemUrlInfo() = FeedItemUrlInfo(
        id = id,
        url = url,
        title = title,
        isBookmarked = isBookmarked,
        commentsUrl = commentsUrl,
        linkOpeningPreference = LinkOpeningPreference.READER_MODE,
        imageUrl = imageUrl,
    )
}
