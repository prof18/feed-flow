package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.shared.domain.feed.FeedImportExportRepository
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImportExportViewModel internal constructor(
    private val logger: Logger,
    private val dateFormatter: DateFormatter,
    private val feedImportExportRepository: FeedImportExportRepository,
) : ViewModel() {

    private val importerMutableState: MutableStateFlow<FeedImportExportState> = MutableStateFlow(
        FeedImportExportState.Idle,
    )
    val importExportState = importerMutableState.asStateFlow()

    private val savedUrlsMutableState: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val savedUrls = savedUrlsMutableState.asStateFlow()

    init {
        loadSavedUrls()
    }

    private fun loadSavedUrls() {
        savedUrlsMutableState.update { feedImportExportRepository.getSavedOpmlUrls() }
    }

    fun importFeed(opmlInput: OpmlInput) {
        viewModelScope.launch {
            importerMutableState.update { FeedImportExportState.LoadingImport }
            try {
                val notValidFeedSources = feedImportExportRepository.addFeedsFromFile(opmlInput)
                importerMutableState.update {
                    FeedImportExportState.ImportSuccess(
                        notValidFeedSources = notValidFeedSources.feedSources.toImmutableList(),
                        feedSourceWithError = notValidFeedSources.feedSourcesWithError.toImmutableList(),
                    )
                }
            } catch (e: Throwable) {
                logger.e(e) { "Error while importing feed" }
                importerMutableState.update { FeedImportExportState.Error }
            }
        }
    }

    fun exportFeed(opmlOutput: OpmlOutput) {
        viewModelScope.launch {
            importerMutableState.update { FeedImportExportState.LoadingImport }
            try {
                feedImportExportRepository.exportFeedsAsOpml(opmlOutput)
                importerMutableState.update { FeedImportExportState.ExportSuccess }
            } catch (e: Throwable) {
                logger.e(e) { "Error while exporting feed" }
                importerMutableState.update { FeedImportExportState.Error }
            }
        }
    }

    fun clearState() {
        importerMutableState.update { FeedImportExportState.Idle }
    }

    fun startExport() {
        importerMutableState.update { FeedImportExportState.LoadingExport }
    }

    fun reportExportError() {
        importerMutableState.update { FeedImportExportState.Error }
    }

    fun getCurrentDateForExport(): String =
        dateFormatter.getCurrentDateForExport()

    fun importFeedFromUrl(url: String, saveUrl: Boolean = true) {
        viewModelScope.launch {
            importerMutableState.update { FeedImportExportState.LoadingImport }
            try {
                val notValidFeedSources = feedImportExportRepository.addFeedsFromUrl(url, saveUrl)
                if (saveUrl) {
                    loadSavedUrls()
                }
                importerMutableState.update {
                    FeedImportExportState.ImportSuccess(
                        notValidFeedSources = notValidFeedSources.feedSources.toImmutableList(),
                        feedSourceWithError = notValidFeedSources.feedSourcesWithError.toImmutableList(),
                    )
                }
            } catch (e: Throwable) {
                logger.e(e) { "Error while importing feed from URL: $url" }
                importerMutableState.update { FeedImportExportState.Error }
            }
        }
    }

    fun removeOpmlUrl(url: String) {
        feedImportExportRepository.removeOpmlUrl(url)
        loadSavedUrls()
    }
}
