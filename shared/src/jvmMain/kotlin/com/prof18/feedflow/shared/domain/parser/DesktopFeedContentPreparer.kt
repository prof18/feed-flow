package com.prof18.feedflow.shared.domain.parser

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.domain.feeditem.FeedContentPreparer
import com.prof18.klead.Klead
import com.prof18.klead.KleadOptions
import com.prof18.klead.KleadOutput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

internal class DesktopFeedContentPreparer(
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
) : FeedContentPreparer {

    override suspend fun prepare(html: String, baseUrl: String?): String = withContext(dispatcherProvider.io) {
        convertToMarkdown(html, baseUrl) ?: html
    }

    private suspend fun convertToMarkdown(html: String, baseUrl: String?): String? = try {
        Klead.parseHtml(
            html = "<html><body><article>$html</article></body></html>",
            url = baseUrl?.takeIf { it.isNotBlank() } ?: FALLBACK_BASE_URL,
            options = KleadOptions(outputs = setOf(KleadOutput.MARKDOWN)),
        ).content.requireMarkdown().trim().takeIf { it.isNotEmpty() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logger.d(e) { "Error converting feed content to markdown with Klead" }
        null
    }
}

private const val FALLBACK_BASE_URL = "https://localhost/"
