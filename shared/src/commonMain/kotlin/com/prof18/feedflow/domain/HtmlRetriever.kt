package com.prof18.feedflow.domain

internal interface HtmlRetriever {
    suspend fun retrieveHtml(url: String): String?
}
