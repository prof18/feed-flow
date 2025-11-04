package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReaderModeViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val feedActionsRepository: FeedActionsRepository,
    private val feedStateRepository: FeedStateRepository,
) : ViewModel() {

    private val readerFontSizeMutableState: MutableStateFlow<Int> = MutableStateFlow(
        settingsRepository.getReaderModeFontSize(),
    )
    val readerFontSizeState = readerFontSizeMutableState.asStateFlow()

    private val currentArticleMutableState: MutableStateFlow<FeedItemUrlInfo?> = MutableStateFlow(null)
    val currentArticleState = currentArticleMutableState.asStateFlow()

    private var currentArticleId: String? = null

    fun setCurrentArticle(articleId: String) {
        currentArticleId = articleId
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

    suspend fun navigateToNextArticle(): FeedItemUrlInfo? {
        val articleId = currentArticleId ?: return null
        val nextArticle = feedStateRepository.getNextArticle(articleId)
        return if (nextArticle != null) {
            val urlInfo = nextArticle.toFeedItemUrlInfo()
            currentArticleId = urlInfo.id
            currentArticleMutableState.update { urlInfo }
            urlInfo
        } else {
            null
        }
    }

    suspend fun navigateToPreviousArticle(): FeedItemUrlInfo? {
        val articleId = currentArticleId ?: return null
        val previousArticle = feedStateRepository.getPreviousArticle(articleId)
        return if (previousArticle != null) {
            val urlInfo = previousArticle.toFeedItemUrlInfo()
            currentArticleId = urlInfo.id
            currentArticleMutableState.update { urlInfo }
            urlInfo
        } else {
            null
        }
    }

    fun canNavigateToNext(): Boolean {
        val articleId = currentArticleId ?: return false
        val feedState = feedStateRepository.feedState.value
        val currentIndex = feedState.indexOfFirst { it.id == articleId }
        return currentIndex != -1 && currentIndex < feedState.size - 1
    }

    fun canNavigateToPrevious(): Boolean {
        val articleId = currentArticleId ?: return false
        val feedState = feedStateRepository.feedState.value
        val currentIndex = feedState.indexOfFirst { it.id == articleId }
        return currentIndex > 0
    }

    private fun FeedItem.toFeedItemUrlInfo() = FeedItemUrlInfo(
        id = id,
        url = url,
        title = title,
        isBookmarked = isBookmarked,
        linkOpeningPreference = feedSource.linkOpeningPreference,
        commentsUrl = commentsUrl,
    )
}
