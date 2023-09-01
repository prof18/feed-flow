package com.prof18.feedflow.core.model

sealed class FeedImportExportState {

    data object Idle : FeedImportExportState()

    data object LoadingImport : FeedImportExportState()
    data object LoadingExport : FeedImportExportState()

    data object Error : FeedImportExportState()

    data class ImportSuccess(
        val notValidFeedSources: List<ParsedFeedSource>,
    ) : FeedImportExportState()

    data object ExportSuccess : FeedImportExportState()
}
