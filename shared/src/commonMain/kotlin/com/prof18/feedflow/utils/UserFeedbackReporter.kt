package com.prof18.feedflow.utils

object UserFeedbackReporter {
    private const val baseUrl = "https://github.com/prof18/feed-flow/issues/new"
    private const val title = "App issue"
    private const val body = "Please describe the issue and provide a link to the RSS Feed that causes the issues:\n"

    fun getFeedbackUrl(): String {
        val encodedTitle = title.replace(" ", "+")
        val encodedContent = body
            .replace(" ", "+")
            .replace("\n", "%0A")
        return "$baseUrl?title=$encodedTitle&body=$encodedContent"
    }
}
