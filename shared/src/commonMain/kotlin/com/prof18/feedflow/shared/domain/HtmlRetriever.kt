package com.prof18.feedflow.shared.domain

internal interface HtmlRetriever {
    suspend fun retrieveHtml(url: String): String?
}
