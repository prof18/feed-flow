package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.shared.domain.DateFormatter
import com.prof18.feedflow.shared.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.shared.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImportExportViewModel internal constructor(
    private val feedManagerRepository: FeedManagerRepository,
    private val feedRetrieverRepository: FeedRetrieverRepository,
    private val logger: Logger,
    private val dateFormatter: DateFormatter,
) : ViewModel() {

    private val importerMutableState: MutableStateFlow<FeedImportExportState> = MutableStateFlow(
        FeedImportExportState.Idle,
    )

    @NativeCoroutinesState
    val importExportState = importerMutableState.asStateFlow()

    fun importFeed(opmlInput: OpmlInput) {
        viewModelScope.launch {
            importerMutableState.update { FeedImportExportState.LoadingImport }
            try {
                val notValidFeedSources = feedManagerRepository.addFeedsFromFile(opmlInput)
                importerMutableState.update {
                    FeedImportExportState.ImportSuccess(
                        notValidFeedSources = notValidFeedSources.feedSources.toImmutableList(),
                    )
                }
                feedRetrieverRepository.fetchFeeds()
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
                feedManagerRepository.exportFeedsAsOpml(opmlOutput)
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
}
