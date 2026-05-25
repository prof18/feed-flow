package com.prof18.feedflow.shared.ui.home.components.list

internal object FeedItemE2eIds {
    fun row(feedItemId: String): String =
        "article_row_${feedItemId.toE2eIdSuffix()}"
}

private fun String.toE2eIdSuffix(): String =
    map { char ->
        if (char.isLetterOrDigit() || char == '_') {
            char
        } else {
            '_'
        }
    }.joinToString(separator = "")
