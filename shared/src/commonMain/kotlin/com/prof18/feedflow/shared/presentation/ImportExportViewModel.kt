package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.ArticleExportFilter
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.core.model.ImportExportContentType
import com.prof18.feedflow.shared.domain.csv.CsvInput
import com.prof18.feedflow.shared.domain.csv.CsvOutput
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

    fun importFeed(opmlInput: OpmlInput) {
        viewModelScope.launch {
            importerMutableState.update {
                FeedImportExportState.LoadingImport(ImportExportContentType.FeedsOpml)
            }
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
            importerMutableState.update {
                FeedImportExportState.LoadingExport(ImportExportContentType.FeedsOpml)
            }
            try {
                feedImportExportRepository.exportFeedsAsOpml(opmlOutput)
                importerMutableState.update { FeedImportExportState.ExportSuccess }
            } catch (e: Throwable) {
                logger.e(e) { "Error while exporting feed" }
                importerMutableState.update { FeedImportExportState.Error }
            }
        }
    }

    fun importArticles(csvInput: CsvInput) {
        viewModelScope.launch {
            importerMutableState.update {
                FeedImportExportState.LoadingImport(ImportExportContentType.ArticlesCsv)
            }
            try {
                feedImportExportRepository.importArticlesFromCsv(csvInput)
                importerMutableState.update {
                    FeedImportExportState.ArticleImportSuccess
                }
            } catch (e: Throwable) {
                logger.e(e) { "Error while importing articles" }
                importerMutableState.update { FeedImportExportState.Error }
            }
        }
    }

    fun exportArticles(
        csvOutput: CsvOutput,
        filter: ArticleExportFilter,
    ) {
        viewModelScope.launch {
            importerMutableState.update {
                FeedImportExportState.LoadingExport(ImportExportContentType.ArticlesCsv)
            }
            try {
                feedImportExportRepository.exportArticlesAsCsv(
                    csvOutput = csvOutput,
                    filter = filter,
                )
                importerMutableState.update { FeedImportExportState.ArticleExportSuccess }
            } catch (e: Throwable) {
                logger.e(e) { "Error while exporting articles" }
                importerMutableState.update { FeedImportExportState.Error }
            }
        }
    }

    fun clearState() {
        importerMutableState.update { FeedImportExportState.Idle }
    }

    fun startExport(contentType: ImportExportContentType) {
        importerMutableState.update { FeedImportExportState.LoadingExport(contentType) }
    }

    fun reportExportError() {
        importerMutableState.update { FeedImportExportState.Error }
    }

    fun getCurrentDateForExport(): String =
        dateFormatter.getCurrentDateForExport()
}
