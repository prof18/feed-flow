package com.prof18.feedflow.core.model

import kotlinx.collections.immutable.ImmutableList

sealed class FeedImportExportState {

    data object Idle : FeedImportExportState()

    data object LoadingImport : FeedImportExportState()
    data object LoadingExport : FeedImportExportState()

    data object Error : FeedImportExportState()

    data class ImportSuccess(
        val notValidFeedSources: ImmutableList<ParsedFeedSource>,
    ) : FeedImportExportState()

    data object ExportSuccess : FeedImportExportState()
}
