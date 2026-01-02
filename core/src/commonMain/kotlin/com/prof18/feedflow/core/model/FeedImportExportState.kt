package com.prof18.feedflow.core.model

import kotlinx.collections.immutable.ImmutableList

sealed class FeedImportExportState {

    data object Idle : FeedImportExportState()

    data class LoadingImport(
        val contentType: ImportExportContentType,
    ) : FeedImportExportState()

    data class LoadingExport(
        val contentType: ImportExportContentType,
    ) : FeedImportExportState()

    data object Error : FeedImportExportState()

    data class ImportSuccess(
        val notValidFeedSources: ImmutableList<ParsedFeedSource>,
        val feedSourceWithError: ImmutableList<ParsedFeedSource>,
    ) : FeedImportExportState()

    data object ExportSuccess : FeedImportExportState()

    data object ArticleImportSuccess : FeedImportExportState()

    data object ArticleExportSuccess : FeedImportExportState()
}
