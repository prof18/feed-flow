package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.ReadArticlesVisibility
import com.prof18.feedflow.core.model.ReadingBehaviorState
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReadingBehaviorSettingsViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val feedStateRepository: FeedStateRepository,
) : ViewModel() {

    private val stateMutableFlow = MutableStateFlow(ReadingBehaviorState())
    val state: StateFlow<ReadingBehaviorState> = stateMutableFlow.asStateFlow()

    init {
        loadSettings()
        viewModelScope.launch {
            combine(
                settingsRepository.showReadArticlesTimelineFlow,
                settingsRepository.hideReadItemsFlow,
                settingsRepository.markFeedAsReadWhenScrollingFlow,
            ) { _, _, _ ->
                settingsRepository.getReadArticlesVisibility()
            }.collect { visibility ->
                stateMutableFlow.update { it.copy(readArticlesVisibility = visibility) }
            }
        }
    }

    private fun loadSettings() {
        val isSaveReaderModeContentEnabled = settingsRepository.isSaveItemContentOnOpenEnabled()
        val isPrefetchArticleContentEnabled = settingsRepository.isPrefetchArticleContentEnabled()
        val isMarkReadWhenScrollingEnabled = settingsRepository.getMarkFeedAsReadWhenScrolling()
        val articleOpenMode = settingsRepository.getArticleOpenMode()

        stateMutableFlow.update {
            ReadingBehaviorState(
                isSaveReaderModeContentEnabled = isSaveReaderModeContentEnabled,
                isPrefetchArticleContentEnabled = isPrefetchArticleContentEnabled,
                isMarkReadWhenScrollingEnabled = isMarkReadWhenScrollingEnabled,
                readArticlesVisibility = settingsRepository.getReadArticlesVisibility(),
                articleOpenMode = articleOpenMode,
                articleBrowserId = settingsRepository.getArticleBrowserId(),
            )
        }
    }

    fun updateArticleOpenMode(value: ArticleOpenMode) {
        settingsRepository.setArticleOpenMode(value)
        stateMutableFlow.update {
            it.copy(articleOpenMode = value)
        }
    }

    fun updateArticleBrowser(browserId: String) {
        settingsRepository.selectArticleBrowser(browserId)
        refreshArticleOpenState()
    }

    // A links browser change can rewrite the stored mode, see SettingsRepository.saveFavouriteBrowserId.
    fun refreshArticleOpenState() {
        val articleOpenMode = settingsRepository.getArticleOpenMode()
        val articleBrowserId = settingsRepository.getArticleBrowserId()
        stateMutableFlow.update {
            it.copy(articleOpenMode = articleOpenMode, articleBrowserId = articleBrowserId)
        }
    }

    fun updateSaveReaderModeContent(value: Boolean) {
        settingsRepository.setSaveItemContentOnOpen(value)
        stateMutableFlow.update {
            it.copy(isSaveReaderModeContentEnabled = value)
        }
    }

    fun updatePrefetchArticleContent(value: Boolean) {
        settingsRepository.setPrefetchArticleContent(value)
        stateMutableFlow.update {
            it.copy(isPrefetchArticleContentEnabled = value)
        }
    }

    fun updateMarkReadWhenScrolling(value: Boolean) {
        settingsRepository.setMarkFeedAsReadWhenScrolling(value)
        stateMutableFlow.update {
            it.copy(isMarkReadWhenScrollingEnabled = value)
        }
        if (!value) {
            feedStateRepository.reapplyAutoHideToCurrentList()
        } else if (settingsRepository.getHideReadItems()) {
            viewModelScope.launch {
                feedStateRepository.getFeeds()
            }
        }
    }

    fun updateReadArticlesVisibility(value: ReadArticlesVisibility) {
        val blocked = value == ReadArticlesVisibility.HIDE_RIGHT_AWAY &&
            settingsRepository.getMarkFeedAsReadWhenScrolling()
        if (blocked || value == settingsRepository.getReadArticlesVisibility()) return
        settingsRepository.setReadArticlesVisibility(value)
        when (value) {
            ReadArticlesVisibility.KEEP_IN_LIST -> viewModelScope.launch { feedStateRepository.getFeeds() }
            ReadArticlesVisibility.HIDE_RIGHT_AWAY -> feedStateRepository.reapplyAutoHideToCurrentList()
            ReadArticlesVisibility.HIDE_ON_REFRESH -> Unit
        }
    }
}
