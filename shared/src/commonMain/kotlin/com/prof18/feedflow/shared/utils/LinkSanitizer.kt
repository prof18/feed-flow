package com.prof18.feedflow.shared.utils

internal fun sanitizeUrl(feedUrl: String): String =
    when {
        feedUrl.startsWith("http:") -> {
            feedUrl.replace("http:", "https:")
        }

        !feedUrl.startsWith("https://") -> {
            "https://$feedUrl"
        }

        else -> feedUrl
    }
