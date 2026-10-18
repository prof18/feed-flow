package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.ArticleReportMessage
import com.prof18.feedflow.core.model.ArticleReportUiState
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.ReaderModeData
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.core.model.ShownContentSource
import com.prof18.feedflow.core.model.canOpenWebReaderMode
import com.prof18.feedflow.core.model.hasNoUrl
import com.prof18.feedflow.core.model.isReaderMode
import com.prof18.feedflow.core.model.resolveWith
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.ReaderContentLanguage
import com.prof18.feedflow.shared.domain.articlereport.ArticleReporter
import com.prof18.feedflow.shared.domain.feed.ArticleNavigationRepository
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feeditem.ArticleContentParser
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.ReaderContentFetcher
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryCoordinator
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.ReaderFetchResult
import com.prof18.feedflow.shared.domain.recovery.RecoveryTicket
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class ReaderModeViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val feedActionsRepository: FeedActionsRepository,
    private val articleContentParser: ArticleContentParser,
    private val readerContentFetcher: ReaderContentFetcher,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val articleNavigationRepository: ArticleNavigationRepository,
    private val databaseHelper: DatabaseHelper,
    private val articleReporter: ArticleReporter,
    private val recoveryRepository: FeedRecoveryRepository,
    coordinator: FeedRecoveryCoordinator,
) : ViewModel() {

    val verificationAvailable: Boolean = coordinator.available
    private val verificationMutableState = MutableStateFlow(ReaderVerificationState())
    val verificationState = verificationMutableState.asStateFlow()
    private var loadTicket: RecoveryTicket? = null
    private var verifyTicket: RecoveryTicket? = null

    private val readerModeMutableState: MutableStateFlow<ReaderModeState> = MutableStateFlow(
        ReaderModeState.Loading,
    )
    val readerModeState = readerModeMutableState.asStateFlow()

    val readerFontSettingsState = settingsRepository.readerFontSettingsState

    private val canNavigateToPreviousMutableState = MutableStateFlow(false)
    val canNavigateToPreviousState = canNavigateToPreviousMutableState.asStateFlow()

    private val canNavigateToNextMutableState = MutableStateFlow(false)
    val canNavigateToNextState = canNavigateToNextMutableState.asStateFlow()

    private val currentArticleMutableState = MutableStateFlow<FeedItemUrlInfo?>(null)
    val currentArticleState = currentArticleMutableState.asStateFlow()
    private val articleReportMutableState = MutableStateFlow(ArticleReportUiState())
    val articleReportState = combine(
        currentArticleState,
        readerModeState,
        articleReportMutableState,
    ) { article, state, report ->
        report.copy(
            canReport = article != null && article.url.isNotBlank() &&
                state.isForArticle(article.id) && !report.sending,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ArticleReportUiState())

    fun requestArticleReport() {
        if (!articleReportState.value.canReport || articleReportMutableState.value.showDisclosure) return
        articleReportMutableState.update { it.copy(showDisclosure = true) }
    }

    fun confirmArticleReport() {
        val article = currentArticleState.value ?: return
        val state = readerModeState.value
        if (
            !articleReportMutableState.value.showDisclosure || article.url.isBlank() ||
            !state.isForArticle(article.id) || articleReportMutableState.value.sending
        ) {
            return
        }
        val source = (state as? ReaderModeState.Success)?.readerModeData?.shownContentSource?.name?.lowercase()
            ?: "unavailable"
        articleReportMutableState.update {
            it.copy(showDisclosure = false, sending = true, message = ArticleReportMessage.SENDING)
        }
        articleReporter.submit(article.url, source) { sent ->
            if (currentArticleState.value?.id == article.id) {
                val result = if (sent) ArticleReportMessage.SENT else ArticleReportMessage.FAILED
                articleReportMutableState.update { report ->
                    report.copy(
                        sending = false,
                        message = result.takeIf { report.message != null },
                    )
                }
            }
        }
    }

    fun cancelArticleReport() {
        articleReportMutableState.update { it.copy(showDisclosure = false) }
    }

    fun dismissArticleReportMessage() {
        articleReportMutableState.update { it.copy(message = null) }
    }

    private var loadReaderModeJob: Job? = null
    private var currentArticleId: String? = null
    private var currentShownSource: ShownContentSource? = null
    private var lastRequestedSource: ShownContentSource? = null

    fun setLoading() {
        loadReaderModeJob?.cancel()
        loadTicket?.revoke()
        loadTicket = null
        readerModeMutableState.value = ReaderModeState.Loading
    }

    fun loadReaderContent(urlInfo: FeedItemUrlInfo) {
        val effectiveSource = urlInfo.effectiveContentSource()
        val isSameArticle = selectArticle(urlInfo) && lastRequestedSource == effectiveSource
        if (!isSameArticle) {
            lastRequestedSource = effectiveSource
            loadArticleContent(urlInfo, effectiveSource)
        }
    }

    suspend fun getAudioEpisodeReaderInfo(feedItemId: FeedItemId): FeedItemUrlInfo? =
        databaseHelper.getFeedItemUrlInfo(feedItemId.id)

    fun clearSelection() {
        articleReportMutableState.value = ArticleReportUiState()
        loadTicket?.revoke()
        loadTicket = null
        currentArticleMutableState.value = null
    }

    fun verifyWebsite() {
        val state = verificationMutableState.value
        val articleId = state.articleId ?: return
        val verificationUrl = state.verificationUrl ?: return
        val urlInfo = currentArticleMutableState.value?.takeIf { it.id == articleId } ?: return
        if ((state.blockedUntil ?: 0) > Clock.System.now().toEpochMilliseconds()) {
            verificationMutableState.value = state.copy(failed = true)
            return
        }
        verifyTicket?.revoke()
        val ticket = recoveryRepository.issueTicket()
        verifyTicket = ticket
        verificationMutableState.value = state.copy(failed = false)
        viewModelScope.launch {
            val result = try {
                readerContentFetcher.verifyForReader(
                    articleId = articleId,
                    verificationUrl = verificationUrl,
                    save = settingsRepository.isSaveItemContentOnOpenEnabled(),
                    ticket = ticket,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ReaderFetchResult.Unavailable
            }
            if (currentArticleId != articleId) return@launch
            if (result is ReaderFetchResult.Content) {
                val data = buildReaderModeData(
                    urlInfo = urlInfo,
                    content = result.content,
                    shownContentSource = ShownContentSource.WEB,
                    canToggleContentSource = hasAvailableFeedContent(articleId),
                )
                verificationMutableState.value = ReaderVerificationState(
                    articleId = articleId,
                    saveFailed = result.saveFailed,
                )
                emitIfStillCurrent(articleId, ReaderModeState.Success(data))
            } else {
                verificationMutableState.update { it.copy(failed = true) }
            }
        }
    }

    fun cancelVerification() {
        verifyTicket?.revoke()
        verifyTicket = null
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
        val position = currentArticleId?.let { articleNavigationRepository.getArticlePosition(it) }
        canNavigateToPreviousMutableState.value = position != null && position.currentPosition > 1
        canNavigateToNextMutableState.value = position != null && position.currentPosition < position.totalArticles
    }

    private fun loadArticleContent(urlInfo: FeedItemUrlInfo, effectiveSource: ShownContentSource) {
        loadReaderModeJob?.cancel()
        val ticket = replaceLoadTicket()
        loadReaderModeJob = viewModelScope.launch {
            readerModeMutableState.value = ReaderModeState.Loading

            val requestedArticleId = urlInfo.id
            val data = if (effectiveSource == ShownContentSource.FEED) {
                // Feed content preferred; fall back to web parsing when the feed ships no content.
                loadFeedContent(urlInfo) ?: loadWebContent(urlInfo, requestedArticleId, ticket)
            } else {
                // Web content preferred; use the feed copy when fetching, parsing, or loading times out.
                loadWebContent(urlInfo, requestedArticleId, ticket) ?: loadFeedContent(urlInfo)
            }

            if (currentArticleId != requestedArticleId) return@launch

            val state = if (data != null) {
                ReaderModeState.Success(data)
            } else {
                contentNotAvailableFor(urlInfo)
            }
            emitIfStillCurrent(requestedArticleId, state)
        }
    }

    private suspend fun loadWebContent(
        urlInfo: FeedItemUrlInfo,
        requestedArticleId: String,
        ticket: RecoveryTicket,
    ): ReaderModeData? {
        if (!urlInfo.canOpenWebReaderMode()) return null

        val content = feedItemContentFileHandler.loadFeedItemContent(requestedArticleId)
            ?.takeIf { it.isNotBlank() }
            ?: parseWebContent(urlInfo, requestedArticleId, ticket)
            ?: return null
        if (currentArticleId != requestedArticleId) return null

        return buildReaderModeData(
            urlInfo = urlInfo,
            content = content,
            shownContentSource = ShownContentSource.WEB,
            canToggleContentSource = hasAvailableFeedContent(urlInfo.id),
        )
    }

    private suspend fun parseWebContent(
        urlInfo: FeedItemUrlInfo,
        requestedArticleId: String,
        ticket: RecoveryTicket,
    ): String? {
        val result = withTimeoutOrNull(PARSE_TIMEOUT) {
            readerContentFetcher.fetchForReader(
                articleId = requestedArticleId,
                url = urlInfo.url,
                save = settingsRepository.isSaveItemContentOnOpenEnabled(),
                ticket = ticket,
            )
        }
        return when (result) {
            is ReaderFetchResult.Content -> {
                val hadState = verificationMutableState.value.articleId == requestedArticleId
                if (currentArticleId == requestedArticleId && (result.saveFailed || hadState)) {
                    // A successful load supersedes an earlier challenge for this article
                    verificationMutableState.value = ReaderVerificationState(
                        articleId = requestedArticleId,
                        saveFailed = result.saveFailed,
                    )
                }
                result.content
            }
            is ReaderFetchResult.NeedsVerification -> {
                if (currentArticleId == requestedArticleId) {
                    verificationMutableState.value = ReaderVerificationState(
                        articleId = requestedArticleId,
                        verificationUrl = result.verificationUrl,
                        blockedUntil = result.blockedUntil,
                    )
                }
                null
            }
            ReaderFetchResult.Unavailable, null -> null
        }
    }

    private fun replaceLoadTicket(): RecoveryTicket {
        loadTicket?.revoke()
        return recoveryRepository.issueTicket().also { loadTicket = it }
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
        val prepared = articleContentParser.prepareFeedContent(html = feedContent, baseUrl = feedBaseUrl)
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
        audioUrl = databaseHelper.getFeedItemAudioUrl(urlInfo.id),
        audioImageUrl = databaseHelper.getFeedItemAudioArtwork(urlInfo.id),
        languageTag = ReaderContentLanguage.fromCachedContent(content),
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
        val ticket = replaceLoadTicket()
        readerModeMutableState.value = ReaderModeState.Loading
        loadReaderModeJob = viewModelScope.launch {
            val requestedArticleId = urlInfo.id
            val data = when (shownSource) {
                ShownContentSource.WEB -> loadFeedContent(urlInfo)
                ShownContentSource.FEED -> loadWebContent(urlInfo, requestedArticleId, ticket)
            }
            if (currentArticleId != requestedArticleId) return@launch

            val state = when {
                data != null -> ReaderModeState.Success(data)
                // Full article parsing failed: fall back to the website, like the initial load does.
                shownSource == ShownContentSource.FEED -> contentNotAvailableFor(urlInfo)
                // The toggle is hidden when the feed ships no content, so keep what is on screen.
                else -> previousState
            }
            emitIfStillCurrent(requestedArticleId, state)
        }
    }

    fun updateFontSize(newFontSize: Int) {
        settingsRepository.setReaderModeFontSize(newFontSize)
    }

    fun updateLineHeight(newLineHeight: Int) {
        settingsRepository.setReaderModeLineHeight(newLineHeight)
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
        if (currentArticleId != urlInfo.id) articleReportMutableState.value = ArticleReportUiState()
        if (verificationMutableState.value.articleId != urlInfo.id) {
            // A Retry-After or a pending verification belongs to one article only
            cancelVerification()
            verificationMutableState.value = ReaderVerificationState()
        }
        currentArticleId = urlInfo.id
        currentArticleMutableState.value = urlInfo
        updateNavigationFlags()
        return isSameArticle
    }

    private fun showFallbackForArticle(urlInfo: FeedItemUrlInfo) {
        loadReaderModeJob?.cancel()
        selectArticle(urlInfo)
        currentShownSource = null
        readerModeMutableState.value = contentNotAvailableFor(urlInfo)
    }

    private fun contentNotAvailableFor(urlInfo: FeedItemUrlInfo) = ReaderModeState.ContentNotAvailable(
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
            val nextArticle = articleNavigationRepository.getNextArticle(articleId)?.toFeedItemUrlInfo()
            if (nextArticle != null) {
                feedActionsRepository.markAsRead(hashSetOf(FeedItemId(nextArticle.id)))
                if (nextArticle.hasNoUrl() || nextArticle.canOpenWebReaderMode()) {
                    loadReaderContent(nextArticle)
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
        val prevArticle = articleNavigationRepository.getPreviousArticle(articleId)?.toFeedItemUrlInfo()
        if (prevArticle != null) {
            viewModelScope.launch {
                feedActionsRepository.markAsRead(hashSetOf(FeedItemId(prevArticle.id)))
            }
            if (prevArticle.hasNoUrl() || prevArticle.canOpenWebReaderMode()) {
                loadReaderContent(prevArticle)
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
        is ReaderModeState.ContentNotAvailable -> id == articleId
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

    override fun onCleared() {
        loadTicket?.revoke()
        verifyTicket?.revoke()
        super.onCleared()
    }

    private companion object {
        private val PARSE_TIMEOUT = 20.seconds
    }
}

data class ReaderVerificationState(
    val articleId: String? = null,
    val verificationUrl: String? = null,
    val blockedUntil: Long? = null,
    val failed: Boolean = false,
    val saveFailed: Boolean = false,
)
