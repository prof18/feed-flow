package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.utils.DispatcherProvider
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import kotlinx.coroutines.withContext

internal class MarkdownToHtmlConverter(
    private val converter: FlexmarkHtmlConverter,
    private val dispatcherProvider: DispatcherProvider,
) {

    suspend fun convertToMarkdown(html: String): String = withContext(dispatcherProvider.io) {
        return@withContext converter.convert(html)
    }
}
