package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

internal class HtmlRetriever(
    private val logger: Logger,
    private val client: HttpClient,
) {
    suspend fun retrieveHtml(url: String): String? {
        try {
            val response = client.get(url)
            return response.bodyAsText()
        } catch (e: Throwable) {
            logger.e(e) { "Unable to retrieve HTML, skipping" }
            return null
        }
    }
}
