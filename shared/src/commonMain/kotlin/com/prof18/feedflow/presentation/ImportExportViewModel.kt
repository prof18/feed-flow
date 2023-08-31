package com.prof18.feedflow.presentation

import co.touchlab.kermit.Logger
import com.prof18.feedflow.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.domain.opml.OpmlInput
import com.prof18.feedflow.domain.opml.OpmlOutput
import com.prof18.feedflow.presentation.model.FeedImportExportState
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImportExportViewModel(
    private val feedManagerRepository: FeedManagerRepository,
    private val feedRetrieverRepository: FeedRetrieverRepository,
    private val logger: Logger,
) : BaseViewModel() {

    private val importerMutableState: MutableStateFlow<FeedImportExportState> = MutableStateFlow(
        FeedImportExportState.Idle,
    )

    @NativeCoroutinesState
    val importExportState = importerMutableState.asStateFlow()

    fun importFeed(opmlInput: OpmlInput) {
        scope.launch {
            importerMutableState.update { FeedImportExportState.Loading }
            try {
                val notValidFeedSources = feedManagerRepository.addFeedsFromFile(opmlInput)
                importerMutableState.update {
                    FeedImportExportState.ImportSuccess(
                        notValidFeedSources = notValidFeedSources.feedSources,
                    )
                }
                feedRetrieverRepository.fetchFeeds(updateLoadingInfo = false)
            } catch (e: Throwable) {
                logger.e(e) { "Error while importing feed" }
                importerMutableState.update { FeedImportExportState.Error }
            }
        }
    }

    fun exportFeed(opmlOutput: OpmlOutput) {
        scope.launch {
            importerMutableState.update { FeedImportExportState.Loading }
            try {
                feedManagerRepository.exportFeedsAsOpml(opmlOutput)
                importerMutableState.update { FeedImportExportState.ExportSuccess }
            } catch (e: Throwable) {
                logger.e(e) { "Error while exporting feed" }
                importerMutableState.update { FeedImportExportState.Error }
            }
        }
    }

    fun clearErrorState() {
        importerMutableState.update { FeedImportExportState.Idle }
    }
}
