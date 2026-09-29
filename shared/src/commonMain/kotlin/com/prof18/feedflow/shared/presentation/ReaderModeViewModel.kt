package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.ReaderFontSettings
import com.prof18.feedflow.core.model.ReaderModeData
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.core.model.ShownContentSource
import com.prof18.feedflow.core.model.canOpenWebReaderMode
import com.prof18.feedflow.core.model.hasNoUrl
import com.prof18.feedflow.core.model.isReaderMode
import com.prof18.feedflow.core.model.resolveWith
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.feedflow.shared.domain.feeditem.ReaderContentFetcher
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
    private val readerContentFetcher: ReaderContentFetcher,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val feedStateRepository: FeedStateRepository,
    private val databaseHelper: DatabaseHelper,
) : ViewModel() {

    private val readerModeMutableState: MutableStateFlow<ReaderModeState> = MutableStateFlow(
        ReaderModeState.Loading,
    )
    val readerModeState = readerModeMutableState.asStateFlow()

    private val readerFontSettingsMutableState: MutableStateFlow<ReaderFontSettings> = MutableStateFlow(
        ReaderFontSettings(
            fontSize = settingsRepository.getReaderModeFontSize(),
            lineHeight = settingsRepository.getReaderModeLineHeight(),
        ),
    )
    val readerFontSettingsState = readerFontSettingsMutableState.asStateFlow()

    private val canNavigateToPreviousMutableState = MutableStateFlow(false)
    val canNavigateToPreviousState = canNavigateToPreviousMutableState.asStateFlow()

    private val canNavigateToNextMutableState = MutableStateFlow(false)
    val canNavigateToNextState = canNavigateToNextMutableState.asStateFlow()

    private val currentArticleMutableState = MutableStateFlow<FeedItemUrlInfo?>(null)
    val currentArticleState = currentArticleMutableState.asStateFlow()

    private var loadReaderModeJob: Job? = null
    private var currentArticleId: String? = null
    private var currentShownSource: ShownContentSource? = null
    private var lastRequestedSource: ShownContentSource? = null

    fun setLoading() {
        loadReaderModeJob?.cancel()
        readerModeMutableState.value = ReaderModeState.Loading
    }

    fun getReaderModeHtml(urlInfo: FeedItemUrlInfo) {
        val effectiveSource = urlInfo.effectiveContentSource()
        val isSameArticle = selectArticle(urlInfo) && lastRequestedSource == effectiveSource
        if (!isSameArticle) {
            lastRequestedSource = effectiveSource
            loadArticleContent(urlInfo, effectiveSource)
        }
    }

    fun clearSelection() {
        currentArticleMutableState.value = null
    }

    fun resetState() {
        loadReaderModeJob?.cancel()
        currentArticleId = null
        currentShownSource = null
        lastRequestedSource = null
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

    private fun loadArticleContent(urlInfo: FeedItemUrlInfo, effectiveSource: ShownContentSource) {
        loadReaderModeJob?.cancel()
        loadReaderModeJob = viewModelScope.launch {
            readerModeMutableState.value = ReaderModeState.Loading

            val requestedArticleId = urlInfo.id
            val data = if (effectiveSource == ShownContentSource.FEED) {
                // Feed content preferred; fall back to web parsing when the feed ships no content.
                loadFeedContent(urlInfo) ?: loadWebContent(urlInfo, requestedArticleId)
            } else {
                // Web content preferred; use the feed copy when fetching, parsing, or loading times out.
                loadWebContent(urlInfo, requestedArticleId) ?: loadFeedContent(urlInfo)
            }

            if (currentArticleId != requestedArticleId) return@launch

            val state = if (data != null) {
                ReaderModeState.Success(data)
            } else {
                htmlNotAvailableFor(urlInfo)
            }
            emitIfStillCurrent(requestedArticleId, state)
        }
    }

    private suspend fun loadWebContent(urlInfo: FeedItemUrlInfo, requestedArticleId: String): ReaderModeData? {
        if (!urlInfo.canOpenWebReaderMode()) return null

        val content = feedItemContentFileHandler.loadFeedItemContent(requestedArticleId)
            ?.takeIf { it.isNotBlank() }
            ?: parseWebContent(urlInfo, requestedArticleId)
            ?: return null
        if (currentArticleId != requestedArticleId) return null

        return buildReaderModeData(
            urlInfo = urlInfo,
            content = content,
            shownContentSource = ShownContentSource.WEB,
            canToggleContentSource = hasAvailableFeedContent(urlInfo.id),
        )
    }

    private suspend fun parseWebContent(urlInfo: FeedItemUrlInfo, requestedArticleId: String): String? =
        withTimeoutOrNull(PARSE_TIMEOUT) {
            readerContentFetcher.fetch(
                feedItemId = requestedArticleId,
                url = urlInfo.url,
                save = settingsRepository.isSaveItemContentOnOpenEnabled(),
            )
        }

    private suspend fun loadFeedContent(urlInfo: FeedItemUrlInfo): ReaderModeData? {
        val feedContent = databaseHelper.getFeedItemContent(urlInfo.id)
        if (feedContent.isNullOrBlank()) return null
        val storedUrlInfo = databaseHelper.getFeedItemUrlInfo(urlInfo.id)
        // Not every entry point carries the feed source base url, so fall back to the stored one:
        // without it relative links and images in feed-provided content cannot resolve.
        val feedBaseUrl = urlInfo.url.ifBlank {
            (urlInfo.feedSourceBaseUrl ?: storedUrlInfo?.feedSourceBaseUrl).orEmpty()
        }
        val prepared = feedItemParserWorker.prepareFeedContent(html = feedContent, baseUrl = feedBaseUrl)
        if (prepared.isBlank()) return null
        return buildReaderModeData(
            urlInfo = urlInfo,
            content = prepared,
            shownContentSource = ShownContentSource.FEED,
            canToggleContentSource = urlInfo.canOpenWebReaderMode(),
            baseUrl = feedBaseUrl,
        )
    }

    private suspend fun hasAvailableFeedContent(articleId: String): Boolean {
        val feedContent = databaseHelper.getFeedItemContent(articleId)
        return !feedContent.isNullOrBlank()
    }

    private suspend fun buildReaderModeData(
        urlInfo: FeedItemUrlInfo,
        content: String,
        shownContentSource: ShownContentSource,
        canToggleContentSource: Boolean,
        baseUrl: String = urlInfo.getBaseUrl(),
    ): ReaderModeData = ReaderModeData(
        id = FeedItemId(urlInfo.id),
        title = urlInfo.title,
        content = content,
        url = urlInfo.url,
        baseUrl = baseUrl,
        fontSize = settingsRepository.getReaderModeFontSize(),
        lineHeight = settingsRepository.getReaderModeLineHeight(),
        isBookmarked = urlInfo.isBookmarked,
        commentsUrl = urlInfo.commentsUrl,
        imageUrl = readerImageUrl(urlInfo, shownContentSource),
        shownContentSource = shownContentSource,
        canToggleContentSource = canToggleContentSource,
        siteName = databaseHelper.getFeedItemUrlInfo(urlInfo.id)?.feedSourceTitle ?: urlInfo.feedSourceTitle,
    )

    private suspend fun readerImageUrl(urlInfo: FeedItemUrlInfo, source: ShownContentSource): String? {
        val imageUrl = urlInfo.imageUrl ?: return null
        if (source == ShownContentSource.FEED) return imageUrl
        val feedContent = databaseHelper.getFeedItemContent(urlInfo.id).orEmpty()
        // Inline feed images may be mastheads or tracking pixels rejected by extraction.
        // Let the parsed article own those images instead of adding them back as a hero.
        return imageUrl.takeUnless {
            feedContent.contains(imageUrl) || feedContent.contains(imageUrl.replace("&", "&amp;"))
        }
    }

    fun toggleContentSource() {
        val urlInfo = currentArticleMutableState.value ?: return
        val shownSource = currentShownSource ?: return
        val previousState = readerModeMutableState.value as? ReaderModeState.Success ?: return
        loadReaderModeJob?.cancel()
        readerModeMutableState.value = ReaderModeState.Loading
        loadReaderModeJob = viewModelScope.launch {
            val requestedArticleId = urlInfo.id
            val data = when (shownSource) {
                ShownContentSource.WEB -> loadFeedContent(urlInfo)
                ShownContentSource.FEED -> loadWebContent(urlInfo, requestedArticleId)
            }
            if (currentArticleId != requestedArticleId) return@launch

            val state = when {
                data != null -> ReaderModeState.Success(data)
                // Full article parsing failed: fall back to the website, like the initial load does.
                shownSource == ShownContentSource.FEED -> htmlNotAvailableFor(urlInfo)
                // The toggle is hidden when the feed ships no content, so keep what is on screen.
                else -> previousState
            }
            emitIfStillCurrent(requestedArticleId, state)
        }
    }

    fun updateFontSize(newFontSize: Int) {
        settingsRepository.setReaderModeFontSize(newFontSize)
        readerFontSettingsMutableState.update { it.copy(fontSize = newFontSize) }
    }

    fun updateLineHeight(newLineHeight: Int) {
        settingsRepository.setReaderModeLineHeight(newLineHeight)
        readerFontSettingsMutableState.update { it.copy(lineHeight = newLineHeight) }
    }

    fun updateBookmarkStatus(feedItemId: FeedItemId, bookmarked: Boolean) {
        viewModelScope.launch {
            feedActionsRepository.updateBookmarkStatus(feedItemId, bookmarked)
        }
    }

    private fun emitIfStillCurrent(articleId: String, state: ReaderModeState) {
        if (currentArticleId == articleId) {
            currentShownSource = (state as? ReaderModeState.Success)?.readerModeData?.shownContentSource
            readerModeMutableState.value = state
        }
    }

    private fun selectArticle(urlInfo: FeedItemUrlInfo): Boolean {
        val isSameArticle = currentArticleId == urlInfo.id && readerModeMutableState.value.isForArticle(urlInfo.id)
        currentArticleId = urlInfo.id
        currentArticleMutableState.value = urlInfo
        updateNavigationFlags()
        return isSameArticle
    }

    private fun showFallbackForArticle(urlInfo: FeedItemUrlInfo) {
        loadReaderModeJob?.cancel()
        selectArticle(urlInfo)
        currentShownSource = null
        readerModeMutableState.value = htmlNotAvailableFor(urlInfo)
    }

    private fun htmlNotAvailableFor(urlInfo: FeedItemUrlInfo) = ReaderModeState.HtmlNotAvailable(
        url = urlInfo.url,
        id = urlInfo.id,
        isBookmarked = urlInfo.isBookmarked,
    )

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
                if (nextArticle.hasNoUrl() || nextArticle.canOpenWebReaderMode()) {
                    getReaderModeHtml(nextArticle)
                } else {
                    showFallbackForArticle(nextArticle)
                }
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
            if (prevArticle.hasNoUrl() || prevArticle.canOpenWebReaderMode()) {
                getReaderModeHtml(prevArticle)
            } else {
                showFallbackForArticle(prevArticle)
            }
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
        articleOpenMode = feedSource.articleOpenMode,
        imageUrl = imageUrl,
        feedSourceTitle = feedSource.title,
        feedSourceBaseUrl = feedSource.websiteUrlFallback(),
    )

    /**
     * Which content the reader should show first. URL-less items can only come from the feed.
     * A feed set to open in a browser says nothing about reader content, so it falls back to the
     * global default — that case is reached by paging into the article from another one.
     */
    private fun FeedItemUrlInfo.effectiveContentSource(): ShownContentSource = when {
        hasNoUrl() -> ShownContentSource.FEED
        else -> {
            val globalDefault = settingsRepository.getArticleOpenMode()
            val resolved = articleOpenMode.resolveWith(globalDefault)
            val readerMode = resolved.takeIf { it.isReaderMode() } ?: globalDefault
            if (readerMode == ArticleOpenMode.FEED_CONTENT) ShownContentSource.FEED else ShownContentSource.WEB
        }
    }

    private companion object {
        private val PARSE_TIMEOUT = 20.seconds
    }
}
