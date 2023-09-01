package com.prof18.feedflow.core.model

sealed interface FeedImportExportState {

    data object Idle : FeedImportExportState

    data object Loading : FeedImportExportState

    data object Error : FeedImportExportState

    data class ImportSuccess(
        val notValidFeedSources: List<ParsedFeedSource>,
    ) : FeedImportExportState

    data object ExportSuccess : FeedImportExportState
}
